// StatisticsController.java
package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.statistics.StatisticsRequest;
import com.jjdx.bookmeeting.model.vo.admin.statistics.StatisticsVO;
import com.jjdx.bookmeeting.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统计接口
 */
@RestController
@RequestMapping("/admin/statistics")
@Slf4j
public class AdminStatisticsController {
    
    @Resource
    private StatisticsService statisticsService;
    
    /**
     * 获取统计数据
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<StatisticsVO> getStatistics(@RequestBody StatisticsRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        StatisticsVO statistics = statisticsService.getStatistics(request);
        return ResultUtils.success(statistics);
    }
    
    /**
     * 导出统计报表
     */
    @PostMapping("/export")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void exportStatistics(@RequestBody StatisticsRequest request, HttpServletResponse response) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        String csvData = statisticsService.exportStatistics(request);
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=statistics.csv");
        
        try {
            response.getOutputStream().write(csvData.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("导出统计报表失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出失败");
        }
    }
}
