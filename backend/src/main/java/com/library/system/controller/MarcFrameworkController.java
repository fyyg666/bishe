package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.MarcFrameworkResponse;
import com.library.system.service.MarcFrameworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/marc/frameworks")
@RequiredArgsConstructor
@Tag(name = "MARC编目框架管理", description = "MARC21编目框架模板的CRUD管理")
@SecurityRequirement(name = "bearerAuth")
public class MarcFrameworkController extends BaseController {

    private final MarcFrameworkService marcFrameworkService;

    @Operation(summary = "查询框架列表", description = "获取所有MARC编目框架")
    @GetMapping
    public ApiResponse<List<MarcFrameworkResponse>> listFrameworks() {
        return ApiResponse.success(marcFrameworkService.listFrameworks());
    }

    @Operation(summary = "获取框架详情", description = "根据ID查询MARC编目框架详细信息")
    @GetMapping("/{id}")
    public ApiResponse<MarcFrameworkResponse> getFramework(@PathVariable Long id) {
        return ApiResponse.success(marcFrameworkService.getFramework(id));
    }

    @Operation(summary = "按代码查询框架", description = "根据框架代码查询MARC编目框架")
    @GetMapping("/code/{code}")
    public ApiResponse<MarcFrameworkResponse> getFrameworkByCode(@PathVariable String code) {
        return ApiResponse.success(marcFrameworkService.getFrameworkByCode(code));
    }

    @Operation(summary = "创建框架", description = "创建MARC编目框架")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<MarcFrameworkResponse> createFramework(@RequestBody MarcFrameworkResponse request) {
        return ApiResponse.success("创建成功", marcFrameworkService.createFramework(request));
    }

    @Operation(summary = "更新框架", description = "更新MARC编目框架信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<MarcFrameworkResponse> updateFramework(
            @PathVariable Long id,
            @RequestBody MarcFrameworkResponse request) {
        return ApiResponse.success("更新成功", marcFrameworkService.updateFramework(id, request));
    }

    @Operation(summary = "删除框架", description = "删除MARC编目框架")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteFramework(@PathVariable Long id) {
        marcFrameworkService.deleteFramework(id);
        return ApiResponse.success("删除成功", null);
    }
}
