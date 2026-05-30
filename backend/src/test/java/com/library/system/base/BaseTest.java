package com.library.system.base;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试基类 - 提供通用日志和初始化能力
 * 所有单元测试应继承此类
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @BeforeAll
    static void globalInit() {
        // 全局测试初始化（如有需要）
    }

    @BeforeEach
    void setUpBase() {
        // 每个测试方法执行前的通用初始化
    }

    /**
     * 生成唯一的测试标识符，避免测试数据冲突
     */
    protected String uniqueId() {
        return "test-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * 生成唯一的测试用户名
     */
    protected String uniqueUsername() {
        return "user_" + System.nanoTime();
    }
}
