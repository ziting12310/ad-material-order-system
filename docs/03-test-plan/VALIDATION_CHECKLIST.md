# AMOMS 校验清单

> 覆盖参数校验、业务状态冲突、乐观锁并发、安全越权四大维度
> 每条包含：测试场景 + 操作步骤 + 预期返回（code + message + HTTP）

---

## 一、参数校验失败（Jakarta Validation @Valid）

| # | 测试场景 | 操作步骤 | 预期 code | 预期 message 包含 | 预期 HTTP |
|---|---|---|---|---|---|
| V-01 | 手机号为空 | `contactPhone: ""` | PARAM_VALIDATION_ERROR | contactPhone 不能为空 | 200 |
| V-02 | 手机号格式错误 | `contactPhone: "123"` | PARAM_VALIDATION_ERROR | 手机号格式不正确 | 200 |
| V-03 | 客户名称过短 | `customerName: "A"` | PARAM_VALIDATION_ERROR | 2~100 字符 | 200 |
| V-04 | 客户名称超长 | 101 字符 | PARAM_VALIDATION_ERROR | 2~100 字符 | 200 |
| V-05 | 数量为 0 | `quantity: 0` | PARAM_VALIDATION_ERROR | 必须大于 0 | 200 |
| V-06 | 数量为负 | `quantity: -1` | PARAM_VALIDATION_ERROR | 必须大于 0 | 200 |
| V-07 | 交付日期为过去 | `deliveryDate: "2020-01-01"` | PARAM_VALIDATION_ERROR | 必须晚于今天 | 200 |
| V-08 | 物料类型空 | `materialType: ""` | PARAM_VALIDATION_ERROR | 不能为空 | 200 |
| V-09 | 文件格式白名单外 | `fileType: "zip"` | PARAM_VALIDATION_ERROR | 格式不在白名单内 | 200 |
| V-10 | version 缺失 | body 不包含 version | PARAM_VALIDATION_ERROR | version 不能为空 | 200 |
| V-11 | 意见为空 | `content: ""` | PARAM_VALIDATION_ERROR | 修改意见不能为空 | 200 |
| V-12 | 意见超长 | 501 字符 | PARAM_VALIDATION_ERROR | 长度 1~500 字符 | 200 |
| V-13 | JSON 格式错误 | body 少 `}` | PARAM_VALIDATION_ERROR | 请求体格式错误 | 200 |
| V-14 | 登录空账号 | `username: ""` | PARAM_VALIDATION_ERROR | 账号不能为空 | 200 |

---

## 二、业务状态冲突（ORDER_STATUS_CONFLICT 422）

| # | 测试场景 | 前置订单状态 | 尝试操作 | 预期 code | 预期 message |
|---|---|---|---|---|---|
| S-01 | 编辑非 CREATED 订单 | ASSIGNED | PUT `/orders/{id}` | ORDER_STATUS_CONFLICT | 当前状态为 [已分派]，编辑需要 [已登记] |
| S-02 | 分派非 CREATED 订单 | ASSIGNED | PUT `/orders/{id}/assign` | ORDER_STATUS_CONFLICT | 当前状态为 [已分派]，分派需要 [已登记] |
| S-03 | 接单非 ASSIGNED 订单 | CREATED | PUT `/orders/{id}/accept` | ORDER_STATUS_CONFLICT | 当前状态为 [已登记]，接单需要 [已分派] |
| S-04 | 上传设计稿非 DESIGNING | WAIT_APPROVE | POST `/design-versions` | ORDER_STATUS_CONFLICT | 当前状态为 [待定稿]，上传需要 [设计中] |
| S-05 | 提交意见非 WAIT_APPROVE | DESIGNING | POST `/order-feedbacks` | ORDER_STATUS_CONFLICT | 当前状态为 [设计中]，提意见需要 [待定稿] |
| S-06 | 驳回非 WAIT_APPROVE | DESIGNING | PUT `/orders/{id}/reject` | ORDER_STATUS_CONFLICT | — |
| S-07 | 定稿非 WAIT_APPROVE | DESIGNING | PUT `/orders/{id}/approve` | ORDER_STATUS_CONFLICT | — |
| S-08 | 交付非 APPROVED | CREATED | PUT `/orders/{id}/deliver` | ORDER_STATUS_CONFLICT | — |
| S-09 | 作废已交付订单 | DELIVERED | PUT `/orders/{id}/cancel` | ORDER_STATUS_CONFLICT | 已交付不能再作废 |
| S-10 | 作废已作废订单 | CANCELLED | PUT `/orders/{id}/cancel` | ORDER_STATUS_CONFLICT | 已作废不能再作废 |

---

## 三、乐观锁并发（ORDER_VERSION_CONFLICT 409）

