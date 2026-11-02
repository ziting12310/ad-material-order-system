# AMOMS 接口调用顺序说明

> **BaseURL**：`http://localhost:8080/api`（application.yml context-path=/api）
> **Postman/Apifox Header 设置**：`Authorization: Bearer <JWT_TOKEN>`

---

## 一、前置条件

### 1.1 初始化数据库

```sql
-- MySQL 8.0
CREATE DATABASE IF NOT EXISTS amoms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE amoms;
SOURCE D:/courses/.../Peak/backend/src/main/resources/schema.sql;
```

### 1.2 启动后端

```bash
cd Peak/backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Swagger 访问：http://localhost:8080/api/doc.html
```

### 1.3 种子用户（schema.sql 已初始化）

| username | password | 角色 | 用途 |
|---|---|---|---|
| am_demo | 123456 | AM 客户经理 | 创建订单 |
| bl_demo | 123456 | BL 业务负责人 | 分派 / 定稿 / 交付 |
| ds_demo | 123456 | DS 设计人员 | 接单 / 上传设计稿 |

---

## 二、完整业务闭环调用顺序（8 步）

```mermaid
sequenceDiagram
    participant 前端 as Vue 前端
    participant 后端 as AMOMS 后端
    participant DB as MySQL

    Note over 前端,DB: ① 登录获取 JWT
    前端->>后端: POST /v1/auth/login {username, password}
    后端->>DB: SELECT users WHERE username=?
    DB-->>后端: User Entity
    后端-->>前端: {code:SUCCESS, data:{token:"eyJhbG..."}}

    Note over 前端,DB: ② 创建订单（AM）
    前端->>后端: POST /v1/orders [Auth: Bearer xxx]
    后端->>后端: 生成订单号 ORD-2026-NNNNN
    后端->>DB: INSERT orders
    后端-->>前端: {id:1, orderNo:"ORD-2026-00001", status:"CREATED"}

    Note over 前端,DB: ③ 分派设计人员（BL）
    前端->>后端: PUT /v1/orders/1/assign [Auth: Bearer xxx]
    后端->>DB: SELECT users WHERE id=3 AND role='DS'
    后端->>DB: UPDATE orders SET status='ASSIGNED', designer_id=3, version=1
    后端-->>前端: {status:"ASSIGNED"}

    Note over 前端,DB: ④ 设计师接单（DS）
    前端->>后端: PUT /v1/orders/1/accept [Auth: Bearer xxx]
    后端->>DB: UPDATE orders SET status='DESIGNING', version=2
    后端-->>前端: {status:"DESIGNING"}

    Note over 前端,DB: ⑤ 上传首版设计稿（DS）
    前端->>后端: POST /v1/design-versions [Auth: Bearer xxx]
    后端->>DB: INSERT design_versions {versionNo:"V1"}
    后端->>DB: UPDATE orders SET status='WAIT_APPROVE', version=3
    后端-->>前端: {orderStatus:"WAIT_APPROVE"}

    Note over 前端,DB: ⑥ 提交修改意见（AM/BL）— 触发 DESIGNING 回退
    前端->>后端: POST /v1/order-feedbacks [Auth: Bearer xxx]
    后端->>DB: INSERT order_feedbacks {status:"PENDING"}
    后端->>DB: UPDATE orders SET status='DESIGNING', version=4
    后端-->>前端: {orderStatus:"DESIGNING"}

    Note over 前端,DB: ⑦ 上传新版本 V2（DS）— 回到 WAIT_APPROVE
    前端->>后端: POST /v1/design-versions [Auth: Bearer xxx]
    后端->>DB: INSERT design_versions {versionNo:"V2"}
    后端->>DB: UPDATE orders SET status='WAIT_APPROVE', version=5
    后端-->>前端: {orderStatus:"WAIT_APPROVE"}

    Note over 前端,DB: ⑧ 确认终稿 + 标记交付（BL）
    前端->>后端: PUT /v1/orders/1/approve [Auth: Bearer xxx]
    后端->>DB: UPDATE design_versions SET is_final=1 WHERE id=?
    后端->>DB: UPDATE order_feedbacks SET status='PROCESSED'
    后端->>DB: UPDATE orders SET status='APPROVED', version=6
    后端-->>前端: {status:"APPROVED"}
    前端->>后端: PUT /v1/orders/1/deliver [Auth: Bearer xxx]
    后端->>DB: UPDATE orders SET status='DELIVERED', archived=1, version=7
    后端-->>前端: {status:"DELIVERED", isArchived:true}
```

