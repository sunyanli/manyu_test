"""Validate the generated regression test report for completeness."""
import re
import sys
from pathlib import Path


def validate_report(report_path: Path) -> bool:
    """Validate report completeness. Returns True if valid."""
    if not report_path.exists():
        print(f"ERROR: Report not found at {report_path}")
        return False

    content = report_path.read_text(encoding="utf-8")
    errors = []

    required_sections = [
        "## 一、测试概述",
        "## 二、回归范围",
        "## 三、测试用例执行统计",
        "## 四、缺陷汇总",
        "## 五、遗留风险",
        "## 六、测试结论",
        "## 七、下一步建议",
    ]
    for section in required_sections:
        if section not in content:
            errors.append(f"Missing required section: {section}")

    # Check for unfilled placeholders
    if "___" in content:
        errors.append("Report contains unfilled placeholders ('___')")
    if "（待填充）" in content:
        errors.append("Report contains unfilled fields ('（待填充）')")
    if "（待补充）" in content:
        errors.append("Report contains unfilled conclusions ('（待补充）')")

    # Check stats consistency
    total_match = re.search(r"用例总数\s*\|\s*(\d+)", content)
    passed_match = re.search(r"通过数\s*\|\s*(\d+)", content)
    failed_match = re.search(r"失败数\s*\|\s*(\d+)", content)
    blocked_match = re.search(r"阻塞数\s*\|\s*(\d+)", content)
    skipped_match = re.search(r"跳过数\s*\|\s*(\d+)", content)

    if all([total_match, passed_match, failed_match, blocked_match, skipped_match]):
        total = int(total_match.group(1))
        passed = int(passed_match.group(1))
        failed = int(failed_match.group(1))
        blocked = int(blocked_match.group(1))
        skipped = int(skipped_match.group(1))
        if total != passed + failed + blocked + skipped:
            errors.append(
                f"Stats inconsistency: total({total}) != passed({passed}) + failed({failed}) + blocked({blocked}) + skipped({skipped})"
            )
    else:
        errors.append("Could not parse all test statistics from the report")

    if errors:
        print(f"Validation FAILED for {report_path}:")
        for err in errors:
            print(f"  - {err}")
        return False

    print(f"Validation PASSED for {report_path}")
    return True


if __name__ == "__main__":
    report_path = Path("reports/0921商业版预发微信回归测试报告_完成版.md")
    success = validate_report(report_path)
    sys.exit(0 if success else 1)
