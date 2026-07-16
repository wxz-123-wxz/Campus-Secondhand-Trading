package com.campus.trade.user.service;

import com.campus.trade.common.dto.UserDTO;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.util.JwtUtil;
import com.campus.trade.user.dto.LoginRequest;
import com.campus.trade.user.dto.LoginResponse;
import com.campus.trade.user.dto.RegisterRequest;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    // ── 注册 ──────────────────────────────────────────────────
    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setCampus(req.getCampus());
        userRepository.save(user);
    }

    // ── 登录 ──────────────────────────────────────────────────
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    // ── 登出（令牌加入 Redis 黑名单）─────────────────────────
    public void logout(String token) {
        String key = "token:blacklist:" + token;
        redisTemplate.opsForValue().set(key, "1", TOKEN_TTL);
    }

    // ── 查询用户信息 ──────────────────────────────────────────
    public UserDTO getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        return toDTO(user);
    }

    // ── 管理员：查看所有用户 ─────────────────────────────────
    public List<UserDTO> listAllUsers(String operator) {
        checkAdmin(operator);
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private void checkAdmin(String username) {
        if (!"admin".equals(username)) {
            throw new BusinessException(403, "只有管理员可以操作");
        }
    }

    // ── 转换 ──────────────────────────────────────────────────
    private UserDTO toDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setCampus(u.getCampus());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
}