| # | 测试场景 | 操作步骤 | 预期 code | 预期 message |
|---|---|---|---|---|
| L-01 | 两用户同时分派 | BL-A 和 BL-B 同时 PUT `/orders/1/assign`，携带 `version=0` | 一个 SUCCESS；另一个 ORDER_VERSION_CONFLICT | 数据已变更，请刷新后重试 |
| L-02 | 分派后立即编辑 | BL 刚分派完（version→1）；AM 仍持 version=0 编辑 | ORDER_VERSION_CONFLICT | — |
| L-03 | 定稿后立即驳回 | BL 定稿（version→6）；另一 BL 仍持 version=5 驳回 | ORDER_VERSION_CONFLICT | — |
| L-04 | 上传设计稿后立即提意见 | DS 上传后 version + 1；AM 持旧 version 提意见 | ORDER_VERSION_CONFLICT | — |
| L-05 | 并发标记交付 | 两个 BL 同时 PUT `/deliver` | 一个 SUCCESS；另一个 ORDER_VERSION_CONFLICT | — |

---

## 四、安全越权

| # | 测试场景 | 步骤 | 预期 code | HTTP |
|---|---|---|---|---|
| A-01 | 无 Token 访问鉴权接口 | GET `/orders` 不带 Authorization | UNAUTHORIZED | 401 |
| A-02 | Token 过期 | 用 2h 前签发的 Token | TOKEN_EXPIRED | 401 |
| A-03 | Token 签名篡改 | 改 payload 里 role 后重签失败版本 | TOKEN_INVALID | 401 |
| A-04 | AM 尝试分派设计 | AM Token 调 PUT `/orders/{id}/assign` | PERMISSION_DENIED | 200 |
| A-05 | DS 尝试确认终稿 | DS Token 调 PUT `/orders/{id}/approve` | PERMISSION_DENIED | 200 |
| A-06 | DS 尝试标记交付 | DS Token 调 PUT `/orders/{id}/deliver` | PERMISSION_DENIED | 200 |
| A-07 | DS 尝试作废他人订单 | DS Token 调 PUT `/orders/{other}/cancel` | PERMISSION_DENIED | 200 |
| A-08 | AM 尝试查他人订单详情 | AM Token GET `/orders/{other_id}` | DATA_SCOPE_DENIED | 200 |
| A-09 | XSS 存储型注入 | `customerName: "<script>alert(1)</script>"` | 存储层 escape；前端渲染安全 | — |
| A-10 | SQL 注入 | `keyword: "' OR 1=1 --"` | Mapper LambdaQueryWrapper 预编译，无 SQL 拼接漏洞 | — |

---

## 五、其他边界场景

| # | 测试场景 | 预期 |
|---|---|---|
| B-01 | 订单不存在 | GET `/orders/999999` → `ORDER_NOT_FOUND` |
| B-02 | 设计稿版本不存在 | GET 关联不存在的 version → `DESIGN_VERSION_NOT_FOUND` |
| B-03 | 分派目标不是 DS 角色 | `designerId = 1`（AM）→ `DESIGNER_ROLE_INVALID` |
| B-04 | 空列表查询 | BL 查全部 /orders 返回空列表 → `list: [], hasMore: false` |
| B-05 | Cursor 解码失败 | `cursor: "not-valid-base64"` → 当作首页处理（不报错） |
| B-06 | 单页最大 limit | `limit=200` → 后端自动 clamp 为 100 |
| B-07 | 登录失败锁定 | 同一账号连续 5 次密码错误 → `ACCOUNT_LOCKED` |
| B-08 | 归档后列表过滤 | DELIVERED 状态已归档，AM 列表不显示（BL 可见） |

---

## 六、错误码 → HTTP 状态码映射

| Error Code | HTTP Status | 触发位置 |
|---|---|---|
| UNAUTHORIZED | 401 | JwtInterceptor（无 Token） |
| TOKEN_EXPIRED | 401 | JwtInterceptor（ExpiredJwtException） |
| TOKEN_INVALID | 401 | JwtInterceptor（SignatureException） |
| PERMISSION_DENIED | 403 | ServiceImpl.requireRole() |
| DATA_SCOPE_DENIED | 403 | ServiceImpl.checkDataScope() |
| ORDER_NOT_FOUND | 404 | ServiceImpl.findOrderOrThrow() |
| ORDER_VERSION_CONFLICT | 409 | ServiceImpl.checkVersion() / MP 拦截 |
| ORDER_STATUS_CONFLICT | 422 | ServiceImpl.checkStatus() |
| PARAM_VALIDATION_ERROR | 200 (业务码) | GlobalExceptionHandler（Jakarta Validation） |

> **注意**：本项目 GlobalExceptionHandler 统一返回 HTTP 200 + 业务 code。
> 若前端依赖 HTTP 状态码做处理，可在 GlobalExceptionHandler 中按 BusinessException.code 调整 response.setStatus()。
