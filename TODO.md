# TODO — 待完善项

> 记录本次微服务改造中已识别、但暂缓处理的问题，按优先级推进。

## 安全（高优先级）

### 1. ✅ user-service 信任 X-User-Id 头 —— 已由网络隔离缓解
- **现状**：Gateway 校验 token 后，仅透传 `X-User-Id` 到下游；user-service 直接信任该头。
- **风险**：若有人绕过 Gateway 直连 `user-service:8081`，可伪造 `X-User-Id: 1` 冒充任意用户（「网关鉴权 + 头透传」架构的固有问题，对应改造方案 §6.5）。
- **决策**：采用方案 1「网络层隔离」——只对外网暴露 Gateway，user-service 等下游仅内网可达（安全组/防火墙/ClusterIP/端口映射均不暴露下游）。代码侧无需处理。
- **其余可选方案**（备查，不采用）：
  2. 内部调用秘钥：Gateway 透传时追加一个只有内部知道的自定义头/秘钥，下游拦截器校验。
  3. 服务网格 / mTLS：全链路双向 TLS，确保请求确实来自可信方。

## Gateway（中优先级）
- 鉴权过滤器用了阻塞式 `StringRedisTemplate`，WebFlux 下建议换成响应式 `ReactiveStringRedisTemplate`，避免阻塞事件循环线程。
- 401 响应用 `Result` 格式手动序列化，后续若引入统一异常/响应处理可收敛。

## user-service（中优先级）
- 密码登录未实现（`LoginFormDTO.password` 空置，登录只支持验证码）。
- 验证码缺少 60s 发送限频（改造方案 §17.2 建议用 Redis SETNX 限频）。
- common 的 `BusinessException` / `GlobalExceptionHandler` 为空壳，需实现统一异常处理。
- 登录把用户快照（id/nickName/icon）写入 Redis hash，用户改资料后 token 里的旧快照可能 stale；方案 A 已让 `/me` 走库查询，其余按需照此处理。

## 基础设施（后续阶段，按改造方案顺序）
- ✅ 已接入 Nacos（服务注册 + 配置中心）。⚠️ 当前 Nacos 用内嵌 derby 且未挂数据卷，容器重建会丢配置 —— 权威副本在 `.docs/nacos/`，重建后需重新发布；后续可改 MySQL 存储或挂载数据卷。
- ✅ 已实现 shop-service / voucher-service / blog-service（数据边界、Feign 通信）。
- 接入 Sentinel（限流/熔断）、RabbitMQ（秒杀异步落库）、Canal（缓存同步）。

## blog-service（中优先级）
- 点赞数仅存 Redis Set（`blog:liked:{id}`），未异步落库到 `tb_blog.liked`，Redis 重启即失（代码已标 TODO）。
- 评论功能未实现（`tb_blog.comments` 字段已预留，`tb_blog_comments` 表未建）。
- 笔记详情查询 `GET /blog/{id}` 未实现（本期聚焦 Feed，详情页可后续补）。
- Feed 刷关注页逐条 Feign 查作者信息，同作者多篇笔记会重复调用（已按请求内 Map 去重，但跨请求未缓存，可批量查询优化 N+1）。
- 收件箱 ZSet（`feed:{userId}`）未做过期清理，长期会无限膨胀（可用 `ZREMRANGEBYSCORE` 按 score 清理过期）。

## ai-service（待完成：Dify 部署 + 联调）

> 已选定方案 b：Dify 自托管（Docker），workflow 内 HTTP 节点直连 Java 服务取数；Dify Cloud 连不到本机，方案 b 下不可用。

- 部署 Dify：本机 Docker `docker compose up`，API 默认端口 5001。
- 建「店铺经营分析」workflow（Workflow 类型）：开始节点 shopId → HTTP 查店铺 `http://host.docker.internal:8082/shop/{{shopId}}` → HTTP 查券 `http://host.docker.internal:8083/voucher/list/{{shopId}}` → 代码节点解析 `Result.data` → LLM 节点（店铺经营分析师 prompt）→ 结束节点输出变量 `report`（须与 DifyClient `outputs.get("report")` 一致）。
- 发布应用 → 复制 API Key → 填入 `.docs/nacos/ai-service.yaml` 的 `api-key`（当前占位 `app-REPLACE_ME`）。
- 重新发布 Nacos 配置：覆盖 `gateway.yaml`（/ai 路由 + /ai/report/* 白名单）、新建 `ai-service.yaml`。
- 联调：启动 ai-service → 注册 Nacos → 网关 POST /ai/report/shop/{id} 拿 taskId → 轮询 GET /ai/report/{taskId}；停 Dify 验证降级 FALLBACK 路径；产出 Postman 测试集合。

## ai-service（中优先级）
- 报告生成用 Dify `response_mode=blocking` 同步等结果，未做 SSE 流式返回（长报告首字延迟高，可改 streaming + SSE 转发）。
- 报告未关联 userId（Redis key 仅 `ai:report:{taskId}`，taskId 即凭证）；后续做「我的报告历史」需在 value 记 userId 并校验。
- 「店铺分析」输入仅用 shop/voucher 现成只读接口，无订单核销统计（需 voucher-service 补 `voucher_order→voucher→shopId` 聚合查询，JOIN 后再接入 Dify workflow）。
- RAG 知识库未接（店铺/博客数据未向量化进 Dify 知识库）。
- Feed 推荐 / 店铺推荐 feature 未做（复用 ai-service + 新增 Dify workflow 即可，架构不变）。⚠️ 注意：个性化推荐需要用户私有数据（关注/点赞/订单），业务服务靠 `X-User-Id` 识别用户，方案 b（Dify 直连）拿不到用户身份 → 需退回方案 a（ai-service Feign 带 X-User-Id 拉数再传 Dify），或另想身份透传方案。
- Dify 部署形态未定：开发可用 Dify Cloud 免费额度，上线再自托管（Docker 多容器较重）。
