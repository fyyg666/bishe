package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.entity.SysConfig;
import com.library.system.mapper.SysConfigMapper;
import com.library.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    @Cacheable(value = "sysConfigCache", key = "#key")
    public String getValue(String key) {
        SysConfig config = sysConfigMapper.selectById(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String value = getValue(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public Integer getIntValue(String key, int defaultValue) {
        String value = getValue(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置项{}的值{}不是有效整数，使用默认值{}", key, maskIfSensitive(key, value), defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    @CacheEvict(value = "sysConfigCache", key = "#key")
    public void setValue(String key, String value, String description) {
        SysConfig existing = sysConfigMapper.selectById(key);
        if (existing != null) {
            existing.setConfigValue(value);
            if (description != null) {
                existing.setDescription(description);
            }
            sysConfigMapper.updateById(existing);
        } else {
            SysConfig config = SysConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(description)
                    .build();
            sysConfigMapper.insert(config);
        }
        log.info("系统配置更新: key={}, value={}", key, maskIfSensitive(key, value));
    }

    @Override
    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigKey));
    }

    @Override
    @CacheEvict(value = "sysConfigCache", key = "#key")
    public void deleteByKey(String key) {
        sysConfigMapper.deleteById(key);
        log.info("系统配置删除: key={}", key);
    }

    private String maskIfSensitive(String key, String value) {
        if (key != null && (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret") || key.toLowerCase().contains("key"))) {
            return value != null && value.length() > 4 ? value.substring(0, 2) + "****" : "****";
        }
        return value;
    }
}
