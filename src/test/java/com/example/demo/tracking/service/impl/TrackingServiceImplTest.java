package com.example.demo.tracking.service.impl;

import com.example.demo.tracking.dao.mapper.ApiCallLogMapper;
import com.example.demo.tracking.model.entity.ApiCallLog;
import com.example.demo.tracking.model.request.CallStatsRequest;
import com.example.demo.tracking.model.request.DimensionStatsRequest;
import com.example.demo.tracking.model.vo.CallStatsVO;
import com.example.demo.tracking.model.vo.DimensionStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 埋点服务单元测试
 *
 * @author AiWork
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("埋点服务测试")
class TrackingServiceImplTest {

    @Mock
    private ApiCallLogMapper apiCallLogMapper;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(trackingService, "trackingEnabled", true);
    }

    // ==================== recordCall ====================

    @Test
    @DisplayName("recordCall：正常记录应持久化到数据库")
    void shouldInsertRecord_whenRecordCall() {
        trackingService.recordCall("helloworld", "user001");

        ArgumentCaptor<ApiCallLog> captor = ArgumentCaptor.forClass(ApiCallLog.class);
        verify(apiCallLogMapper, times(1)).insert(captor.capture());
        ApiCallLog log = captor.getValue();
        assertEquals("helloworld", log.getApiName());
        assertEquals("user001", log.getUserId());
    }

    @Test
    @DisplayName("recordCall：tracking.enabled=false 应跳过记录")
    void shouldSkipRecord_whenTrackingDisabled() {
        ReflectionTestUtils.setField(trackingService, "trackingEnabled", false);

        trackingService.recordCall("helloworld", "user001");

        verify(apiCallLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("recordCall：数据库异常不应向上抛出")
    void shouldNotThrowException_whenDbError() {
        when(apiCallLogMapper.insert(any())).thenThrow(new RuntimeException("DB error"));

        // 不应抛出异常
        trackingService.recordCall("helloworld", "user001");
    }

    // ==================== queryCallStats ====================

    @Test
    @DisplayName("queryCallStats：正常查询应返回时序数据")
    void shouldReturnSeriesData_whenQueryCallStats() {
        CallStatsRequest request = new CallStatsRequest();
        request.setStartTime("2026-09-01");
        request.setEndTime("2026-09-03");

        List<Map<String, Object>> mockRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("time", "2026-09-01");
        row1.put("count", 10L);
        mockRows.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("time", "2026-09-02");
        row2.put("count", 20L);
        mockRows.add(row2);

        when(apiCallLogMapper.callStatsByDay(any(), anyString(), anyString()))
                .thenReturn(mockRows);

        CallStatsVO result = trackingService.queryCallStats(request);

        assertNotNull(result);
        assertEquals(2, result.getSeries().size());
        assertEquals(30L, result.getTotal());
        assertEquals("2026-09-01", result.getSeries().get(0).getTime());
        assertEquals(10L, result.getSeries().get(0).getCount());
    }

    @Test
    @DisplayName("queryCallStats：无数据时应返回空序列")
    void shouldReturnEmptySeries_whenNoData() {
        CallStatsRequest request = new CallStatsRequest();
        request.setStartTime("2026-09-01");
        request.setEndTime("2026-09-03");

        when(apiCallLogMapper.callStatsByDay(any(), anyString(), anyString()))
                .thenReturn(new ArrayList<>());

        CallStatsVO result = trackingService.queryCallStats(request);

        assertNotNull(result);
        assertTrue(result.getSeries().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    // ==================== queryDimensionStats ====================

    @Test
    @DisplayName("queryDimensionStats：正常查询应返回维度统计")
    void shouldReturnDimensionData_whenQueryDimensionStats() {
        DimensionStatsRequest request = new DimensionStatsRequest();
        request.setStartTime("2026-09-01");
        request.setEndTime("2026-09-03");
        request.setDimension("user_type");

        List<Map<String, Object>> mockRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("label", "staff");
        row1.put("count", 100L);
        mockRows.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("label", "contractor");
        row2.put("count", 50L);
        mockRows.add(row2);

        when(apiCallLogMapper.dimensionStats(anyString(), anyString(), anyString()))
                .thenReturn(mockRows);

        DimensionStatsVO result = trackingService.queryDimensionStats(request);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals("staff", result.getItems().get(0).getLabel());
        assertEquals(100L, result.getItems().get(0).getCount());
        // 100/150 ≈ 66.67%
        assertTrue(result.getItems().get(0).getPercentage() > 66.0);
    }

    @Test
    @DisplayName("queryDimensionStats：无数据时应返回空列表")
    void shouldReturnEmptyItems_whenNoDimensionData() {
        DimensionStatsRequest request = new DimensionStatsRequest();
        request.setStartTime("2026-09-01");
        request.setEndTime("2026-09-03");
        request.setDimension("user_type");

        when(apiCallLogMapper.dimensionStats(anyString(), anyString(), anyString()))
                .thenReturn(new ArrayList<>());

        DimensionStatsVO result = trackingService.queryDimensionStats(request);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }
}