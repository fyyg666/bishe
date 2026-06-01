package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.entity.DigitalResource;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.DigitalResourceMapper;
import com.library.system.service.DigitalResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalResourceServiceImpl implements DigitalResourceService {

    private final DigitalResourceMapper digitalResourceMapper;

    @Override
    public PageResult<DigitalResource> list(Long current, Long size, String keyword, String resourceType) {
        Page<DigitalResource> page = new Page<>(current, size);
        LambdaQueryWrapper<DigitalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DigitalResource::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DigitalResource::getTitle, keyword)
                    .or().like(DigitalResource::getAuthor, keyword)
                    .or().like(DigitalResource::getIsbn, keyword));
        }
        if (StringUtils.hasText(resourceType)) {
            wrapper.eq(DigitalResource::getResourceType, resourceType);
        }
        wrapper.orderByDesc(DigitalResource::getBorrowCount);
        Page<DigitalResource> result = digitalResourceMapper.selectPage(page, wrapper);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public DigitalResource getById(Long id) {
        DigitalResource resource = digitalResourceMapper.selectById(id);
        if (resource == null || resource.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "数字资源不存在");
        }
        return resource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalResource create(DigitalResource resource) {
        resource.setBorrowCount(0);
        if (resource.getStatus() == null) {
            resource.setStatus(0);
        }
        digitalResourceMapper.insert(resource);
        log.info("数字资源创建成功: {}", resource.getTitle());
        return resource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalResource update(Long id, DigitalResource resource) {
        DigitalResource existing = digitalResourceMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "数字资源不存在");
        }
        resource.setId(id);
        digitalResourceMapper.updateById(resource);
        log.info("数字资源更新成功: id={}", id);
        return digitalResourceMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DigitalResource existing = digitalResourceMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "数字资源不存在");
        }
        digitalResourceMapper.deleteById(id);
        log.info("数字资源删除成功: id={}", id);
    }

    @Override
    public List<DigitalResource> search(String keyword) {
        LambdaQueryWrapper<DigitalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DigitalResource::getDeleted, 0);
        wrapper.eq(DigitalResource::getStatus, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DigitalResource::getTitle, keyword)
                    .or().like(DigitalResource::getAuthor, keyword)
                    .or().like(DigitalResource::getIsbn, keyword));
        }
        wrapper.orderByDesc(DigitalResource::getBorrowCount);
        return digitalResourceMapper.selectList(wrapper);
    }
}
