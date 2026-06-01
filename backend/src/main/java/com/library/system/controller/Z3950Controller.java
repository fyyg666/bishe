package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.Z3950SearchResult;
import com.library.system.entity.Z3950Source;
import com.library.system.service.Z3950Service;
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
@RequestMapping("/z3950")
@RequiredArgsConstructor
@Tag(name = "Z39.50联机编目", description = "Z39.50远程编目数据源管理和检索")
@SecurityRequirement(name = "bearerAuth")
public class Z3950Controller extends BaseController {

    private final Z3950Service z3950Service;

    @Operation(summary = "获取数据源列表")
    @GetMapping("/sources")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Z3950Source>> listSources() {
        return ApiResponse.success(z3950Service.listSources());
    }

    @Operation(summary = "创建数据源")
    @PostMapping("/sources")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Z3950Source> createSource(@RequestBody Z3950Source source) {
        return ApiResponse.success("创建成功", z3950Service.createSource(source));
    }

    @Operation(summary = "更新数据源")
    @PutMapping("/sources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Z3950Source> updateSource(@PathVariable Long id, @RequestBody Z3950Source source) {
        return ApiResponse.success("更新成功", z3950Service.updateSource(id, source));
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/sources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSource(@PathVariable Long id) {
        z3950Service.deleteSource(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "检索远程数据源")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Z3950SearchResult> search(
            @RequestParam Long sourceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "keyword") String queryType,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ApiResponse.success(z3950Service.search(sourceId, query, queryType, maxResults));
    }

    @Operation(summary = "检索所有数据源")
    @GetMapping("/search-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Z3950SearchResult>> searchAll(
            @RequestParam String query,
            @RequestParam(defaultValue = "keyword") String queryType,
            @RequestParam(defaultValue = "10") int maxResults) {
        return ApiResponse.success(z3950Service.searchAll(query, queryType, maxResults));
    }

    @Operation(summary = "导入到本地编目")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> importToMarc(
            @RequestParam Long sourceId,
            @RequestParam String query,
            @RequestParam(defaultValue = "keyword") String queryType) {
        z3950Service.importToMarc(sourceId, query, queryType);
        return ApiResponse.success("导入成功", null);
    }
}
