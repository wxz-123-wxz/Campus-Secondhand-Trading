package com.campus.trade.order.service;

import com.campus.trade.common.dto.OrderDTO;
import com.campus.trade.common.dto.ProductDTO;
import com.campus.trade.common.dto.UserDTO;
import com.campus.trade.common.enums.OrderStatus;
import com.campus.trade.common.enums.ProductStatus;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.response.Result;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.feign.ProductFeignClient;
import com.campus.trade.order.feign.UserFeignClient;
import com.campus.trade.order.mq.OrderMessageProducer;
import com.campus.trade.order.repository.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final TradeOrderRepository    orderRepository;
    private final ProductFeignClient      productFeignClient;
    private final UserFeignClient         userFeignClient;
    private final OrderMessageProducer    messageProducer;
    private final StringRedisTemplate     redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    // ── 下单（Redis 分布式锁防并发） ──────────────────────────
    @Transactional
    public OrderDTO placeOrder(Long productId, Long buyerId, String remark) {
        String lockKey = "order:lock:" + productId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(buyerId), LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(409, "该商品正被其他人下单，请稍后重试");
        }
        try {
            // 通过 Feign 查商品
            Result<ProductDTO> productResult = productFeignClient.getProduct(productId);
            if (productResult == null || !productResult.isOk()) {
                throw new BusinessException(404, "商品不存在");
            }
            ProductDTO product = productResult.getData();

            if (product.getSellerId().equals(buyerId)) {
                throw new BusinessException(400, "不能购买自己的商品");
            }
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(400, "商品当前不可购买");
            }

            // 复用待支付订单
            return orderRepository
                    .findByBuyerIdAndProductIdAndStatus(buyerId, productId, OrderStatus.CREATED)
                    .map(this::toDTO)
                    .orElseGet(() -> createNewOrder(product, buyerId, remark));
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private OrderDTO createNewOrder(ProductDTO product, Long buyerId, String remark) {
        TradeOrder o = new TradeOrder();
        o.setOrderNo(generateOrderNo());
        o.setProductId(product.getId());
        o.setBuyerId(buyerId);
        o.setSellerId(product.getSellerId());
        o.setPrice(product.getPrice());
        o.setRemark(remark);
        o.setStatus(OrderStatus.CREATED);
        orderRepository.save(o);
        // 通知 product-service 锁定商品
        messageProducer.sendOrderCreated(product.getId());
        return toDTO(o);
    }

    // ── 取消订单 ──────────────────────────────────────────────
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        TradeOrder o = getAndCheck(orderId, userId);
        if (o.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException(400, "只有待支付的订单才能取消");
        }
        o.setStatus(OrderStatus.CANCELED);
        orderRepository.save(o);
        // 通知 product-service 解锁商品，恢复为在售
        messageProducer.sendOrderCanceled(o.getProductId());
    }

    // ── 查询我的订单 ──────────────────────────────────────────
    public List<OrderDTO> myBuyOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> mySellOrders(Long sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── 管理员：查看所有订单 ─────────────────────────────────
    public List<OrderDTO> adminListAllOrders(String operator) {
        checkAdmin(operator);
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── 根据订单号查询（支付服务调用） ─────────────────────────
    public OrderDTO getByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    // ── 内部工具 ──────────────────────────────────────────────
    private TradeOrder getAndCheck(Long orderId, Long userId) {
        TradeOrder o = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!o.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
        return o;
    }

    private void checkAdmin(String username) {
        if (!"admin".equals(username)) {
            throw new BusinessException(403, "只有管理员可以操作");
        }
    }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "CT" + ts + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private OrderDTO toDTO(TradeOrder o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setOrderNo(o.getOrderNo());
        dto.setProductId(o.getProductId());
        dto.setBuyerId(o.getBuyerId());
        dto.setSellerId(o.getSellerId());
        dto.setPrice(o.getPrice());
        dto.setRemark(o.getRemark());
        dto.setAlipayTradeNo(o.getAlipayTradeNo());
        dto.setStatus(o.getStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setPaidAt(o.getPaidAt());
        enrichOrder(dto);
        return dto;
    }

    private void enrichOrder(OrderDTO dto) {
        try {
            Result<ProductDTO> productResult = productFeignClient.getProduct(dto.getProductId());
            if (productResult != null && productResult.isOk() && productResult.getData() != null) {
                dto.setProductTitle(productResult.getData().getTitle());
            }
        } catch (Exception e) {
            log.warn("获取订单商品信息失败，orderId={}", dto.getId());
        }
        dto.setBuyerName(getUserName(dto.getBuyerId()));
        dto.setSellerName(getUserName(dto.getSellerId()));
    }

    private String getUserName(Long userId) {
        try {
            Result<UserDTO> userResult = userFeignClient.getUser(userId);
            if (userResult != null && userResult.isOk() && userResult.getData() != null) {
                UserDTO user = userResult.getData();
                return user.getNickname() != null ? user.getNickname() : user.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取订单用户信息失败，userId={}", userId);
        }
        return null;
    }
}
