package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.SerialSubscription;
import com.library.system.service.SerialSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/serial/subscriptions")
@RequiredArgsConstructor
@Tag(name = "期刊订阅管理", description = "期刊订阅的增删改查及预期到刊生成")
@SecurityRequirement(name = "bearerAuth")
public class SerialSubscriptionController extends BaseController {

    private final SerialSubscriptionService subscriptionService;

    @Operation(summary = "获取期刊订阅列表", description = "分页查询期刊订阅列表")
    @GetMapping
    public ApiResponse<PageResult<SerialSubscription>> listSubscriptions(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) String status) {
        log.debug("查询期刊订阅列表: current={}, size={}, status={}", current, size, status);
        return ApiResponse.success(subscriptionService.listSubscriptions(current, size, status));
    }

    @Operation(summary = "获取期刊订阅详情", description = "根据ID查询期刊订阅详情")
    @GetMapping("/{id}")
    public ApiResponse<SerialSubscription> getSubscription(
            @Parameter(description = "订阅ID", required = true)
            @PathVariable Long id) {
        log.debug("查询期刊订阅详情: id={}", id);
        return ApiResponse.success(subscriptionService.getSubscription(id));
    }

    @Operation(summary = "新增期刊订阅", description = "创建新期刊订阅（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialSubscription> createSubscription(@RequestBody SerialSubscription subscription) {
        log.info("新增期刊订阅: {}", subscription.getTitle());
        return ApiResponse.success("期刊订阅创建成功", subscriptionService.createSubscription(subscription));
    }

    @Operation(summary = "更新期刊订阅", description = "更新期刊订阅信息（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialSubscription> updateSubscription(
            @Parameter(description = "订阅ID", required = true)
            @PathVariable Long id,
            @RequestBody SerialSubscription subscription) {
        log.info("更新期刊订阅: id={}", id);
        return ApiResponse.success("期刊订阅更新成功", subscriptionService.updateSubscription(id, subscription));
    }

    @Operation(summary = "删除期刊订阅", description = "删除期刊订阅（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteSubscription(
            @Parameter(description = "订阅ID", required = true)
            @PathVariable Long id) {
        log.info("删除期刊订阅: id={}", id);
        subscriptionService.deleteSubscription(id);
        return ApiResponse.success("期刊订阅删除成功", null);
    }

    @Operation(summary = "生成预期到刊", description = "根据订阅频率和日期范围生成预期到刊记录")
    @PostMapping("/{id}/generate-issues")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> generateExpectedIssues(
            @Parameter(description = "订阅ID", required = true)
            @PathVariable Long id) {
        log.info("生成预期到刊: subscriptionId={}", id);
        subscriptionService.generateExpectedIssues(id);
        return ApiResponse.success("预期到刊生成成功", null);
    }
}
