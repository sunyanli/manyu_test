from flask import Flask, jsonify, request, Response
from flask_cors import CORS
from hello_world import hello_world
from hash_algo import compute_hash
from sort_api import sort_data
from tracking import track_call, get_overview, get_chart_data, get_tab_data, get_all_records
from export import export_to_csv
from weather import get_weather_forecast, get_weather_trend

app = Flask(__name__)
CORS(app)


def success(data):
    """统一成功响应"""
    return jsonify({"code": "OK", "msg": "SUCCESS", "data": data})


def error(code, msg, status=400):
    """统一错误响应"""
    return jsonify({"code": code, "msg": msg, "data": None}), status


# ==================== 业务接口 ====================

@app.route('/api/hello', methods=['GET'])
def api_hello():
    caller = request.args.get('caller', 'anonymous')
    user_type = request.args.get('user_type', 'developer')
    user_level = request.args.get('user_level', 'mid')
    department = request.args.get('department', 'engineering')
    track_call('/api/hello', caller, user_type, user_level, department)
    result = hello_world()
    return success(result)


@app.route('/api/hash', methods=['POST'])
def api_hash():
    data = request.get_json()
    if not data or 'input' not in data:
        return error("HASH_002", "Missing 'input' field")
    input_str = data['input']
    algorithm = data.get('algorithm', 'sha256')
    caller = data.get('caller', 'anonymous')
    user_type = data.get('user_type', 'developer')
    user_level = data.get('user_level', 'mid')
    department = data.get('department', 'engineering')
    try:
        result = compute_hash(input_str, algorithm)
        track_call('/api/hash', caller, user_type, user_level, department)
        return success(result)
    except ValueError as e:
        return error("HASH_001", str(e))


@app.route('/api/sort', methods=['POST'])
def api_sort():
    data = request.get_json()
    if not data or 'data' not in data:
        return error("SORT_001", "Missing 'data' field")
    if not isinstance(data['data'], list):
        return error("SORT_002", "'data' must be an array")
    caller = data.get('caller', 'anonymous')
    user_type = data.get('user_type', 'developer')
    user_level = data.get('user_level', 'mid')
    department = data.get('department', 'engineering')
    result = sort_data(data['data'])
    track_call('/api/sort', caller, user_type, user_level, department)
    return success(result)


# ==================== 埋点接口 ====================

@app.route('/api/track', methods=['POST'])
def api_track():
    data = request.get_json()
    if not data:
        return error("TRACK_001", "Missing request body")
    record = track_call(
        data.get('api', 'unknown'),
        data.get('caller', 'anonymous'),
        data.get('user_type', 'developer'),
        data.get('user_level', 'mid'),
        data.get('department', 'engineering')
    )
    return success({"status": "ok", "id": record["id"]})


@app.route('/api/stats/overview', methods=['GET'])
def api_stats_overview():
    return success(get_overview())


@app.route('/api/stats/chart', methods=['GET'])
def api_stats_chart():
    dimension = request.args.get('dimension', 'user_type')
    chart_type = request.args.get('chart_type', 'pie')
    try:
        result = get_chart_data(dimension, chart_type)
        return success(result)
    except ValueError as e:
        if "dimension" in str(e):
            return error("TRACK_002", str(e))
        return error("TRACK_003", str(e))


# ==================== 导出接口 ====================

@app.route('/api/export', methods=['GET'])
def api_export():
    tab = request.args.get('tab')
    csv_data = export_to_csv(tab)
    if not csv_data:
        return error("EXPORT_001", "No data to export", 404)
    filename = f"tracking_{tab or 'all'}.csv"
    return Response(
        csv_data,
        mimetype="text/csv",
        headers={"Content-Disposition": f"attachment; filename={filename}"}
    )


# ==================== 天气接口 ====================

@app.route('/api/weather', methods=['GET'])
def api_weather():
    city = request.args.get('city', '杭州')
    days = int(request.args.get('days', 7))
    result = get_weather_forecast(city, days)
    if "error" in result:
        return error("WEATHER_001", result["error"])
    return success(result)


@app.route('/api/weather/trend', methods=['GET'])
def api_weather_trend():
    city = request.args.get('city', '杭州')
    result = get_weather_trend(city)
    if "error" in result:
        return error("WEATHER_002", result["error"])
    return success(result)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)