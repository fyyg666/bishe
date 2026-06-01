package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.BookReservationResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.BookReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/book-reservations")
@RequiredArgsConstructor
@Tag(name = "图书预约排队", description = "图书预约排队管理")
@SecurityRequirement(name = "bearerAuth")
public class BookReservationController extends BaseController {

    private final BookReservationService bookReservationService;

    @Operation(summary = "预约排队", description = "对无库存图书进行预约排队")
    @PostMapping
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<BookReservationResponse> createReservation(
            @RequestParam Long bookId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("预约排队成功", bookReservationService.createReservation(userId, bookId));
    }

    @Operation(summary = "取消预约", description = "取消图书预约排队")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> cancelReservation(
            @PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        bookReservationService.cancelReservation(userId, id);
        return ApiResponse.success("取消成功", null);
    }

    @Operation(summary = "我的预约列表", description = "查询当前用户的图书预约列表")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<BookReservationResponse>> getMyReservations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success(bookReservationService.getMyReservations(userId, current, size));
    }
}
