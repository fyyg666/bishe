package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.MarcRecordResponse;
import com.library.system.entity.MarcField;
import com.library.system.entity.MarcRecord;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.MarcFieldMapper;
import com.library.system.mapper.MarcRecordMapper;
import com.library.system.service.MarcRecordService;
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
public class MarcRecordServiceImpl implements MarcRecordService {

    private final MarcRecordMapper marcRecordMapper;
    private final MarcFieldMapper marcFieldMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarcRecordResponse createRecord(MarcRecordRequest request) {
        MarcRecord record = MarcRecord.builder()
                .recordType(request.getRecordType())
                .leader(request.getLeader())
                .controlNumber(request.getControlNumber())
                .bookId(request.getBookId())
                .status("DRAFT")
                .build();
        marcRecordMapper.insert(record);

        if (request.getFields() != null) {
            for (MarcRecordRequest.FieldRequest fieldReq : request.getFields()) {
                MarcField field = MarcField.builder()
                        .recordId(record.getId())
                        .tag(fieldReq.getTag())
                        .indicator1(fieldReq.getIndicator1())
                        .indicator2(fieldReq.getIndicator2())
                        .subfields(fieldReq.getSubfields())
                        .displayValue(fieldReq.getDisplayValue())
                        .sortOrder(fieldReq.getSortOrder())
                        .build();
                marcFieldMapper.insert(field);
            }
        }

        return getRecord(record.getId());
    }

    @Override
    public MarcRecordResponse getRecord(Long id) {
        MarcRecord record = marcRecordMapper.selectById(id);
        if (record == null) {
            throw new ResourceNotFoundException("MARC记录不存在: " + id);
        }
        return convertToResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarcRecordResponse updateRecord(Long id, MarcRecordRequest request) {
        MarcRecord record = marcRecordMapper.selectById(id);
        if (record == null) {
            throw new ResourceNotFoundException("MARC记录不存在: " + id);
        }

        record.setRecordType(request.getRecordType());
        record.setLeader(request.getLeader());
        record.setControlNumber(request.getControlNumber());
        record.setBookId(request.getBookId());
        marcRecordMapper.updateById(record);

        if (request.getFields() != null) {
            LambdaQueryWrapper<MarcField> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(MarcField::getRecordId, id);
            marcFieldMapper.delete(deleteWrapper);

            for (MarcRecordRequest.FieldRequest fieldReq : request.getFields()) {
                MarcField field = MarcField.builder()
                        .recordId(id)
                        .tag(fieldReq.getTag())
                        .indicator1(fieldReq.getIndicator1())
                        .indicator2(fieldReq.getIndicator2())
                        .subfields(fieldReq.getSubfields())
                        .displayValue(fieldReq.getDisplayValue())
                        .sortOrder(fieldReq.getSortOrder())
                        .build();
                marcFieldMapper.insert(field);
            }
        }

        return getRecord(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        marcRecordMapper.deleteById(id);
        LambdaQueryWrapper<MarcField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcField::getRecordId, id);
        marcFieldMapper.delete(wrapper);
    }

    @Override
    public List<MarcRecordResponse> listRecords(Long current, Long size, String recordType, String keyword) {
        LambdaQueryWrapper<MarcRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(recordType)) {
            wrapper.eq(MarcRecord::getRecordType, recordType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MarcRecord::getControlNumber, keyword);
        }
        wrapper.orderByDesc(MarcRecord::getCreateTime);

        if (current != null && size != null && current > 0 && size > 0) {
            Page<MarcRecord> page = new Page<>(current, size);
            Page<MarcRecord> result = marcRecordMapper.selectPage(page, wrapper);
            return result.getRecords().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        List<MarcRecord> records = marcRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MarcRecordResponse getRecordByBookId(Long bookId) {
        LambdaQueryWrapper<MarcRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcRecord::getBookId, bookId);
        MarcRecord record = marcRecordMapper.selectOne(wrapper);
        if (record == null) {
            throw new ResourceNotFoundException("图书 " + bookId + " 对应的MARC记录不存在");
        }
        return convertToResponse(record);
    }

    @Override
    public void linkToBook(Long recordId, Long bookId) {
        MarcRecord record = marcRecordMapper.selectById(recordId);
        if (record == null) {
            throw new ResourceNotFoundException("MARC记录不存在: " + recordId);
        }
        record.setBookId(bookId);
        marcRecordMapper.updateById(record);
    }

    private MarcRecordResponse convertToResponse(MarcRecord record) {
        LambdaQueryWrapper<MarcField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcField::getRecordId, record.getId())
                .orderByAsc(MarcField::getSortOrder);
        List<MarcField> fields = marcFieldMapper.selectList(wrapper);

        List<MarcRecordResponse.MarcFieldDto> fieldDtos = fields.stream()
                .map(f -> MarcRecordResponse.MarcFieldDto.builder()
                        .id(f.getId())
                        .tag(f.getTag())
                        .indicator1(f.getIndicator1())
                        .indicator2(f.getIndicator2())
                        .subfields(f.getSubfields())
                        .displayValue(f.getDisplayValue())
                        .sortOrder(f.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return MarcRecordResponse.builder()
                .id(record.getId())
                .recordType(record.getRecordType())
                .leader(record.getLeader())
                .controlNumber(record.getControlNumber())
                .bookId(record.getBookId())
                .status(record.getStatus())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .fields(fieldDtos)
                .build();
    }
}
