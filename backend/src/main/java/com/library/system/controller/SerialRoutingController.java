package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.SerialRoutingRequest;
import com.library.system.dto.SerialRoutingResponse;
import com.library.system.dto.SerialRoutingTemplateRequest;
import com.library.system.entity.SerialRoutingTemplate;
import com.library.system.service.SerialRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/serial-routings")
@RequiredArgsConstructor
@Tag(name = "期刊路由分发管理", description = "期刊路由分发的创建、发出、签收等操作")
@SecurityRequirement(name = "bearerAuth")
public class SerialRoutingController extends BaseController {

    private final SerialRoutingService routingService;

    @Operation(summary = "获取期刊路由分发列表", description = "分页查询路由分发记录，支持按状态和目标筛选")
    @GetMapping
    public ApiResponse<PageResult<SerialRoutingResponse>> listRoutings(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "分发状态")
            @RequestParam(required = false) String routingStatus,
            @Parameter(description = "分发目标")
            @RequestParam(required = false) String destination) {
        log.debug("查询期刊路由分发列表: current={}, size={}, routingStatus={}, destination={}", current, size, routingStatus, destination);
        return ApiResponse.success(routingService.listRoutings(current, size, routingStatus, destination));
    }

    @Operation(summary = "获取路由分发详情", description = "根据ID获取路由分发记录详情")
    @GetMapping("/{id}")
    public ApiResponse<SerialRoutingResponse> getRouting(
            @Parameter(description = "路由分发ID", required = true)
            @PathVariable Long id) {
        log.debug("获取路由分发详情: id={}", id);
        return ApiResponse.success(routingService.getRouting(id));
    }

    @Operation(summary = "创建路由分发", description = "手动创建一条路由分发记录（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialRoutingResponse> createRouting(@RequestBody SerialRoutingRequest request) {
        log.info("创建路由分发: subscriptionId={}", request.getSubscriptionId());
        return ApiResponse.success("创建成功", routingService.createRouting(request));
    }

    @Operation(summary = "批量创建路由分发", description = "根据模板批量创建路由分发记录（需要管理员权限）")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> batchCreateRoutings(@RequestBody Map<String, Long> body) {
        Long issueId = body.get("issueId");
        Long subscriptionId = body.get("subscriptionId");
        log.info("批量创建路由分发: issueId={}, subscriptionId={}", issueId, subscriptionId);
        routingService.batchCreateRoutings(issueId, subscriptionId);
        return ApiResponse.success("批量创建成功", null);
    }

    @Operation(summary = "发出路由分发", description = "将路由分发标记为发出状态（需要管理员权限）")
    @PutMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialRoutingResponse> sendRouting(
            @Parameter(description = "路由分发ID", required = true)
            @PathVariable Long id) {
        log.info("发出路由分发: id={}", id);
        return ApiResponse.success("发出成功", routingService.sendRouting(id));
    }

    @Operation(summary = "签收路由分发", description = "将路由分发标记为已签收状态（需要管理员权限）")
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialRoutingResponse> deliverRouting(
            @Parameter(description = "路由分发ID", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String receivedBy = body.get("receivedBy");
        log.info("签收路由分发: id={}, receivedBy={}", id, receivedBy);
        return ApiResponse.success("签收成功", routingService.deliverRouting(id, receivedBy));
    }

    @Operation(summary = "删除路由分发", description = "删除一条路由分发记录（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteRouting(
            @Parameter(description = "路由分发ID", required = true)
            @PathVariable Long id) {
        log.info("删除路由分发: id={}", id);
        routingService.deleteRouting(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "获取路由分发模板列表", description = "查询某订阅的路由分发模板")
    @GetMapping("/templates")
    public ApiResponse<List<SerialRoutingTemplate>> listTemplates(
            @Parameter(description = "订阅ID", required = true)
            @RequestParam Long subscriptionId) {
        log.debug("查询路由分发模板: subscriptionId={}", subscriptionId);
        return ApiResponse.success(routingService.listTemplates(subscriptionId));
    }

    @Operation(summary = "创建路由分发模板", description = "创建一条路由分发模板（需要管理员权限）")
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialRoutingTemplate> createTemplate(@RequestBody SerialRoutingTemplateRequest request) {
        log.info("创建路由分发模板: subscriptionId={}", request.getSubscriptionId());
        return ApiResponse.success("创建成功", routingService.createTemplate(request));
    }

    @Operation(summary = "删除路由分发模板", description = "删除一条路由分发模板（需要管理员权限）")
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id) {
        log.info("删除路由分发模板: id={}", id);
        routingService.deleteTemplate(id);
        return ApiResponse.success("删除成功", null);
    }
}
