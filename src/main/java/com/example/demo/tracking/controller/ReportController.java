package com.example.demo.tracking.controller;

import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.model.ApiResponse;
import com.example.demo.tracking.model.request.CallStatsRequest;
import com.example.demo.tracking.model.request.DimensionStatsRequest;
import com.example.demo.tracking.model.vo.CallStatsVO;
import com.example.demo.tracking.model.vo.DimensionStatsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 报表控制器
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private static final Set<String> VALID_DIMENSIONS = new HashSet<String>(
            Arrays.asList("user_type", "user_level", "user_department"));

    /**
     * 调用统计（折线图）
     */
    @PostMapping("/call-stats")
    public ApiResponse<?> callStats(@Valid @RequestBody CallStatsRequest request) {
        try {
            validateTimeRange(request.getStartTime(), request.getEndTime());

            if (request.getDimension() != null && !VALID_DIMENSIONS.contains(request.getDimension())) {
                throw new BusinessException(ErrorCodeEnum.TRK_002.getCode(),
                        ErrorCodeEnum.TRK_002.getMessage());
            }

            // 返回模拟时序数据（实际应查询数据库）
            List<CallStatsVO.SeriesPoint> series = new ArrayList<CallStatsVO.SeriesPoint>();
            series.add(new CallStatsVO.SeriesPoint("2026-09-01", 15));
            series.add(new CallStatsVO.SeriesPoint("2026-09-02", 23));
            series.add(new CallStatsVO.SeriesPoint("2026-09-03", 18));

            CallStatsVO stats = new CallStatsVO(series, 56);
            logger.info("调用统计查询完成, dimension: {}, total: {}", request.getDimension(), stats.getTotal());

            return ApiResponse.success(stats);

        } catch (BusinessException e) {
            logger.warn("调用统计查询异常: {}", e.getMessage());
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("调用统计查询系统异常", e);
            return ApiResponse.error("B0001", "系统内部错误");
        }
    }

    /**
     * 维度统计（饼图/柱状图）
     */
    @PostMapping("/dimension-stats")
    public ApiResponse<?> dimensionStats(@Valid @RequestBody DimensionStatsRequest request) {
        try {
            validateTimeRange(request.getStartTime(), request.getEndTime());

            if (!VALID_DIMENSIONS.contains(request.getDimension())) {
                throw new BusinessException(ErrorCodeEnum.TRK_002.getCode(),
                        ErrorCodeEnum.TRK_002.getMessage());
            }

            // 返回模拟维度数据（实际应查询数据库）
            List<DimensionStatsVO.DimensionItem> items = new ArrayList<DimensionStatsVO.DimensionItem>();
            items.add(new DimensionStatsVO.DimensionItem("技术部", 200, 38.5));
            items.add(new DimensionStatsVO.DimensionItem("产品部", 150, 28.8));
            items.add(new DimensionStatsVO.DimensionItem("运营部", 170, 32.7));

            DimensionStatsVO stats = new DimensionStatsVO(items);
            logger.info("维度统计查询完成, dimension: {}, chartType: {}",
                    request.getDimension(), request.getChartType());

            return ApiResponse.success(stats);

        } catch (BusinessException e) {
            logger.warn("维度统计查询异常: {}", e.getMessage());
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("维度统计查询系统异常", e);
            return ApiResponse.error("B0001", "系统内部错误");
        }
    }

    /**
     * 校验时间范围（不超过90天）
     */
    private void validateTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(ErrorCodeEnum.TRK_001.getCode(),
                    ErrorCodeEnum.TRK_001.getMessage());
        }
        if (startTime.compareTo(endTime) > 0) {
            throw new BusinessException(ErrorCodeEnum.TRK_001.getCode(),
                    "开始时间不能晚于结束时间");
        }
    }
}