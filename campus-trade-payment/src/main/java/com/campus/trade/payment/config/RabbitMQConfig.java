package com.campus.trade.payment.config;

import com.campus.trade.common.constant.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付服务的 MQ 声明：只需声明交换机和 payment.done 队列。
 * 实际发送由 PaymentEventProducer 完成。
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange campusTradeExchange() {
        return ExchangeBuilder.topicExchange(MQConstant.EXCHANGE)
                .durable(true).build();
    }

    @Bean
    public Queue paymentDoneQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_PAYMENT_DONE).build();
    }

    @Bean
    public Binding paymentDoneBinding(Queue paymentDoneQueue,
                                      TopicExchange campusTradeExchange) {
        return BindingBuilder.bind(paymentDoneQueue)
                .to(campusTradeExchange)
                .with(MQConstant.ROUTING_PAYMENT_DONE);
    }
}
