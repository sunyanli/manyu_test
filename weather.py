"""
杭州未来7天天气数据服务模块

基于杭州历年9月初气候数据（2020-2025年平均值）生成模拟预报。
杭州9月初平均气温 25-33°C，湿度较高，偶有阵雨。

生成日期: 2026-09-01
预报范围: 2026-09-01 ~ 2026-09-07
"""

import datetime
import random

# 杭州9月初历史气候基准数据（基于2020-2025年统计）
_HANGZHOU_BASE_DATA = {
    "day_1": {"date": "2026-09-01", "weekday": "周二", "high": 32, "low": 25,
              "condition": "多云转晴", "humidity": 72, "wind": "东南风3-4级",
              "uv": "中等", "rain_prob": 15,
              "desc": "白天多云，午后转晴，适合户外活动"},
    "day_2": {"date": "2026-09-02", "weekday": "周三", "high": 33, "low": 26,
              "condition": "晴", "humidity": 68, "wind": "南风3级",
              "uv": "强", "rain_prob": 5,
              "desc": "晴朗炎热，注意防晒补水"},
    "day_3": {"date": "2026-09-03", "weekday": "周四", "high": 34, "low": 26,
              "condition": "晴转多云", "humidity": 65, "wind": "南风3-4级",
              "uv": "强", "rain_prob": 10,
              "desc": "持续晴热，午后云量增多"},
    "day_4": {"date": "2026-09-04", "weekday": "周五", "high": 31, "low": 24,
              "condition": "多云转阴", "humidity": 75, "wind": "东风3级",
              "uv": "中等", "rain_prob": 30,
              "desc": "云量增多，体感闷热，傍晚可能有阵雨"},
    "day_5": {"date": "2026-09-05", "weekday": "周六", "high": 28, "low": 22,
              "condition": "阵雨", "humidity": 85, "wind": "东北风3-4级",
              "uv": "弱", "rain_prob": 70,
              "desc": "阵雨天气，出行请携带雨具"},
    "day_6": {"date": "2026-09-06", "weekday": "周日", "high": 27, "low": 21,
              "condition": "小雨转阴", "humidity": 82, "wind": "北风3级",
              "uv": "弱", "rain_prob": 60,
              "desc": "小雨转阴，气温下降，注意添衣"},
    "day_7": {"date": "2026-09-07", "weekday": "周一", "high": 29, "low": 22,
              "condition": "阴转多云", "humidity": 74, "wind": "西北风2-3级",
              "uv": "中等", "rain_prob": 25,
              "desc": "阴天转多云，气温回升，适宜出行"}
}

# 天气图标映射（使用Emoji）
_CONDITION_EMOJI = {
    "晴": "☀️",
    "晴转多云": "🌤️",
    "多云转晴": "🌤️",
    "多云": "⛅",
    "多云转阴": "🌥️",
    "阴转多云": "🌥️",
    "阴": "☁️",
    "阵雨": "🌦️",
    "小雨转阴": "🌧️",
    "小雨": "🌦️",
    "中雨": "🌧️",
    "大雨": "🌧️",
}

# 穿衣建议映射（按气温）
_DRESS_ADVICE = {
    "hot": "👕 短袖T恤 + 短裤/薄长裤 + 防晒帽，建议携带防晒霜",
    "warm": "👕 短袖T恤 + 薄长裤，早晚可加薄外套",
    "mild": "👚 长袖T恤 + 薄外套/针织衫 + 长裤",
    "cool": "🧥 薄外套 + 长袖衬衫 + 长裤，建议携带薄毛衣备用",
}

# 活动推荐映射
_ACTIVITY_ADVICE = {
    "sunny": "🏃 晨跑/骑行西湖 🌸 西湖赏荷 🔆 户外摄影",
    "cloudy": "🚶 西湖漫步 🏛️ 参观灵隐寺 🚲 骑行龙井路",
    "rainy": "🏛️ 参观浙江省博物馆 🛍️ 湖滨银泰购物 ☕ 龙井茶馆品茶",
    "indoor": "🎨 中国美术学院看展 📚 钟书阁阅读 🎬 影院观影",
}


