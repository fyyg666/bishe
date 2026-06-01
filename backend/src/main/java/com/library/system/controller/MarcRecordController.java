package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.MarcRecordResponse;
import com.library.system.service.MarcBookSyncService;
import com.library.system.service.MarcExportService;
import com.library.system.service.MarcImportService;
import com.library.system.service.MarcRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/marc-records")
@RequiredArgsConstructor
@Tag(name = "MARC记录管理", description = "MARC21记录的CRUD、导入导出等")
@SecurityRequirement(name = "bearerAuth")
public class MarcRecordController extends BaseController {

    private final MarcRecordService marcRecordService;
    private final MarcImportService marcImportService;
    private final MarcExportService marcExportService;
    private final MarcBookSyncService marcBookSyncService;

    @Operation(summary = "查询MARC记录列表", description = "按记录类型和关键词筛选MARC记录，支持分页")
    @GetMapping
    public ApiResponse<List<MarcRecordResponse>> listRecords(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(marcRecordService.listRecords(current, size, recordType, keyword));
    }

    @Operation(summary = "获取MARC记录详情", description = "根据ID查询MARC记录详细信息")
    @GetMapping("/{id}")
    public ApiResponse<MarcRecordResponse> getRecord(@PathVariable Long id) {
        return ApiResponse.success(marcRecordService.getRecord(id));
    }

    @Operation(summary = "创建MARC记录", description = "手动创建MARC记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<MarcRecordResponse> createRecord(@RequestBody MarcRecordRequest request) {
        return ApiResponse.success("创建成功", marcRecordService.createRecord(request));
    }

    @Operation(summary = "更新MARC记录", description = "更新MARC记录信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<MarcRecordResponse> updateRecord(
            @PathVariable Long id,
            @RequestBody MarcRecordRequest request) {
        return ApiResponse.success("更新成功", marcRecordService.updateRecord(id, request));
    }

    @Operation(summary = "删除MARC记录", description = "删除MARC记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        marcRecordService.deleteRecord(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "导入MARC文件", description = "上传ISO 2709格式的MARC文件批量导入")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<MarcRecordResponse>> importRecords(
            @RequestParam("file") MultipartFile file) throws Exception {
        log.info("MARC文件导入: fileName={}", file.getOriginalFilename());
        List<MarcRecordResponse> result = marcImportService.importRecords(file.getInputStream());
        return ApiResponse.success("导入完成，成功" + result.size() + "条", result);
    }

    @Operation(summary = "预览MARC导入", description = "预览ISO 2709文件前N条记录")
    @PostMapping("/import/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<MarcRecordResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int count) throws Exception {
        return ApiResponse.success(marcImportService.previewImport(file.getInputStream(), count));
    }

    @Operation(summary = "导出MARC文件", description = "导出选中记录为ISO 2709格式")
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public void exportRecords(@RequestBody List<Long> ids,
                               HttpServletResponse response) throws Exception {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=marc_export.mrc");
        marcExportService.exportRecords(ids, response.getOutputStream());
    }

    @Operation(summary = "图书转MARC记录", description = "将图书信息转换为MARC21编目记录")
    @PostMapping("/sync/book-to-marc/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<MarcRecordResponse> bookToMarc(@PathVariable Long bookId) {
        return ApiResponse.success("转换成功", marcBookSyncService.bookToMarc(bookId));
    }

    @Operation(summary = "MARC记录转图书", description = "将MARC21编目记录转换为图书")
    @PostMapping("/sync/marc-to-book/{marcRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> marcToBook(@PathVariable Long marcRecordId) {
        marcBookSyncService.marcToBook(marcRecordId);
        return ApiResponse.success("转换成功", null);
    }

    @Operation(summary = "从图书同步MARC", description = "将图书最新信息同步到关联的MARC记录")
    @PostMapping("/sync/from-book/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> syncFromBook(@PathVariable Long bookId) {
        marcBookSyncService.syncFromBook(bookId);
        return ApiResponse.success("同步成功", null);
    }

    @Operation(summary = "从MARC同步图书", description = "将MARC记录最新信息同步到关联的图书")
    @PostMapping("/sync/from-marc/{marcRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> syncFromMarc(@PathVariable Long marcRecordId) {
        marcBookSyncService.syncFromMarc(marcRecordId);
        return ApiResponse.success("同步成功", null);
    }
}
