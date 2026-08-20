package com.manyu.algodemo.export.service.impl;

import com.manyu.algodemo.common.exception.BizException;
import com.manyu.algodemo.common.exception.ErrorCode;
import com.manyu.algodemo.export.model.dto.ExportRequest;
import com.manyu.algodemo.export.model.enums.ExportFormat;
import com.manyu.algodemo.export.model.enums.ExportTarget;
import com.manyu.algodemo.export.service.ExportService;
import com.manyu.algodemo.export.util.CsvExportUtil;
import com.manyu.algodemo.tracking.annotation.TrackCall;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import com.manyu.algodemo.tracking.model.enums.BizType;
import com.manyu.algodemo.tracking.service.TrackingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * 页面结果导出服务实现，导出动作通过 {@link TrackCall} 计入埋点。
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter FILE_NAME_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CSV_CONTENT_TYPE = "text/csv;charset=utf-8";

    private final TrackingService trackingService;
    private final boolean enabled;
    private final Semaphore concurrencyGate;
    private final int pageRecordLimit;

    /**
     * 构造器注入。
     *
     * @param trackingService  埋点统计服务（页面记录与报表数据源）
     * @param enabled          导出开关
     * @param maxConcurrency   并发导出台数上限
     * @param pageRecordLimit  页面导出默认记录条数
     */
    public ExportServiceImpl(
            TrackingService trackingService,
            @Value("${export.enabled:true}") boolean enabled,
            @Value("${export.max-concurrency:5}") int maxConcurrency,
            @Value("${export.page-record-limit:100}") int pageRecordLimit) {
        this.trackingService = trackingService;
        this.enabled = enabled;
        this.concurrencyGate = new Semaphore(maxConcurrency);
        this.pageRecordLimit = pageRecordLimit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TrackCall(type = BizType.EXPORT)
    public ExportFile export(ExportRequest request) {
        if (!enabled) {
            throw new BizException(ErrorCode.EXPORT_001, "导出功能已关闭");
        }
        ExportTarget target = parseTarget(request.getTarget());
        ExportFormat format = ExportFormat.parse(request.getFormat());
        LocalDateTime end = request.getEndTime() == null || request.getEndTime().isBlank()
                ? LocalDateTime.now()
                : LocalDateTime.parse(request.getEndTime(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime start = request.getStartTime() == null || request.getStartTime().isBlank()
                ? end.minusDays(30)
                : LocalDateTime.parse(request.getStartTime(), DateTimeFormatter.ISO_DATE_TIME);

        try {
            if (!concurrencyGate.tryAcquire()) {
                throw new BizException(ErrorCode.EXPORT_001, "导出并发超限，请稍后重试");
            }
            byte[] content = buildContent(target, format, start, end);
            String fileName = target.name().toLowerCase(Locale.ROOT) + "_page_"
                    + LocalDateTime.now().format(FILE_NAME_DATE)
                    + (format == ExportFormat.CSV ? ".csv" : ".xlsx");
            return new ExportFile(fileName, CSV_CONTENT_TYPE, content);
        } finally {
            concurrencyGate.release();
        }
    }

    private byte[] buildContent(ExportTarget target, ExportFormat format, LocalDateTime start, LocalDateTime end) {
        if (format != ExportFormat.CSV) {
            throw new BizException(ErrorCode.EXPORT_001, "暂不支持 " + format + " 格式");
        }
        if (target == ExportTarget.REPORT) {
            return CsvExportUtil.toCsv(buildReportHeader(), buildReportRows(start, end));
        }
        return CsvExportUtil.toCsv(buildPageHeader(), buildPageRows(target, start, end));
    }

    private List<String> buildPageHeader() {
        return List.of("调用时间", "业务类型", "调用人姓名", "人员类型", "人员层级", "部门",
                "入参摘要", "出参摘要", "耗时(ms)", "结果状态", "错误码");
    }

    private List<List<String>> buildPageRows(ExportTarget target, LocalDateTime start, LocalDateTime end) {
        List<CallRecordDO> records =
                trackingService.pageRecords(target.name(), start, end, pageRecordLimit);
        if (records.isEmpty()) {
            throw new BizException(ErrorCode.EXPORT_002);
        }
        List<List<String>> rows = new ArrayList<>();
        for (CallRecordDO r : records) {
            rows.add(List.of(
                    String.valueOf(r.getGmtCreate()),
                    r.getBizType(),
                    r.getCallerName(),
                    r.getCallerType(),
                    r.getCallerLevel(),
                    r.getCallerDeptName(),
                    nullToEmpty(r.getReqSummary()),
                    nullToEmpty(r.getRespSummary()),
                    String.valueOf(r.getCostTimeMs()),
                    r.getResultStatus(),
                    nullToEmpty(r.getErrorCode())
            ));
        }
        return rows;
    }

    private List<String> buildReportHeader() {
        return List.of("报表项", "指标值");
    }

    private List<List<String>> buildReportRows(LocalDateTime start, LocalDateTime end) {
        List<List<String>> rows = new ArrayList<>();
        var overview = trackingService.overview(start, end);
        rows.add(List.of("统计范围", overview.getPeriod().getStartTime() + " ~ " + overview.getPeriod().getEndTime()));
        rows.add(List.of("总调用次数", String.valueOf(overview.getTotalCalls())));
        rows.add(List.of("调用人数", String.valueOf(overview.getTotalCallers())));
        rows.add(List.of("成功率(%)", String.valueOf(overview.getSuccessRate())));
        rows.add(List.of("平均耗时(ms)", String.valueOf(overview.getAvgCostTimeMs())));
        rows.add(List.of("调用最多的人", overview.getTopCaller().getName()));
        rows.add(List.of("人数分布-人员类型", formatStatsRow(trackingService.stats("CALLER_TYPE", start, end).getItems())));
        rows.add(List.of("人数分布-人员层级", formatStatsRow(trackingService.stats("CALLER_LEVEL", start, end).getItems())));
        rows.add(List.of("人数分布-人员部门", formatStatsRow(trackingService.stats("CALLER_DEPT", start, end).getItems())));
        rows.add(List.of("人数分布-业务类型", formatStatsRow(trackingService.stats("BIZ_TYPE", start, end).getItems())));
        rows.add(List.of("趋势-按天", formatTrendRow(trackingService.trend("DAY", start, end).getPoints())));
        return rows;
    }

    private String formatStatsRow(java.util.List<com.manyu.algodemo.tracking.model.dto.StatsItemVO> items) {
        StringBuilder sb = new StringBuilder();
        for (var item : items) {
            sb.append(item.getName()).append('=').append(item.getValue()).append(";");
        }
        return sb.toString();
    }

    private String formatTrendRow(java.util.List<com.manyu.algodemo.tracking.model.dto.TrendPointVO> points) {
        StringBuilder sb = new StringBuilder();
        for (var p : points) {
            sb.append(p.getTime()).append('=').append(p.getCalls()).append(";");
        }
        return sb.toString();
    }

    private ExportTarget parseTarget(String target) {
        try {
            return ExportTarget.valueOf(target.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.EXPORT_001, "不支持的导出目标: " + target);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
