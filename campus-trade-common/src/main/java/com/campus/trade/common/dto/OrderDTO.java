package com.campus.trade.common.dto;

import com.campus.trade.common.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息 DTO（跨服务传输）
 */
@Data
public class OrderDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String orderNo;
    private BigDecimal price;
    private String remark;
    private String alipayTradeNo;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
