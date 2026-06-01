package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.ReportTemplateRequest;
import com.library.system.dto.ReportTemplateResponse;
import com.library.system.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "自定义报表", description = "自定义报表模板的增删改查和执行导出")
@SecurityRequirement(name = "bearerAuth")
public class ReportTemplateController extends BaseController {

    private final ReportTemplateService reportTemplateService;

    @Operation(summary = "获取报表模板列表", description = "查询报表模板列表，支持按分类筛选")
    @GetMapping("/templates")
    public ApiResponse<List<ReportTemplateResponse>> listTemplates(
            @Parameter(description = "分类筛选")
            @RequestParam(required = false) String category) {
        log.debug("查询报表模板列表: category={}", category);
        return ApiResponse.success(reportTemplateService.listTemplates(category));
    }

    @Operation(summary = "获取报表模板详情", description = "根据ID查询报表模板详情")
    @GetMapping("/templates/{id}")
    public ApiResponse<ReportTemplateResponse> getTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id) {
        log.debug("查询报表模板详情: id={}", id);
        return ApiResponse.success(reportTemplateService.getTemplate(id));
    }

    @Operation(summary = "创建报表模板", description = "创建新的报表模板（需要管理员权限）")
    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportTemplateResponse> createTemplate(
            @RequestBody @Valid ReportTemplateRequest request,
            Authentication authentication) {
        log.info("创建报表模板: {}", request.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("报表模板创建成功", reportTemplateService.createTemplate(request, userId));
    }

    @Operation(summary = "更新报表模板", description = "更新报表模板（需要管理员权限）")
    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportTemplateResponse> updateTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid ReportTemplateRequest request) {
        log.info("更新报表模板: id={}", id);
        return ApiResponse.success("报表模板更新成功", reportTemplateService.updateTemplate(id, request));
    }

    @Operation(summary = "删除报表模板", description = "删除报表模板（需要管理员权限）")
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id) {
        log.info("删除报表模板: id={}", id);
        reportTemplateService.deleteTemplate(id);
        return ApiResponse.success("报表模板删除成功", null);
    }

    @Operation(summary = "执行报表查询", description = "根据模板和参数执行报表查询（需要管理员或馆员权限）")
    @PostMapping("/templates/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Map<String, Object>>> executeTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params) {
        log.info("执行报表查询: id={}", id);
        return ApiResponse.success(reportTemplateService.executeTemplate(id, params != null ? params : Map.of()));
    }

    @Operation(summary = "导出报表", description = "根据模板和参数导出报表为Excel/CSV/PDF（需要管理员或馆员权限）")
    @GetMapping("/templates/{id}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public void exportReport(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "导出格式: excel/csv/pdf")
            @RequestParam(defaultValue = "excel") String format,
            @Parameter(description = "查询参数")
            @RequestParam(required = false) Map<String, Object> params,
            HttpServletResponse response) throws IOException {
        log.info("导出报表: id={}, format={}", id, format);

        Map<String, Object> filteredParams = new HashMap<>(params != null ? params : Map.of());
        filteredParams.remove("format");

        ReportTemplateResponse template = reportTemplateService.getTemplate(id);
        String fileName = URLEncoder.encode(template.getName() + "_" + LocalDate.now(), "UTF-8");

        switch (format.toLowerCase()) {
            case "csv" -> {
                response.setContentType("text/csv; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".csv");
                reportTemplateService.exportToCsv(id, filteredParams, response.getOutputStream());
            }
            case "pdf" -> {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".pdf");
                reportTemplateService.exportToPdf(id, filteredParams, response.getOutputStream());
            }
            default -> {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("utf-8");
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
                reportTemplateService.exportToExcel(id, filteredParams, response.getOutputStream());
            }
        }
    }
}
