package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.MarcFrameworkResponse;
import com.library.system.entity.MarcFramework;
import com.library.system.entity.MarcFrameworkField;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.MarcFrameworkFieldMapper;
import com.library.system.mapper.MarcFrameworkMapper;
import com.library.system.service.MarcFrameworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarcFrameworkServiceImpl implements MarcFrameworkService {

    private final MarcFrameworkMapper marcFrameworkMapper;
    private final MarcFrameworkFieldMapper marcFrameworkFieldMapper;

    @Override
    public List<MarcFrameworkResponse> listFrameworks() {
        LambdaQueryWrapper<MarcFramework> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(MarcFramework::getIsDefault)
                .orderByAsc(MarcFramework::getId);
        List<MarcFramework> frameworks = marcFrameworkMapper.selectList(wrapper);
        return frameworks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MarcFrameworkResponse getFramework(Long id) {
        MarcFramework framework = marcFrameworkMapper.selectById(id);
        if (framework == null) {
            throw new ResourceNotFoundException("MARC框架不存在: " + id);
        }
        return convertToResponse(framework);
    }

    @Override
    public MarcFrameworkResponse getFrameworkByCode(String code) {
        LambdaQueryWrapper<MarcFramework> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcFramework::getCode, code);
        MarcFramework framework = marcFrameworkMapper.selectOne(wrapper);
        if (framework == null) {
            throw new ResourceNotFoundException("MARC框架不存在: " + code);
        }
        return convertToResponse(framework);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarcFrameworkResponse createFramework(MarcFrameworkResponse request) {
        MarcFramework framework = MarcFramework.builder()
                .name(request.getName())
                .code(request.getCode())
                .recordType(request.getRecordType())
                .description(request.getDescription())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : 0)
                .build();
        marcFrameworkMapper.insert(framework);

        if (request.getFields() != null) {
            for (MarcFrameworkResponse.FrameworkFieldDto fieldDto : request.getFields()) {
                MarcFrameworkField field = MarcFrameworkField.builder()
                        .frameworkId(framework.getId())
                        .tag(fieldDto.getTag())
                        .indicator1(fieldDto.getIndicator1())
                        .indicator2(fieldDto.getIndicator2())
                        .required(fieldDto.getRequired() != null ? fieldDto.getRequired() : 0)
                        .repeatable(fieldDto.getRepeatable() != null ? fieldDto.getRepeatable() : 1)
                        .defaultSubfields(fieldDto.getDefaultSubfields())
                        .sortOrder(fieldDto.getSortOrder() != null ? fieldDto.getSortOrder() : 0)
                        .build();
                marcFrameworkFieldMapper.insert(field);
            }
        }

        return getFramework(framework.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarcFrameworkResponse updateFramework(Long id, MarcFrameworkResponse request) {
        MarcFramework framework = marcFrameworkMapper.selectById(id);
        if (framework == null) {
            throw new ResourceNotFoundException("MARC框架不存在: " + id);
        }

        framework.setName(request.getName());
        framework.setCode(request.getCode());
        framework.setRecordType(request.getRecordType());
        framework.setDescription(request.getDescription());
        framework.setIsDefault(request.getIsDefault());
        marcFrameworkMapper.updateById(framework);

        if (request.getFields() != null) {
            LambdaQueryWrapper<MarcFrameworkField> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(MarcFrameworkField::getFrameworkId, id);
            marcFrameworkFieldMapper.delete(deleteWrapper);

            for (MarcFrameworkResponse.FrameworkFieldDto fieldDto : request.getFields()) {
                MarcFrameworkField field = MarcFrameworkField.builder()
                        .frameworkId(id)
                        .tag(fieldDto.getTag())
                        .indicator1(fieldDto.getIndicator1())
                        .indicator2(fieldDto.getIndicator2())
                        .required(fieldDto.getRequired() != null ? fieldDto.getRequired() : 0)
                        .repeatable(fieldDto.getRepeatable() != null ? fieldDto.getRepeatable() : 1)
                        .defaultSubfields(fieldDto.getDefaultSubfields())
                        .sortOrder(fieldDto.getSortOrder() != null ? fieldDto.getSortOrder() : 0)
                        .build();
                marcFrameworkFieldMapper.insert(field);
            }
        }

        return getFramework(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFramework(Long id) {
        marcFrameworkMapper.deleteById(id);
        LambdaQueryWrapper<MarcFrameworkField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcFrameworkField::getFrameworkId, id);
        marcFrameworkFieldMapper.delete(wrapper);
    }

    private MarcFrameworkResponse convertToResponse(MarcFramework framework) {
        LambdaQueryWrapper<MarcFrameworkField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcFrameworkField::getFrameworkId, framework.getId())
                .orderByAsc(MarcFrameworkField::getSortOrder);
        List<MarcFrameworkField> fields = marcFrameworkFieldMapper.selectList(wrapper);

        List<MarcFrameworkResponse.FrameworkFieldDto> fieldDtos = fields.stream()
                .map(f -> MarcFrameworkResponse.FrameworkFieldDto.builder()
                        .id(f.getId())
                        .tag(f.getTag())
                        .indicator1(f.getIndicator1())
                        .indicator2(f.getIndicator2())
                        .required(f.getRequired())
                        .repeatable(f.getRepeatable())
                        .defaultSubfields(f.getDefaultSubfields())
                        .sortOrder(f.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return MarcFrameworkResponse.builder()
                .id(framework.getId())
                .name(framework.getName())
                .code(framework.getCode())
                .recordType(framework.getRecordType())
                .description(framework.getDescription())
                .isDefault(framework.getIsDefault())
                .createTime(framework.getCreateTime())
                .updateTime(framework.getUpdateTime())
                .fields(fieldDtos)
                .build();
    }
}
