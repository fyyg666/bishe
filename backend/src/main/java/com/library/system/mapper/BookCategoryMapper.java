package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.BookCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书分类Mapper接口
 *
 * @author Library Team
 * @version 2.0.0
 */
@Mapper
public interface BookCategoryMapper extends BaseMapper<BookCategory> {
}
