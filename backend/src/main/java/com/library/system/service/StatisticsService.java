package com.library.system.service;

import com.library.system.dto.BookResponse;
import com.library.system.dto.StatisticsResponse;

import java.util.List;
import java.util.Map;

/**
 * 统计分析服务接口 
 * <p>
 * 提供系统各类统计数据的查询，包括综合概览、借阅统计、图书统计、
 * 读者统计、座位统计等。将业务逻辑从Controller层剥离。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface StatisticsService {

    /**
     * 获取综合统计概览
     */
    StatisticsResponse getOverview();

    /**
     * 获取借阅统计
     */
    StatisticsResponse.BorrowStatistics getBorrowStatistics();

    /**
     * 获取图书统计
     */
    StatisticsResponse.BookStatistics getBookStatistics();

    /**
     * 获取读者统计
     */
    StatisticsResponse.ReaderStatistics getReaderStatistics();

    /**
     * 获取座位统计
     */
    StatisticsResponse.SeatStatistics getSeatStatistics();

    /**
     * 获取借阅趋势
     */
    List<Map<String, Object>> getBorrowTrend(Integer days);

    /**
     * 获取热门图书
     */
    List<BookResponse> getHotBooks(Integer limit);

    /**
     * 获取分类分布
     */
    List<Map<String, Object>> getCategoryDistribution();

    /**
     * 获取月度统计
     */
    List<Map<String, Object>> getMonthlyStats(Integer months);

    /**
     * 获取座位使用率热力图数据（论文§5.2(4)）
     * 返回各时间段各区域的座位使用率
     */
    List<Map<String, Object>> getSeatHeatmap();
}
