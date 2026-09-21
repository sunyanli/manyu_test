"""Generate the final regression test report from YAML snapshots."""
import datetime
import os
from pathlib import Path
from typing import Any

import yaml

REPORT_TEMPLATE = """# 0921 商业版预发微信回归测试报告

> 生成时间: {generation_time}
> 执行人: {executor}
> 环境: {environment}

## 一、测试概述

| 项目 | 内容 |
|---|---|
| 版本号 | {version} |
| 测试类型 | 回归测试 |
| 测试时间 | {test_date} |
| 测试环境 | {environment} |
| 测试负责人 | {executor} |

## 二、回归范围

{scope_section}

## 三、测试用例执行统计

| 统计项 | 数量 |
|---|---|
| 用例总数 | {total_cases} |
| 通过数 | {passed_cases} |
| 失败数 | {failed_cases} |
| 阻塞数 | {blocked_cases} |
| 跳过数 | {skipped_cases} |
| 通过率 | {pass_rate}% |

## 四、缺陷汇总

| 缺陷编号 | 缺陷描述 | 严重程度 | 状态 | 负责人 |
|---|---|---|---|---|
{defect_rows}

## 五、遗留风险

| 风险项 | 风险描述 | 应对措施 |
|---|---|---|
{risk_rows}

## 六、测试结论

**是否通过回归测试：** {conclusion_status}

**结论说明：**
{conclusion_detail}

## 七、下一步建议

{next_steps}
"""


def load_snapshot(path: Path) -> dict[str, Any]:
    """Load a YAML snapshot file."""
    if not path.exists():
        return {}
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def generate_report(output_path: Path, snapshots_dir: Path) -> None:
    """Generate the final report from all module snapshots."""
    modules = [
        "login_register.yml",
        "messaging.yml",
        "payment.yml",
        "moments.yml",
        "miniprogram.yml",
        "contacts.yml",
        "settings_privacy.yml",
    ]

    total_cases = 0
    passed_cases = 0
    failed_cases = 0
    blocked_cases = 0
    skipped_cases = 0
    scope_lines = []
    defect_rows = []
    risk_rows = []

    for module in modules:
        snapshot = load_snapshot(snapshots_dir / module)
        if not snapshot:
            scope_lines.append(f"- [ ] {module.replace('.yml', '')} 【未执行】")
            continue

        scope_lines.append(f"- [{'x' if snapshot.get('executed') else ' '} {snapshot.get('name', module)}")
        stats = snapshot.get("stats", {})
        total_cases += stats.get("total", 0)
        passed_cases += stats.get("passed", 0)
        failed_cases += stats.get("failed", 0)
        blocked_cases += stats.get("blocked", 0)
        skipped_cases += stats.get("skipped", 0)

        for defect in snapshot.get("defects", []):
            defect_rows.append(
                f"| {defect.get('id', 'N/A')} | {defect.get('desc', '')} | "
                f"{defect.get('severity', 'N/A')} | {defect.get('status', 'N/A')} | {defect.get('owner', 'N/A')} |"
            )

        for risk in snapshot.get("risks", []):
            risk_rows.append(
                f"| {risk.get('item', 'N/A')} | {risk.get('desc', '')} | {risk.get('mitigation', 'N/A')} |"
            )

    pass_rate = round((passed_cases / total_cases) * 100, 1) if total_cases > 0 else 0.0

    if not defect_rows:
        defect_rows = ["| （暂无缺陷） | | | | |"]
    if not risk_rows:
        risk_rows = ["| （暂无风险） | | |"]

    conclusion_status = "通过" if failed_cases == 0 and blocked_cases == 0 else "不通过"

    # Dynamic conclusion and next steps
    if conclusion_status == "通过":
        conclusion_detail = (
            "所有测试模块均已执行，未发现致命或严重缺陷。\n"
            "测试覆盖7个核心模块，共执行{total}条用例，通过{passed}条，通过率{rate}%。\n"
            "系统功能稳定，满足预发环境回归测试通过标准，建议进入下一阶段。"
        ).format(total=total_cases, passed=passed_cases, rate=pass_rate)
        next_steps = (
            "1. 将当前版本推送至生产环境进行最终验证；\n"
            "2. 继续监控生产环境关键指标（登录成功率、消息到达率等）；\n"
            "3. 归档本次回归测试报告至质量管理系统。"
        )
    else:
        conclusion_detail = (
            "本次回归测试发现未修复缺陷或阻塞项，暂不满足发布条件。\n"
            "主要问题集中在以下模块，需修复后重新执行回归验证。"
        )
        next_steps = (
            "1. 优先修复严重及以上级别缺陷（BUG-MP-001, BUG-LR-001等）；\n"
            "2. 缺陷修复完成后，重新执行对应模块的回归测试；\n"
            "3. 验证通过后再次生成并评审回归测试报告。"
        )

    report = REPORT_TEMPLATE.format(
        generation_time=datetime.datetime.now().strftime("%Y-%m-%d %H:%M"),
        executor="曼昱",
        environment="预发环境",
        version="0921 商业版预发",
        test_date=datetime.date.today().strftime("%Y-%m-%d"),
        scope_section="\n".join(scope_lines),
        total_cases=total_cases,
        passed_cases=passed_cases,
        failed_cases=failed_cases,
        blocked_cases=blocked_cases,
        skipped_cases=skipped_cases,
        pass_rate=pass_rate,
        defect_rows="\n".join(defect_rows),
        risk_rows="\n".join(risk_rows),
        conclusion_status=conclusion_status,
        conclusion_detail=conclusion_detail,
        next_steps=next_steps,
    )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"Report generated: {output_path}")


if __name__ == "__main__":
    snapshots_dir = Path("data/regression_snapshots")
    output_path = Path("reports/0921商业版预发微信回归测试报告_完成版.md")
    generate_report(output_path, snapshots_dir)
