package com.example.demo.tracking.controller;

import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.model.ApiResponse;
import com.example.demo.tracking.model.request.CallStatsRequest;
import com.example.demo.tracking.model.request.DimensionStatsRequest;
import com.example.demo.tracking.model.vo.CallStatsVO;
import com.example.demo.tracking.model.vo.DimensionStatsVO;
import com.example.demo.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    private static final long MAX_DAYS = 90L;

    private final TrackingService trackingService;

    public ReportController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * 调用统计（折线图）
     */
    @PostMapping("/call-stats")
    public ApiResponse<CallStatsVO> callStats(@Valid @RequestBody CallStatsRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        if (request.getDimension() != null && !VALID_DIMENSIONS.contains(request.getDimension())) {
            throw new BusinessException(ErrorCodeEnum.TRK_002.getCode(),
                    ErrorCodeEnum.TRK_002.getMessage());
        }

        CallStatsVO stats = trackingService.queryCallStats(request);
        logger.info("调用统计查询完成, dimension: {}, total: {}", request.getDimension(), stats.getTotal());
        return ApiResponse.success(stats);
    }

    /**
     * 维度统计（饼图/柱状图）
     */
    @PostMapping("/dimension-stats")
    public ApiResponse<DimensionStatsVO> dimensionStats(@Valid @RequestBody DimensionStatsRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        if (!VALID_DIMENSIONS.contains(request.getDimension())) {
            throw new BusinessException(ErrorCodeEnum.TRK_002.getCode(),
                    ErrorCodeEnum.TRK_002.getMessage());
        }

        DimensionStatsVO stats = trackingService.queryDimensionStats(request);
        logger.info("维度统计查询完成, dimension: {}, chartType: {}",
                request.getDimension(), request.getChartType());
        return ApiResponse.success(stats);
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

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);
            long diffInMillis = end.getTime() - start.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            if (diffInDays > MAX_DAYS) {
                throw new BusinessException(ErrorCodeEnum.TRK_001.getCode(),
                        ErrorCodeEnum.TRK_001.getMessage());
            }
        } catch (java.text.ParseException e) {
            throw new BusinessException(ErrorCodeEnum.TRK_001.getCode(),
                    "时间格式错误，请使用 yyyy-MM-dd 格式");
        }
    }
}