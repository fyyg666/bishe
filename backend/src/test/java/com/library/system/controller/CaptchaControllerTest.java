package com.library.system.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.library.system.base.ControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.awt.image.BufferedImage;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CaptchaController 验证码测试")
class CaptchaControllerTest extends ControllerTestBase {

    @Mock
    private DefaultKaptcha captchaProducer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CaptchaController captchaController;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(captchaProducer.createText()).thenReturn("ABCD");
        when(captchaProducer.createImage("ABCD")).thenReturn(new BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB));
        initMockMvc(captchaController);
    }

    @Nested
    @DisplayName("获取验证码")
    class GetCaptcha {

        @Test
        @DisplayName("请求验证码 - 返回 200 和 Base64 图片")
        void getCaptcha_shouldReturn200AndImage() throws Exception {
            mockMvc.perform(get("/captcha"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.captchaKey").isNotEmpty())
                    .andExpect(jsonPath("$.data.captchaImage").value(startsWith("data:image/png;base64,")));
        }
    }
}
