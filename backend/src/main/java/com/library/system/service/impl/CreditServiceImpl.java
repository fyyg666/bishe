package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.*;
import com.library.system.entity.CreditLog;
import com.library.system.entity.User;
import com.library.system.mapper.CreditLogMapper;
import com.library.system.common.Constants;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.service.CreditService;
import com.library.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final SysConfigService sysConfigService;

    private static final Map<String, String> TYPE_DESC_MAP;

    static {
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
    public CreditLevelResponse getUserLevel(Long userId) {
        Integer score = getUserCredit(userId);
        String level;
        Integer nextLevelScore;

        if (score >= Constants.Credit.PLATINUM_THRESHOLD) {
            level = "白金";
            nextLevelScore = null;
        } else if (score >= Constants.Credit.GOLD_THRESHOLD) {
            level = "金牌";
            nextLevelScore = Constants.Credit.PLATINUM_THRESHOLD;
        } else if (score >= Constants.Credit.SILVER_THRESHOLD) {
            level = "银牌";
            nextLevelScore = Constants.Credit.GOLD_THRESHOLD;
        } else if (score >= Constants.Credit.BRONZE_THRESHOLD) {
            level = "铜牌";
            nextLevelScore = Constants.Credit.SILVER_THRESHOLD;
        } else {
            level = "普通";
            nextLevelScore = Constants.Credit.BRONZE_THRESHOLD;
        }

        return CreditLevelResponse.builder()
                .score(score)
                .level(level)
                .nextLevelScore(nextLevelScore)
                .build();
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

        // 积分上下限截断：确保积分在 [MIN_SCORE, MAX_SCORE] 范围内
        if (newBalance > Constants.Credit.MAX_SCORE) {
            newBalance = Constants.Credit.MAX_SCORE;
        } else if (newBalance < Constants.Credit.MIN_SCORE) {
            newBalance = Constants.Credit.MIN_SCORE;
        }

        // 计算实际增减值（截断后的差值）
        int actualDelta = newBalance - user.getCreditScore();
        if (actualDelta == 0) {
            log.debug("积分未变动（已达上限或下限）: userId={}, currentScore={}", userId, user.getCreditScore());
            return; // 积分已达上下限，无需更新
        }

        // 更新用户积分
        int updated = userMapper.updateCreditScore(userId, actualDelta, user.getVersion());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CREDIT_ADJUST_FAILED, "积分更新失败，请重试"); 
        }

        // 记录积分日志
        CreditLog creditLog = CreditLog.builder()
                .userId(userId)
                .username(user.getUsername())
                .creditChange(actualDelta)
                .creditBalance(newBalance)
                .changeType(type)
                .remark(description)
                .bizId(relatedId)
                .build();

        creditLogMapper.insert(creditLog);

        log.info("积分变动: userId={}, value={}, type={}, newBalance={}",
                userId, actualDelta, type, newBalance);
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
        int value = sysConfigService.getIntValue("credit.borrow_reward", Constants.Credit.BORROW_REWARD);
        addCredit(userId, value, "BORROW",
                "借阅图书奖励",
                borrowId, "BORROW_RECORD");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnCredit(Long userId, Long borrowId, Integer overdueDays,
                                    LocalDateTime dueDate, LocalDateTime returnDate) {
        if (overdueDays > 0) {
            // 逾期扣减积分
            int dailyPenalty = sysConfigService.getIntValue("credit.overdue_per_day", Constants.Credit.OVERDUE_PER_DAY);
            int totalPenalty = dailyPenalty * overdueDays;
            deductCredit(userId, Math.abs(totalPenalty), "OVERDUE",
                    "图书逾期" + overdueDays + "天",
                    borrowId, "BORROW_RECORD");
        } else if (returnDate != null && dueDate != null && returnDate.toLocalDate().isBefore(dueDate.toLocalDate())) {
            // 提前归还奖励积分
            int value = sysConfigService.getIntValue("credit.return_early", Constants.Credit.RETURN_EARLY);
            addCredit(userId, value, "RETURN_EARLY",
                    "提前归还图书奖励",
                    borrowId, "BORROW_RECORD");
        } else {
            // 按时归还奖励积分
            int value = sysConfigService.getIntValue("credit.return_on_time", Constants.Credit.RETURN_ON_TIME);
            addCredit(userId, value, "RETURN",
                    "按时归还图书奖励",
                    borrowId, "BORROW_RECORD");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCheckInCredit(Long userId, Long reservationId) {
        int value = sysConfigService.getIntValue("credit.checkin_reward", Constants.Credit.CHECKIN_REWARD);
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

    @Override
    public List<CreditRuleResponse> getCreditRules() {
        List<CreditRuleResponse> rules = new ArrayList<>();
        rules.add(CreditRuleResponse.builder()
                .ruleKey("borrow_reward").ruleName("借阅奖励")
                .score(sysConfigService.getIntValue("credit.borrow_reward", Constants.Credit.BORROW_REWARD))
                .description("成功借阅图书奖励积分").type("REWARD").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("return_on_time").ruleName("按时归还奖励")
                .score(sysConfigService.getIntValue("credit.return_on_time", Constants.Credit.RETURN_ON_TIME))
                .description("按时归还图书奖励积分").type("REWARD").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("return_early").ruleName("提前归还奖励")
                .score(sysConfigService.getIntValue("credit.return_early", Constants.Credit.RETURN_EARLY))
                .description("提前归还图书额外奖励").type("REWARD").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("overdue_per_day").ruleName("逾期扣分")
                .score(sysConfigService.getIntValue("credit.overdue_per_day", Constants.Credit.OVERDUE_PER_DAY))
                .description("逾期归还图书每天扣除积分").type("PENALTY").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("no_show").ruleName("预约未签到扣分")
                .score(sysConfigService.getIntValue("credit.no_show", Constants.Credit.NO_SHOW))
                .description("预约座位未签到扣除积分").type("PENALTY").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("damage_penalty").ruleName("图书损坏扣分")
                .score(sysConfigService.getIntValue("credit.damage_penalty", Constants.Credit.DAMAGE_PENALTY))
                .description("损坏图书扣除积分").type("PENALTY").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("lost_penalty").ruleName("图书丢失扣分")
                .score(sysConfigService.getIntValue("credit.lost_penalty", Constants.Credit.LOST_PENALTY))
                .description("丢失图书扣除积分").type("PENALTY").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("volunteer_per_hour").ruleName("志愿服务奖励")
                .score(sysConfigService.getIntValue("credit.volunteer_per_hour", Constants.Credit.VOLUNTEER_PER_HOUR))
                .description("每小时志愿服务奖励积分").type("REWARD").build());
        rules.add(CreditRuleResponse.builder()
                .ruleKey("checkin_reward").ruleName("签到奖励")
                .score(sysConfigService.getIntValue("credit.checkin_reward", Constants.Credit.CHECKIN_REWARD))
                .description("座位签到奖励积分").type("REWARD").build());
        return rules;
    }
}
