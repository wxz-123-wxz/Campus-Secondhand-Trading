package com.campus.trade.product.controller;

import com.campus.trade.common.dto.ProductDTO;
import com.campus.trade.common.dto.ProductMessageDTO;
import com.campus.trade.common.exception.GlobalExceptionHandler;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController extends GlobalExceptionHandler {

    private final ProductService productService;

    /** GET /api/products?keyword=&page=1&size=10 — 公开，白名单 */
    @GetMapping
    public Result<PageResult<ProductDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.listOnSale(keyword, page, size));
    }

    /** GET /api/products/{id} — 公开 */
    @GetMapping("/{id}")
    public Result<ProductDTO> detail(@PathVariable Long id) {
        return Result.ok(productService.getDetail(id));
    }

    /** POST /api/products — 需登录，网关注入 X-User-Id */
    @PostMapping
    public Result<ProductDTO> create(
            @Valid @RequestBody ProductDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(productService.create(dto, userId));
    }

    /** PUT /api/products/{id}/offline */
    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id,
                                @RequestHeader("X-User-Id") Long userId) {
        productService.offline(id, userId);
        return Result.ok();
    }

    /** POST /api/products/{id}/favorite */
    @PostMapping("/{id}/favorite")
    public Result<Boolean> favorite(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId) {
        boolean added = productService.toggleFavorite(id, userId);
        return Result.ok(added);
    }

    /** POST /api/products/{id}/messages */
    @PostMapping("/{id}/messages")
    public Result<Void> addMessage(@PathVariable Long id,
                                   @RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-Username") String username,
                                   @RequestBody @NotBlank String content) {
        productService.addMessage(id, userId, username, content);
        return Result.ok();
    }

    /** GET /api/products/{id}/messages — 查看商品留言 */
    @GetMapping("/{id}/messages")
    public Result<List<ProductMessageDTO>> messages(@PathVariable Long id) {
        return Result.ok(productService.listMessages(id));
    }

    /** GET /api/products/my — 我发布的商品 */
    @GetMapping("/my")
    public Result<List<ProductDTO>> myProducts(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(productService.myProducts(userId));
    }

    /** GET /api/products/favorites — 我收藏的商品 */
    @GetMapping("/favorites")
    public Result<List<ProductDTO>> myFavorites(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(productService.myFavorites(userId));
    }

    /** GET /api/products/admin/all — 管理员查看所有商品 */
    @GetMapping("/admin/all")
    public Result<List<ProductDTO>> adminAllProducts(@RequestHeader("X-Username") String username) {
        return Result.ok(productService.adminListAllProducts(username));
    }

    /** PUT /api/products/admin/{id}/offline — 管理员强制下架 */
    @PutMapping("/admin/{id}/offline")
    public Result<Void> adminOffline(@PathVariable Long id,
                                     @RequestHeader("X-Username") String username) {
        productService.adminOffline(id, username);
        return Result.ok();
    }

    /** GET /api/products/admin/messages — 管理员查看所有留言 */
    @GetMapping("/admin/messages")
    public Result<List<ProductMessageDTO>> adminAllMessages(@RequestHeader("X-Username") String username) {
        return Result.ok(productService.adminListAllMessages(username));
    }

    /** DELETE /api/products/admin/messages/{id} — 管理员删除违规留言 */
    @DeleteMapping("/admin/messages/{id}")
    public Result<Void> adminDeleteMessage(@PathVariable Long id,
                                           @RequestHeader("X-Username") String username) {
        productService.adminDeleteMessage(id, username);
        return Result.ok();
    }
}
