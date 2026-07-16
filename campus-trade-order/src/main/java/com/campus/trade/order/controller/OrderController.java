package com.campus.trade.order.controller;

import com.campus.trade.common.dto.OrderDTO;
import com.campus.trade.common.exception.GlobalExceptionHandler;
import com.campus.trade.common.response.Result;
import com.campus.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController extends GlobalExceptionHandler {

    private final OrderService orderService;

    /** POST /api/orders — 下单 */
    @PostMapping
    public Result<OrderDTO> placeOrder(
            @RequestParam Long productId,
            @RequestParam(required = false) String remark,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(orderService.placeOrder(productId, userId, remark));
    }

    /** PUT /api/orders/{id}/cancel */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.cancelOrder(id, userId);
        return Result.ok();
    }

    /** GET /api/orders/buy — 我的买单列表 */
    @GetMapping("/buy")
    public Result<List<OrderDTO>> myBuyOrders(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(orderService.myBuyOrders(userId));
    }

    /** GET /api/orders/sell — 我的卖单列表 */
    @GetMapping("/sell")
    public Result<List<OrderDTO>> mySellOrders(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(orderService.mySellOrders(userId));
    }

    /** GET /api/orders/admin/all — 管理员查看所有订单 */
    @GetMapping("/admin/all")
    public Result<List<OrderDTO>> adminAllOrders(@RequestHeader("X-Username") String username) {
        return Result.ok(orderService.adminListAllOrders(username));
    }

    /**
     * GET /api/orders/by-no?orderNo=CT...
     * 供支付服务通过 Feign 查询订单（内部接口）
     */
    @GetMapping("/by-no")
    public Result<OrderDTO> getByOrderNo(@RequestParam String orderNo) {
        return Result.ok(orderService.getByOrderNo(orderNo));
    }
}
