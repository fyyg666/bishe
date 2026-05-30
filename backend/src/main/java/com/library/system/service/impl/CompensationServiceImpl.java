package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.CompensationRequest;
import com.library.system.dto.CompensationResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Compensation;
import com.library.system.entity.User;
import com.library.system.enums.CompensationStatus;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.CompensationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.CompensationService;
import com.library.system.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 赔偿服务实现类
 * 支持现金/积分抵扣/志愿服务三种赔偿方式
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationServiceImpl implements CompensationService {

    private final CompensationMapper compensationMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompensationResponse createCompensation(CompensationRequest request, Long operatorId) {
        // 生成赔偿单号: COMP + yyyyMMdd + 6位随机
        String orderNo = "COMP" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        Compensation compensation = new Compensation();
        compensation.setOrderNo(orderNo);
        compensation.setUserId(request.getUserId());
        compensation.setBorrowId(request.getBorrowId());
        compensation.setBookId(request.getBookId());
        compensation.setBookTitle(request.getBookTitle());
        compensation.setIsbn(request.getIsbn());
        compensation.setCompType(request.getCompType());
        compensation.setAmount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
        compensation.setStatus(CompensationStatus.PENDING.name());
        compensation.setPaymentMethod(request.getPaymentMethod());
        compensation.setCreditDeducted(request.getCreditAmount() != null ? request.getCreditAmount() : 0);
        compensation.setVolunteerHours(request.getVolunteerHours() != null ? request.getVolunteerHours() : BigDecimal.ZERO);
        compensation.setRemark(request.getRemark());

        compensationMapper.insert(compensation);

        log.info("赔偿订单创建成功: orderNo={}, userId={}, type={}", orderNo, request.getUserId(), request.getCompType());
        return convertToResponse(compensation);
    }

    @Override
    public PageResult<CompensationResponse> listCompensations(Long current, Long size, String status) {
        LambdaQueryWrapper<Compensation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Compensation::getDeleted, 0);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Compensation::getStatus, status);
        }
        wrapper.orderByDesc(Compensation::getCreateTime);

        Page<Compensation> page = new Page<>(current, size);
        Page<Compensation> result = compensationMapper.selectPage(page, wrapper);

        List<CompensationResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public CompensationResponse getCompensationById(Long id) {
        Compensation compensation = compensationMapper.selectById(id);
        if (compensation == null || compensation.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.INTERNAL_ERROR, "赔偿记录不存在");
        }
        return convertToResponse(compensation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompensationResponse processCashPayment(Long id, Long operatorId, String remark) {
        Compensation compensation = getValidCompensation(id);
        compensation.setStatus(CompensationStatus.PAID.name());
        compensation.setPaymentMethod("CASH");
        compensation.setReviewerId(operatorId);
        compensation.setReviewTime(LocalDateTime.now());
        compensation.setRemark(remark);
        compensationMapper.updateById(compensation);
        log.info("现金赔偿处理完成: orderNo={}, operatorId={}", compensation.getOrderNo(), operatorId);
        return convertToResponse(compensation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompensationResponse processCreditPayment(Long id, Long operatorId, Integer creditAmount, String remark) {
        Compensation compensation = getValidCompensation(id);

        // 根据赔偿类型动态确定积分扣减值
        String compType = compensation.getCompType();
        int penalty = "LOST".equals(compType)
                ? Constants.Credit.LOST_PENALTY
                : Constants.Credit.DAMAGE_PENALTY;

        // 调用积分服务扣除积分
        creditService.deductCredit(compensation.getUserId(), penalty, compType,
                "图书赔偿扣除积分", compensation.getId(), "COMPENSATION");

        compensation.setStatus(CompensationStatus.PAID.name());
        compensation.setPaymentMethod("CREDIT");
        compensation.setCreditDeducted(creditAmount);
        compensation.setReviewerId(operatorId);
        compensation.setReviewTime(LocalDateTime.now());
        compensation.setRemark(remark);
        compensationMapper.updateById(compensation);
        log.info("积分赔偿处理完成: orderNo={}, creditAmount={}", compensation.getOrderNo(), creditAmount);
        return convertToResponse(compensation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompensationResponse processVolunteerPayment(Long id, Long operatorId, BigDecimal hours, String remark) {
        Compensation compensation = getValidCompensation(id);
        compensation.setStatus(CompensationStatus.PAID.name());
        compensation.setPaymentMethod("VOLUNTEER");
        compensation.setVolunteerHours(hours);
        compensation.setReviewerId(operatorId);
        compensation.setReviewTime(LocalDateTime.now());
        compensation.setRemark(remark);
        compensationMapper.updateById(compensation);
        log.info("志愿服务抵扣赔偿完成: orderNo={}, hours={}", compensation.getOrderNo(), hours);
        return convertToResponse(compensation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCompensation(Long id, Long operatorId, String reason) {
        Compensation compensation = compensationMapper.selectById(id);
        if (compensation == null || compensation.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.INTERNAL_ERROR, "赔偿记录不存在");
        }
        if (!CompensationStatus.PENDING.name().equals(compensation.getStatus())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "只能取消待处理的赔偿订单");
        }
        compensation.setStatus(CompensationStatus.CANCELLED.name());
        compensation.setReviewerId(operatorId);
        compensation.setReviewTime(LocalDateTime.now());
        compensation.setRemark(reason);
        compensationMapper.updateById(compensation);
        log.info("赔偿订单已取消: orderNo={}", compensation.getOrderNo());
    }

    /**
     * 获取有效的待处理赔偿记录
     */
    private Compensation getValidCompensation(Long id) {
        Compensation compensation = compensationMapper.selectById(id);
        if (compensation == null || compensation.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.INTERNAL_ERROR, "赔偿记录不存在");
        }
        if (!CompensationStatus.PENDING.name().equals(compensation.getStatus())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "该赔偿订单已处理");
        }
        return compensation;
    }

    /**
     * 实体转DTO
     */
    private CompensationResponse convertToResponse(Compensation comp) {
        String username = "";
        User user = userMapper.selectById(comp.getUserId());
        if (user != null) {
            username = user.getUsername();
        }

        String reviewerName = "";
        if (comp.getReviewerId() != null) {
            User reviewer = userMapper.selectById(comp.getReviewerId());
            if (reviewer != null) {
                reviewerName = reviewer.getUsername();
            }
        }

        String compTypeDesc = "LOST".equals(comp.getCompType()) ? "丢失" : "损坏";
        String statusDesc = switch (comp.getStatus()) {
            case "PAID" -> "已赔付";
            case "CANCELLED" -> "已取消";
            default -> "待处理";
        };
        String paymentDesc = switch (comp.getPaymentMethod() != null ? comp.getPaymentMethod() : "") {
            case "CASH" -> "现金";
            case "CREDIT" -> "积分抵扣";
            case "VOLUNTEER" -> "志愿服务";
            default -> "";
        };

        return CompensationResponse.builder()
                .id(comp.getId())
                .orderNo(comp.getOrderNo())
                .userId(comp.getUserId())
                .username(username)
                .borrowId(comp.getBorrowId())
                .bookId(comp.getBookId())
                .bookTitle(comp.getBookTitle())
                .isbn(comp.getIsbn())
                .compType(comp.getCompType())
                .compTypeDesc(compTypeDesc)
                .amount(comp.getAmount())
                .status(comp.getStatus())
                .statusDesc(statusDesc)
                .paymentMethod(comp.getPaymentMethod())
                .paymentMethodDesc(paymentDesc)
                .creditDeducted(comp.getCreditDeducted())
                .volunteerHours(comp.getVolunteerHours())
                .remark(comp.getRemark())
                .reviewerId(comp.getReviewerId())
                .reviewerName(reviewerName)
                .reviewTime(comp.getReviewTime())
                .createTime(comp.getCreateTime())
                .build();
    }
}
