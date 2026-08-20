# Step5 全局约定（global）

## 全局约定
- **错误码格式**：`{MODULE}_{SEQ}`，模块码：DEMO / EXPORT / TRACKING / COMMON。
- **通用出参结构**：
```json
{
  "code": "OK",
  "msg": "SUCCESS",
  "data": {}
}
```
- 业务失败返回非 OK 的业务错误码 + msg；系统异常由全局异常处理器统一转换为 COMMON_500。
- **通用入参/出参包装**：入参使用强类型 DTO；出参使用 VO；分页接口统一 page/pageSize。
- **时间约定**：接口返回采用 ISO-8601 字符串（UTC，yyyy-MM-dd'T'HH:mm:ssZ）；库内 datetime。
- **人员上下文**：统一从登录上下文解析 CallerInfo（人员ID/姓名/类型/层级/部门），注入到各 Service 的 DemoContext/CallRecord（A03）。
- **埋点接入方式**：在受监控接口方法上标注 @TrackCall(type=...)，AOP 环绕通知异步记录调用次数与调用人。
- **模块映射**：
  | 模块 | 代码前缀 | 主要表 |
  |------|----------|--------|
  | demo | DEMO | -（无表） |
  | export | EXPORT | 复用 call_record（导出动作本身也埋点） |
  | tracking | TRACKING | call_record |
  | common | COMMON | - |