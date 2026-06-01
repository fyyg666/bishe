package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.entity.BookCategory;
import com.library.system.service.BookCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "图书分类", description = "图书分类列表查询")
public class BookCategoryController extends BaseController {

    private final BookCategoryService bookCategoryService;

    @Operation(summary = "获取图书分类列表", description = "返回所有图书分类，供图书搜索下拉框使用")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ApiResponse<List<BookCategory>> listCategories() {
        log.debug("查询所有图书分类");
        List<BookCategory> categories = bookCategoryService.listCategories();
        return ApiResponse.success(categories);
    }
}
