package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.exception.UnauthorizedException;
import com.library.system.mapper.UserMapper;
import com.library.system.util.DataMaskingUtil;
import com.library.system.util.JwtUtils;
import com.library.system.service.AccountLockService;
import com.library.system.service.AuthService;
import com.library.system.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 认证服务实现类
 * FIXED: SEC-002 使用Redis分布式账户锁定替代本地缓存
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final AccountLockService accountLockService;  
    private final StringRedisTemplate redisTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String CAPTCHA_REDIS_PREFIX = "captcha:";

    @Override
    public LoginResponse login(LoginRequest request) {
        // 验证码校验
        validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        // 查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new UnauthorizedException(ErrorCode.AUTH_FAILED, "用户名或密码错误"); 
        }

        if (accountLockService.isLocked(user.getId())) {
            long remainingSeconds = accountLockService.getRemainingLockSeconds(user.getId());
            long remainingMinutes = (remainingSeconds + 59) / 60;
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账户已被锁定，请" + remainingMinutes + "分钟后再试"); 
        }

        // 检查账户状态（禁用状态仍从数据库读取）
        if (Constants.UserStatus.DISABLED.equals(user.getStatus())) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_DISABLED, "账户已被禁用"); 
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            
            int failCount = accountLockService.recordLoginFailure(user.getId(), user.getUsername());
            if (failCount >= AccountLockService.MAX_LOGIN_FAILURES) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账户已被锁定，请15分钟后再试"); 
            }
            int remainingAttempts = AccountLockService.MAX_LOGIN_FAILURES - failCount;
            throw new UnauthorizedException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "用户名或密码错误，剩余尝试次数：" + remainingAttempts); 
        }

        // 生成Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        accountLockService.clearLoginFailures(user.getId());

        log.info("用户登录成功: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessExpirationSeconds())
                .userInfo(buildUserInfo(user))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse.UserInfo register(RegisterRequest request) {
        // 验证码校验 — FIXED: SEC-HIGH 注册接口缺少验证码防护
        validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        // 检查两次输入的密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "两次输入的密码不一致");
        }

        if (request.getPassword().length() < 8
                || !request.getPassword().matches(".*[a-zA-Z].*")
                || !request.getPassword().matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.PASSWORD_COMPLEXITY, "密码长度至少8位，且必须包含字母和数字");
        }

        // 检查用户名是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在"); 
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Constants.Role.READER);
        user.setStatus(Constants.UserStatus.NORMAL);
        user.setCreditScore(Constants.Credit.INITIAL_SCORE);
        user.setBorrowCount(0);
        
        // 生成读者卡号
        String cardNumber = generateCardNumber();
        user.setCardNumber(cardNumber);

        userMapper.insert(user);

        log.info("用户注册成功: {}", user.getUsername());

        return buildUserInfo(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 验证refresh token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh Token无效"); 
        }

        Long userId = jwtUtils.getUserIdFromToken(refreshToken);

        // 将旧Refresh Token加入黑名单（轮换）
        String oldJti = jwtUtils.getJtiFromToken(refreshToken);
        if (oldJti != null) {
            String oldBlacklistKey = Constants.Token.BLACKLIST_PREFIX + oldJti;
            long remainingTtl = getTokenRemainingTtl(refreshToken);
            if (remainingTtl > 0) {
                tokenBlacklistService.addToBlacklist(oldBlacklistKey, remainingTtl);
                log.info("Refresh Token轮换: 旧Token已加入黑名单, jti={}", oldJti);
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在");
        }

        // 使用数据库中的最新role和username生成新Token
        String username = user.getUsername();
        String role = user.getRole();

        // 生成新Token
        String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessExpirationSeconds())
                .userInfo(buildUserInfo(user))
                .build();
    }

    @Override
    public void logout(String token) {
        // 将token加入黑名单
        // FIXED: CRITICAL-BUG - 统一使用JTI作为黑名单key，与JwtFilter保持一致
        String jti = jwtUtils.getJtiFromToken(token);
        if (jti == null) {
            log.warn("登出时无法提取Token的JTI");
            return;
        }
        String blacklistKey = Constants.Token.BLACKLIST_PREFIX + jti;
        // 获取token剩余有效期
        Long ttl = getTokenRemainingTtl(token);
        if (ttl > 0) {
            tokenBlacklistService.addToBlacklist(blacklistKey, ttl);
        }
        log.info("用户登出，Token已加入黑名单: jti={}", jti);
    }

    @Override
    public LoginResponse.UserInfo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在"); 
        }
        return buildUserInfo(user);
    }

    /**
     * 生成读者卡号
     */
    private String generateCardNumber() {
        String dateKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            Long sequence = redisTemplate.opsForValue().increment("card_seq:" + dateKey);
            return "RD" + dateKey + String.format("%06d", sequence);
        } catch (Exception e) {
            log.warn("Redis不可用，回退到随机卡号生成: {}", e.getMessage());
            return "RD" + dateKey + String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        }
    }

    /**
     * 构建用户信息
     */
    private LoginResponse.UserInfo buildUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .cardNumber(user.getCardNumber())
                .creditScore(user.getCreditScore())
                .currentBorrows(user.getBorrowCount())
                .avatar(user.getAvatar())
                .status(user.getStatus() != null ? 
                    (Constants.UserStatus.DISABLED.equals(user.getStatus()) ? 0 : 
                     Constants.UserStatus.LOCKED.equals(user.getStatus()) ? 2 : 1) : 1)
                .phone(DataMaskingUtil.maskPhone(user.getPhone()))
                .email(DataMaskingUtil.maskEmail(user.getEmail()))
                .createTime(user.getCreateTime() != null ? 
                    user.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .build();
    }

    // 现在统一使用 AccountLockService 进行分布式账户锁定

    @Override
    public boolean validateToken(String token) {
        try {
            return jwtUtils.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在"); 
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "原密码错误"); 
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    /**
     * 获取Token剩余有效期
     */
    private Long getTokenRemainingTtl(String token) {
        try {
            var expiration = jwtUtils.getExpirationDateFromToken(token);
            if (expiration != null) {
                long remaining = java.time.Duration.between(LocalDateTime.now(), expiration).getSeconds();
                return remaining > 0 ? remaining : 0;
            }
        } catch (Exception e) {
            log.warn("获取Token剩余有效期失败: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 验证码校验
     * 当captchaKey和captchaCode都不为空时进行校验
     */
    private void validateCaptcha(String captchaKey, String captchaCode) {
        if (!org.springframework.util.StringUtils.hasText(captchaKey)
                || !org.springframework.util.StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "验证码不能为空");
        }
        String redisKey = CAPTCHA_REDIS_PREFIX + captchaKey;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "验证码已过期，请重新获取");
        }
        // 验证后立即删除（一次性使用）
        redisTemplate.delete(redisKey);
        if (!storedCode.equals(captchaCode)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "验证码错误");
        }
    }
}
