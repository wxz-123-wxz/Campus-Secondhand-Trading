package com.campus.trade.order.repository;

import com.campus.trade.common.enums.OrderStatus;
import com.campus.trade.order.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderNo(String orderNo);

    List<TradeOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<TradeOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<TradeOrder> findAllByOrderByCreatedAtDesc();

    /** 检查买家对该商品是否已存在待支付订单（防重复下单） */
    Optional<TradeOrder> findByBuyerIdAndProductIdAndStatus(
            Long buyerId, Long productId, OrderStatus status);
}
