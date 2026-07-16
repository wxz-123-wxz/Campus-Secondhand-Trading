package com.campus.trade.product.repository;

import com.campus.trade.product.entity.ProductMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<ProductMessage, Long> {
    List<ProductMessage> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<ProductMessage> findAllByOrderByCreatedAtDesc();
}
