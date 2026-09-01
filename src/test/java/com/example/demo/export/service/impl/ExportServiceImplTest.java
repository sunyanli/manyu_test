package com.example.demo.export.service.impl;

import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.export.model.request.ExportRequest;
import com.example.demo.tracking.dao.mapper.ApiCallLogMapper;
import com.example.demo.tracking.model.entity.ApiCallLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 导出服务单元测试
 *
 * @author AiWork
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("导出服务测试")
class ExportServiceImplTest {

    @Mock
    private ApiCallLogMapper apiCallLogMapper;

    @InjectMocks
    private ExportServiceImpl exportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "exportEnabled", true);
        ReflectionTestUtils.setField(exportService, "maxRecords", 10000);
    }

    // ==================== 合法导出类型 ====================

    @Test
    @DisplayName("exportData：合法导出类型应返回 Excel 字节数组")
    void shouldReturnExcelBytes_whenValidExportType() {
        ExportRequest request = new ExportRequest();
        request.setExportType("helloworld");

        List<ApiCallLog> mockLogs = new ArrayList<>();
        ApiCallLog log = new ApiCallLog();
        log.setApiName("helloworld");
        log.setUserId("user001");
        log.setResponseCode("OK");
        log.setGmtCreate(new Date());
        mockLogs.add(log);

        when(apiCallLogMapper.selectForExport(eq("helloworld"), anyString(), anyString(), anyInt()))
                .thenReturn(mockLogs);

        byte[] result = exportService.exportData(request);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("exportData：空数据时应返回仅含表头的 Excel")
    void shouldReturnHeaderOnlyExcel_whenNoData() {
        ExportRequest request = new ExportRequest();
        request.setExportType("hash");

        when(apiCallLogMapper.selectForExport(eq("hash"), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());

        byte[] result = exportService.exportData(request);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ==================== 非法导出类型 ====================

    @Test
    @DisplayName("exportData：非法导出类型应抛出 BusinessException")
    void shouldThrowException_whenInvalidExportType() {
        ExportRequest request = new ExportRequest();
        request.setExportType("invalid_type");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> exportService.exportData(request));
        assertEquals(ErrorCodeEnum.EXP_001.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("exportData：null request 应抛出 BusinessException")
    void shouldThrowException_whenRequestNull() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> exportService.exportData(null));
        assertEquals(ErrorCodeEnum.EXP_001.getCode(), exception.getErrorCode());
    }

    // ==================== 导出功能禁用 ====================

    @Test
    @DisplayName("exportData：export.enabled=false 应抛出异常")
    void shouldThrowException_whenExportDisabled() {
        ReflectionTestUtils.setField(exportService, "exportEnabled", false);

        ExportRequest request = new ExportRequest();
        request.setExportType("helloworld");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> exportService.exportData(request));
        assertEquals(ErrorCodeEnum.EXP_003.getCode(), exception.getErrorCode());
    }
}