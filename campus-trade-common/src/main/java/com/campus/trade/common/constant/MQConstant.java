package com.campus.trade.common.constant;

/**
 * RabbitMQ 交换机 / 队列 / 路由键常量。
 *
 * 使用 topic 模式：
 *   campus.trade.exchange  →  order.created  → queue.order.created  (product-service: 锁定商品)
 *   campus.trade.exchange  →  order.paid     → queue.order.paid     (product-service: 标记商品已售)
 *   campus.trade.exchange  →  payment.done   → queue.payment.done   (order-service: 更新订单状态)
 */
public final class MQConstant {

    private MQConstant() {}

    // 交换机
    public static final String EXCHANGE = "campus.trade.exchange";

    // 路由键
    public static final String ROUTING_ORDER_CREATED  = "order.created";
    public static final String ROUTING_ORDER_PAID     = "order.paid";
    public static final String ROUTING_ORDER_CANCELED = "order.canceled";
    public static final String ROUTING_PAYMENT_DONE   = "payment.done";

    // 队列
    public static final String QUEUE_ORDER_CREATED  = "queue.order.created";
    public static final String QUEUE_ORDER_PAID     = "queue.order.paid";
    public static final String QUEUE_ORDER_CANCELED = "queue.order.canceled";
    public static final String QUEUE_PAYMENT_DONE   = "queue.payment.done";
}
