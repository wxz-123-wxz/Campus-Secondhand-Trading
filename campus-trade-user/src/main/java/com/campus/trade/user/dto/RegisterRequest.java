package com.campus.trade.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度3~30")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只允许字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度6~30")
    private String password;

    @Size(max = 30)
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String campus;
}
