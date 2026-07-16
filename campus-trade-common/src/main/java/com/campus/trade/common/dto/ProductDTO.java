package com.campus.trade.common.dto;

import com.campus.trade.common.enums.ProductStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品信息 DTO（跨服务传输）
 */
@Data
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private String tradePlace;
    private ProductStatus status;
    private Long sellerId;
    private String sellerName;
    private List<ProductMessageDTO> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
