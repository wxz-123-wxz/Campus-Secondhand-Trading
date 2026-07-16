package com.campus.trade.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductMessageDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private Long userId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;
}
