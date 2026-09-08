"""数据模型"""

from datetime import datetime

from database import db


class InvokeLog(db.Model):
    """接口调用日志表"""
    __tablename__ = "invoke_log"

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    api_name = db.Column(db.String(128), nullable=False, index=True)
    caller_id = db.Column(db.String(64), nullable=False, default="anonymous")
    caller_name = db.Column(db.String(128), nullable=False, default="Unknown")
    caller_type = db.Column(db.String(32), nullable=True, index=True)
    caller_level = db.Column(db.String(32), nullable=True)
    caller_dept = db.Column(db.String(128), nullable=True, index=True)
    request_params = db.Column(db.Text, nullable=True)
    response_code = db.Column(db.Integer, nullable=False, default=200)
    result = db.Column(db.String(32), nullable=False, default="SUCCESS")
    duration_ms = db.Column(db.Integer, nullable=False, default=0)
    gmt_create = db.Column(db.DateTime, nullable=False, default=datetime.utcnow, index=True)

    def to_dict(self):
        """转换为字典"""
        return {
            "id": self.id,
            "api_name": self.api_name,
            "caller_id": self.caller_id,
            "caller_name": self.caller_name,
            "caller_type": self.caller_type,
            "caller_level": self.caller_level,
            "caller_dept": self.caller_dept,
            "request_params": self.request_params,
            "response_code": self.response_code,
            "result": self.result,
            "duration_ms": self.duration_ms,
            "gmt_create": self.gmt_create.strftime("%Y-%m-%d %H:%M:%S") if self.gmt_create else None
        }