package com.library.system.integration;

import com.library.system.base.IntegrationTestBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 认证流程集成测试 - 完整 API 链路
 */
@DisplayName("认证流程集成测试")
class AuthFlowIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Nested
    @DisplayName("登录流程")
    class LoginFlow {

        @Test
        @DisplayName("完整登录流程 - 登录成功返回双Token")
        void login_fullFlow_shouldReturnTokens() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", "admin", "password", "test123"))
            .when()
                    .post("/auth/login")
            .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .body("data.accessToken", notNullValue())
                    .body("data.refreshToken", notNullValue())
                    .body("data.userInfo.username", equalTo("admin"));
        }

        @Test
        @DisplayName("密码错误 - 返回业务错误")
        void login_wrongPassword_shouldReturnError() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", "admin", "password", "wrongpass"))
            .when()
                    .post("/auth/login")
            .then()
                    .statusCode(200)
                    .body("code", not(equalTo(200)));
        }

        @Test
        @DisplayName("参数缺失 - 返回 400")
        void login_missingParams_shouldReturn400() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", ""))
            .when()
                    .post("/auth/login")
            .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Token 刷新流程")
    class RefreshFlow {

        @Test
        @DisplayName("刷新 Token - 成功")
        void refreshToken_success() {
            // 先登录获取 refreshToken
            String refreshToken = given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", "admin", "password", "test123"))
                    .post("/auth/login")
                    .path("data.refreshToken");

            // 使用 refreshToken 刷新
            given()
                    .header("Authorization", "Bearer " + refreshToken)
            .when()
                    .post("/auth/refresh")
            .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .body("data.accessToken", notNullValue());
        }
    }

    @Nested
    @DisplayName("注册流程")
    class RegisterFlow {

        @Test
        @DisplayName("注册新用户")
        void register_newUser_shouldSucceed() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "username", "newreader_" + System.currentTimeMillis(),
                            "password", "Pass@1234",
                            "realName", "新读者",
                            "phone", "13900139000"
                    ))
            .when()
                    .post("/auth/register")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("注册 - 密码强度不足")
        void register_weakPassword_shouldReturn400() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "username", "weakpwd",
                            "password", "123",
                            "realName", "弱密码"
                    ))
            .when()
                    .post("/auth/register")
            .then()
                    .statusCode(400);
        }
    }
}
