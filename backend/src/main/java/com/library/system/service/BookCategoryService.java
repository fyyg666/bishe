package com.library.system.service;

import com.library.system.entity.BookCategory;

import java.util.List;

public interface BookCategoryService {

    List<BookCategory> listCategories();

    BookCategory createCategory(BookCategory category);

    BookCategory updateCategory(Long id, BookCategory category);

    void deleteCategory(Long id);
}
