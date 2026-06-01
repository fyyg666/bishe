package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.BookService;
import com.library.system.service.IsbnLookupService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@SecurityRequirement(name = "bearerAuth")
public class BookController extends BaseController {

    private final BookService bookService;
    private final IsbnLookupService isbnLookupService;

    /**
     * 分页查询图书列表
     */
    @Operation(summary = "分页查询图书列表", description = "支持按关键词（标题/作者/ISBN）和分类筛选")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResult.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping
    public ApiResponse<PageResult<BookResponse>> listBooks(
            @Parameter(description = "当前页（默认1）") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词（标题/作者/ISBN）") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "作者") @RequestParam(required = false) String author) {
        log.debug("查询图书列表: current={}, size={}, keyword={}, categoryId={}, author={}",
                current, size, keyword, categoryId, author);
        PageResult<BookResponse> result = bookService.listBooks(current, size, keyword, categoryId, author);
        return ApiResponse.success(result);
    }

    /**
     * 获取热门图书
     */
    @Operation(summary = "获取热门图书", description = "按借阅次数排序，返回热门图书列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/hot")
    public ApiResponse<List<BookResponse>> getHotBooks(
            @Parameter(description = "数量限制（默认10）") @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("查询热门图书: limit={}", limit);
        List<BookResponse> books = bookService.getHotBooks(limit);
        return ApiResponse.success(books);
    }

    /**
     * 获取新书推荐
     */
    @Operation(summary = "获取新书推荐", description = "按上架时间排序，返回最新图书列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/new")
    public ApiResponse<List<BookResponse>> getNewBooks(
            @Parameter(description = "数量限制（默认10）") @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("查询新书: limit={}", limit);
        List<BookResponse> books = bookService.getNewBooks(limit);
        return ApiResponse.success(books);
    }

    /**
     * 获取图书详情
     */
    @Operation(summary = "获取图书详情", description = "根据图书ID查询详细信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "图书不存在")
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "图书不存在")
    })
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "图书不存在")
    })
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/check-isbn")
    public ApiResponse<Boolean> checkIsbn(
            @Parameter(description = "ISBN号", required = true) @RequestParam String isbn) {
        boolean exists = bookService.isIsbnExists(isbn);
        return ApiResponse.success(exists);
    }

    @Operation(summary = "ISBN查询书目数据", description = "通过ISBN从Open Library/Google Books查询书目信息")
    @GetMapping("/isbn-lookup")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IsbnLookupResponse> lookupIsbn(
            @Parameter(description = "ISBN号", required = true) @RequestParam String isbn) {
        return ApiResponse.success(isbnLookupService.lookup(isbn).orElse(null));
    }

    @Operation(summary = "导出图书列表Excel", description = "导出图书列表为Excel文件（需要管理员权限）")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public void exportBooks(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = java.net.URLEncoder.encode("图书列表_" + java.time.LocalDate.now(), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
        List<BookExportDTO> data = bookService.getExportData(keyword, categoryId);
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), BookExportDTO.class)
                .autoCloseStream(false)
                .sheet("图书列表")
                .doWrite(data);
    }

    @Operation(summary = "批量导入图书", description = "从Excel文件批量导入图书数据（需要管理员权限）")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<ImportResultDTO> importBooks(
            @Parameter(description = "Excel文件", required = true) @RequestParam("file") MultipartFile file) throws java.io.IOException {
        log.info("批量导入图书: fileName={}", file.getOriginalFilename());
        ImportResultDTO result = bookService.importBooks(file.getInputStream());
        return ApiResponse.success(result);
    }

    @Operation(summary = "高级组合检索", description = "多条件组合检索图书")
    @GetMapping("/advanced-search")
    public ApiResponse<PageResult<BookResponse>> advancedSearch(
            @Parameter(description = "书名") @RequestParam(required = false) String title,
            @Parameter(description = "作者") @RequestParam(required = false) String author,
            @Parameter(description = "ISBN") @RequestParam(required = false) String isbn,
            @Parameter(description = "出版社") @RequestParam(required = false) String publisher,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "出版日期起") @RequestParam(required = false) String publishDateStart,
            @Parameter(description = "出版日期止") @RequestParam(required = false) String publishDateEnd,
            @Parameter(description = "排序方式: default/borrowCount/date") @RequestParam(defaultValue = "default") String orderBy,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return ApiResponse.success(bookService.advancedSearch(current, size, title, author, isbn,
                publisher, categoryId, publishDateStart, publishDateEnd, orderBy));
    }

    @Operation(summary = "分类聚合", description = "获取图书分类聚合数据（面状导航）")
    @GetMapping("/facets/categories")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getCategoryFacet() {
        return ApiResponse.success(bookService.getCategoryFacet());
    }

    @Operation(summary = "作者聚合", description = "获取图书作者聚合数据（面状导航TOP10）")
    @GetMapping("/facets/authors")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getAuthorFacet() {
        return ApiResponse.success(bookService.getAuthorFacet());
    }
}
