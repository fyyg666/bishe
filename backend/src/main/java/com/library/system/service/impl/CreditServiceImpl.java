package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.*;
import com.library.system.entity.CreditLog;
import com.library.system.entity.User;
import com.library.system.mapper.CreditLogMapper;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 信用积分服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;

    private static final Map<String, String> TYPE_DESC_MAP;
    private static final Map<String, Integer> CREDIT_RULES;

    static {
        // 类型描述
        Map<String, String> typeDescMap = new HashMap<>();
        typeDescMap.put("BORROW", "借阅奖励");
        typeDescMap.put("RETURN", "按时归还奖励");
        typeDescMap.put("OVERDUE", "逾期扣减");
        typeDescMap.put("DAMAGE", "图书损坏扣减");
        typeDescMap.put("LOST", "图书丢失扣减");
        typeDescMap.put("VOLUNTEER", "志愿服务奖励");
        typeDescMap.put("CHECKIN", "签到奖励");
        typeDescMap.put("OTHER", "其他");
        TYPE_DESC_MAP = Collections.unmodifiableMap(typeDescMap);

        // 积分规则
        Map<String, Integer> creditRules = new HashMap<>();
        creditRules.put("BORROW", 5);        // 借阅奖励5分
        creditRules.put("RETURN", 10);       // 按时归还奖励10分
        creditRules.put("RETURN_EARLY", 15); // 提前归还奖励15分
        creditRules.put("OVERDUE_PER_DAY", -5);  // 逾期每天扣5分
        creditRules.put("DAMAGE", -50);      // 损坏扣50分
        creditRules.put("LOST", -100);       // 丢失扣100分
        creditRules.put("VOLUNTEER_PER_HOUR", 10); // 志愿服务每小时10分
        creditRules.put("CHECKIN", 2);       // 签到奖励2分
        CREDIT_RULES = Collections.unmodifiableMap(creditRules);
    }

    @Override
    public Integer getUserCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在"); 
        }
        return user.getCreditScore();
    }

    @Override
    public PageResult<CreditLogResponse> getCreditLogs(Long userId, Long current, Long size) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditLog::getUserId, userId);
        wrapper.orderByDesc(CreditLog::getCreateTime);

        Page<CreditLog> page = new Page<>(current, size);
        Page<CreditLog> logPage = creditLogMapper.selectPage(page, wrapper);

        List<CreditLogResponse> records = logPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(logPage.getCurrent(), logPage.getSize(),
                logPage.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCredit(Long userId, Integer value, String type, String description,
                          Long relatedId, String relatedType) {
        // 查询用户当前积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在"); 
        }

        int newBalance = user.getCreditScore() + value;

        // 更新用户积分
        int updated = userMapper.updateCreditScore(userId, value, user.getVersion());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CREDIT_ADJUST_FAILED, "积分更新失败，请重试"); 
        }

        // 记录积分日志
        CreditLog creditLog = CreditLog.builder()
                .userId(userId)
                .username(user.getUsername())
                .creditChange(value)
                .creditBalance(newBalance)
                .changeType(type)
                .remark(description)
                .bizId(relatedId != null ? String.valueOf(relatedId) : null)
                .build();

        creditLogMapper.insert(creditLog);

        log.info("积分变动: userId={}, value={}, type={}, newBalance={}",
                userId, value, type, newBalance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductCredit(Long userId, Integer value, String type, String description,
                             Long relatedId, String relatedType) {
        // 扣减积分即增加负值
        addCredit(userId, -Math.abs(value), type, description, relatedId, relatedType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowCredit(Long userId, Long borrowId) {
        Integer value = CREDIT_RULES.get("BORROW");
        addCredit(userId, value, "BORROW",
                "借阅图书奖励",
                borrowId, "BORROW_RECORD");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnCredit(Long userId, Long borrowId, Integer overdueDays) {
        if (overdueDays > 0) {
            // 逾期扣减积分
            Integer dailyPenalty = CREDIT_RULES.get("OVERDUE_PER_DAY");
            int totalPenalty = dailyPenalty * overdueDays;
            deductCredit(userId, Math.abs(totalPenalty), "OVERDUE",
                    "图书逾期" + overdueDays + "天",
                    borrowId, "BORROW_RECORD");
        } else {
            // 按时归还奖励积分
            Integer value = CREDIT_RULES.get("RETURN");
            addCredit(userId, value, "RETURN",
                    "按时归还图书奖励",
                    borrowId, "BORROW_RECORD");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCheckInCredit(Long userId, Long reservationId) {
        Integer value = CREDIT_RULES.get("CHECKIN");
        addCredit(userId, value, "CHECKIN",
                "座位签到奖励",
                reservationId, "SEAT_RESERVATION");
    }

    /**
     * 将CreditLog实体转换为CreditLogResponse DTO
     */
    private CreditLogResponse convertToResponse(CreditLog log) {
        return CreditLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .changeValue(log.getChangeValue())
                .balance(log.getBalance())
                .type(log.getType())
                .typeDesc(TYPE_DESC_MAP.getOrDefault(log.getType(), "其他"))
                .description(log.getDescription())
                .createTime(log.getCreateTime())
                .build();
    }
}
