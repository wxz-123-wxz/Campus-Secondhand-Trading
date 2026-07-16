package com.campus.trade.product.config;

import com.campus.trade.common.constant.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 声明 Topic 交换机和队列。
 * product-service 监听：
 *   order.created  → 锁定商品（LOCKED）
 *   order.paid     → 标记商品已售出（SOLD）
 *   order.canceled → 恢复商品为在售（ON_SALE）
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange campusTradeExchange() {
        return ExchangeBuilder.topicExchange(MQConstant.EXCHANGE)
                .durable(true).build();
    }

    // ── 订单创建队列 ─────────────────────────────────────────
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_ORDER_CREATED).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue,
                                       TopicExchange campusTradeExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(campusTradeExchange)
                .with(MQConstant.ROUTING_ORDER_CREATED);
    }

    // ── 订单支付完成队列 ─────────────────────────────────────
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_ORDER_PAID).build();
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue,
                                    TopicExchange campusTradeExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(campusTradeExchange)
                .with(MQConstant.ROUTING_ORDER_PAID);
    }

    // ── 订单取消队列（解锁商品） ─────────────────────────────
    @Bean
    public Queue orderCanceledQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_ORDER_CANCELED).build();
    }

    @Bean
    public Binding orderCanceledBinding(Queue orderCanceledQueue,
                                        TopicExchange campusTradeExchange) {
        return BindingBuilder.bind(orderCanceledQueue)
                .to(campusTradeExchange)
                .with(MQConstant.ROUTING_ORDER_CANCELED);
    }
}
