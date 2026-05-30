package com.library.system.integration;

import com.library.system.base.IntegrationTestBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 借阅流程集成测试 - 覆盖完整借阅业务链路
 */
@DisplayName("借阅流程集成测试")
class BorrowFlowIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    private String readerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        // 登录获取读者 Token
        readerToken = "Bearer " + given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "reader1", "password", "test123"))
                .post("/auth/login")
                .path("data.accessToken");
    }

    @Test
    @DisplayName("借阅完整流程：登录→借书→我的借阅→还书")
    void borrowFullFlow() {
        // 1. 借书
        Long borrowId = given()
                .header("Authorization", readerToken)
                .contentType(ContentType.JSON)
                .body(Map.of("bookId", 100, "days", 30))
        .when()
                .post("/borrow-records")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract()
                .path("data.id");

        // 2. 查看我的借阅
        given()
                .header("Authorization", readerToken)
        .when()
                .get("/borrow-records/my")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.records", not(empty()));

        // 3. 还书
        if (borrowId != null) {
            given()
                    .header("Authorization", readerToken)
            .when()
                    .put("/borrow-records/" + borrowId + "/return")
            .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
        }
    }

    @Test
    @DisplayName("未认证访问 - 返回 401")
    void borrowWithoutAuth_shouldReturn401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("bookId", 100, "days", 30))
        .when()
                .post("/borrow-records")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("管理员查看全部借阅记录")
    void adminViewAllBorrows() {
        String adminToken = "Bearer " + given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin", "password", "test123"))
                .post("/auth/login")
                .path("data.accessToken");

        given()
                .header("Authorization", adminToken)
        .when()
                .get("/borrow-records/all")
        .then()
                .statusCode(200);
    }
}
