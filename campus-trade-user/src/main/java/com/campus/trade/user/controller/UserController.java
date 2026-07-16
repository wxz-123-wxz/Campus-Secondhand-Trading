package com.campus.trade.user.controller;

import com.campus.trade.common.dto.UserDTO;
import com.campus.trade.common.exception.GlobalExceptionHandler;
import com.campus.trade.common.response.Result;
import com.campus.trade.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends GlobalExceptionHandler {

    private final UserService userService;

    /**
     * GET /api/users/{id}
     * 内部接口：供其他微服务通过 OpenFeign 查询用户信息。
     * 网关验证后，下游服务可直接调用。
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * GET /api/users/me
     * 前端调用：获取当前登录用户信息（userId 由网关注入请求头）
     */
    @GetMapping("/me")
    public Result<UserDTO> me(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(userService.getById(userId));
    }

    /** GET /api/users/admin/all — 管理员查看所有用户 */
    @GetMapping("/admin/all")
    public Result<java.util.List<UserDTO>> allUsers(@RequestHeader("X-Username") String username) {
        return Result.ok(userService.listAllUsers(username));
    }
}
