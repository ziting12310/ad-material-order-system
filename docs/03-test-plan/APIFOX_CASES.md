# AMOMS Apifox 接口用例

> 本文档可直接导入 Apifox / Postman / Swagger
> 统一 BaseURL：`http://localhost:8080/api`
> 统一鉴权 Header：`Authorization: Bearer <JWT>`

---

## 一、认证模块（2 接口）

### TC-AUTH-01 登录成功

| 项 | 值 |
|---|---|
| **Method** | POST |
| **URL** | `/v1/auth/login` |
| **Headers** | `Content-Type: application/json` |
| **Body** | `{"username":"am_demo","password":"123456"}` |
| **预期 HTTP** | 200 |
| **预期 code** | `SUCCESS` |
| **预期 data 关键字段** | `data.token` 非空；`data.user.role = "AM"`；`data.expiresIn = 7200` |
| **后置动作** | 保存 `data.token` 到环境变量 `{{AM_TOKEN}}` |

### TC-AUTH-02 登录失败（密码错误）

| 项 | 值 |
|---|---|
| **Body** | `{"username":"am_demo","password":"wrong_password"}` |
| **预期 HTTP** | 200（业务异常仍返回 200，code 区分） |
| **预期 code** | `LOGIN_FAILED` |
| **预期 message** | 包含"账号或密码错误" |

---

## 二、订单查询（3 接口）

### TC-ORD-01 订单列表（Cursor 分页）

| 项 | 值 |
|---|---|
| **Method** | GET |
| **URL** | `/v1/orders?limit=10` |
| **Headers** | `Authorization: Bearer {{AM_TOKEN}}` |
| **预期 code** | SUCCESS |
| **预期 data** | `data.list` 数组；`data.hasMore` 布尔；`data.nextCursor` 字符串；无 `offset/page` 字段 |

### TC-ORD-02 订单列表（按状态过滤）

| 项 | 值 |
|---|---|
| **URL** | `/v1/orders?status=CREATED&limit=20` |
| **预期 data** | 所有返回项 `status = "CREATED"` |

### TC-ORD-03 订单详情

| 项 | 值 |
|---|---|
| **URL** | `/v1/orders/1` |
| **预期 code** | SUCCESS |
| **预期 data 结构** | 含 `referenceFiles` 数组、`designVersions` 数组、`feedbacks` 数组；**不含** `passwordHash` |

---

## 三、订单写入（7 接口）

### TC-ORD-CREATE 创建订单

| 项 | 值 |
|---|---|
| **Method** | POST |
| **URL** | `/v1/orders` |
| **Body** | `{"customerName":"可口可乐","contactName":"李四","contactPhone":"13800138000","materialType":"POSTER","quantity":1000,"specification":"A3 300g 铜版纸","deliveryDate":"2026-12-01","remark":"蓝色主色调"}` |
| **预期 code** | SUCCESS |
| **预期 data** | `status = "CREATED"`；`orderNo` 匹配 `ORD-2026-0000N` 格式；`version = 0` |

### TC-ORD-UPDATE 编辑订单（仅 CREATED 可编辑）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}` |
| **Body** | 与 Create 类似，额外 `version: 0` |
| **前置条件** | 目标订单状态为 CREATED |
| **预期 code** | SUCCESS |

### TC-ORD-ASSIGN 分派设计人员（BL 角色）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}/assign` |
| **Body** | `{"designerId": 3, "assignRemark":"优先", "version": 1}` |
| **预期 data** | `status = "ASSIGNED"`；`designerName` 非空；`assignedAt` 非空 |

### TC-ORD-ACCEPT 设计师接单（DS 角色）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}/accept` |
| **Body** | `{"version": 2}` |
| **预期 data** | `status = "DESIGNING"` |

### TC-ORD-REJECT 驳回设计（BL 角色）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}/reject` |
| **Body** | `{"rejectNote":"颜色偏差太大，请重新设计","version": 3}` |
| **前置条件** | 目标订单状态为 WAIT_APPROVE |
| **预期 data** | `status = "DESIGNING"`；`feedbackId` 非空 |
| **副作用** | 自动插入一条 Feedback（content = rejectNote） |

### TC-ORD-APPROVE 确认终稿（BL 角色）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}/approve` |
| **Body** | `{"approveNote":"通过","version": 4}` |
| **预期 data** | `status = "APPROVED"`；`finalVersionId` 非空；`approvedAt` 非空 |
| **副作用** | 所有 PENDING Feedback 置 PROCESSED；finalVersion.isFinal = 1 |

