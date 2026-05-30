package com.library.system.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 寒暑假日期计算工具类
 * 从sys_config动态读取假期配置
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Component
public class HolidayUtil {

    private final JdbcTemplate jdbcTemplate;

    private static final long CACHE_TTL_SECONDS = 3600;
    private List<HolidayPeriod> cachedHolidays = Collections.emptyList();
    private long lastLoadTime = 0;

    public HolidayUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 计算两个日期之间落在寒暑假区间内的天数
     */
    public long countHolidayDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return 0;

        List<HolidayPeriod> holidays = getHolidays();
        long total = 0;

        for (HolidayPeriod period : holidays) {
            if (period.end.isBefore(start) || period.start.isAfter(end)) continue;
            LocalRange overlap = new LocalRange(period.start, period.end);
            long days = ChronoUnit.DAYS.between(overlap.start, overlap.end) + 1;
            if (days > 0) total += days;
        }
        return total;
    }

    private List<HolidayPeriod> getHolidays() {
        long now = System.currentTimeMillis();
        if (now - lastLoadTime > CACHE_TTL_SECONDS * 1000) {
            loadHolidays();
        }
        return cachedHolidays;
    }

    private void loadHolidays() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT config_key, config_value FROM sys_config WHERE config_key LIKE '%holiday%'");

            List<HolidayPeriod> periods = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String key = (String) row.get("config_key");
                String value = (String) row.get("config_value");
                if (value != null && value.contains(",")) {
                    String[] parts = value.split(",");
                    LocalDate start = LocalDate.parse(parts[0].trim());
                    LocalDate end = LocalDate.parse(parts[1].trim());
                    periods.add(new HolidayPeriod(key, start, end));
                    log.info("加载假期配置: {} = {} ~ {}", key, start, end);
                }
            }
            cachedHolidays = periods;
            lastLoadTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("加载假期配置失败（若无sys_config表则需先建表）: {}", e.getMessage());
        }
    }

    private record HolidayPeriod(String key, LocalDate start, LocalDate end) {}
    private record LocalRange(LocalDate start, LocalDate end) {}
}
