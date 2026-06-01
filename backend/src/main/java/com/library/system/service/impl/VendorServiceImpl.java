package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.entity.Vendor;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.VendorMapper;
import com.library.system.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorMapper vendorMapper;

    @Override
    public PageResult<Vendor> listVendors(Long current, Long size, String keyword, String status) {
        LambdaQueryWrapper<Vendor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vendor::getDeleted, 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Vendor::getStatus, status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Vendor::getName, keyword)
                    .or()
                    .like(Vendor::getContact, keyword));
        }

        wrapper.orderByDesc(Vendor::getCreateTime);

        Page<Vendor> page = new Page<>(current, size);
        Page<Vendor> resultPage = vendorMapper.selectPage(page, wrapper);

        List<Vendor> records = resultPage.getRecords();
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), records);
    }

    @Override
    public Vendor getVendorById(Long id) {
        Vendor vendor = vendorMapper.selectById(id);
        if (vendor == null || vendor.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "供应商不存在");
        }
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vendor createVendor(Vendor vendor) {
        vendor.setStatus(vendor.getStatus() != null ? vendor.getStatus() : "ACTIVE");
        vendorMapper.insert(vendor);
        log.info("供应商创建成功: {}", vendor.getName());
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vendor updateVendor(Long id, Vendor vendor) {
        Vendor existing = vendorMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "供应商不存在");
        }

        existing.setName(vendor.getName());
        existing.setContact(vendor.getContact());
        existing.setPhone(vendor.getPhone());
        existing.setEmail(vendor.getEmail());
        existing.setAddress(vendor.getAddress());
        if (vendor.getStatus() != null) {
            existing.setStatus(vendor.getStatus());
        }

        vendorMapper.updateById(existing);
        log.info("供应商更新成功: id={}", id);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVendor(Long id) {
        Vendor existing = vendorMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "供应商不存在");
        }

        vendorMapper.deleteById(id);
        log.info("供应商删除成功: id={}", id);
    }
}
