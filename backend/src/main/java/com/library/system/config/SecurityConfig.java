package com.library.system.config;

import com.library.system.filter.JwtFilter;
import com.library.system.filter.RateLimitFilter;
import com.library.system.filter.XssFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * 配置JWT认证、授权规则、CORS等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final XssFilter xssFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtFilter jwtFilter, XssFilter xssFilter, UserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.xssFilter = xssFilter;
        this.userDetailsService = userDetailsService;
    }

    @Autowired(required = false)
    private RateLimitFilter rateLimitFilter;

    // FIXED: SEC-003 CORS配置从application.yml读取，支持环境变量覆盖
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}")
    private String corsAllowedHeaders;

    @Value("${cors.exposed-headers:X-RateLimit-Limit,X-RateLimit-Remaining,Authorization}")
    private String corsExposedHeaders;

    @Value("${cors.max-age:1800}")
    private long corsMaxAge;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 安全过滤器链
     * FIXED: P0-003 添加安全响应头, P0-004 Actuator端点需要认证
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（使用JWT不需要）
                .csrf(AbstractHttpConfigurer::disable)

                .headers(headers -> headers
                        // 防止点击劫持
                        .frameOptions(frame -> frame.deny())
                        // 防止MIME类型嗅探
                        .contentTypeOptions(contentType -> {})
                        // XSS防护头
                        .xssProtection(xss -> xss.disable())
                        // Referrer策略
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // 缓存控制
                        .cacheControl(cache -> cache.disable())
                        // 自定义安全头
                        .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                        .addHeaderWriter(new StaticHeadersWriter("X-Frame-Options", "DENY"))
                        .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                        .addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
                        // Content Security Policy (CSP)
                        .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy", 
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'"))
                )

                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置会话管理（无状态）
                
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                .sessionFixation().none()  // JWT无状态模式不需要session fixation防护
                )

                // 配置认证提供者
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()

                        // Swagger/OpenAPI 文档接口 - 所有人可访问（文档仅展示，实际操作仍需认证）
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources", "/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**", "/doc.html", "/favicon.ico").permitAll()
                        .requestMatchers("/captcha").permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "LIBRARIAN")

                        // 图书查询接口允许所有人访问
                        .requestMatchers(HttpMethod.GET, "/books", "/books/hot", "/books/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/books/check-isbn").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()

                        // 座位查询接口允许所有人访问
                        .requestMatchers(HttpMethod.GET, "/seats", "/seats/check-availability").permitAll()

                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )

                // 添加过滤器
                .addFilterBefore(xssFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // 条件添加RateLimitFilter（仅在Redis可用时）
        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * CORS配置
     * FIXED: SEC-003 从配置文件和环境变量读取allowedOrigins，不再硬编码
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // FIXED: SEC-003 - 从配置文件读取CORS允许的源，支持通过CORS_ALLOWED_ORIGINS环境变量覆盖
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(corsAllowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(corsAllowedHeaders.split(",")));
        configuration.setExposedHeaders(Arrays.asList(corsExposedHeaders.split(",")));
        configuration.setAllowCredentials(true);  // 允许携带凭证
        configuration.setMaxAge(corsMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
