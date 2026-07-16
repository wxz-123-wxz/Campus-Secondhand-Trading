package com.campus.trade.order.mq;

import com.campus.trade.common.constant.MQConstant;
import com.campus.trade.common.enums.OrderStatus;
import com.campus.trade.order.mq.OrderMessageProducer;
import com.campus.trade.order.repository.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 监听支付完成事件：将订单置为 PAID，并通过 MQ 通知商品服务标记为已售出。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final TradeOrderRepository orderRepository;
    private final OrderMessageProducer  messageProducer;

    @RabbitListener(queues = MQConstant.QUEUE_PAYMENT_DONE)
    @Transactional
    public void onPaymentDone(String orderNo) {
        log.info("收到 payment.done 消息，orderNo={}", orderNo);
        orderRepository.findByOrderNo(orderNo).ifPresent(o -> {
            if (o.getStatus() == OrderStatus.CREATED) {
                o.setStatus(OrderStatus.PAID);
                o.setPaidAt(LocalDateTime.now());
                orderRepository.save(o);
                // 通知 product-service 将商品置为 SOLD
                messageProducer.sendOrderPaid(o.getProductId());
            }
        });
    }
}
