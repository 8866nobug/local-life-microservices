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
- 实现 shop-service / voucher-service / blog-service（数据边界、Feign 通信）。
- 接入 Sentinel（限流/熔断）、RabbitMQ（秒杀异步落库）、Canal（缓存同步）。
