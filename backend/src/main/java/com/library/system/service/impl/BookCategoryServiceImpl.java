package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.entity.BookCategory;
import com.library.system.mapper.BookCategoryMapper;
import com.library.system.service.BookCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookCategoryServiceImpl implements BookCategoryService {

    private final BookCategoryMapper bookCategoryMapper;

    @Override
    @Cacheable(value = "categoryCache", key = "'all'")
    public List<BookCategory> listCategories() {
        return bookCategoryMapper.selectList(new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getDeleted, 0));
    }

    @Override
    @CacheEvict(value = "categoryCache", allEntries = true)
    public BookCategory createCategory(BookCategory category) {
        bookCategoryMapper.insert(category);
        return category;
    }

    @Override
    @CacheEvict(value = "categoryCache", allEntries = true)
    public BookCategory updateCategory(Long id, BookCategory category) {
        category.setId(id);
        bookCategoryMapper.updateById(category);
        return category;
    }

    @Override
    @CacheEvict(value = "categoryCache", allEntries = true)
    public void deleteCategory(Long id) {
        bookCategoryMapper.deleteById(id);
    }
}
