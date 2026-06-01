package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseSuggestionRequest;
import com.library.system.dto.PurchaseSuggestionResponse;
import com.library.system.entity.PurchaseSuggestion;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.PurchaseSuggestionMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.PurchaseSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSuggestionServiceImpl implements PurchaseSuggestionService {

    private final PurchaseSuggestionMapper purchaseSuggestionMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSuggestionResponse createSuggestion(Long userId, PurchaseSuggestionRequest request) {
        PurchaseSuggestion suggestion = new PurchaseSuggestion();
        suggestion.setUserId(userId);
        suggestion.setTitle(request.getTitle());
        suggestion.setAuthor(request.getAuthor());
        suggestion.setIsbn(request.getIsbn());
        suggestion.setReason(request.getReason());
        suggestion.setStatus("PENDING");
        purchaseSuggestionMapper.insert(suggestion);
        log.info("荐购建议创建成功: id={}, userId={}, title={}", suggestion.getId(), userId, request.getTitle());
        return convertToResponse(suggestion);
    }

    @Override
    public PageResult<PurchaseSuggestionResponse> listSuggestions(Long current, Long size, String status) {
        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PurchaseSuggestion::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseSuggestion::getCreateTime);

        Page<PurchaseSuggestion> page = new Page<>(current, size);
        Page<PurchaseSuggestion> result = purchaseSuggestionMapper.selectPage(page, wrapper);

        List<PurchaseSuggestionResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public PageResult<PurchaseSuggestionResponse> getMySuggestions(Long userId, Long current, Long size) {
        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseSuggestion::getUserId, userId);
        wrapper.orderByDesc(PurchaseSuggestion::getCreateTime);

        Page<PurchaseSuggestion> page = new Page<>(current, size);
        Page<PurchaseSuggestion> result = purchaseSuggestionMapper.selectPage(page, wrapper);

        List<PurchaseSuggestionResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSuggestionResponse approveSuggestion(Long id, Long reviewerId, String remark) {
        PurchaseSuggestion suggestion = getValidSuggestion(id);
        suggestion.setStatus("APPROVED");
        suggestion.setReviewerId(reviewerId);
        suggestion.setReviewRemark(remark);
        purchaseSuggestionMapper.updateById(suggestion);
        log.info("荐购建议已批准: id={}, reviewerId={}", id, reviewerId);
        return convertToResponse(suggestion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSuggestionResponse rejectSuggestion(Long id, Long reviewerId, String remark) {
        PurchaseSuggestion suggestion = getValidSuggestion(id);
        suggestion.setStatus("REJECTED");
        suggestion.setReviewerId(reviewerId);
        suggestion.setReviewRemark(remark);
        purchaseSuggestionMapper.updateById(suggestion);
        log.info("荐购建议已拒绝: id={}, reviewerId={}", id, reviewerId);
        return convertToResponse(suggestion);
    }

    private PurchaseSuggestion getValidSuggestion(Long id) {
        PurchaseSuggestion suggestion = purchaseSuggestionMapper.selectById(id);
        if (suggestion == null || suggestion.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.SUGGESTION_NOT_FOUND, "荐购记录不存在");
        }
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new BusinessException(ErrorCode.SUGGESTION_STATUS_ERROR, "该荐购建议已处理");
        }
        return suggestion;
    }

    private PurchaseSuggestionResponse convertToResponse(PurchaseSuggestion s) {
        String username = "";
        User user = userMapper.selectById(s.getUserId());
        if (user != null) {
            username = user.getUsername();
        }

        String reviewerName = "";
        if (s.getReviewerId() != null) {
            User reviewer = userMapper.selectById(s.getReviewerId());
            if (reviewer != null) {
                reviewerName = reviewer.getUsername();
            }
        }

        String statusDesc = switch (s.getStatus()) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            default -> s.getStatus();
        };

        return PurchaseSuggestionResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .username(username)
                .title(s.getTitle())
                .author(s.getAuthor())
                .isbn(s.getIsbn())
                .reason(s.getReason())
                .status(s.getStatus())
                .statusDesc(statusDesc)
                .reviewerId(s.getReviewerId())
                .reviewerName(reviewerName)
                .reviewRemark(s.getReviewRemark())
                .createTime(s.getCreateTime())
                .updateTime(s.getUpdateTime())
                .build();
    }
}
