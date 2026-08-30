# 智能客服 Agent 方案

> 状态：已评审通过（2026-08-30）。
> 前提：Dify 自托管、纯内网、不连外网、控制台不暴露 —— 接受「Dify 为内网可信身份源」。

## 1. 目标与范围

一个 Dify **Agent 应用**，作为智能客服，回答用户**有权咨询的所有信息**（店铺、优惠券、订单、售后、FAQ）。

- 多轮对话、SSE 流式、Dify 内部 RAG。
- 数据获取：**公开数据**（店铺/券）与**私有数据**（订单/售后）均由 Dify 的 HTTP 节点**直连内网业务服务**，靠 `X-User-Id` 传递身份。
- 公开 + 私有数据同批交付，不分期。

## 2. 总体架构与信任链

```
浏览器 ──> Gateway:8080（校验 token → 透传权威 X-User-Id）
              │
              └──> ai-service:8085  POST /ai/customer_chat  (SSE)
                     │  1. 从 X-User-Id 取权威 userId（唯一来源，忽略一切用户参数）
                     │  2. 读/建 conversation_id（Redis）
                     │  3. WebClient 调 Dify /v1/chat-messages (streaming)
                     │       inputs = { userId, conversation_id, query }
                     └──> Dify Agent:5001
                            ├─ 公开数据：HTTP 节点直连 shop:8082 / voucher:8083（无身份）
                            ├─ 私有数据：HTTP 节点直连业务服务，Header 带 X-User-Id: {{userId}}
                            └─ RAG：Dify 知识库检索（店铺/券规则/FAQ）
                     │  4. Dify SSE → ai-service 转发 SSE → 前端
                     └──> Redis：ai:chat:conv:{userId} / ai:chat:history:{userId}
```

**信任链**：`token → Gateway → 权威 X-User-Id → ai-service → Dify（{{userId}}）→ 业务服务`。
`X-User-Id` 的签发点从「仅 Gateway」扩展为「Gateway + Dify」，前提是 Dify 内网可信。

## 3. 关键设计

### 3.1 身份传递（唯一硬纪律）
- `ChatController` **只**从网关透传的 `X-User-Id` 头取 userId，**不提供任何 userId 参数**给前端。用户改请求体/参数无效。
- 传给 Dify 的 `inputs.userId` 就是这个权威值；Dify 里私有数据的 HTTP 节点 Header 用 `{{userId}}` 动态填充，**不得写死**。

### 3.2 会话管理（多轮，Redis）
| Key | 类型 | 内容 | TTL |
|---|---|---|---|
| `ai:chat:conv:{userId}` | String | Dify `conversation_id` | 30min 滑动 |
| `ai:chat:history:{userId}` | List | 用户问题 + 回答（lpush，截断 50 条） | 24h |

- Dify 靠 `conversation_id` **自己续上下文**，ai-service **不重传历史**，只把 `conversation_id` 回传给 Dify。
- `history` 仅用于前端展示历史，与对话续接无关。
- 开新会话：`POST /ai/customer_chat/reset` 删 conv + history。

### 3.3 流式 SSE
- `DifyClient` 从 `RestTemplate`（阻塞）升级为 **`WebClient`（reactor-netty）**，读 Dify `response_mode=streaming` 的 `text/event-stream`。
- Dify streaming 关键事件：`message`（增量 answer）、`message_end`（**含 `conversation_id`，必须取来续上下文**）、`error`、`ping`。
- ai-service 保持 Spring MVC，Controller 返回 **`SseEmitter`**，把 answer 增量 `send` 给前端；`message_end` 取 `conversation_id` 写回 Redis 再 `complete`。
- **Gateway**：WebFlux 可透传 SSE，需调 `spring.cloud.gateway.httpclient.response-timeout`，并确认无全局 filter 缓冲 response body。

### 3.4 RAG（Dify 内部）
- Dify 控制台建**知识库**，灌入：店铺信息、优惠券规则、FAQ、售后政策。
- Agent 应用**挂载知识库**，LLM 自动检索，不自己搭向量库。
- ⚠️ 内容准备是运营工作，数据源在 shop/voucher 库；后续可做「导出 → 知识库」同步脚本，先手动整理一版。

### 3.5 降级
- Dify 不可用 / 超时：返回固定话术「客服暂时不可用，请稍后重试」+ 引导 FAQ。对话式场景不做规则模板降级。

## 4. 实施顺序（同批交付）

1. Dify：建 Agent 应用 + 知识库（店铺/券/FAQ）+ 公开数据 HTTP 工具节点。
2. ai-service：`/ai/customer_chat`（SSE）+ DifyClient 加 `chatMessages` 流式 + 会话 Redis。
3. Dify：接私有数据 HTTP 节点（订单/售后），Header `X-User-Id: {{userId}}`。
4. Gateway：加 `/ai/customer_chat` 路由 + SSE 超时；联调降级路径。

## 5. 改动清单（文件级）

| 位置 | 改动 |
|---|---|
| `ai-service/config/DifyProperties.java` | 应用名加 `customer-service`；chat 相关超时配置 |
| `ai-service/client/DifyClient.java` | 加 `APP_CUSTOMER_SERVICE`；新增 `chatMessages(...)` 流式方法（WebClient） |
| `ai-service/controller/ChatController.java`（新增） | `POST /ai/customer_chat` 返回 SseEmitter；`POST /ai/customer_chat/reset` |
| `ai-service/service/ChatService.java`（新增） | 会话管理 + 流式转发 |
| `ai-service` 依赖 | 加 `spring-boot-starter-webflux`（仅用 WebClient） |
| `.docs/nacos/ai-service.yaml` | `dify.apps` 加 `customer-service` |
| `.docs/nacos/gateway.yaml` | 加 `/ai/customer_chat` 路由 + SSE 超时（**需登录，不进白名单**） |
| 前端 | 客服页，`EventSource` 消费 SSE |
| Dify | Agent 应用、知识库、HTTP 工具节点 |

## 6. 安全边界（约束）

1. `userId` 只来自网关透传 `X-User-Id`，禁止任何用户可控的 userId 输入。
2. Dify 私有数据节点 `X-User-Id` 动态取 `{{userId}}`，禁止写死。
3. Dify 实例纯内网、控制台不暴露、不连外网。
4. 会话 Redis key 带 userId，跨用户隔离。

## 7. 接口契约

- `POST /ai/customer_chat`（需登录，网关鉴权透传 X-User-Id）
  - 请求体：`{ "query": "用户输入" }`（无 userId）
  - 响应：SSE 流，逐段返回 answer。
- `POST /ai/customer_chat/reset`（需登录）：清空当前用户会话。

## 8. 待办 / 依赖

- [ ] Dify 部署并建 Agent 应用、知识库、HTTP 工具节点（依赖 Dify 就绪）。
- [ ] 生成 `customer-service` 应用的 API Key，填入 `.docs/nacos/ai-service.yaml`（当前占位 `app-REPLACE_ME`）。
- [ ] 知识库内容准备（店铺/券/FAQ，运营）。
- [ ] 私有数据接口确认：voucher-service 需补 `voucher_order → voucher → shopId` 聚合查询（见根目录 TODO.md）。
