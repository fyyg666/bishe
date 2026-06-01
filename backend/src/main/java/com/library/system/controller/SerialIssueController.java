package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.SerialIssue;
import com.library.system.service.SerialIssueService;
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
@RequestMapping("/serial/issues")
@RequiredArgsConstructor
@Tag(name = "期刊到刊管理", description = "期刊到刊记录查询、到刊登记、缺刊标记")
@SecurityRequirement(name = "bearerAuth")
public class SerialIssueController extends BaseController {

    private final SerialIssueService issueService;

    @Operation(summary = "获取期刊到刊列表", description = "分页查询某订阅的到刊记录")
    @GetMapping
    public ApiResponse<PageResult<SerialIssue>> listIssues(
            @Parameter(description = "订阅ID", required = true)
            @RequestParam Long subscriptionId,
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size) {
        log.debug("查询期刊到刊列表: subscriptionId={}, current={}, size={}", subscriptionId, current, size);
        return ApiResponse.success(issueService.listIssues(subscriptionId, current, size));
    }

    @Operation(summary = "到刊登记", description = "标记期刊为已到刊（需要管理员权限）")
    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialIssue> receiveIssue(
            @Parameter(description = "到刊记录ID", required = true)
            @PathVariable Long id) {
        log.info("到刊登记: issueId={}", id);
        return ApiResponse.success("到刊登记成功", issueService.receiveIssue(id));
    }

    @Operation(summary = "标记缺刊", description = "标记期刊为缺刊（需要管理员权限）")
    @PostMapping("/{id}/missing")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialIssue> markMissing(
            @Parameter(description = "到刊记录ID", required = true)
            @PathVariable Long id) {
        log.info("标记缺刊: issueId={}", id);
        return ApiResponse.success("缺刊标记成功", issueService.markMissing(id));
    }

    @Operation(summary = "检查逾期到刊", description = "自动标记预期到刊超过30天的为缺刊（需要管理员权限）")
    @PostMapping("/check-overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Integer> checkOverdueIssues() {
        log.info("检查逾期到刊");
        int count = issueService.checkOverdueIssues();
        return ApiResponse.success("检查完成，共标记" + count + "条缺刊", count);
    }
}
