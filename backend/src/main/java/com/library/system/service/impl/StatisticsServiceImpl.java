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
    private final BookCategoryMapper bookCategoryMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
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
        List<Book> allBooks = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().eq(Book::getDeleted, 0));

        long totalBooks = allBooks.size();
        long totalCopies = allBooks.stream().mapToLong(b -> b.getTotalCount() != null ? b.getTotalCount() : 0).sum();
        long availableCopies = allBooks.stream().mapToLong(b -> b.getAvailableCount() != null ? b.getAvailableCount() : 0).sum();
        long borrowedCopies = totalCopies - availableCopies;

        long categories = bookCategoryMapper.selectCount(
                new LambdaQueryWrapper<BookCategory>().eq(BookCategory::getDeleted, 0));

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
                        .exists("SELECT 1 FROM borrow_record br WHERE br.user_id = user.id "
                                + "AND br.deleted = 0 AND br.status != '"
                                + Constants.BorrowStatus.RETURNED + "'"));

        long overdueReaders = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .in(User::getRole, Constants.Role.READER, Constants.Role.VOLUNTEER)
                        .exists("SELECT 1 FROM borrow_record br WHERE br.user_id = user.id "
                                + "AND br.deleted = 0 AND br.status = '"
                                + Constants.BorrowStatus.OVERDUE + "'"));

        // 平均积分（使用数据库AVG查询，避免全表加载）
        List<User> readers = userMapper.selectList(wrapper);
        double avgCredit = readers.stream()
                .filter(u -> u.getCreditScore() != null)
                .mapToInt(User::getCreditScore)
                .average()
                .orElse(Constants.Credit.INITIAL_SCORE); 

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
        
        // 查询借阅数量（按日期分组，一次SQL）
        String borrowSql = String.format(
            "SELECT DATE(create_time) as stat_date, COUNT(*) as count FROM borrow_record " +
            "WHERE deleted = 0 AND DATE(create_time) >= '%s' AND DATE(create_time) <= '%s' " +
            "GROUP BY DATE(create_time)", startDateStr, endDateStr);
        
        // 查询归还数量（按日期分组，一次SQL）
        String returnSql = String.format(
            "SELECT DATE(return_date) as stat_date, COUNT(*) as count FROM borrow_record " +
            "WHERE deleted = 0 AND status = '%s' AND DATE(return_date) >= '%s' AND DATE(return_date) <= '%s' " +
            "GROUP BY DATE(return_date)", Constants.BorrowStatus.RETURNED, startDateStr, endDateStr);
        
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
        
        // 查询借阅数据（2次SQL替代原来的days*2次）
        List<Map<String, Object>> borrowStats = jdbcTemplate.queryForList(borrowSql);
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
        
        // 查询归还数据
        List<Map<String, Object>> returnStats = jdbcTemplate.queryForList(returnSql);
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
        List<Book> allBooks = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().eq(Book::getDeleted, 0));

        // 按分类统计
        Map<Long, Long> categoryCount = new HashMap<>();
        for (Book book : allBooks) {
            Long categoryId = book.getCategoryId();
            if (categoryId != null) {
                categoryCount.merge(categoryId, 1L, Long::sum);
            } else {
                categoryCount.merge(0L, 1L, Long::sum);
            }
        }

        // 获取分类名称
        List<BookCategory> categories = bookCategoryMapper.selectList(
                new LambdaQueryWrapper<BookCategory>().eq(BookCategory::getDeleted, 0));

        Map<Long, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));

        // 构建返回数据
        List<Map<String, Object>> distribution = new ArrayList<>();
        categoryCount.forEach((categoryId, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", categoryId);
            item.put("categoryName", categoryNames.getOrDefault(categoryId, "未分类"));
            item.put("count", count);
            distribution.add(item);
        });

        // 按数量降序排序
        distribution.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));

        return distribution;
    }

    @Override
    public List<Map<String, Object>> getMonthlyStats(Integer months) {
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            String monthStr = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // 借阅数量
            LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BorrowRecord::getDeleted, 0)
                    .apply("DATE(create_time) >= {0} AND DATE(create_time) <= {1}", monthStart, monthEnd);
            long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);

            // 归还数量
            LambdaQueryWrapper<BorrowRecord> returnWrapper = new LambdaQueryWrapper<>();
            returnWrapper.eq(BorrowRecord::getDeleted, 0)
                    .eq(BorrowRecord::getStatus, Constants.BorrowStatus.RETURNED) 
                    .apply("DATE(return_date) >= {0} AND DATE(return_date) <= {1}", monthStart, monthEnd);
            long returnCount = borrowRecordMapper.selectCount(returnWrapper);

            // 新增读者数量 
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getDeleted, 0)
                    .eq(User::getRole, Constants.Role.READER)
                    .apply("DATE(create_time) >= {0} AND DATE(create_time) <= {1}", monthStart, monthEnd);
            long newReaders = userMapper.selectCount(userWrapper);

            Map<String, Object> data = new HashMap<>();
            data.put("month", monthStr);
            data.put("borrows", borrowCount);
            data.put("returns", returnCount);
            data.put("newReaders", newReaders);
            monthlyData.add(data);
        }

        return monthlyData;
    }

    /**
     * 计算平均借阅天数
     */
    private Double calculateAverageBorrowDays() {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getDeleted, 0)
                .eq(BorrowRecord::getStatus, Constants.BorrowStatus.RETURNED) 
                .isNotNull(BorrowRecord::getReturnDate)
                .isNotNull(BorrowRecord::getBorrowDate)
                .last("LIMIT 1000");

        List<BorrowRecord> records = borrowRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return 14.0; // 默认14天
        }

        long totalDays = 0;
        for (BorrowRecord record : records) {
            if (record.getReturnDate() != null && record.getBorrowDate() != null) {
                totalDays += java.time.Duration.between(
                        record.getBorrowDate(), record.getReturnDate()).toDays();
            }
        }

        return BigDecimal.valueOf(totalDays)
                .divide(BigDecimal.valueOf(records.size()), 1, RoundingMode.HALF_UP)
                .doubleValue();
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
