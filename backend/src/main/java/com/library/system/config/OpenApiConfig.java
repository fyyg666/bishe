package com.library.system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 配置类
 * 集成 springdoc-openapi-starter-webmvc-ui
 *
 * API文档访问地址：
 * - Swagger UI: /api/swagger-ui.html
 * - OpenAPI JSON: /api/v3/api-docs
 * - OpenAPI YAML: /api/v3/api-docs.yaml
 *
 * @author Library Team
 * @version 2.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${openapi.server.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${openapi.server.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        // 定义安全scheme名称
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("图书馆管理系统 API")
                        .description("## 概述\n" +
                                "图书馆管理系统V2.0 RESTful API文档，基于Spring Boot 3.5.13 + MyBatis-Plus构建。\n\n" +
                                "## 认证方式\n" +
                                "本系统采用JWT双Token认证机制：\n" +
                                "- Access Token：用于API访问，有效期2小时\n" +
                                "- Refresh Token：用于刷新Access Token，有效期7天\n\n" +
                                "## 使用方式\n" +
                                "1. 调用 `/api/v1/auth/login` 获取Token\n" +
                                "2. 在请求头中添加 `Authorization: Bearer {accessToken}`\n" +
                                "3. Token过期时调用 `/api/v1/auth/refresh` 刷新")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("图书馆管理系统团队")
                                .email("library@example.com")
                                .url("https://github.com/library-system"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(devUrl)
                                .description("开发环境服务器"),
                        new Server()
                                .url(prodUrl)
                                .description("生产环境服务器")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入有效的JWT Token，格式：Bearer {accessToken}")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
