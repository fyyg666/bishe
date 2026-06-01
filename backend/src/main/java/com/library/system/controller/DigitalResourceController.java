package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.DigitalResource;
import com.library.system.service.DigitalResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/digital-resources")
@RequiredArgsConstructor
@Tag(name = "数字资源管理", description = "数字资源的CRUD和检索")
@SecurityRequirement(name = "bearerAuth")
public class DigitalResourceController extends BaseController {

    private final DigitalResourceService digitalResourceService;

    @Operation(summary = "分页查询数字资源")
    @GetMapping
    public ApiResponse<PageResult<DigitalResource>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType) {
        PageResult<DigitalResource> result = digitalResourceService.list(current, size, keyword, resourceType);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取数字资源详情")
    @GetMapping("/{id}")
    public ApiResponse<DigitalResource> getById(@PathVariable Long id) {
        DigitalResource resource = digitalResourceService.getById(id);
        return ApiResponse.success(resource);
    }

    @Operation(summary = "新增数字资源")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<DigitalResource> create(@RequestBody DigitalResource resource) {
        log.info("新增数字资源: {}", resource.getTitle());
        DigitalResource created = digitalResourceService.create(resource);
        return ApiResponse.success("数字资源创建成功", created);
    }

    @Operation(summary = "更新数字资源")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<DigitalResource> update(@PathVariable Long id, @RequestBody DigitalResource resource) {
        log.info("更新数字资源: id={}", id);
        DigitalResource updated = digitalResourceService.update(id, resource);
        return ApiResponse.success("数字资源更新成功", updated);
    }

    @Operation(summary = "删除数字资源")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除数字资源: id={}", id);
        digitalResourceService.delete(id);
        return ApiResponse.success("数字资源删除成功", null);
    }

    @Operation(summary = "搜索数字资源")
    @GetMapping("/search")
    public ApiResponse<java.util.List<DigitalResource>> search(@RequestParam String keyword) {
        return ApiResponse.success(digitalResourceService.search(keyword));
    }
}
