package com.library.system.service;

import com.library.system.entity.SysConfig;
import java.util.List;

public interface SysConfigService {

    String getValue(String key);

    String getValue(String key, String defaultValue);

    Integer getIntValue(String key, int defaultValue);

    void setValue(String key, String value, String description);

    List<SysConfig> listAll();

    void deleteByKey(String key);
}
