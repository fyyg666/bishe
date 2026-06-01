package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告控制器
 * <p>
 * 处理公告的CRUD和查询操作，业务逻辑委托给 {@link AnnouncementService}。
 * 新增、修改、发布、删除操作需要ADMIN或LIBRARIAN角色权限。
 * FIXED: ARCH-003 移除直接注入UserMapper，改用ReaderService.findByUsername
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Tag(name = "公告管理", description = "系统公告的增删改查和发布操作")
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController extends BaseController {

    private final AnnouncementService announcementService;

    /**
     * 获取公告列表
     */
    @Operation(summary = "获取公告列表", description = "分页查询公告列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public ApiResponse<PageResult<AnnouncementResponse>> listAnnouncements(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词搜索")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) String status) {
        log.debug("查询公告列表: current={}, size={}, keyword={}, status={}",
                current, size, keyword, status);
        return ApiResponse.success(announcementService.listAnnouncements(current, size, keyword, status));
    }

    /**
     * 获取公告详情
     */
    @Operation(summary = "获取公告详情", description = "根据ID查询公告详情")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<AnnouncementResponse> getAnnouncementById(
            @Parameter(description = "公告ID", required = true)
            @PathVariable Long id) {
        log.debug("查询公告详情: id={}", id);
        return ApiResponse.success(announcementService.getAnnouncementById(id));
    }

    /**
     * 获取最新公告
     */
    @Operation(summary = "获取最新公告", description = "获取最新发布的公告列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/latest")
    public ApiResponse<List<AnnouncementResponse>> getLatestAnnouncements(
            @Parameter(description = "返回数量（默认5）")
            @RequestParam(defaultValue = "5") Integer limit) {
        log.debug("查询最新公告: limit={}", limit);
        return ApiResponse.success(announcementService.getLatestAnnouncements(limit));
    }

    /**
     * 新增公告
     */
    @Operation(summary = "新增公告", description = "创建新公告（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "announcements", allEntries = true)
    public ApiResponse<AnnouncementResponse> createAnnouncement(
            @Parameter(description = "公告创建请求体", required = true)
            @RequestBody @Valid AnnouncementRequest request,
            Authentication authentication) {
        log.info("新增公告: {}", request.getTitle());
        return ApiResponse.success("公告创建成功",
                announcementService.createAnnouncement(request, authentication.getName()));
    }

    /**
     * 更新公告
     */
    @Operation(summary = "更新公告", description = "更新公告信息（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "announcements", allEntries = true)
    public ApiResponse<AnnouncementResponse> updateAnnouncement(
            @Parameter(description = "公告ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "公告更新请求体", required = true)
            @RequestBody @Valid AnnouncementRequest request) {
        log.info("更新公告: id={}", id);
        return ApiResponse.success("公告更新成功", announcementService.updateAnnouncement(id, request));
    }

    /**
     * 发布公告
     */
    @Operation(summary = "发布公告", description = "发布指定公告（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发布成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作")
    })
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "announcements", allEntries = true)
    public ApiResponse<Void> publishAnnouncement(
            @Parameter(description = "公告ID", required = true)
            @PathVariable Long id) {
        log.info("发布公告: id={}", id);
        announcementService.publishAnnouncement(id);
        return ApiResponse.success("公告发布成功", null);
    }

    /**
     * 删除公告
     */
    @Operation(summary = "删除公告", description = "删除公告（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "announcements", allEntries = true)
    public ApiResponse<Void> deleteAnnouncement(
            @Parameter(description = "公告ID", required = true)
            @PathVariable Long id) {
        log.info("删除公告: id={}", id);
        announcementService.deleteAnnouncement(id);
        return ApiResponse.success("公告删除成功", null);
    }
}
