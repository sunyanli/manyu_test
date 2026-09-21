"""Unit tests for validate_report.py."""
from pathlib import Path

from scripts.validate_report import validate_report


REPORT_TEMPLATE = """# 0921 商业版预发微信回归测试报告

## 一、测试概述
| 项目 | 内容 |

## 二、回归范围

## 三、测试用例执行统计

| 统计项 | 数量 |
|---|---|
| 用例总数 | 10 |
| 通过数 | 8 |
| 失败数 | 1 |
| 阻塞数 | 0 |
| 跳过数 | 1 |
| 通过率 | 80.0% |

## 四、缺陷汇总
| 缺陷编号 | 缺陷描述 | 严重程度 | 状态 | 负责人 |
|---|---|---|---|---|
| BUG-001 | 示例 | 严重 | 已提交 | 测试 |

## 五、遗留风险
| 风险项 | 风险描述 | 应对措施 |
|---|---|---|
| 示例 | 示例 | 示例 |

## 六、测试结论
**是否通过回归测试：** 不通过
**结论说明：** 测试说明

## 七、下一步建议
1. 步骤1
"""


def test_validate_report_valid(tmp_path):
    """Valid report passes validation."""
    report_path = tmp_path / "report.md"
    report_path.write_text(REPORT_TEMPLATE, encoding="utf-8")
    assert validate_report(report_path) is True


def test_validate_report_missing_file(tmp_path):
    """Missing report file fails validation."""
    report_path = tmp_path / "report.md"
    assert validate_report(report_path) is False


def test_validate_report_missing_section(tmp_path):
    """Report missing a required section fails validation."""
    report_path = tmp_path / "report.md"
    report_path.write_text(REPORT_TEMPLATE.replace("## 四、缺陷汇总", ""), encoding="utf-8")
    assert validate_report(report_path) is False


def test_validate_report_unfilled_placeholder(tmp_path):
    """Report with unfilled placeholders fails validation."""
    report_path = tmp_path / "report.md"
    report_path.write_text(REPORT_TEMPLATE + "\n（待补充）", encoding="utf-8")
    assert validate_report(report_path) is False
