"""Unit tests for generate_report.py."""
from pathlib import Path

import yaml

from scripts.generate_report import generate_report, load_snapshot


def test_load_snapshot_missing_file():
    """Loading a non-existent snapshot returns empty dict."""
    result = load_snapshot(Path("/nonexistent/file.yml"))
    assert result == {}


def test_load_snapshot_valid_yaml(tmp_path):
    """Loading a valid YAML snapshot returns the parsed data."""
    snapshot_file = tmp_path / "test.yml"
    snapshot_file.write_text("name: test\nexecuted: true\n", encoding="utf-8")
    result = load_snapshot(snapshot_file)
    assert result["name"] == "test"
    assert result["executed"] is True


def test_generate_report_creates_file(tmp_path):
    """generate_report creates the report file."""
    output_path = tmp_path / "report.md"
    snapshots_dir = tmp_path / "snapshots"
    snapshots_dir.mkdir()
    generate_report(output_path, snapshots_dir)
    assert output_path.exists()


def test_generate_report_contains_required_sections(tmp_path):
    """Generated report contains all required sections."""
    output_path = tmp_path / "report.md"
    snapshots_dir = tmp_path / "snapshots"
    snapshots_dir.mkdir()

    # Create a sample snapshot
    snapshot = {
        "name": "测试模块",
        "executed": True,
        "stats": {"total": 5, "passed": 4, "failed": 1, "blocked": 0, "skipped": 0},
        "defects": [
            {"id": "BUG-001", "desc": "示例缺陷", "severity": "严重", "status": "已提交", "owner": "测试"}
        ],
        "risks": [],
    }
    (snapshots_dir / "test.yml").write_text(yaml.dump(snapshot), encoding="utf-8")

    generate_report(output_path, snapshots_dir)
    content = output_path.read_text(encoding="utf-8")

    assert "## 一、测试概述" in content
    assert "## 二、回归范围" in content
    assert "## 三、测试用例执行统计" in content
    assert "## 四、缺陷汇总" in content
    assert "## 五、遗留风险" in content
    assert "## 六、测试结论" in content
    assert "## 七、下一步建议" in content
