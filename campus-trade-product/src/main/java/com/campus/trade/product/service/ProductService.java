package com.campus.trade.product.service;

import com.campus.trade.common.dto.ProductDTO;
import com.campus.trade.common.dto.ProductMessageDTO;
import com.campus.trade.common.dto.UserDTO;
import com.campus.trade.common.enums.ProductStatus;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.product.entity.Favorite;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.entity.ProductMessage;
import com.campus.trade.product.feign.UserFeignClient;
import com.campus.trade.product.repository.FavoriteRepository;
import com.campus.trade.product.repository.MessageRepository;
import com.campus.trade.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository    productRepository;
    private final FavoriteRepository   favoriteRepository;
    private final MessageRepository    messageRepository;
    private final UserFeignClient      userFeignClient;
    private final StringRedisTemplate  redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    // ── 商品列表（带搜索） ────────────────────────────────────
    public PageResult<ProductDTO> listOnSale(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> result = StringUtils.hasText(keyword)
                ? productRepository.searchByKeyword(keyword, ProductStatus.ON_SALE, pageable)
                : productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ON_SALE, pageable);
        List<ProductDTO> dtos = result.getContent().stream()
                .map(this::toDTO).collect(Collectors.toList());
        return new PageResult<>(dtos, result.getTotalElements(), page, size);
    }

    // ── 商品详情（带 Redis 缓存） ─────────────────────────────
    public ProductDTO getDetail(Long id) {
        String cacheKey = "product:detail:" + id;
        // 简单缓存：此处存 productId 标记，真实场景可用 JSON 序列化
        // 为避免引入额外序列化，这里直接查 DB（生产可换 @Cacheable）
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        // 标记访问，用于热度排行（incr）
        redisTemplate.opsForValue().increment("product:pv:" + id);
        return toDTO(p, true);
    }

    // ── 发布商品 ──────────────────────────────────────────────
    @Transactional
    public ProductDTO create(ProductDTO dto, Long sellerId) {
        Product p = new Product();
        p.setTitle(dto.getTitle());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setCategory(dto.getCategory());
        p.setImageUrl(dto.getImageUrl());
        p.setTradePlace(dto.getTradePlace());
        p.setSellerId(sellerId);
        p.setStatus(ProductStatus.ON_SALE);
        return toDTO(productRepository.save(p));
    }

    // ── 下架商品 ──────────────────────────────────────────────
    @Transactional
    public void offline(Long productId, Long userId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (!p.getSellerId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(400, "只有在售商品可以下架");
        }
        p.setStatus(ProductStatus.OFFLINE);
        productRepository.save(p);
        redisTemplate.delete("product:detail:" + productId);
    }

    // ── 收藏切换 ──────────────────────────────────────────────
    @Transactional
    public boolean toggleFavorite(Long productId, Long userId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        return favoriteRepository.findByUserIdAndProductId(userId, productId)
                .map(f -> { favoriteRepository.delete(f); return false; })
                .orElseGet(() -> {
                    if (p.getStatus() != ProductStatus.ON_SALE) {
                        throw new BusinessException(400, "只有在售商品可以收藏");
                    }
                    Favorite fav = new Favorite();
                    fav.setUserId(userId);
                    fav.setProduct(p);
                    favoriteRepository.save(fav);
                    return true;
                });
    }

    // ── 发表留言 ──────────────────────────────────────────────
    @Transactional
    public void addMessage(Long productId, Long userId, String username, String content) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(400, "该商品已下架或已售出，不能继续留言");
        }
        ProductMessage msg = new ProductMessage();
        msg.setProduct(p);
        msg.setUserId(userId);
        msg.setUserName(username);
        msg.setContent(content);
        messageRepository.save(msg);
        redisTemplate.delete("product:detail:" + productId);
    }

    public List<ProductMessageDTO> listMessages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(404, "商品不存在");
        }
        return messageRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    // ── 我的商品 ──────────────────────────────────────────────
    public List<ProductDTO> myProducts(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── 我的收藏 ──────────────────────────────────────────────
    public List<ProductDTO> myFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(f -> toDTO(f.getProduct())).collect(Collectors.toList());
    }

    // ── 管理员功能 ────────────────────────────────────────────
    public List<ProductDTO> adminListAllProducts(String operator) {
        checkAdmin(operator);
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminOffline(Long productId, String operator) {
        checkAdmin(operator);
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(400, "只有在售商品可以强制下架");
        }
        p.setStatus(ProductStatus.OFFLINE);
        productRepository.save(p);
        redisTemplate.delete("product:detail:" + productId);
    }

    public List<ProductMessageDTO> adminListAllMessages(String operator) {
        checkAdmin(operator);
        return messageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminDeleteMessage(Long messageId, String operator) {
        checkAdmin(operator);
        ProductMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(404, "留言不存在"));
        Long productId = message.getProduct().getId();
        messageRepository.delete(message);
        redisTemplate.delete("product:detail:" + productId);
    }

    private void checkAdmin(String username) {
        if (!"admin".equals(username)) {
            throw new BusinessException(403, "只有管理员可以操作");
        }
    }

    // ── DTO 转换 ──────────────────────────────────────────────
    private ProductDTO toDTO(Product p) {
        return toDTO(p, false);
    }

    private ProductDTO toDTO(Product p, boolean includeMessages) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setCategory(p.getCategory());
        dto.setImageUrl(p.getImageUrl());
        dto.setTradePlace(p.getTradePlace());
        dto.setStatus(p.getStatus());
        dto.setSellerId(p.getSellerId());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        // Feign 查卖家名（熔断降级：失败时保留 null）
        try {
            Result<UserDTO> userResult = userFeignClient.getUser(p.getSellerId());
            if (userResult != null && userResult.isOk()) {
                dto.setSellerName(userResult.getData().getNickname());
            }
        } catch (Exception e) {
            log.warn("获取卖家信息失败，productId={}", p.getId());
        }
        if (includeMessages) {
            List<ProductMessageDTO> messages = messageRepository.findByProductIdOrderByCreatedAtDesc(p.getId())
                    .stream()
                    .map(this::toMessageDTO)
                    .collect(Collectors.toList());
            dto.setMessages(messages);
        }
        return dto;
    }

    private ProductMessageDTO toMessageDTO(ProductMessage message) {
        ProductMessageDTO dto = new ProductMessageDTO();
        dto.setId(message.getId());
        dto.setProductId(message.getProduct().getId());
        dto.setProductTitle(message.getProduct().getTitle());
        dto.setUserId(message.getUserId());
        dto.setUserName(message.getUserName());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
