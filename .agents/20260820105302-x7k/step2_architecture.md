# Step2 架构与模块划分

## 2.1 功能架构
```mermaid
graph TB
    subgraph appName[三接口演示应用（后端 manyu_test / 前端 manyu_test1）]
        subgraph interactionLayer[交互层]
            WebConsole[Web 控制台 oneapi：三Tab页面/导出按钮/报表区]
        end
        subgraph coreServiceLayer[核心服务层]
            subgraph moduleDemo[demo 示例接口模块]
                F01[helloworld]
                F02[哈希算法]
                F03[冒泡排序]
            end
            subgraph moduleExport[export 导出模块]
                F05[各页面结果导出]
            end
            subgraph moduleTrack[tracking 埋点统计模块]
                F06[埋点采集]
                F07[统计报表]
            end
        end
    end
```

- **交互层说明**：Vue3 单页应用，一页三 Tab（演示/执行、导出按钮、报表区）；仅消费 oneapi（/api 前缀）接口。
- **核心服务层说明**：
  - demo 模块：三个示例接口，执行算法并触发埋点（F01-F03）。
  - export 模块：按页面导出展示结果（F05）。
  - tracking 模块：调用记录采集（F06）与多维度统计查询（F07）。
- **扩展/集成层说明**：本设计不引入外部业务系统集成；登录人员信息依赖统一登录上下文（A03）。

## 2.2 应用集成架构
```mermaid
flowchart TB
    user[用户浏览器: 三Tab页面/导出/报表]
    subgraph app[三接口演示应用]
        WebConsole[Web 控制台 oneapi]
        CoreServices[核心服务层: demo/export/tracking]
    end
    subgraph middleware[中间件服务]
        DB[(MySQL 8: call_record)]
    end
    user -->|HTTPS oneapi| WebConsole
    WebConsole -->|JVM| CoreServices
    CoreServices -->|JDBC 参数化SQL| DB
```

**集成关系说明：**
| 调用方 | 被调用方 | 协议 | 接口类型 | 说明 |
|--------|----------|------|----------|------|
| 用户浏览器 | 应用 Web控制台 | HTTPS | oneapi REST | 三 Tab 执行、导出、报表全部走 /api |
| 应用核心服务层 | MySQL | JDBC | SQL（参数化） | call_record 埋点表读写 |
| 应用（埋点） | 登录上下文（统一 userInfo/请求头） | JVM | 内部解析 | 获取人员类型/层级/部门快照（A03） |

## 2.3 部署架构
```mermaid
graph TB
    subgraph deployment[部署架构]
        subgraph lbLayer[负载均衡层]
            LB[SLB/Nginx]
        end
        subgraph appLayer[应用层]
            Instance1[应用实例A]
            Instance2[应用实例B]
        end
        subgraph dataLayer[数据层]
            DBMaster[(MySQL 主库)]
            DBSlave[(MySQL 从库)]
        end
    end
    Client[浏览器] --> LB
    LB --> Instance1
    LB --> Instance2
    Instance1 --> DBMaster
    Instance2 --> DBMaster
    DBMaster -.->|主从同步| DBSlave
```

**部署说明：**
- **负载均衡层**：SLB/Nginx，HTTPS 终结，按会话（登录态）转发。
- **应用层**：无状态应用 ≥2 副本，滚动发布；埋点线程池为 JVM 内异步（失败静默）。
- **数据层**：MySQL 主从（从库可承接报表只读查询），默认 InnoDB、RC 隔离级别。
- 公有云默认同城双机房容灾；私有化默认容器化部署（假设：无明确部署环境时按容器化）。

## 2.4 模块清单
| 模块 | 职责 | 依赖 |
|------|------|------|
| demo（示例接口模块） | helloworld、哈希、冒泡排序三个接口，入参校验、算法执行、结果组装、触发埋点 | tracking（埋点注解）、登录上下文解析 |
| export（导出模块） | 按页面导出展示结果（CSV/XLSX），导出动作计入埋点 | tracking（call_record 查询）、demo（结果数据） |
| tracking（埋点统计模块） | call_record 写入（AOP+异步批量）、多维度统计/趋势查询、导出数据源 | MySQL |

依赖方向：demo → tracking；export → tracking/demo；无循环依赖。