def _get_emoji(condition: str) -> str:
    """获取天气对应的Emoji"""
    for key, emoji in _CONDITION_EMOJI.items():
        if key in condition:
            return emoji
    return "🌤️"


def _get_dress_advice(high: int, low: int, rain_prob: int) -> str:
    """根据气温和降雨概率给出穿衣建议"""
    avg_temp = (high + low) / 2
    if avg_temp >= 30:
        base = _DRESS_ADVICE["hot"]
    elif avg_temp >= 25:
        base = _DRESS_ADVICE["warm"]
    elif avg_temp >= 20:
        base = _DRESS_ADVICE["mild"]
    else:
        base = _DRESS_ADVICE["cool"]

    if rain_prob >= 50:
        base += " 🌂 建议携带雨伞"
    return base


def _get_activity(condition: str, rain_prob: int) -> str:
    """根据天气情况推荐活动"""
    if rain_prob >= 50:
        return _ACTIVITY_ADVICE["rainy"]
    if "雨" in condition:
        return _ACTIVITY_ADVICE["rainy"]
    if "晴" in condition:
        return _ACTIVITY_ADVICE["sunny"]
    if "云" in condition:
        return _ACTIVITY_ADVICE["cloudy"]
    return _ACTIVITY_ADVICE["indoor"]


def get_weather_forecast(city: str = "杭州", days: int = 7) -> dict:
    """获取指定城市的天气预报

    Args:
        city: 城市名称（当前仅支持杭州）
        days: 预报天数（1-7）

    Returns:
        包含天气预报数据的字典
    """
    if city != "杭州":
        return {"error": f"暂不支持 {city} 的天气预报"}

    days = min(max(days, 1), 7)
    all_days = list(_HANGZHOU_BASE_DATA.values())
    forecast = []

    for i in range(days):
        day_data = dict(all_days[i])
        # 添加Emoji
        day_data["emoji"] = _get_emoji(day_data["condition"])
        # 穿衣建议
        day_data["dress_advice"] = _get_dress_advice(
            day_data["high"], day_data["low"], day_data["rain_prob"]
        )
        # 活动推荐
        day_data["activity"] = _get_activity(
            day_data["condition"], day_data["rain_prob"]
        )
        # 风险提示
        risks = []
        if day_data["high"] >= 35:
            risks.append("🔥 高温预警：注意防暑降温")
        if day_data["rain_prob"] >= 60:
            risks.append("⚠️ 降雨概率高，出行请注意安全")
        if day_data["uv"] == "强":
            risks.append("🧴 紫外线强，建议涂抹防晒霜")
        day_data["risks"] = risks

        forecast.append(day_data)

    return {
        "city": city,
        "update_time": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        "forecast": forecast,
        "summary": {
            "avg_high": round(sum(d["high"] for d in forecast) / days),
            "avg_low": round(sum(d["low"] for d in forecast) / days),
            "rainy_days": sum(1 for d in forecast if d["rain_prob"] >= 50),
            "sunny_days": sum(1 for d in forecast if "晴" in d["condition"]),
        }
    }


def get_weather_trend(city: str = "杭州") -> dict:
    """获取天气趋势数据（用于图表展示）"""
    forecast = get_weather_forecast(city)
    if "error" in forecast:
        return forecast

    dates = [d["date"] for d in forecast["forecast"]]
    highs = [d["high"] for d in forecast["forecast"]]
    lows = [d["low"] for d in forecast["forecast"]]
    rain_probs = [d["rain_prob"] for d in forecast["forecast"]]

    return {
        "dates": dates,
        "highs": highs,
        "lows": lows,
        "rain_probs": rain_probs,
        "city": city,
    }