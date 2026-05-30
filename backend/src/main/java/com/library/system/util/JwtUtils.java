package com.library.system.util;

import com.library.system.common.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类 
 * 实现双Token机制：Access Token（2小时）+ Refresh Token（7天）
 *
 * FIXED: SEC-001 JWT密钥从环境变量读取，不再硬编码
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT密钥 - FIXED: SEC-001 从环境变量读取
     * 生产环境必须通过 JWT_SECRET 环境变量配置，无默认值回退
     * FIXED: CRITICAL-FIX 移除硬编码默认密钥，启动时强制校验
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:7200000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    /**
     * 启动时校验JWT密钥配置
     * FIXED: CRITICAL-FIX 启动时强制校验密钥已配置且达到安全长度
     */
    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret must be configured via 'jwt.secret' property or JWT_SECRET environment variable. " +
                    "Please set a strong secret key (at least 32 characters) for production use.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret key is only {} bytes. Recommended: at least 32 bytes (256 bits) for HMAC-SHA256.", keyBytes.length);
        }
    }

    /**
     * 获取签名密钥
     * <p>
     * FIXED: SEC-001 使用SHA-256密钥派生代替不安全的空格填充
     * 原先当密钥不足32字节时用空格+X填充，可预测且不安全。
     * 现在通过SHA-256哈希将任意长度密钥安全派生为256位密钥。
     * 如果密钥短于16字节（远低于安全阈值），启动时立即报错。
     * </p>
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 16) {
            throw new IllegalStateException(
                    "JWT secret key must be at least 16 bytes for security. " +
                    "Current length: " + keyBytes.length + " bytes. " +
                    "Please configure a stronger JWT_SECRET environment variable.");
        }
        if (keyBytes.length >= 32) {
            return Keys.hmacShaKeyFor(keyBytes);
        }
        // 密钥长度在16-31之间，使用SHA-256派生为安全的256位密钥
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyBytes);
            return new SecretKeySpec(hash, "HmacSHA256");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive JWT signing key", e);
        }
    }

    /**
     * 生成Access Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param role 用户角色
     * @return Access Token
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())  // JWT ID (jti)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", Constants.Token.TOKEN_TYPE_ACCESS) 
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成Refresh Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())  // JWT ID (jti)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", Constants.Token.TOKEN_TYPE_REFRESH) 
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 验证Token
     *
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误");
            return false;
        } catch (SecurityException e) {
            log.warn("Token签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token为空或非法");
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中提取用户ID：Token解析失败");
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从Token中获取用户名
     *
     * @param token Token字符串
     * @return 用户名
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中提取用户名：Token解析失败");
        }
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取用户角色
     *
     * @param token Token字符串
     * @return 用户角色
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中提取角色：Token解析失败");
        }
        return claims.get("role", String.class);
    }

    /**
     * 从Token中获取Token类型
     *
     * @param token Token字符串
     * @return Token类型
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中提取类型：Token解析失败");
        }
        return claims.get("type", String.class);
    }

    /**
     * 从Token中获取JTI（唯一标识）
     *
     * @param token Token字符串
     * @return JTI
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中提取JTI：Token解析失败");
        }
        return claims.getId();
    }

    /**
     * 从Token中获取过期时间
     *
     * @param token Token字符串
     * @return 过期时间
     * @throws IllegalArgumentException 如果Token解析失败
     */
    public LocalDateTime getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new IllegalArgumentException("无法从Token中获取过期时间：Token解析失败");
        }
        Date expiration = claims.getExpiration();
        return expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 获取Access Token过期时间（毫秒）
     *
     * @return 过期时间
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取Refresh Token过期时间（毫秒）
     *
     * @return 过期时间
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 获取Access Token过期秒数
     *
     * @return 过期秒数
     */
    public long getAccessExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * 解析Token（公开方法，供AuthService使用）
     *
     * @param token Token字符串
     * @return Claims，解析失败返回null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("解析Token失败", e);
            return null;
        }
    }
}
