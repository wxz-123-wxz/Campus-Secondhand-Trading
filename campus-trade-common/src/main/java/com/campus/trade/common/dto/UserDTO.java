package com.campus.trade.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息 DTO（跨服务传输）
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String campus;
    private LocalDateTime createdAt;
}
