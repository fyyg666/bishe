package com.library.system.security;

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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("JWT Token 黑名单集成测试")
class JwtFilterTokenBlacklistIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Nested
    @DisplayName("Token 吊销流程")
    class TokenRevocationFlow {

        @Test
        @DisplayName("登出后使用已吊销Token访问受保护接口 - 返回401")
        void revokedToken_accessProtected_shouldReturn401() {
            String accessToken = given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", "admin", "password", "test123"))
                .when()
                    .post("/auth/login")
                .then()
                    .statusCode(200)
                    .body("data.accessToken", notNullValue())
                    .extract()
                    .path("data.accessToken");

            given()
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .post("/auth/logout")
                .then()
                    .statusCode(200)
                    .body("code", equalTo(0));

            given()
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get("/auth/info")
                .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("无Token访问受保护接口 - 返回401")
        void noToken_accessProtected_shouldReturn401() {
            given()
                    .contentType(ContentType.JSON)
                .when()
                    .get("/auth/info")
                .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("有效Token访问受保护接口 - 返回200")
        void validToken_accessProtected_shouldReturn200() {
            String accessToken = given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("username", "admin", "password", "test123"))
                .when()
                    .post("/auth/login")
                .then()
                    .statusCode(200)
                    .extract()
                    .path("data.accessToken");

            given()
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get("/auth/info")
                .then()
                    .statusCode(200)
                    .body("code", equalTo(0));
        }
    }
}
