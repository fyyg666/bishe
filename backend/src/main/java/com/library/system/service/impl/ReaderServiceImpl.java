package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.ImportResultDTO;
import com.library.system.dto.PageResult;
import com.library.system.dto.ReaderImportDTO;
import com.library.system.dto.ReaderResponse;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.exception.BusinessException;
import com.library.system.mapper.UserMapper;
import com.library.system.service.ReaderService;
import com.library.system.util.DataMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 读者服务实现类 
 * <p>
 * 实现读者的CRUD操作和管理功能，使用缓存优化查询性能。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "readers")
public class ReaderServiceImpl implements ReaderService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public PageResult<ReaderResponse> listReaders(Long current, Long size, String keyword, String role) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        // 角色筛选（默认查询READER和VOLUNTEER）
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        } else {
            wrapper.in(User::getRole, Constants.Role.READER, Constants.Role.VOLUNTEER); 
        }

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getCardNumber, keyword));
        }

        // 按创建时间降序排序
        wrapper.orderByDesc(User::getCreateTime);

        // 分页查询
        Page<User> page = new Page<>(current, size);
        Page<User> resultPage = userMapper.selectPage(page, wrapper);

        // 转换为响应DTO
        List<ReaderResponse> records = resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), records);
    }

    @Override
    @Cacheable(key = "#id", unless = "#result == null")
    public ReaderResponse getReaderById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }
        return convertToResponse(user);
    }

    @Override
    public ReaderResponse getCurrentReader(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在");
        }
        return convertToResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderResponse registerReader(String username, String password, String realName, String phone, String email) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            
            throw new BusinessException(ErrorCode.READER_ALREADY_EXISTS, "用户名已存在");
        }

        // 检查手机号是否已存在
        if (phone != null && !phone.isEmpty()) {
            User phoneUser = userMapper.selectByPhone(phone);
            if (phoneUser != null) {
                
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "手机号已被注册");
            }
        }

        // 生成读者卡号
        String cardNumber = generateCardNumber();

        // 创建读者用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(Constants.Role.READER); 
        user.setStatus(Constants.UserStatus.NORMAL); 
        user.setCreditScore(Constants.Credit.INITIAL_SCORE); 
        user.setCardNumber(cardNumber);
        user.setBorrowCount(0);
        user.setMaxBorrowCount(Constants.BorrowLimit.MAX_BORROW_COUNT); 

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.READER_ALREADY_EXISTS, "用户名已存在");
        }
        return convertToResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#id")
    public ReaderResponse updateReader(Long id, Long currentUserId, boolean isAdmin,
                                       String realName, String phone, String email, String avatar,
                                       String role, String status, Integer creditScore, Integer maxBorrowCount) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }

        // 检查权限：仅本人或管理员可修改
        boolean isSelf = user.getId().equals(currentUserId);
        if (!isAdmin && !isSelf) {
            
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权限修改此读者信息");
        }

        // 更新基本信息
        if (realName != null) {
            user.setRealName(realName);
        }
        if (phone != null) {
            User phoneUser = userMapper.selectByPhone(phone);
            if (phoneUser != null && !phoneUser.getId().equals(id)) {
                
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "手机号已被使用");
            }
            user.setPhone(phone);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        // 管理员可修改的内容
        if (isAdmin) {
            if (role != null) {
                user.setRole(role);
            }
            if (status != null) {
                user.setStatus(status);
            }
            if (creditScore != null) {
                user.setCreditScore(creditScore);
            }
            if (maxBorrowCount != null) {
                user.setMaxBorrowCount(maxBorrowCount);
            }
        }

        userMapper.updateById(user);

        log.info("读者信息更新成功: id={}", id);
        return convertToResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, Long currentUserId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }

        // 仅本人可修改密码
        if (!user.getId().equals(currentUserId)) {
            
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权限修改此密码");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            
            throw new BusinessException(ErrorCode.AUTH_FAILED, "旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("密码修改成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#id")
    public void deleteReader(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }

        // 检查是否有未归还的图书
        if (user.getBorrowCount() != null && user.getBorrowCount() > 0) {
            
            throw new BusinessException(ErrorCode.BORROW_LIMIT_EXCEEDED, "该读者有未归还的图书，无法删除");
        }

        userMapper.deleteById(id);

        log.info("读者删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }

        // 重置为随机安全密码 — FIXED: SEC-HIGH 硬编码"123456"替换为随机密码
        String newPassword = Constants.Security.generateDefaultPassword(SECURE_RANDOM);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("密码已重置: id={}, 新密码已生成", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#id")
    public void updateReaderStatus(Long id, Boolean disabled) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "读者不存在");
        }

        user.setStatus(disabled ? Constants.UserStatus.DISABLED : Constants.UserStatus.NORMAL); 
        userMapper.updateById(user);

        log.info("读者状态更新: id={}, status={}", id, user.getStatus());
    }

    @Override
    public User findByUsername(String username) {
        // FIXED: ARCH-003 提供按用户名查询方法，供Controller层替代直接注入UserMapper
        return userMapper.selectByUsername(username);
    }

    @Override
    public Long getUserIdByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        return user != null ? user.getId() : null;
    }

    @Override
    public boolean isCurrentUserAdmin(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) return false;
        return Constants.Role.ADMIN.equals(user.getRole()) ||
               Constants.Role.LIBRARIAN.equals(user.getRole());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public ImportResultDTO importReaders(InputStream inputStream) {
        List<ReaderImportDTO> dataList = com.alibaba.excel.EasyExcel.read(inputStream)
                .head(ReaderImportDTO.class)
                .sheet()
                .doReadSync();

        ImportResultDTO result = ImportResultDTO.builder()
                .totalCount(dataList.size())
                .successCount(0)
                .failCount(0)
                .errors(new ArrayList<>())
                .build();

        for (int i = 0; i < dataList.size(); i++) {
            ReaderImportDTO dto = dataList.get(i);
            int rowNum = i + 2;
            try {
                if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                    result.getErrors().add("第" + rowNum + "行: 用户名不能为空");
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                User existing = findByUsername(dto.getUsername());
                if (existing != null) {
                    result.getErrors().add("第" + rowNum + "行: 用户名" + dto.getUsername() + "已存在");
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                String role = dto.getRole();
                if (role == null || role.trim().isEmpty()) {
                    role = Constants.Role.READER;
                }
                User user = new User();
                user.setUsername(dto.getUsername());
                user.setPassword(passwordEncoder.encode(
                        Constants.Security.generateDefaultPassword(SECURE_RANDOM)));
                user.setRealName(dto.getRealName());
                user.setPhone(dto.getPhone());
                user.setEmail(dto.getEmail());
                user.setRole(role);
                user.setStatus(Constants.UserStatus.NORMAL);
                user.setCreditScore(Constants.Credit.INITIAL_SCORE);
                user.setCardNumber(generateCardNumber());
                user.setBorrowCount(0);
                user.setMaxBorrowCount(Constants.BorrowLimit.MAX_BORROW_COUNT);
                user.setViolationCount(0);
                userMapper.insert(user);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("第" + rowNum + "行: " + e.getMessage());
                result.setFailCount(result.getFailCount() + 1);
            }
        }
        return result;
    }

    /**
     * 生成读者卡号
     * FIXED: P2-007 使用SecureRandom替代Math.random()，防止卡号可预测
     */
    private String generateCardNumber() {
        String dateKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            Long sequence = redisTemplate.opsForValue().increment("card_seq:" + dateKey);
            return "RD" + dateKey + String.format("%06d", sequence);
        } catch (Exception e) {
            log.warn("Redis不可用，回退到随机卡号生成: {}", e.getMessage());
            return "RD" + dateKey + String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        }
    }

    /**
     * 将User实体转换为ReaderResponse DTO
     */
    private ReaderResponse convertToResponse(User user) {
        return ReaderResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(DataMaskingUtil.maskPhone(user.getPhone()))
                .email(DataMaskingUtil.maskEmail(user.getEmail()))
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .creditScore(user.getCreditScore())
                .cardNumber(user.getCardNumber())
                .borrowCount(user.getBorrowCount())
                .maxBorrowCount(user.getMaxBorrowCount())
                .createTime(user.getCreateTime() != null ?
                        user.getCreateTime().toString() : null)
                .build();
    }
}
