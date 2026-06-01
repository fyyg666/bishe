package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.service.AcquisitionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/acquisition")
@RequiredArgsConstructor
@Tag(name = "采购编目", description = "采购入库到编目系统的集成接口")
@SecurityRequirement(name = "bearerAuth")
public class AcquisitionController extends BaseController {

    private final AcquisitionSyncService acquisitionSyncService;

    @Operation(summary = "采购明细入库到编目", description = "将已收货的采购明细入库到图书编目系统")
    @PostMapping("/items/{itemId}/catalog")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> receiveToCatalog(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "1") int quantity) {
        log.info("采购入库请求: itemId={}, quantity={}", itemId, quantity);
        acquisitionSyncService.receiveToCatalog(itemId, quantity);
        return ApiResponse.success("入库成功", null);
    }
}
