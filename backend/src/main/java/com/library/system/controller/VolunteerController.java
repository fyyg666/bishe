package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.dto.VolunteerStatsDto;
import com.library.system.service.VolunteerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 志愿服务控制器
 * <p>
 * 处理志愿服务的CRUD和审核操作，业务逻辑委托给 {@link VolunteerService}。
 * 审核操作需要ADMIN或LIBRARIAN角色权限。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/volunteers")
@RequiredArgsConstructor
@Tag(name = "志愿服务管理", description = "志愿服务申请的增删改查和审核操作")
@SecurityRequirement(name = "bearerAuth")
public class VolunteerController {

    private final VolunteerService volunteerService;

    /**
     * 获取志愿服务列表
     */
    @Operation(summary = "获取志愿服务列表", description = "分页查询志愿服务记录")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public ApiResponse<PageResult<VolunteerResponse>> listVolunteers(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态筛选（可选）")
            @RequestParam(required = false) String status) {
        log.debug("查询志愿服务记录: current={}, size={}, status={}", current, size, status);
        return ApiResponse.success(volunteerService.listVolunteers(current, size, status));
    }

    /**
     * 获取我的志愿服务
     */
    @Operation(summary = "获取我的志愿服务", description = "查询当前用户的志愿服务记录")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/my")
    public ApiResponse<PageResult<VolunteerResponse>> getMyVolunteers(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            Authentication authentication) {
        log.debug("查询我的志愿服务记录: current={}, size={}", current, size);
        // FIXED: 从JWT的principal中直接获取userId（authentication.getName()返回的是userId字符串）
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return ApiResponse.success(volunteerService.getMyVolunteers(current, size, userId));
    }

    /**
     * 获取志愿服务详情
     */
    @Operation(summary = "获取志愿服务详情", description = "根据ID查询志愿服务详情")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "志愿服务记录不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<VolunteerResponse> getVolunteerById(
            @Parameter(description = "志愿服务ID", required = true)
            @PathVariable Long id) {
        log.debug("查询志愿服务详情: id={}", id);
        return ApiResponse.success(volunteerService.getVolunteerById(id));
    }

    /**
     * 申请志愿服务
     */
    @Operation(summary = "申请志愿服务", description = "提交志愿服务申请")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申请成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'READER', 'VOLUNTEER')")
    public ApiResponse<VolunteerResponse> createVolunteer(
            @Parameter(description = "志愿服务申请请求体", required = true)
            @RequestBody @Valid VolunteerRequest request,
            Authentication authentication) {
        log.info("申请志愿服务");
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return ApiResponse.success("志愿服务申请成功",
                volunteerService.createVolunteer(userId, request));
    }

    /**
     * 更新志愿服务
     */
    @Operation(summary = "更新志愿服务", description = "更新志愿服务记录")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权修改"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PutMapping("/{id}")
    public ApiResponse<VolunteerResponse> updateVolunteer(
            @Parameter(description = "志愿服务ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新请求体", required = true)
            @RequestBody @Valid VolunteerRequest request,
            Authentication authentication) {
        log.info("更新志愿服务记录: id={}", id);
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return ApiResponse.success("志愿服务记录更新成功",
                volunteerService.updateVolunteer(id, userId, request));
    }

    /**
     * 取消志愿服务
     */
    @Operation(summary = "取消志愿服务申请", description = "取消已提交的志愿服务申请")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权取消")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelVolunteer(
            @Parameter(description = "志愿服务ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        log.info("取消志愿服务: id={}", id);
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        volunteerService.cancelVolunteer(id, userId);
        return ApiResponse.success("志愿服务申请已取消", null);
    }

    /**
     * 审核志愿服务
     */
    @Operation(summary = "审核志愿服务", description = "审核志愿服务申请（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权审核")
    })
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<VolunteerResponse> reviewVolunteer(
            @Parameter(description = "志愿服务ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "是否通过", required = true)
            @RequestParam Boolean approved,
            @Parameter(description = "审核备注")
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        log.info("审核志愿服务: id={}, approved={}", id, approved);
        Long reviewerId = Long.valueOf(authentication.getPrincipal().toString());
        return ApiResponse.success("志愿服务审核完成",
                volunteerService.reviewVolunteer(id, reviewerId, approved, remark));
    }

    /**
     * 获取待审核列表
     */
    @Operation(summary = "获取待审核志愿服务", description = "查询待审核的志愿服务列表（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<VolunteerResponse>> getPendingVolunteers(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size) {
        log.debug("查询待审核志愿服务: current={}, size={}", current, size);
        return ApiResponse.success(volunteerService.getPendingVolunteers(current, size));
    }

    /**
     * 删除志愿服务记录
     */
    @Operation(summary = "删除志愿服务记录", description = "删除志愿服务记录（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权删除")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteVolunteer(
            @Parameter(description = "志愿服务ID", required = true)
            @PathVariable Long id) {
        log.info("删除志愿服务记录: id={}", id);
        volunteerService.deleteVolunteer(id);
        return ApiResponse.success("志愿服务记录删除成功", null);
    }

    /**
     * 获取志愿服务统计
     */
    @Operation(summary = "获取志愿服务统计", description = "获取当前用户的志愿服务统计信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/stats")
    public ApiResponse<VolunteerStatsDto> getVolunteerStats(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return ApiResponse.success(volunteerService.getVolunteerStats(userId));
    }
}
