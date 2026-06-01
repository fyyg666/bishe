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

/**
 * 座位预约流程集成测试
 */
@DisplayName("座位预约流程集成测试")
class SeatFlowIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    private String readerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        readerToken = "Bearer " + given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "reader1", "password", "test123"))
                .post("/auth/login")
                .path("data.accessToken");
    }

    @Test
    @DisplayName("查询可用座位")
    void getAvailableSeats() {
        given()
        .when()
                .get("/seats/room/1")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("预约座位并签到流程")
    void reserveAndCheckIn() {
        // 1. 预约座位
        given()
                .header("Authorization", readerToken)
                .contentType(ContentType.JSON)
                .body(Map.of("seatId", 100, "date", "2026-05-10"))
        .when()
                .post("/seat-reservations")
        .then()
                .statusCode(200);

        // 2. 签到
        given()
                .header("Authorization", readerToken)
        .when()
                .post("/seat-reservations/check-in")
        .then()
                .statusCode(200);

        // 3. 签退
        given()
                .header("Authorization", readerToken)
        .when()
                .post("/seat-reservations/check-out")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("预约座位 - 未认证返回 401")
    void reserveWithoutAuth_shouldReturn401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("seatId", 100, "date", "2026-05-10"))
        .when()
                .post("/seat-reservations")
        .then()
                .statusCode(401);
    }
}
