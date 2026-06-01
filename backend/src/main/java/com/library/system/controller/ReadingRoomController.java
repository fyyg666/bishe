package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.entity.ReadingRoom;
import com.library.system.mapper.ReadingRoomMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 阅览室控制器
 * <p>
 * 提供阅览室列表查询接口。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/reading-rooms")
@RequiredArgsConstructor
@Tag(name = "阅览室管理", description = "阅览室列表查询")
public class ReadingRoomController extends BaseController {

    private final ReadingRoomMapper readingRoomMapper;

    /**
     * 获取所有阅览室列表
     */
    @Operation(summary = "获取阅览室列表", description = "返回所有阅览室信息")
    @GetMapping
    public ApiResponse<List<ReadingRoom>> listReadingRooms() {
        log.debug("查询所有阅览室");
        List<ReadingRoom> rooms = readingRoomMapper.selectList(null);
        return ApiResponse.success(rooms);
    }
}
