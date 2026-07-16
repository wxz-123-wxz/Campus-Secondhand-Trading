package com.campus.trade.order.mq;

import com.campus.trade.common.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单事件生产者。
 *
 * order.created  → product-service 锁定商品
 * order.paid     → product-service 将商品置为已售出
 * order.canceled → product-service 解锁商品，恢复在售
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendOrderCreated(Long productId) {
        log.info("发送 order.created, productId={}", productId);
        rabbitTemplate.convertAndSend(
                MQConstant.EXCHANGE,
                MQConstant.ROUTING_ORDER_CREATED,
                productId);
    }

    public void sendOrderPaid(Long productId) {
        log.info("发送 order.paid, productId={}", productId);
        rabbitTemplate.convertAndSend(
                MQConstant.EXCHANGE,
                MQConstant.ROUTING_ORDER_PAID,
                productId);
    }

    public void sendOrderCanceled(Long productId) {
        log.info("发送 order.canceled, productId={}", productId);
        rabbitTemplate.convertAndSend(
                MQConstant.EXCHANGE,
                MQConstant.ROUTING_ORDER_CANCELED,
                productId);
    }
}
