package com.campus.trade.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 全局 JWT 鉴权过滤器。
 * 白名单路径直接放行；其余路径必须携带有效 Bearer Token。
 * 验证通过后，将 X-User-Id 和 X-Username 注入请求头，供下游服务使用。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final SecretKey key;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthGlobalFilter(
            @Value("${jwt.secret}") String secret,
            ReactiveStringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 白名单放行
        if (isPublicEndpoint(exchange)) {
            return chain.filter(exchange);
        }

        // 获取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        // 检查 Redis 黑名单（已登出的 token）
        String blacklistKey = "token:blacklist:" + token;
        return redisTemplate.hasKey(blacklistKey).flatMap(inBlacklist -> {
            if (Boolean.TRUE.equals(inBlacklist)) {
                return unauthorized(exchange);
            }
            // 校验 JWT
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId   = claims.getSubject();
                String username = claims.get("username", String.class);

                // 将用户信息注入下游请求头
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (JwtException | IllegalArgumentException e) {
                return unauthorized(exchange);
            }
        });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private boolean isPublicEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)) {
            return true;
        }

        if ("/api/payment/return".equals(path) || "/api/payment/notify".equals(path)) {
            return true;
        }

        if (HttpMethod.GET.equals(method)) {
            // 商品列表和商品详情公开；发布、收藏、我的商品等接口必须走 JWT 鉴权。
            return "/api/products".equals(path)
                    || path.matches("^/api/products/\\d+$")
                    || path.matches("^/api/products/\\d+/messages$");
        }

        return false;
    }

    @Override
    public int getOrder() {
        return -100; // 最高优先级
    }
}
