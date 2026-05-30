package com.library.system.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.library.system.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 * <p>
 * 生成图片验证码，验证码文本存入Redis（5分钟过期），
 * 返回UUID + Base64图片数据给前端。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "验证码", description = "登录验证码生成")
public class CaptchaController {

    private final DefaultKaptcha captchaProducer;
    private final StringRedisTemplate redisTemplate;

    private static final long CAPTCHA_EXPIRE_SECONDS = 300; // 5分钟
    private static final String CAPTCHA_REDIS_PREFIX = "captcha:";

    /**
     * 生成验证码
     * 返回captchaKey(UUID)和captchaImage(Base64图片数据)
     */
    @Operation(summary = "获取验证码", description = "生成图片验证码，返回UUID和Base64编码的图片数据")
    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> getCaptcha(HttpServletResponse response) {
        // 生成验证码文本
        String captchaText = captchaProducer.createText();

        // 生成UUID作为key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存入Redis（5分钟过期）
        redisTemplate.opsForValue().set(
                CAPTCHA_REDIS_PREFIX + captchaKey,
                captchaText.toLowerCase(),
                CAPTCHA_EXPIRE_SECONDS,
                TimeUnit.SECONDS);

        // 生成图片
        BufferedImage image = captchaProducer.createImage(captchaText);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, String> result = new HashMap<>();
            result.put("captchaKey", captchaKey);
            result.put("captchaImage", "data:image/png;base64," + base64Image);

            log.debug("验证码生成成功: captchaKey={}", captchaKey);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证码图片生成失败", e);
            return ApiResponse.error(500, "验证码生成失败");
        }
    }
}
