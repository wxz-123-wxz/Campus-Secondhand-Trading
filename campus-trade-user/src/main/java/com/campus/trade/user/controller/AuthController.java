package com.campus.trade.user.controller;

import com.campus.trade.common.exception.GlobalExceptionHandler;
import com.campus.trade.common.response.Result;
import com.campus.trade.user.dto.LoginRequest;
import com.campus.trade.user.dto.LoginResponse;
import com.campus.trade.user.dto.RegisterRequest;
import com.campus.trade.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends GlobalExceptionHandler {

    private final UserService userService;

    /** POST /api/auth/register */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        userService.register(req);
        return Result.ok();
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    /**
     * POST /api/auth/logout
     * 前端把 token 放在 Authorization: Bearer <token> 中，
     * 网关已将其注入请求，这里直接从头读取。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userService.logout(authHeader.substring(7));
        }
        return Result.ok();
    }
}
