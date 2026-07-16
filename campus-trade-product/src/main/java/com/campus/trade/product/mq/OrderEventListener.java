package com.campus.trade.product.mq;

import com.campus.trade.common.constant.MQConstant;
import com.campus.trade.common.enums.ProductStatus;
import com.campus.trade.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 监听订单事件，更新商品状态。
 *
 * order.created → 商品置为 LOCKED（交易中）
 * order.paid    → 商品置为 SOLD（已售出），并清除 Redis 缓存
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = MQConstant.QUEUE_ORDER_CREATED)
    @Transactional
    public void onOrderCreated(Long productId) {
        log.info("收到 order.created 消息，productId={}", productId);
        productRepository.findById(productId).ifPresent(p -> {
            if (p.getStatus() == ProductStatus.ON_SALE) {
                p.setStatus(ProductStatus.LOCKED);
                productRepository.save(p);
                evictCache(productId);
            }
        });
    }

    @RabbitListener(queues = MQConstant.QUEUE_ORDER_PAID)
    @Transactional
    public void onOrderPaid(Long productId) {
        log.info("收到 order.paid 消息，productId={}", productId);
        productRepository.findById(productId).ifPresent(p -> {
            p.setStatus(ProductStatus.SOLD);
            productRepository.save(p);
            evictCache(productId);
        });
    }

    @RabbitListener(queues = MQConstant.QUEUE_ORDER_CANCELED)
    @Transactional
    public void onOrderCanceled(Long productId) {
        log.info("收到 order.canceled 消息，productId={}", productId);
        productRepository.findById(productId).ifPresent(p -> {
            if (p.getStatus() == ProductStatus.LOCKED) {
                p.setStatus(ProductStatus.ON_SALE);
                productRepository.save(p);
                evictCache(productId);
            }
        });
    }

    private void evictCache(Long productId) {
        redisTemplate.delete("product:detail:" + productId);
    }
}
