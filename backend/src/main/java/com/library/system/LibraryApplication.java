package com.library.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 图书馆管理系统主启动类
 * 
 * @author Library Team
 * @version 2.0.0
 */
@SpringBootApplication
@MapperScan("com.library.system.mapper")
@EnableCaching
@EnableScheduling
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
