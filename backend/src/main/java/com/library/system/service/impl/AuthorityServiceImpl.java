package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.AuthorityRecordRequest;
import com.library.system.dto.AuthorityRecordResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.AuthorityRecord;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.AuthorityRecordMapper;
import com.library.system.service.AuthorityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityServiceImpl implements AuthorityService {

    private final AuthorityRecordMapper authorityRecordMapper;

    @Override
    public PageResult<AuthorityRecordResponse> listAuthorities(Long current, Long size, String authorityType, String keyword) {
        Page<AuthorityRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AuthorityRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(authorityType)) {
            wrapper.eq(AuthorityRecord::getAuthorityType, authorityType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AuthorityRecord::getHeading, keyword);
        }
        wrapper.orderByDesc(AuthorityRecord::getCreateTime);
        Page<AuthorityRecord> result = authorityRecordMapper.selectPage(page, wrapper);
        List<AuthorityRecordResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return PageResult.of(current, size, result.getTotal(), records);
    }

    @Override
    public AuthorityRecordResponse getAuthority(Long id) {
        AuthorityRecord record = authorityRecordMapper.selectById(id);
        if (record == null) {
            throw new ResourceNotFoundException("规范记录不存在: " + id);
        }
        return convertToResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthorityRecordResponse createAuthority(AuthorityRecordRequest request) {
        AuthorityRecord record = AuthorityRecord.builder()
                .authorityType(request.getAuthorityType())
                .heading(request.getHeading())
                .variantHeadings(request.getVariantHeadings())
                .source(request.getSource())
                .sourceId(request.getSourceId())
                .note(request.getNote())
                .build();
        authorityRecordMapper.insert(record);
        return convertToResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthorityRecordResponse updateAuthority(Long id, AuthorityRecordRequest request) {
        AuthorityRecord record = authorityRecordMapper.selectById(id);
        if (record == null) {
            throw new ResourceNotFoundException("规范记录不存在: " + id);
        }
        record.setAuthorityType(request.getAuthorityType());
        record.setHeading(request.getHeading());
        record.setVariantHeadings(request.getVariantHeadings());
        record.setSource(request.getSource());
        record.setSourceId(request.getSourceId());
        record.setNote(request.getNote());
        authorityRecordMapper.updateById(record);
        return convertToResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuthority(Long id) {
        authorityRecordMapper.deleteById(id);
    }

    @Override
    public List<AuthorityRecordResponse> searchByHeading(String authorityType, String keyword) {
        LambdaQueryWrapper<AuthorityRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(authorityType)) {
            wrapper.eq(AuthorityRecord::getAuthorityType, authorityType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AuthorityRecord::getHeading, keyword);
        }
        wrapper.orderByDesc(AuthorityRecord::getCreateTime);
        wrapper.last("LIMIT 20");
        List<AuthorityRecord> records = authorityRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private AuthorityRecordResponse convertToResponse(AuthorityRecord record) {
        return AuthorityRecordResponse.builder()
                .id(record.getId())
                .authorityType(record.getAuthorityType())
                .heading(record.getHeading())
                .variantHeadings(record.getVariantHeadings())
                .source(record.getSource())
                .sourceId(record.getSourceId())
                .note(record.getNote())
                .createTime(record.getCreateTime())
                .build();
    }
}
