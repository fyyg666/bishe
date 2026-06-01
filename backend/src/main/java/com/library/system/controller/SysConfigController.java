package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.SysConfigResponse;
import com.library.system.entity.SysConfig;
import com.library.system.service.SysConfigService;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理")
@SecurityRequirement(name = "bearerAuth")
public class SysConfigController extends BaseController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取所有配置", description = "获取所有系统配置项（需要管理员权限）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SysConfigResponse>> listAll() {
        List<SysConfigResponse> configs = sysConfigService.listAll().stream()
                .map(c -> SysConfigResponse.builder()
                        .configKey(c.getConfigKey())
                        .configValue(c.getConfigValue())
                        .description(c.getDescription())
                        .updateTime(c.getUpdateTime() != null ? c.getUpdateTime().toString() : null)
                        .build())
                .collect(Collectors.toList());
        return ApiResponse.success(configs);
    }

    @Operation(summary = "按key查询配置", description = "根据key查询配置值")
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SysConfigResponse> getByKey(
            @Parameter(description = "配置键", required = true) @PathVariable String key) {
        String value = sysConfigService.getValue(key);
        if (value == null) {
            return ApiResponse.success(null);
        }
        SysConfig config = sysConfigService.listAll().stream()
                .filter(c -> key.equals(c.getConfigKey()))
                .findFirst().orElse(null);
        return ApiResponse.success(SysConfigResponse.builder()
                .configKey(key)
                .configValue(value)
                .description(config != null ? config.getDescription() : null)
                .updateTime(config != null && config.getUpdateTime() != null ? config.getUpdateTime().toString() : null)
                .build());
    }

    @Operation(summary = "更新配置", description = "更新指定key的配置值（需要管理员权限）")
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        String description = body.get("description");
        sysConfigService.setValue(key, value, description);
        return ApiResponse.success("配置更新成功", null);
    }

    @Operation(summary = "删除配置", description = "删除指定key的配置（需要管理员权限）")
    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteConfig(@PathVariable String key) {
        sysConfigService.deleteByKey(key);
        return ApiResponse.success("配置删除成功", null);
    }
}