---

## 三、curl 调用示例（完整闭环）

### 步骤 1：登录（无 JWT）

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"am_demo","password":"123456"}'

# 保存 JWT_TOKEN 供后续调用
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 步骤 2：创建订单

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "customerName": "可口可乐",
    "contactName": "李四",
    "contactPhone": "13800138000",
    "materialType": "POSTER",
    "quantity": 1000,
    "specification": "A3 尺寸 300g 铜版纸双面印刷",
    "deliveryDate": "2026-11-15",
    "remark": "蓝色主色调"
  }'
```

### 步骤 3：查看列表（Cursor 分页）

```bash
curl "http://localhost:8080/api/v1/orders?limit=10" \
  -H "Authorization: Bearer $JWT_TOKEN"

# 翻页：带上一次返回的 nextCursor
curl "http://localhost:8080/api/v1/orders?cursor=eyJpZCI6MX0=&limit=10" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### 步骤 4：分派设计人员（需 BL 角色）

```bash
# 先 BL 登录拿新 Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bl_demo","password":"123456"}'

curl -X PUT http://localhost:8080/api/v1/orders/1/assign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BL_JWT" \
  -d '{"designerId": 3, "assignRemark": "优先处理", "version": 0}'
```

### 步骤 5：设计师接单（需 DS 角色）

```bash
curl -X PUT http://localhost:8080/api/v1/orders/1/accept \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DS_JWT" \
  -d '{"version": 1}'
```

### 步骤 6：上传首版设计稿

```bash
curl -X POST http://localhost:8080/api/v1/design-versions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DS_JWT" \
  -d '{
    "orderId": 1,
    "fileId": "oss_presigned_url_here",
    "fileName": "poster_v1.psd",
    "fileSize": 20971520,
    "fileType": "psd",
    "versionNote": "首版蓝色主色调设计",
    "version": 2
  }'
```

### 步骤 7：提交修改意见 → 订单回退 DESIGNING

```bash
curl -X POST http://localhost:8080/api/v1/order-feedbacks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AM_JWT" \
  -d '{"orderId": 1, "content": "标题需要上移 20px", "version": 3}'
```

### 步骤 8：上传 V2 → 确认终稿 → 交付

```bash
# 8.1 上传 V2
curl -X POST http://localhost:8080/api/v1/design-versions \
  -H "Authorization: Bearer $DS_JWT" \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"fileId":"oss...","fileName":"v2.psd","fileSize":20971520,"fileType":"psd","versionNote":"标题已上移","version":4}'

# 8.2 确认终稿（BL）
curl -X PUT http://localhost:8080/api/v1/orders/1/approve \
  -H "Authorization: Bearer $BL_JWT" \
  -H "Content-Type: application/json" \
  -d '{"approveNote": "通过", "version": 5}'

# 8.3 标记交付（BL）
curl -X PUT http://localhost:8080/api/v1/orders/1/deliver \
  -H "Authorization: Bearer $BL_JWT" \
  -H "Content-Type: application/json" \
  -d '{"deliverRemark": "快递已发出", "version": 6}'
```

---

## 四、非鉴权接口清单（可直接调用）

| 接口 | 原因 |
|---|---|
| POST /v1/auth/login | 登录本身不需要 Token |
| /doc.html /v3/api-docs/** | Knife4j Swagger 文档 |
| POST /v1/files/presign | 文件直传签名（MVP 阶段也放行；可选加白名单） |

## 五、鉴权接口统一 Header 格式

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

- 前缀固定 `Bearer `（注意空格）
- Token 来源：登录接口返回的 `data.token` 字段
- 有效期：7200 秒（2 小时），过期后返回 401 TOKEN_EXPIRED
