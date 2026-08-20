package com.manyu.algodemo.export.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV 导出工具测试：公式注入防护与字段转义。
 */
class CsvExportUtilTest {

    @Test
    @DisplayName("公式注入前缀被转义")
    void should_escapeFormulaPrefix() {
        byte[] csv = CsvExportUtil.toCsv(
                List.of("字段"),
                List.of(List.of("=SUM(A1)", "+1", "-2", "@cmd", "正常值")));
        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).contains("'=SUM(A1)").contains("'+1").contains("'-2").contains("'@cmd");
        assertThat(text).contains("正常值");
    }

    @Test
    @DisplayName("含逗号引号换行的字段被正确包裹")
    void should_quoteSpecialChars() {
        byte[] csv = CsvExportUtil.toCsv(
                List.of("a", "b"),
                List.of(List.of("含,逗号", "含\"引号\"\n换行")));
        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).contains("\"含,逗号\"");
        assertThat(text).contains("\"含\"\"引号\"\"\n换行\"");
    }

    @Test
    @DisplayName("输出含 UTF-8 BOM 便于 Excel 识别")
    void should_includeBom() {
        byte[] csv = CsvExportUtil.toCsv(List.of("h"), List.of(List.of("v")));
        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);
    }
}
