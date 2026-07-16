package com.campus.trade.payment.controller;

import com.campus.trade.common.exception.GlobalExceptionHandler;
import com.campus.trade.common.response.Result;
import com.campus.trade.payment.service.AlipayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController extends GlobalExceptionHandler {

    private final AlipayService alipayService;

    /**
     * GET /api/payment/pay?orderNo=CT...
     * 返回支付宝自提交表单 HTML，前端直接写入 document
     */
    @GetMapping("/pay")
    @ResponseBody
    public String pay(@RequestParam String orderNo,
                      @RequestHeader("X-User-Id") Long userId) {
        return alipayService.createPayForm(orderNo);
    }

    /**
     * POST /api/payment/sync?orderNo=CT...
     * 本地沙箱环境下，支付宝异步通知无法回调 localhost；前端回到订单页后主动触发一次查询同步。
     */
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam String orderNo,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(alipayService.syncOrderStatus(orderNo, userId));
    }

    /**
     * GET /api/payment/return — 支付宝同步回调（用户浏览器跳转）
     * 支付宝跳转回来不会携带 Authorization，请在网关白名单放行。
     */
    @GetMapping("/return")
    public ResponseEntity<Void> returnCallback(@RequestParam Map<String, String> params) {
        String orderNoRaw = params.getOrDefault("out_trade_no", "");
        String payStatus = "success";
        try {
            alipayService.handleReturn(params);
        } catch (Exception e) {
            payStatus = "processing";
        }

        String orderNo = UriUtils.encode(orderNoRaw, StandardCharsets.UTF_8);
        URI location = URI.create("http://localhost:3000/my-orders.html?pay=" + payStatus + "&orderNo=" + orderNo);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }

    /**
     * POST /api/payment/notify — 支付宝异步通知（服务器回调）
     * 必须在白名单中；返回 "success" 告知支付宝停止重试
     */
    @PostMapping("/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach(
                (k, v) -> params.put(k, String.join(",", v)));
        try {
            alipayService.handleNotify(params);
            return "success";
        } catch (Exception e) {
            return "failure";
        }
    }
}
