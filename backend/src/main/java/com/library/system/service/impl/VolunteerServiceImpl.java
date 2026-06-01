package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.dto.VolunteerStatsDto;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.mapper.VolunteerServiceMapper;
import com.library.system.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 志愿服务实现类 
 * <p>
 * 实现志愿服务的CRUD操作和审核功能，审核通过自动增加信用积分。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements com.library.system.service.VolunteerService {

    private final VolunteerServiceMapper volunteerServiceMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    /** 审核通过每服务1小时增加的积分数（论文§3.2(4): 志愿服务每小时可加10分） */ 
    private static final BigDecimal CREDIT_PER_HOUR = BigDecimal.valueOf(Constants.Credit.VOLUNTEER_PER_HOUR);
    /** 审核通过最多增加的积分上限（论文§7.2: 单次服务可获得的积分上限为50分） */ 
    private static final BigDecimal MAX_CREDIT_BONUS = BigDecimal.valueOf(50);
    /** 单次服务时长上限（小时） */ 
    private static final BigDecimal MAX_SERVICE_HOURS = BigDecimal.valueOf(12);

    @Override
    public PageResult<VolunteerResponse> listVolunteers(Long current, Long size, String status) {
        LambdaQueryWrapper<com.library.system.entity.VolunteerService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.library.system.entity.VolunteerService::getDeleted, 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(com.library.system.entity.VolunteerService::getStatus, status);
        }

        wrapper.orderByDesc(com.library.system.entity.VolunteerService::getCreateTime);

        Page<com.library.system.entity.VolunteerService> page = new Page<>(current, size);
        Page<com.library.system.entity.VolunteerService> resultPage = volunteerServiceMapper.selectPage(page, wrapper);

        List<com.library.system.entity.VolunteerService> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            Map<Long, User> userMap = batchLoadUserMap(records);
            
            List<VolunteerResponse> responseList = records.stream()
                    .map(v -> convertToResponseWithUsers(v, userMap))
                    .collect(Collectors.toList());
            
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                    resultPage.getTotal(), responseList);
        }

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), List.of());
    }

    @Override
    public PageResult<VolunteerResponse> getMyVolunteers(Long current, Long size, Long userId) {
        Page<com.library.system.entity.VolunteerService> page = new Page<>(current, size);
        Page<com.library.system.entity.VolunteerService> resultPage = volunteerServiceMapper.selectByUserId(page, userId);

        List<com.library.system.entity.VolunteerService> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            Map<Long, User> userMap = batchLoadUserMap(records);
            
            List<VolunteerResponse> responseList = records.stream()
                    .map(v -> convertToResponseWithUsers(v, userMap))
                    .collect(Collectors.toList());
            
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                    resultPage.getTotal(), responseList);
        }

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), List.of());
    }

    @Override
    public VolunteerResponse getVolunteerById(Long id) {
        com.library.system.entity.VolunteerService volunteer = volunteerServiceMapper.selectById(id);
        if (volunteer == null || volunteer.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.VOLUNTEER_NOT_FOUND, "志愿服务记录不存在");
        }
        return convertToResponseWithUsers(volunteer, batchLoadUserMap(List.of(volunteer)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VolunteerResponse createVolunteer(Long userId, VolunteerRequest request) {
        // 计算服务时长
        BigDecimal serviceHours = request.getServiceHours();
        if (serviceHours == null && request.getStartTime() != null && request.getEndTime() != null) {
            Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
            serviceHours = BigDecimal.valueOf(duration.toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        // 服务时长不能超过上限
        if (serviceHours != null && serviceHours.compareTo(MAX_SERVICE_HOURS) > 0) {
            
            throw new BusinessException(ErrorCode.VOLUNTEER_HOURS_EXCEEDED,
                    "单次服务时长不能超过" + MAX_SERVICE_HOURS.intValue() + "小时");
        }

        com.library.system.entity.VolunteerService volunteer = new com.library.system.entity.VolunteerService();
        volunteer.setUserId(userId);
        volunteer.setServiceDate(request.getServiceDate());
        volunteer.setStartTime(request.getStartTime());
        volunteer.setEndTime(request.getEndTime());
        volunteer.setServiceHours(serviceHours);
        volunteer.setServiceType(request.getServiceType());
        volunteer.setDescription(request.getDescription());
        volunteer.setStatus("PENDING");

        volunteerServiceMapper.insert(volunteer);

        log.info("志愿服务申请成功: userId={}", userId);
        return convertToResponseWithUsers(volunteer, batchLoadUserMap(List.of(volunteer)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VolunteerResponse updateVolunteer(Long id, Long userId, VolunteerRequest request) {
        com.library.system.entity.VolunteerService volunteer = volunteerServiceMapper.selectById(id);
        if (volunteer == null || volunteer.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.VOLUNTEER_NOT_FOUND, "志愿服务记录不存在");
        }

        // 仅本人可修改
        if (!volunteer.getUserId().equals(userId)) {
            
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权限修改此记录");
        }

        // 仅待审核状态可修改
        if (!"PENDING".equals(volunteer.getStatus())) {
            
            throw new BusinessException(ErrorCode.VOLUNTEER_STATUS_ERROR, "当前状态不允许修改");
        }

        // 计算服务时长
        BigDecimal serviceHours = request.getServiceHours();
        if (serviceHours == null && request.getStartTime() != null && request.getEndTime() != null) {
            Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
            serviceHours = BigDecimal.valueOf(duration.toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        volunteer.setServiceDate(request.getServiceDate());
        volunteer.setStartTime(request.getStartTime());
        volunteer.setEndTime(request.getEndTime());
        volunteer.setServiceHours(serviceHours);
        volunteer.setServiceType(request.getServiceType());
        volunteer.setDescription(request.getDescription());

        volunteerServiceMapper.updateById(volunteer);

        log.info("志愿服务记录更新成功: id={}", id);
        return convertToResponseWithUsers(volunteer, batchLoadUserMap(List.of(volunteer)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelVolunteer(Long id, Long userId) {
        com.library.system.entity.VolunteerService volunteer = volunteerServiceMapper.selectById(id);
        if (volunteer == null || volunteer.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.VOLUNTEER_NOT_FOUND, "志愿服务记录不存在");
        }

        // 仅本人可取消
        if (!volunteer.getUserId().equals(userId)) {
            
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权限取消此记录");
        }

        // 仅待审核状态可取消
        if (!"PENDING".equals(volunteer.getStatus())) {
            
            throw new BusinessException(ErrorCode.VOLUNTEER_STATUS_ERROR, "当前状态不允许取消");
        }

        volunteer.setStatus("CANCELLED");
        volunteerServiceMapper.updateById(volunteer);

        log.info("志愿服务申请已取消: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VolunteerResponse reviewVolunteer(Long id, Long reviewerId, Boolean approved, String remark) {
        com.library.system.entity.VolunteerService volunteer = volunteerServiceMapper.selectById(id);
        if (volunteer == null || volunteer.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.VOLUNTEER_NOT_FOUND, "志愿服务记录不存在");
        }

        // 仅待审核状态可审核
        if (!"PENDING".equals(volunteer.getStatus())) {
            
            throw new BusinessException(ErrorCode.VOLUNTEER_STATUS_ERROR, "当前状态不允许审核");
        }

        volunteer.setStatus(approved ? "APPROVED" : "REJECTED");
        volunteer.setReviewerId(reviewerId);
        volunteer.setReviewTime(LocalDateTime.now());
        volunteer.setReviewRemark(remark);

        volunteerServiceMapper.updateById(volunteer);

        // 如果审核通过，增加用户积分（通过CreditService确保credit_log写入）
        if (approved && volunteer.getServiceHours() != null) {
            int creditBonus = volunteer.getServiceHours()
                    .multiply(CREDIT_PER_HOUR)
                    .min(MAX_CREDIT_BONUS)
                    .intValue();
            if (creditBonus > 0) {
                creditService.addCredit(volunteer.getUserId(), creditBonus, "VOLUNTEER",
                        "志愿服务审核通过，" + volunteer.getServiceHours() + "小时",
                        volunteer.getId(), "VOLUNTEER_SERVICE");
                log.info("审核通过，增加积分: userId={}, creditBonus={}", volunteer.getUserId(), creditBonus);
            }
        }

        log.info("志愿服务审核完成: id={}, approved={}", id, approved);
        return convertToResponseWithUsers(volunteer, batchLoadUserMap(List.of(volunteer)));
    }

    @Override
    public PageResult<VolunteerResponse> getPendingVolunteers(Long current, Long size) {
        Page<com.library.system.entity.VolunteerService> page = new Page<>(current, size);
        Page<com.library.system.entity.VolunteerService> resultPage = volunteerServiceMapper.selectPendingReview(page);

        List<com.library.system.entity.VolunteerService> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            Map<Long, User> userMap = batchLoadUserMap(records);
            
            List<VolunteerResponse> responseList = records.stream()
                    .map(v -> convertToResponseWithUsers(v, userMap))
                    .collect(Collectors.toList());
            
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                    resultPage.getTotal(), responseList);
        }

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVolunteer(Long id) {
        com.library.system.entity.VolunteerService volunteer = volunteerServiceMapper.selectById(id);
        if (volunteer == null || volunteer.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.VOLUNTEER_NOT_FOUND, "志愿服务记录不存在");
        }

        volunteerServiceMapper.deleteById(id);

        log.info("志愿服务记录删除成功: id={}", id);
    }

    @Override
    public VolunteerStatsDto getVolunteerStats(Long userId) {
        LambdaQueryWrapper<com.library.system.entity.VolunteerService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.library.system.entity.VolunteerService::getDeleted, 0)
               .eq(com.library.system.entity.VolunteerService::getUserId, userId)
               .eq(com.library.system.entity.VolunteerService::getStatus, "APPROVED");

        List<com.library.system.entity.VolunteerService> approvedList = volunteerServiceMapper.selectList(wrapper);

        BigDecimal totalHours = BigDecimal.ZERO;
        for (com.library.system.entity.VolunteerService v : approvedList) {
            if (v.getServiceHours() != null) {
                totalHours = totalHours.add(v.getServiceHours());
            }
        }

        return VolunteerStatsDto.builder()
                .totalRecords((long) approvedList.size())
                .totalHours(totalHours)
                .pendingCount(volunteerServiceMapper.selectCount(
                        new LambdaQueryWrapper<com.library.system.entity.VolunteerService>()
                                .eq(com.library.system.entity.VolunteerService::getDeleted, 0)
                                .eq(com.library.system.entity.VolunteerService::getUserId, userId)
                                .eq(com.library.system.entity.VolunteerService::getStatus, "PENDING")))
                .build();
    }

    private Map<Long, User> batchLoadUserMap(List<com.library.system.entity.VolunteerService> records) {
        Set<Long> userIds = new java.util.HashSet<>();
        records.forEach(r -> {
            if (r.getUserId() != null) userIds.add(r.getUserId());
            if (r.getReviewerId() != null) userIds.add(r.getReviewerId());
        });
        return userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 使用预加载的用户Map转换为VolunteerResponse DTO（批量查询优化）
     */
    private VolunteerResponse convertToResponseWithUsers(com.library.system.entity.VolunteerService volunteer, Map<Long, User> userMap) {
        VolunteerResponse.VolunteerResponseBuilder builder = VolunteerResponse.builder()
                .id(volunteer.getId())
                .userId(volunteer.getUserId())
                .serviceDate(volunteer.getServiceDate())
                .startTime(volunteer.getStartTime())
                .endTime(volunteer.getEndTime())
                .serviceHours(volunteer.getServiceHours())
                .serviceType(volunteer.getServiceType())
                .description(volunteer.getDescription())
                .status(volunteer.getStatus())
                .reviewerId(volunteer.getReviewerId())
                .reviewTime(volunteer.getReviewTime())
                .reviewRemark(volunteer.getReviewRemark())
                .createTime(volunteer.getCreateTime());

        // 从预加载的Map中获取用户信息
        if (volunteer.getUserId() != null) {
            User user = userMap.get(volunteer.getUserId());
            if (user != null) {
                builder.username(user.getUsername())
                       .realName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }

        // 获取审核人姓名
        if (volunteer.getReviewerId() != null) {
            User reviewer = userMap.get(volunteer.getReviewerId());
            if (reviewer != null) {
                builder.reviewerName(reviewer.getRealName() != null ?
                        reviewer.getRealName() : reviewer.getUsername());
            }
        }

        return builder.build();
    }
}
