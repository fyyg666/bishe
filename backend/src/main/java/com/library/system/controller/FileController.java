package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、访问和删除")
@SecurityRequirement(name = "bearerAuth")
public class FileController extends BaseController {

    private final FileService fileService;

    @Operation(summary = "上传文件", description = "上传文件到对象存储，支持jpg/png/gif/pdf，最大10MB")
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> uploadFile(
            @Parameter(description = "上传文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "存储目录(如covers/avatars)") @RequestParam(defaultValue = "general") String directory) {
        String objectKey = fileService.upload(file, directory);
        String url = fileService.getUrl(objectKey);
        return ApiResponse.success("上传成功", Map.of("objectKey", objectKey, "url", url));
    }

    @Operation(summary = "获取文件访问URL", description = "获取文件的临时访问URL（1小时有效）")
    @GetMapping("/{objectKey}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> getFileUrl(
            @Parameter(description = "文件对象Key", required = true) @PathVariable String objectKey) {
        String url = fileService.getUrl(objectKey);
        return ApiResponse.success(Map.of("url", url));
    }

    @Operation(summary = "删除文件", description = "删除对象存储中的文件（需要管理员权限）")
    @DeleteMapping("/{objectKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "文件对象Key", required = true) @PathVariable String objectKey) {
        fileService.delete(objectKey);
        return ApiResponse.success("删除成功", null);
    }
}
