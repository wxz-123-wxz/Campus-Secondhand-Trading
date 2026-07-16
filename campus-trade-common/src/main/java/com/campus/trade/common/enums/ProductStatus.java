package com.campus.trade.common.enums;

/**
 * 商品状态枚举（与各服务共享）
 */
public enum ProductStatus {
    ON_SALE("在售"),
    LOCKED("交易中"),
    SOLD("已售出"),
    OFFLINE("已下架");

    private final String desc;

    ProductStatus(String desc) { this.desc = desc; }

    public String getDesc() { return desc; }
}
