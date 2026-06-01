package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.common.Constants;
import com.library.system.dto.BookResponse;
import com.library.system.dto.StatisticsResponse;
import com.library.system.entity.*;
import com.library.system.mapper.*;
import com.library.system.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务实现类 
 * <p>
 * 实现系统各类统计数据的查询，使用缓存优化查询性能。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final SeatMapper seatMapper;
    private final SeatReservationMapper seatReservationMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Cacheable(value = "statisticsCache", key = "'overview'")
    public StatisticsResponse getOverview() {
        return StatisticsResponse.builder()
                .borrowStatistics(getBorrowStatistics())
                .bookStatistics(getBookStatistics())
                .readerStatistics(getReaderStatistics())
                .seatStatistics(getSeatStatistics())
                .build();
    }

    @Override
    public StatisticsResponse.BorrowStatistics getBorrowStatistics() {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getDeleted, 0);

        long totalBorrows = borrowRecordMapper.selectCount(wrapper);

        // 在借数量 
        long activeBorrows = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getDeleted, 0)
                        .eq(BorrowRecord::getStatus, Constants.BorrowStatus.BORROWING));

        // 逾期数量
        long overdueBorrows = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getDeleted, 0)
                        .eq(BorrowRecord::getStatus, Constants.BorrowStatus.OVERDUE));

        // 今日归还
        long returnedToday = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getDeleted, 0)
                        .eq(BorrowRecord::getStatus, Constants.BorrowStatus.RETURNED)
                        .apply("DATE(return_date) = CURDATE()"));

        // 平均借阅天数
        Double avgDays = calculateAverageBorrowDays();

        return StatisticsResponse.BorrowStatistics.builder()
                .totalBorrows(totalBorrows)
                .activeBorrows(activeBorrows)
                .overdueBorrows(overdueBorrows)
                .returnedToday(returnedToday)
                .averageBorrowDays(avgDays)
                .build();
    }

    @Override
    public StatisticsResponse.BookStatistics getBookStatistics() {
        // FIXED: PERF-004 使用聚合查询替代全表加载
        String sql = "SELECT " +
            "  COUNT(*) as total_books, " +
            "  SUM(total_count) as total_copies, " +
            "  SUM(available_count) as available_copies, " +
            "  COUNT(DISTINCT category_id) as categories " +
            "FROM sys_book WHERE deleted = 0";

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);

        long totalBooks = ((Number) result.get("total_books")).longValue();
        long totalCopies = ((Number) result.get("total_copies")).longValue();
        long availableCopies = ((Number) result.get("available_copies")).longValue();
        long borrowedCopies = totalCopies - availableCopies;
        long categories = ((Number) result.get("categories")).longValue();

        return StatisticsResponse.BookStatistics.builder()
                .totalBooks(totalBooks)
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .borrowedCopies(borrowedCopies)
                .categories(categories)
                .build();
    }

    @Override
    public StatisticsResponse.ReaderStatistics getReaderStatistics() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .in(User::getRole, Constants.Role.READER, Constants.Role.VOLUNTEER); 

        long totalReaders = userMapper.selectCount(wrapper);

        long activeReaders = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .in(User::getRole, Constants.Role.READER, Constants.Role.VOLUNTEER)
                        .inSql(User::getId, "SELECT br.user_id FROM borrow_record br WHERE br.deleted = 0 AND br.status != '" + Constants.BorrowStatus.RETURNED + "'"));

        long overdueReaders = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .in(User::getRole, Constants.Role.READER, Constants.Role.VOLUNTEER)
                        .inSql(User::getId, "SELECT br.user_id FROM borrow_record br WHERE br.deleted = 0 AND br.status = '" + Constants.BorrowStatus.OVERDUE + "'"));

        // 平均积分（使用JdbcTemplate AVG聚合查询，避免全表加载）
        Double avgCredit = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(credit_score), ?) FROM sys_user WHERE deleted = 0 AND role IN (?, ?)",
                Double.class, (double) Constants.Credit.INITIAL_SCORE, Constants.Role.READER, Constants.Role.VOLUNTEER); 

        return StatisticsResponse.ReaderStatistics.builder()
                .totalReaders(totalReaders)
                .activeReaders(activeReaders)
                .overdueReaders(overdueReaders)
                .averageCreditScore(BigDecimal.valueOf(avgCredit).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build();
    }

    @Override
    public StatisticsResponse.SeatStatistics getSeatStatistics() {
        long totalSeats = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>().eq(Seat::getDeleted, 0));

        long availableSeats = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getDeleted, 0)
                        .eq(Seat::getStatus, Constants.SeatStatus.AVAILABLE)); 

        long occupiedSeats = totalSeats - availableSeats;

        // 今日预约
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayReservations = seatReservationMapper.selectCount(
                new LambdaQueryWrapper<SeatReservation>()
                        .eq(SeatReservation::getDeleted, 0)
                        .eq(SeatReservation::getStatus, Constants.ReservationStatus.CHECKED_IN) 
                        .between(SeatReservation::getStartTime, todayStart, todayEnd));

        return StatisticsResponse.SeatStatistics.builder()
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .occupiedSeats(occupiedSeats)
                .todayReservations(todayReservations)
                .build();
    }

    @Override
    public List<Map<String, Object>> getBorrowTrend(Integer days) {
        // FIXED: PERF-003 使用单次聚合查询替代循环N+1查询
        if (days == null || days <= 0) {
            days = 30;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        String startDateStr = startDate.format(DateTimeFormatter.ISO_DATE);
        String endDateStr = today.format(DateTimeFormatter.ISO_DATE);
        
        // 查询借阅数量（按日期分组，参数化查询防SQL注入）
        String borrowSql = "SELECT DATE(create_time) as stat_date, COUNT(*) as count FROM borrow_record " +
            "WHERE deleted = 0 AND DATE(create_time) >= ? AND DATE(create_time) <= ? " +
            "GROUP BY DATE(create_time)";
        
        // 查询归还数量（按日期分组，参数化查询防SQL注入）
        String returnSql = "SELECT DATE(return_date) as stat_date, COUNT(*) as count FROM borrow_record " +
            "WHERE deleted = 0 AND status = ? AND DATE(return_date) >= ? AND DATE(return_date) <= ? " +
            "GROUP BY DATE(return_date)";
        
        // 使用Map存储每日统计
        Map<LocalDate, Map<String, Object>> trendMap = new LinkedHashMap<>();
        
        // 初始化所有日期
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ISO_DATE));
            dayData.put("borrows", 0L);
            dayData.put("returns", 0L);
            trendMap.put(date, dayData);
        }
        
        // 查询借阅数据（参数化查询）
        List<Map<String, Object>> borrowStats = jdbcTemplate.queryForList(borrowSql, startDateStr, endDateStr);
        for (Map<String, Object> stat : borrowStats) {
            Object dateObj = stat.get("stat_date");
            Object count = stat.get("count");
            if (dateObj != null && count != null) {
                LocalDate date = parseDate(dateObj);
                if (date != null && trendMap.containsKey(date)) {
                    trendMap.get(date).put("borrows", ((Number) count).longValue());
                }
            }
        }
        
        // 查询归还数据（参数化查询）
        List<Map<String, Object>> returnStats = jdbcTemplate.queryForList(returnSql,
            Constants.BorrowStatus.RETURNED, startDateStr, endDateStr);
        for (Map<String, Object> stat : returnStats) {
            Object dateObj = stat.get("stat_date");
            Object count = stat.get("count");
            if (dateObj != null && count != null) {
                LocalDate date = parseDate(dateObj);
                if (date != null && trendMap.containsKey(date)) {
                    trendMap.get(date).put("returns", ((Number) count).longValue());
                }
            }
        }
        
        return new ArrayList<>(trendMap.values());
    }
    
    /**
     * 解析日期对象（兼容Date和LocalDate）
     */
    private LocalDate parseDate(Object dateObj) {
        if (dateObj instanceof java.time.LocalDate) {
            return (LocalDate) dateObj;
        } else if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof java.util.Date) {
            return ((java.util.Date) dateObj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } else if (dateObj instanceof String) {
            return LocalDate.parse((String) dateObj);
        }
        return null;
    }

    @Override
    public List<BookResponse> getHotBooks(Integer limit) {
        List<Book> hotBooks = bookMapper.selectHotBooks(limit);
        return hotBooks.stream()
                .map(this::convertBookToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCategoryDistribution() {
        // 使用SQL聚合查询替代全表加载，提升性能
        String sql = "SELECT bc.id as category_id, bc.name as category_name, COUNT(b.id) as count " +
            "FROM sys_book b LEFT JOIN book_category bc ON b.category_id = bc.id " +
            "WHERE b.deleted = 0 GROUP BY bc.id, bc.name ORDER BY count DESC";
        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(sql);

        // 转换字段名格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : distribution) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", row.get("category_id"));
            item.put("categoryName", row.getOrDefault("category_name", "未分类"));
            item.put("count", row.get("count"));
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getMonthlyStats(Integer months) {
        if (months == null || months <= 0) {
            months = 12;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(months - 1).withDayOfMonth(1);

        // 单次SQL获取所有月度借阅/归还统计（替代原来的months*3次N+1查询）
        String sql =
            "SELECT " +
            "  DATE_FORMAT(create_time, '%Y-%m') as month, " +
            "  SUM(CASE WHEN 1=1 THEN 1 ELSE 0 END) as borrows, " +
            "  SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) as returns " +
            "FROM borrow_record " +
            "WHERE deleted = 0 AND DATE(create_time) >= ? AND DATE(create_time) <= ? " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
            "ORDER BY month";

        // 初始化所有月份的数据
        Map<String, Map<String, Object>> monthMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            String monthStr = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> data = new HashMap<>();
            data.put("month", monthStr);
            data.put("borrows", 0L);
            data.put("returns", 0L);
            data.put("newReaders", 0L);
            monthMap.put(monthStr, data);
        }

        // 填充借阅/归还数据
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql,
            Constants.BorrowStatus.RETURNED, startDate, today);
        for (Map<String, Object> stat : stats) {
            String monthStr = String.valueOf(stat.get("month"));
            if (monthMap.containsKey(monthStr)) {
                monthMap.get(monthStr).put("borrows", stat.get("borrows"));
                monthMap.get(monthStr).put("returns", stat.get("returns"));
            }
        }

        // 新增读者统计（单独查询）
        String readerSql =
            "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as new_readers " +
            "FROM sys_user WHERE deleted = 0 AND role = ? " +
            "AND DATE(create_time) >= ? AND DATE(create_time) <= ? " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m')";
        List<Map<String, Object>> readerStats = jdbcTemplate.queryForList(readerSql,
            Constants.Role.READER, startDate, today);
        for (Map<String, Object> stat : readerStats) {
            String monthStr = String.valueOf(stat.get("month"));
            if (monthMap.containsKey(monthStr)) {
                monthMap.get(monthStr).put("newReaders", stat.get("new_readers"));
            }
        }

        return new ArrayList<>(monthMap.values());
    }

    /**
     * 计算平均借阅天数（使用SQL聚合查询，避免全表加载）
     */
    private Double calculateAverageBorrowDays() {
        // 使用SQL聚合查询直接计算平均天数
        String sql = "SELECT AVG(DATEDIFF(return_date, borrow_date)) as avg_days " +
            "FROM borrow_record " +
            "WHERE deleted = 0 AND status = ? " +
            "AND return_date IS NOT NULL AND borrow_date IS NOT NULL";

        Double avgDays = jdbcTemplate.queryForObject(sql, Double.class, 
            Constants.BorrowStatus.RETURNED);

        return avgDays != null ? BigDecimal.valueOf(avgDays)
            .setScale(1, RoundingMode.HALF_UP).doubleValue() : 14.0;
    }

    @Override
    public List<Map<String, Object>> getSeatHeatmap() {
        // 查询今日各时间段各区域的预约使用率
        LocalDate today = LocalDate.now();
        String sql = "SELECT " +
            "  SUBSTRING_INDEX(s.seat_number, '-', 1) as area, " +
            "  HOUR(sr.start_time) as hour_slot, " +
            "  COUNT(*) as reserved_count, " +
            "  (SELECT COUNT(*) FROM seat WHERE deleted = 0) as total_seats " +
            "FROM seat_reservation sr " +
            "JOIN seat s ON sr.seat_id = s.id " +
            "WHERE sr.deleted = 0 " +
            "  AND DATE(sr.start_time) = ? " +
            "  AND sr.status IN ('PENDING', 'CHECKED_IN', 'COMPLETED') " +
            "GROUP BY SUBSTRING_INDEX(s.seat_number, '-', 1), HOUR(sr.start_time) " +
            "ORDER BY area, hour_slot";

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sql, today);

        // 构建完整的区域×时间段矩阵
        String[] areas = {"A", "B", "C"};
        String[] areaLabels = {"A区-安静区", "B区-讨论区", "C区-电脑区"};
        List<Map<String, Object>> result = new ArrayList<>();

        // 按时间段和区域组织数据
        Map<String, Map<String, Object>> dataMap = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String area = (String) row.get("area");
            Object hourObj = row.get("hour_slot");
            if (area == null || hourObj == null) continue;
            String key = area + ":" + hourObj;
            dataMap.put(key, row);
        }

        long totalSeats = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>().eq(Seat::getDeleted, 0));

        // 生成8:00-22:00每个时段的数据（每2小时一个时段）
        for (String area : areas) {
            for (int hour = 8; hour < 22; hour += 2) {
                String key = area + ":" + hour;
                Map<String, Object> cell;
                if (dataMap.containsKey(key)) {
                    cell = new HashMap<>(dataMap.get(key));
                    Number reservedCount = (Number) cell.get("reserved_count");
                    double rate = totalSeats > 0
                            ? (double) reservedCount.longValue() / totalSeats * 100
                            : 0;
                    cell.put("usageRate", Math.round(rate * 10.0) / 10.0);
                } else {
                    cell = new HashMap<>();
                    cell.put("area", area);
                    cell.put("hourSlot", hour + ":00-" + (hour + 2) + ":00");
                    cell.put("reservedCount", 0);
                    cell.put("usageRate", 0.0);
                }
                cell.put("areaLabel", getAreaLabel(area, areaLabels, areas));
                cell.put("hourSlot", hour + ":00-" + (hour + 2) + ":00");
                result.add(cell);
            }
        }

        return result;
    }

    private String getAreaLabel(String area, String[] areaLabels, String[] areas) {
        for (int i = 0; i < areas.length; i++) {
            if (areas[i].equals(area)) return areaLabels[i];
        }
        return area;
    }

    /**
     * 将Book实体转换为BookResponse DTO
     */
    private BookResponse convertBookToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate() != null ? book.getPublishDate().toString() : null)
                .categoryId(book.getCategoryId())
                .categoryName(book.getCategoryName())
                .description(book.getDescription())
                .coverImage(book.getCoverImage())
                .location(book.getLocation())
                .totalCount(book.getTotalCount())
                .availableCount(book.getAvailableCount())
                .price(book.getPrice())
                .borrowCount(book.getBorrowCount())
                .status(book.getStatus())
                .createTime(book.getCreateTime())
                .updateTime(book.getUpdateTime())
                .build();
    }
}
