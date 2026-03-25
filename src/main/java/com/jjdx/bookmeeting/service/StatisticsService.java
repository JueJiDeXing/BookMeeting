// StatisticsService.java
package com.jjdx.bookmeeting.service;

import com.jjdx.bookmeeting.model.dto.admin.statistics.StatisticsRequest;
import com.jjdx.bookmeeting.model.vo.admin.statistics.StatisticsVO;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取统计数据
     */
    StatisticsVO getStatistics(StatisticsRequest request);
    
    /**
     * 导出统计数据报表（CSV格式）
     */
    String exportStatistics(StatisticsRequest request);
}
