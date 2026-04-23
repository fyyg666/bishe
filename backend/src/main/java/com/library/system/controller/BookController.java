package com.library.system.controller;

import com.library.system.annotation.AuditLog;
import com.library.system.dto.*;
import com.library.system.service.BookService;
import io.swagger.v3.oas.annotations.jakarta.Operation;
import io.swagger.v3.oas.annotations.jakarta.Parameter;
import io.swagger.v3.oas.annotations.jakarta.media.Content;
import io.swagger.v3.oas.annotations.jakarta.media.Schema;
import io.swagger.v3.oas.annotations.jakarta.responses.ApiResponse;
import io.swagger.v3.oas.annotations.jakarta.responses.ApiResponses;
import io.swagger.v3.oas.annotations.jakarta.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书控制器
 * <p>
 * 处理图书的CRUD和查询操作，包括分页查询、热门图书、ISBN去重检查等。
 * 新增、修改、删除操作需要ADMIN或LIBRARIAN角色权限。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "图书管理", description = "图书的CRUD、分页查询、热门图书推荐等")
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询图书列表
     */
    @Operation(summary = "分页查询图书列表", description = "支持按关键词（标题/作者/ISBN）和分类筛选")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResult.class))),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping
    public ApiResponse<PageResult<BookResponse>> listBooks(
            @Parameter(description = "当前页（默认1）") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词（标题/作者/ISBN）") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId) {
        log.debug("查询图书列表: current={}, size={}, keyword={}, categoryId={}",
                current, size, keyword, categoryId);
        PageResult<BookResponse> result = bookService.listBooks(current, size, keyword, categoryId);
        return ApiResponse.success(result);
    }

    /**
     * 获取热门图书
     */
    @Operation(summary = "获取热门图书", description = "按借阅次数排序，返回热门图书列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/hot")
    public ApiResponse<List<BookResponse>> getHotBooks(
            @Parameter(description = "数量限制（默认10）") @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("查询热门图书: limit={}", limit);
        List<BookResponse> books = bookService.getHotBooks(limit);
        return ApiResponse.success(books);
    }

    /**
     * 获取图书详情
     */
    @Operation(summary = "获取图书详情", description = "根据图书ID查询详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<BookResponse> getBookById(
            @Parameter(description = "图书ID", required = true) @PathVariable Long id) {
        log.debug("查询图书详情: id={}", id);
        BookResponse book = bookService.getBookById(id);
        return ApiResponse.success(book);
    }

    /**
     * 新增图书（需要图书管理员权限）
     */
    @Operation(summary = "新增图书", description = "新增图书记录，需要ADMIN或LIBRARIAN角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @AuditLog(module = "图书管理", operation = "新增图书")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BookResponse> createBook(
            @Parameter(description = "图书请求体", required = true) @Valid @RequestBody BookRequest request) {
        log.info("新增图书: {}", request.getTitle());
        BookResponse book = bookService.createBook(request);
        return ApiResponse.success("图书创建成功", book);
    }

    /**
     * 更新图书（需要图书管理员权限）
     */
    @Operation(summary = "更新图书", description = "更新图书信息，需要ADMIN或LIBRARIAN角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @AuditLog(module = "图书管理", operation = "更新图书")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BookResponse> updateBook(
            @Parameter(description = "图书ID", required = true) @PathVariable Long id,
            @Parameter(description = "图书请求体", required = true) @Valid @RequestBody BookRequest request) {
        log.info("更新图书: id={}", id);
        BookResponse book = bookService.updateBook(id, request);
        return ApiResponse.success("图书更新成功", book);
    }

    /**
     * 删除图书（需要图书管理员权限）
     */
    @Operation(summary = "删除图书", description = "删除图书记录，需要ADMIN或LIBRARIAN角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @AuditLog(module = "图书管理", operation = "删除图书")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteBook(
            @Parameter(description = "图书ID", required = true) @PathVariable Long id) {
        log.info("删除图书: id={}", id);
        bookService.deleteBook(id);
        return ApiResponse.success("图书删除成功", null);
    }

    /**
     * 检查ISBN是否存在
     */
    @Operation(summary = "检查ISBN是否存在", description = "检查指定ISBN是否已被使用")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/check-isbn")
    public ApiResponse<Boolean> checkIsbn(
            @Parameter(description = "ISBN号", required = true) @RequestParam String isbn) {
        boolean exists = bookService.isIsbnExists(isbn);
        return ApiResponse.success(exists);
    }
}
