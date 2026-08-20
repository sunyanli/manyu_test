package com.manyu.algodemo.tracking.service.impl;

import com.manyu.algodemo.common.exception.BizException;
import com.manyu.algodemo.common.exception.ErrorCode;
import com.manyu.algodemo.tracking.dao.TrackingMapper;
import com.manyu.algodemo.tracking.model.dto.OverviewVO;
import com.manyu.algodemo.tracking.model.dto.StatsVO;
import com.manyu.algodemo.tracking.model.dto.TrendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 埋点统计服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private TrackingMapper trackingMapper;

    private TrackingServiceImpl trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new TrackingServiceImpl(trackingMapper);
    }

    @Test
    @DisplayName("概况查询正确组装成功率与调用人数")
    void should_buildOverview() {
        when(trackingMapper.selectOverview(any(), any())).thenReturn(Map.of(
                "totalCalls", 100L,
                "totalCallers", 8L,
                "successCount", 95L,
                "avgCostTimeMs", 35L));
        when(trackingMapper.selectTopCaller(any(), any()))
                .thenReturn(Map.of("name", "张三", "calls", 42L));

        OverviewVO vo = trackingService.overview(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(vo.getTotalCalls()).isEqualTo(100L);
        assertThat(vo.getTotalCallers()).isEqualTo(8L);
        assertThat(vo.getSuccessRate()).isEqualByComparingTo("95.00");
        assertThat(vo.getAvgCostTimeMs()).isEqualTo(35L);
        assertThat(vo.getTopCaller().getName()).isEqualTo("张*");
        assertThat(vo.getTopCaller().getCalls()).isEqualTo(42L);
    }

    @Test
    @DisplayName("起始时间晚于截止时间抛 TRACKING_001")
    void should_throw_whenRangeInverted() {
        assertThatThrownBy(() -> trackingService.overview(LocalDateTime.now(), LocalDateTime.now().minusDays(1)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.TRACKING_001.getCode());
    }

    @Test
    @DisplayName("时间跨度超 90 天抛 TRACKING_003")
    void should_throw_whenRangeTooWide() {
        assertThatThrownBy(() -> trackingService.overview(
                LocalDateTime.now().minusDays(100), LocalDateTime.now()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.TRACKING_003.getCode());
    }

    @Test
    @DisplayName("不支持的维度抛 TRACKING_002")
    void should_throw_whenDimensionInvalid() {
        assertThatThrownBy(() -> trackingService.stats("CALLER_ADDRESS",
                LocalDateTime.now().minusDays(7), LocalDateTime.now()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.TRACKING_002.getCode());
    }

    @Test
    @DisplayName("维度统计组装名称/次数/占比")
    void should_buildStats() {
        when(trackingMapper.selectStatsByDimension(any(), any(), any(), any())).thenReturn(List.of(
                Map.of("name", "EMPLOYEE", "value", 1200L),
                Map.of("name", "OUTSOURCER", "value", 300L)));

        StatsVO vo = trackingService.stats("CALLER_TYPE",
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(vo.getDimension()).isEqualTo("CALLER_TYPE");
        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getItems().get(0).getPercent()).isEqualByComparingTo("80.0");
        assertThat(vo.getItems().get(1).getPercent()).isEqualByComparingTo("20.0");
    }

    @Test
    @DisplayName("趋势查询默认 DAY 粒度并组装时间点")
    void should_buildTrend() {
        when(trackingMapper.selectTrend(any(), any(), any())).thenReturn(List.of(
                Map.of("time", "2026-08-19", "calls", 82L, "successCount", 82L),
                Map.of("time", "2026-08-20", "calls", 100L, "successCount", 99L)));

        TrendVO vo = trackingService.trend(null, LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(vo.getGranularity()).isEqualTo("DAY");
        assertThat(vo.getPoints()).hasSize(2);
        assertThat(vo.getPoints().get(0).getTime()).isEqualTo("2026-08-19");
        assertThat(vo.getPoints().get(0).getSuccessRate()).isEqualByComparingTo("100.0");
        assertThat(vo.getPoints().get(1).getSuccessRate()).isEqualByComparingTo("99.0");
    }
}
