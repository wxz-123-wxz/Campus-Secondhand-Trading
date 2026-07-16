package com.campus.trade.payment.service;

import com.alipay.api.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.campus.trade.common.dto.OrderDTO;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.response.Result;
import com.campus.trade.payment.config.AlipayProperties;
import com.campus.trade.payment.entity.PaymentRecord;
import com.campus.trade.payment.feign.OrderFeignClient;
import com.campus.trade.payment.mq.PaymentEventProducer;
import com.campus.trade.payment.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlipayService {

    private final AlipayProperties alipayProps;
    private final OrderFeignClient orderFeignClient;
    private final PaymentRecordRepository recordRepository;
    private final PaymentEventProducer messageProducer;

    private AlipayClient getClient() {
        return new DefaultAlipayClient(
                alipayProps.getGatewayUrl(),
                alipayProps.getAppId(),
                alipayProps.getPrivateKey(),
                "json", "UTF-8",
                alipayProps.getAlipayPublicKey(),
                "RSA2");
    }

    public String createPayForm(String orderNo) {
        Result<OrderDTO> orderResult = orderFeignClient.getByOrderNo(orderNo);
        if (orderResult == null || !orderResult.isOk()) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderDTO order = orderResult.getData();

        recordRepository.findByOrderNo(orderNo).orElseGet(() -> {
            PaymentRecord rec = new PaymentRecord();
            rec.setOrderNo(orderNo);
            rec.setAmount(order.getPrice());
            rec.setStatus("WAIT_PAY");
            return recordRepository.save(rec);
        });

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(alipayProps.getReturnUrl());
        request.setNotifyUrl(alipayProps.getNotifyUrl());
        request.setBizContent(String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"校园二手交易\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                orderNo, order.getPrice()));

        try {
            return getClient().pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付失败", e);
            throw new BusinessException("创建支付失败：" + e.getMessage());
        }
    }

    @Transactional
    public String syncOrderStatus(String orderNo, Long userId) {
        Result<OrderDTO> orderResult = orderFeignClient.getByOrderNo(orderNo);
        if (orderResult == null || !orderResult.isOk()) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderDTO order = orderResult.getData();
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "无权同步此订单");
        }

        if (order.getStatus() != null && "PAID".equals(order.getStatus().name())) {
            return "PAID";
        }

        try {
            AlipayTradeQueryResponse response = queryTrade(orderNo);
            String tradeStatus = response.getTradeStatus();
            if (isPaid(tradeStatus)) {
                markPaymentSuccess(orderNo, response.getTradeNo());
                return "PAID";
            }
            return tradeStatus == null || tradeStatus.isBlank() ? "WAIT_PAY" : tradeStatus;
        } catch (AlipayApiException e) {
            log.error("主动同步支付宝交易状态失败，orderNo={}", orderNo, e);
            throw new BusinessException("主动同步支付状态失败：" + e.getMessage());
        }
    }

    @Transactional
    public void handleReturn(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new BusinessException("同步回调缺少订单号");
        }

        try {
            AlipayTradeQueryResponse response = queryTrade(outTradeNo);
            if (isPaid(response.getTradeStatus())) {
                markPaymentSuccess(outTradeNo, response.getTradeNo());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝同步回跳查询失败，orderNo={}", outTradeNo, e);
            throw new BusinessException("支付宝同步回跳查询失败：" + e.getMessage());
        }
    }

    @Transactional
    public void handleNotify(Map<String, String> params) {
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProps.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2");
            if (!verified) {
                throw new BusinessException("签名验证失败");
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("签名验证异常：" + e.getMessage());
        }

        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");

        if (isPaid(tradeStatus)) {
            markPaymentSuccess(outTradeNo, tradeNo);
        }
    }

    private AlipayTradeQueryResponse queryTrade(String orderNo) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", orderNo));
        AlipayTradeQueryResponse response = getClient().execute(request);
        if (response == null || !response.isSuccess()) {
            throw new BusinessException("支付宝交易查询失败");
        }
        return response;
    }

    private boolean isPaid(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private void markPaymentSuccess(String orderNo, String tradeNo) {
        PaymentRecord rec = recordRepository.findByOrderNo(orderNo)
                .orElseGet(() -> createRecordFromOrder(orderNo));

        if (!"SUCCESS".equals(rec.getStatus())) {
            rec.setStatus("SUCCESS");
            if (tradeNo != null && !tradeNo.isBlank()) {
                rec.setAlipayTradeNo(tradeNo);
            }
            rec.setPaidAt(LocalDateTime.now());
            recordRepository.save(rec);
        } else if ((rec.getAlipayTradeNo() == null || rec.getAlipayTradeNo().isBlank())
                && tradeNo != null && !tradeNo.isBlank()) {
            rec.setAlipayTradeNo(tradeNo);
            recordRepository.save(rec);
        }
        messageProducer.sendPaymentDone(orderNo);
    }

    private PaymentRecord createRecordFromOrder(String orderNo) {
        Result<OrderDTO> orderResult = orderFeignClient.getByOrderNo(orderNo);
        if (orderResult == null || !orderResult.isOk()) {
            throw new BusinessException("订单不存在");
        }
        PaymentRecord rec = new PaymentRecord();
        rec.setOrderNo(orderNo);
        rec.setAmount(orderResult.getData().getPrice());
        rec.setStatus("WAIT_PAY");
        return recordRepository.save(rec);
    }
}