### TC-ORD-DELIVER 标记交付（BL 角色）

| 项 | 值 |
|---|---|
| **Method** | PUT |
| **URL** | `/v1/orders/{id}/deliver` |
| **Body** | `{"deliverRemark":"快递 SF123456","version": 5}` |
| **预期 data** | `status = "DELIVERED"`；`isArchived = true`；`deliveredAt` 非空 |

---

## 四、设计稿版本（1 接口）

### TC-DV-01 上传首版

| 项 | 值 |
|---|---|
| **Method** | POST |
| **URL** | `/v1/design-versions` |
| **Body** | `{"orderId":1,"fileId":"oss-key-001","fileName":"v1.psd","fileSize":20971520,"fileType":"psd","versionNote":"首版设计","version":3}` |
| **前置条件** | 订单状态 DESIGNING |
| **预期 data** | `versionNo = "V1"`；`orderStatus = "WAIT_APPROVE"`；`orderVersion = 4` |

### TC-DV-02 上传新版本 V2（迭代）

| 项 | 值 |
|---|---|
| **Body** | `{"orderId":1,"fileId":"oss-key-002","fileName":"v2.psd","fileSize":20971520,"fileType":"psd","versionNote":"标题上移 20px","version":4}` |
| **预期 data** | `versionNo = "V2"` |

---

## 五、修改意见（1 接口）

### TC-FB-01 提交修改意见

| 项 | 值 |
|---|---|
| **Method** | POST |
| **URL** | `/v1/order-feedbacks` |
| **Body** | `{"orderId":1,"content":"标题上移 20px","version":3}` |
| **前置条件** | 订单状态 WAIT_APPROVE |
| **预期 data** | `status = "PENDING"`；`orderStatus = "DESIGNING"`（触发状态回退） |

---

## 六、并发 / 越权 / 安全用例

### TC-SEC-01 并发分派（乐观锁）

| 项 | 值 |
|---|---|
| **场景** | BL-A 和 BL-B 同时 PUT `/orders/1/assign`，携带相同 version=0 |
| **预期** | 一个成功 SUCCESS；另一个返回 `ORDER_VERSION_CONFLICT`（409） |

### TC-SEC-02 数据越权

| 项 | 值 |
|---|---|
| **场景** | AM 角色 GET `/orders/999`（id=999 是另一个 AM 创建的订单） |
| **预期 code** | `DATA_SCOPE_DENIED` |

### TC-SEC-03 非法 JWT

| 项 | 值 |
|---|---|
| **场景** | Header 传 `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature` |
| **预期 code** | `TOKEN_INVALID` |
| **预期 HTTP** | 401 |

### TC-SEC-04 过期 JWT

| 项 | 值 |
|---|---|
| **场景** | 用 2 小时前签发的 Token 请求 |
| **预期 code** | `TOKEN_EXPIRED` |
| **预期 HTTP** | 401 |

### TC-SEC-05 未携带 JWT

| 项 | 值 |
|---|---|
| **场景** | 请求不带 Authorization Header |
| **预期 code** | `UNAUTHORIZED` |
| **预期 HTTP** | 401 |

### TC-SEC-06 越权角色（AM 尝试分派）

| 项 | 值 |
|---|---|
| **场景** | 用 AM Token 调 PUT `/orders/1/assign`（分派只能 BL） |
| **预期 code** | `PERMISSION_DENIED` |

### TC-SEC-07 XSS 注入

| 项 | 值 |
|---|---|
| **场景** | 创建订单 body `customerName: "<script>alert(1)</script>"` |
| **预期** | 存储层值被 escape；前端渲染无 script 执行；DB 存 `&lt;script&gt;alert(1)&lt;/script&gt;` |

---

## 七、Apifox 环境变量建议

| 变量名 | 值示例 | 用途 |
|---|---|---|
| `baseUrl` | `http://localhost:8080/api` | BaseURL |
| `AM_TOKEN` | `eyJhbG...` | 客户经理 Token |
| `BL_TOKEN` | `eyJhbG...` | 业务负责人 Token |
| `DS_TOKEN` | `eyJhbG...` | 设计人员 Token |
| `ORDER_ID` | `1` | 动态订单 ID |
