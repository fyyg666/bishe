package com.library.system.controller;

import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅控制器
 * <p>
 * 处理图书借阅、归还、续借等操作，以及借阅记录的查询。
 * 借阅/归还/续借操作需要READER角色权限。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
@Tag(name = "借阅管理", description = "图书借阅、归还、续借和借阅记录查询")
@SecurityRequirement(name = "bearerAuth")
public class BorrowController extends BaseController { // FIXED: ARCH-002 继承BaseController

    private final BorrowService borrowService;

    /**
     * 借阅图书
     */
    @Operation(summary = "借阅图书", description = "借阅图书，需要READER角色")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "借阅成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = BorrowResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权借阅"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<BorrowResponse> borrowBook(
            @Parameter(description = "借阅请求体", required = true)
            @Valid @RequestBody BorrowRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("借阅图书: userId={}, bookId={}", userId, request.getBookId());
        BorrowResponse response = borrowService.borrowBook(userId, request);
        return ApiResponse.success("借阅成功", response);
    }

    /**
     * 归还图书
     */
    @Operation(summary = "归还图书", description = "归还已借阅的图书")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "归还成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "借阅记录不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权归还")
    })
    @PostMapping("/{borrowId}/return")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<BorrowResponse> returnBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long borrowId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("归还图书: userId={}, borrowId={}", userId, borrowId);
        BorrowResponse response = borrowService.returnBook(userId, borrowId);
        return ApiResponse.success("归还成功", response);
    }

    /**
     * 续借图书
     */
    @Operation(summary = "续借图书", description = "续借已借阅的图书，可指定续借天数")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "续借成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "续借失败（如已超期）"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权续借")
    })
    @PostMapping("/{borrowId}/renew")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<BorrowResponse> renewBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long borrowId,
            @Parameter(description = "续借天数（可选，默认15天）")
            @RequestParam(required = false) Integer days,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("续借图书: userId={}, borrowId={}, days={}", userId, borrowId, days);
        BorrowResponse response = borrowService.renewBook(userId, borrowId, days);
        return ApiResponse.success("续借成功", response);
    }

    /**
     * 获取我的借阅列表
     */
    @Operation(summary = "获取我的借阅列表", description = "分页查询当前用户的借阅记录")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<BorrowResponse>> getMyBorrows(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "借阅状态筛选（可选）")
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.debug("查询我的借阅: userId={}, current={}, size={}, status={}",
                userId, current, size, status);
        PageResult<BorrowResponse> result = borrowService.getMyBorrows(userId, current, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 获取所有借阅列表（管理员用）
     */
    @Operation(summary = "获取所有借阅列表", description = "分页查询所有借阅记录（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<BorrowResponse>> listAllBorrows(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "借阅状态筛选（可选）")
            @RequestParam(required = false) String status) {
        log.debug("查询所有借阅: current={}, size={}, status={}", current, size, status);
        PageResult<BorrowResponse> result = borrowService.getAllBorrows(current, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 获取借阅详情
     */
    @Operation(summary = "获取借阅详情", description = "根据ID查询借阅记录详情")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该记录"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "借阅记录不存在")
    })
    @GetMapping("/{borrowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'READER')")
    public ApiResponse<BorrowResponse> getBorrowById(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long borrowId,
            Authentication authentication) {
        log.debug("查询借阅详情: borrowId={}", borrowId);

        Long currentUserId = getUserIdFromAuthentication(authentication);
        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace(Constants.Token.ROLE_PREFIX, "")) 
                .orElse("READER");

        BorrowResponse response = borrowService.getBorrowByIdWithOwnershipCheck(borrowId, currentUserId, currentRole);
        return ApiResponse.success(response);
    }

    /**
     * 检查是否有逾期未还图书
     */
    @Operation(summary = "检查逾期", description = "检查当前用户是否有逾期未还的图书")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/check-overdue")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<Boolean> checkOverdue(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        boolean hasOverdue = borrowService.hasOverdueBooks(userId);
        return ApiResponse.success(hasOverdue);
    }
}
