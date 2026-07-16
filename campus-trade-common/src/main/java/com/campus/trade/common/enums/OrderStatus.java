package com.campus.trade.common.enums;

/**
 * 订单状态枚举（与各服务共享）
 */
public enum OrderStatus {
    CREATED("待支付"),
    PAID("已支付"),
    COMPLETED("已完成"),
    CANCELED("已取消");

    private final String desc;

    OrderStatus(String desc) { this.desc = desc; }

    public String getDesc() { return desc; }
}
