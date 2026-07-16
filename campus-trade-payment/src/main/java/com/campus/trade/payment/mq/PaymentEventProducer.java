package com.campus.trade.payment.mq;

import com.campus.trade.common.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 支付完成后，发布 payment.done 事件。
 * order-service 监听后将订单置为 PAID；
 * product-service 监听后将商品置为 SOLD（由 order.paid 路由键触发）。
 *
 * 这里只发一条消息给 order-service，
 * 订单服务变更状态后再由 order-service 发 order.paid 通知 product-service。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendPaymentDone(String orderNo) {
        log.info("发送 payment.done，orderNo={}", orderNo);
        rabbitTemplate.convertAndSend(
                MQConstant.EXCHANGE,
                MQConstant.ROUTING_PAYMENT_DONE,
                orderNo);
    }
}
