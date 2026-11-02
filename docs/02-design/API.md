# API 接口设计文档

> 文档版本：v1.0  
> 适用范围：广告制作公司宣传物料订单管理系统 MVP  
> 前置文档：PRD.md / GLOSSARY.md / STATE_MACHINE.md / PERMISSION_MATRIX.md / FILE_CONTRACT.md  
> BaseURL：`/api/v1`  
> 数据格式：JSON（UTF-8）  
> 命名规范：字段 camelCase，与 GLOSSARY 第 4 节逐字一致；枚举值使用 GLOSSARY 定义的 code

---

## 一、接口总览

### 1.1 统一响应格式

所有接口统一返回如下结构：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "traceId": "abc123def456"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 业务状态码，详见错误码字典 |
| message | string | 人类可读的提示信息 |
| data | object / array / null | 响应数据，失败时为 null |
| traceId | string | 全链路追踪 ID，用于问题排查 |

### 1.2 分页约定（Cursor 游标分页）

**禁止使用 offset / page 分页。** 所有列表接口统一使用 Cursor 游标分页。

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| cursor | string | 否 | 游标，首次请求不传，后续使用返回的 nextCursor |
| limit | integer | 否 | 每页条数，默认 20，最大 50 |

**响应 data 结构：**
```json
{
  "list": [],
  "nextCursor": "eyJpZCI6MTAwfQ==",
  "hasMore": true,
  "total": 128
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| list | array | 当前页数据列表 |
| nextCursor | string / null | 下一页游标，无更多数据时为 null |
| hasMore | boolean | 是否还有下一页 |
| total | integer | 总记录数（仅列表首页返回准确值，后续页可省略） |

### 1.3 接口列表总览

| # | 接口 | 方法 | Domain Action | 可访问角色 | 前置状态 | 需 version |
|---|---|---|---|---|---|---|
| 1 | /auth/login | POST | —（辅助） | 全部（未登录） | — | 否 |
| 2 | /files/presign | POST | —（辅助） | 全部已登录 | — | 否 |
| 3 | /orders | GET | ListOrders | AM / BL / DS | — | 否 |
| 4 | /orders | POST | CreateOrder | AM | WAIT_CREATE → CREATED | 否 |
| 5 | /orders/{id} | GET | GetOrder | AM / BL / DS | 任意 | 否 |
| 6 | /orders/{id} | PUT | UpdateOrder | AM（本人）/ BL | CREATED | 是 |
| 7 | /orders/{id}/assign | PUT | AssignOrder | BL | CREATED → ASSIGNED（T-02） | 是 |
| 8 | /orders/{id}/accept | PUT | AcceptTask | DS | ASSIGNED → DESIGNING（T-03） | 是 |
| 9 | /orders/{id}/reject | PUT | RejectDesign | BL | WAIT_APPROVE → DESIGNING（T-06） | 是 |
| 10 | /orders/{id}/approve | PUT | ApproveFinal | BL | WAIT_APPROVE → APPROVED（T-08） | 是 |
| 11 | /orders/{id}/deliver | PUT | MarkDelivered | BL | APPROVED → DELIVERED（T-09） | 是 |
| 12 | /orders/{id}/cancel | PUT | CancelOrder | BL | 非终态 → CANCELLED（T-10） | 是 |
| 13 | /design-versions | POST | UploadFirstDesign | DS | DESIGNING → WAIT_APPROVE（T-04） | 是 |
| 14 | /design-versions | POST | UploadNewVersion | DS | DESIGNING → WAIT_APPROVE（T-07） | 是 |
| 15 | /order-feedbacks | POST | SubmitFeedback | AM / BL | WAIT_APPROVE → DESIGNING（T-05） | 是 |

> **说明：** UploadFirstDesign 与 UploadNewVersion 共用同一接口端点 `POST /design-versions`，通过订单当前状态与是否存在待处理意见区分业务场景。两者在 GLOSSARY 中为两个独立 Domain Action，对应同一条物理接口。

---

## 二、辅助接口

### 2.1 POST /auth/login — 用户登录

| 项目 | 内容 |
|---|---|
| Domain Action | —（辅助接口） |
| 可访问角色 | 全部（未登录用户） |
| 数据范围 | — |
| 前置状态 | — |
| 是否需 version | 否 |

**请求体：**
```json
{
  "username": "zhangsan",
  "password": "********"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 登录账号 |
| password | string | 是 | 登录密码（明文传输，由 HTTPS 保障安全） |

**响应 data：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "username": "zhangsan",
    "name": "张三",
    "role": "AM",
    "phone": "138****1234"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | JWT 访问令牌 |
| tokenType | string | 令牌类型，固定为 Bearer |
| expiresIn | integer | 过期时间（秒），默认 7200（2 小时） |
| user.id | long | 用户 ID |
| user.username | string | 登录账号 |
| user.name | string | 用户姓名 |
| user.role | string | 用户角色编码（AM / BL / DS），对应 UserRole 枚举 |
| user.phone | string | 用户手机号（脱敏显示） |

---

### 2.2 POST /files/presign — 获取文件直传签名

| 项目 | 内容 |
|---|---|
| Domain Action | —（辅助接口） |
| 可访问角色 | AM / BL / DS |
| 数据范围 | 本人发起的上传 |
| 前置状态 | — |
| 是否需 version | 否 |

> 文件上传完整流程、OSS 路径规则、格式白名单、大小限制详见 **FILE_CONTRACT.md**，本处仅定义 presign 接口契约。

**请求体：**
```json
{
  "fileType": "reference",
  "fileName": "logo.png",
  "fileSize": 1048576,
  "contentType": "image/png"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileType | string | 是 | 文件类型：`reference`（参考文件）/ `design`（设计稿） |
| fileName | string | 是 | 原始文件名 |
| fileSize | long | 是 | 文件大小（字节） |
| contentType | string | 是 | 文件 MIME 类型 |

**响应 data：**
```json
{
  "uploadUrl": "https://bucket.oss-cn-hangzhou.aliyuncs.com/",
  "fileId": "ref_abc123",
  "accessUrl": "https://bucket.oss-cn-hangzhou.aliyuncs.com/ref/2026/10/ref_abc123.webp",
  "formData": {
    "key": "ref/2026/10/ref_abc123.webp",
    "policy": "eyJleHBpcmF0aW9uIjoi...",
    "OSSAccessKeyId": "LTAI...",
    "success_action_status": "200",
    "signature": "abc123..."
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| uploadUrl | string | OSS 上传地址，前端用 POST 表单上传 |
| fileId | string | 文件唯一标识，用于后续关联业务记录 |
| accessUrl | string | 上传成功后的访问 URL |
| formData | object | OSS Policy 签名表单字段，前端需原样携带 |

---

## 三、订单接口

### 3.1 GET /orders — 查询订单列表

| 项目 | 内容 |
|---|---|
| Domain Action | ListOrders |
| 可访问角色 | AM / BL / DS |
| 数据范围 | AM：本人创建；BL：全部；DS：本人负责 |
| 前置状态 | — |
| 是否需 version | 否 |

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| cursor | string | 否 | 游标，首次不传 |
| limit | integer | 否 | 每页条数，默认 20，最大 50 |
| status | string | 否 | 订单状态筛选（OrderStatus 枚举值：CREATED / ASSIGNED / DESIGNING / WAIT_APPROVE / APPROVED / DELIVERED / CANCELLED） |
| keyword | string | 否 | 搜索关键词，匹配订单编号 / 客户名称（模糊匹配） |

**响应 data（分页结构）：**
```json
{
  "list": [
    {
      "id": 1,
      "orderNo": "ORD-2026-00001",
      "customerName": "星辰科技有限公司",
      "materialType": "POSTER",
      "quantity": 500,
      "status": "WAIT_APPROVE",
      "deliveryDate": "2026-11-15",
      "designerName": "李明",
      "createdAt": "2026-10-20T10:30:00"
    }
  ],
  "nextCursor": "eyJpZCI6MjB9",
  "hasMore": true,
  "total": 128
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |
| orderNo | string | 订单编号 |
| customerName | string | 客户名称 |
| materialType | string | 物料类型（MaterialType 枚举值） |
| quantity | integer | 数量 |
| status | string | 订单状态（OrderStatus 枚举值） |
| deliveryDate | string | 交付日期（YYYY-MM-DD） |
| designerName | string | 设计负责人姓名（未分派时为 null） |
| createdAt | string | 创建时间（ISO 8601） |

---

### 3.2 POST /orders — 创建订单

| 项目 | 内容 |
|---|---|
| Domain Action | CreateOrder |
| 可访问角色 | AM |
| 数据范围 | 本人创建 |
| 前置状态 | WAIT_CREATE → CREATED（T-01） |
| 是否需 version | 否 |

**请求体：**
```json
{
  "customerName": "星辰科技有限公司",
  "contactName": "王经理",
  "contactPhone": "13800138000",
  "materialType": "POSTER",
  "quantity": 500,
  "specification": "A2铜版纸双面彩印",
  "deliveryDate": "2026-11-15",
  "remark": "客户要求风格偏科技感",
  "referenceFileIds": ["ref_abc123", "ref_def456"]
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| customerName | string | 是 | 2~100 字符，不可全为空格，XSS 清洗 | 客户名称 |
| contactName | string | 是 | 2~20 字符，仅中文/英文 | 联系人姓名 |
| contactPhone | string | 是 | 11 位，1[3-9]\d{9} | 联系电话 |
| materialType | string | 是 | MaterialType 枚举值 | 物料类型 |
| quantity | integer | 是 | 1~99999，正整数 | 数量 |
| specification | string | 是 | 5~200 字符，XSS 清洗 | 规格说明 |
| deliveryDate | string | 是 | YYYY-MM-DD，晚于今天，早于今天+365天 | 交付日期 |
| remark | string | 否 | ≤500 字符，XSS 清洗 | 备注说明 |
| referenceFileIds | array\<string\> | 否 | 最多 5 个，每个为 presign 返回的 fileId | 参考文件 ID 列表 |

**响应 data：**
```json
{
  "id": 1,
  "orderNo": "ORD-2026-00001",
  "status": "CREATED",
  "createdAt": "2026-10-20T10:30:00"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 新创建订单 ID |
| orderNo | string | 订单编号 |
| status | string | 订单状态（CREATED） |
| createdAt | string | 创建时间 |

---

### 3.3 GET /orders/{id} — 查询订单详情

| 项目 | 内容 |
|---|---|
| Domain Action | GetOrder |
| 可访问角色 | AM / BL / DS |
| 数据范围 | AM：本人创建；BL：全部；DS：本人负责 |
| 前置状态 | 任意 |
| 是否需 version | 否 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**响应 data：**
```json
{
  "id": 1,
  "orderNo": "ORD-2026-00001",
  "customerName": "星辰科技有限公司",
  "contactName": "王经理",
  "contactPhone": "13800138000",
  "materialType": "POSTER",
  "quantity": 500,
  "specification": "A2铜版纸双面彩印",
  "deliveryDate": "2026-11-15",
  "remark": "客户要求风格偏科技感",
  "status": "WAIT_APPROVE",
  "designerId": 3,
  "designerName": "李明",
  "createdBy": 2,
  "createdByName": "张三",
  "createdAt": "2026-10-20T10:30:00",
  "assignedAt": "2026-10-20T11:00:00",
  "isArchived": false,
  "version": 3,
  "referenceFiles": [
    {
      "id": 10,
      "fileUrl": "https://.../ref1.webp",
      "fileName": "参考图1.png",
      "fileSize": 524288
    }
  ],
  "designVersions": [
    {
      "id": 5,
      "versionNo": "V2",
      "fileUrl": "https://.../v2.psd",
      "fileName": "海报设计稿V2.psd",
      "fileSize": 15728640,
      "versionNote": "已按意见调整标题字体和Logo位置",
      "isFinal": false,
      "uploadedByName": "李明",
      "uploadedAt": "2026-10-22T15:30:00"
    }
  ],
  "feedbacks": [
    {
      "id": 8,
      "content": "主标题字体改为粗体，底部Logo向右移动20px",
      "versionId": 4,
      "versionNo": "V1",
      "status": "PROCESSED",
      "submittedByName": "张三",
      "submittedAt": "2026-10-22T10:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |
| orderNo | string | 订单编号 |
| customerName | string | 客户名称 |
| contactName | string | 联系人姓名 |
| contactPhone | string | 联系电话（完整显示） |
| materialType | string | 物料类型（MaterialType 枚举） |
| quantity | integer | 数量 |
| specification | string | 规格说明 |
| deliveryDate | string | 交付日期 |
| remark | string | 备注说明 |
| status | string | 订单状态（OrderStatus 枚举） |
| designerId | long | 设计负责人 ID（未分派时为 null） |
| designerName | string | 设计负责人姓名 |
| createdBy | long | 创建人 ID |
| createdByName | string | 创建人姓名 |
| createdAt | string | 创建时间 |
| assignedAt | string | 分派时间（未分派时为 null） |
| isArchived | boolean | 是否已归档 |
| version | integer | 乐观锁版本号 |
| referenceFiles | array | 参考文件列表 |
| designVersions | array | 设计稿版本列表（按版本号倒序，最新在前） |
| feedbacks | array | 修改意见列表（按提交时间倒序，最新在前） |

---

### 3.4 PUT /orders/{id} — 编辑订单

| 项目 | 内容 |
|---|---|
| Domain Action | UpdateOrder |
| 可访问角色 | AM（本人创建）/ BL（全部） |
| 数据范围 | AM：本人创建且状态为 CREATED；BL：状态为 CREATED 的全部 |
| 前置状态 | CREATED（仅待分派状态可编辑） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "customerName": "星辰科技有限公司",
  "contactName": "王经理",
  "contactPhone": "13800138000",
  "materialType": "POSTER",
  "quantity": 600,
  "specification": "A2铜版纸双面彩印，覆哑膜",
  "deliveryDate": "2026-11-18",
  "remark": "数量调整为600，交期延后3天",
  "referenceFileIds": ["ref_abc123"],
  "version": 1
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| customerName | string | 是 | 同创建 | 客户名称 |
| contactName | string | 是 | 同创建 | 联系人姓名 |
| contactPhone | string | 是 | 同创建 | 联系电话 |
| materialType | string | 是 | 同创建 | 物料类型 |
| quantity | integer | 是 | 同创建 | 数量 |
| specification | string | 是 | 同创建 | 规格说明 |
| deliveryDate | string | 是 | 同创建 | 交付日期 |
| remark | string | 否 | 同创建 | 备注说明 |
| referenceFileIds | array | 否 | 同创建 | 参考文件 ID 列表（全量替换） |
| version | integer | 是 | 必须等于当前订单版本号 | 乐观锁版本号 |

**响应 data：** 同订单详情（返回更新后的完整订单信息）

---

### 3.5 PUT /orders/{id}/assign — 分派订单

| 项目 | 内容 |
|---|---|
| Domain Action | AssignOrder |
| 可访问角色 | BL |
| 数据范围 | 全部订单 |
| 前置状态 | CREATED → ASSIGNED（T-02） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "designerId": 3,
  "assignRemark": "这个单比较急，优先处理",
  "version": 1
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| designerId | long | 是 | 必须为 DS 角色的有效用户 | 设计负责人 ID |
| assignRemark | string | 否 | ≤100 字符，XSS 清洗 | 分派备注 |
| version | integer | 是 | 必须等于当前版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "ASSIGNED",
  "designerId": 3,
  "designerName": "李明",
  "assignedAt": "2026-10-20T11:00:00",
  "version": 2
}
```

---

### 3.6 PUT /orders/{id}/accept — 确认接收任务

| 项目 | 内容 |
|---|---|
| Domain Action | AcceptTask |
| 可访问角色 | DS |
| 数据范围 | 分派给本人的订单 |
| 前置状态 | ASSIGNED → DESIGNING（T-03） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "version": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version | integer | 是 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "DESIGNING",
  "version": 3
}
```

---

### 3.7 PUT /orders/{id}/reject — 驳回设计稿

| 项目 | 内容 |
|---|---|
| Domain Action | RejectDesign |
| 可访问角色 | BL |
| 数据范围 | 全部订单 |
| 前置状态 | WAIT_APPROVE → DESIGNING（T-06） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "rejectNote": "整体风格与客户品牌调性不符，需重新调整配色方案",
  "version": 3
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| rejectNote | string | 是 | 1~500 字符，XSS 清洗 | 驳回原因（必填） |
| version | integer | 是 | 必须等于当前版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "DESIGNING",
  "feedbackId": 10,
  "version": 4
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |
| status | string | 更新后的状态（DESIGNING） |
| feedbackId | long | 自动生成的修改意见记录 ID |
| version | integer | 更新后的版本号 |

---

### 3.8 PUT /orders/{id}/approve — 确认终稿

| 项目 | 内容 |
|---|---|
| Domain Action | ApproveFinal |
| 可访问角色 | BL |
| 数据范围 | 全部订单 |
| 前置状态 | WAIT_APPROVE → APPROVED（T-08） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "approveNote": "设计稿符合客户需求，确认终稿",
  "version": 5
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| approveNote | string | 否 | ≤200 字符，XSS 清洗 | 审核意见 |
| version | integer | 是 | 必须等于当前版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "APPROVED",
  "finalVersionId": 5,
  "approvedAt": "2026-10-25T14:00:00",
  "version": 6
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |
| status | string | 更新后的状态（APPROVED） |
| finalVersionId | long | 被确认为最终版的设计稿版本 ID |
| approvedAt | string | 定稿时间 |
| version | integer | 更新后的版本号 |

---

### 3.9 PUT /orders/{id}/deliver — 标记交付

| 项目 | 内容 |
|---|---|
| Domain Action | MarkDelivered |
| 可访问角色 | BL |
| 数据范围 | 全部订单 |
| 前置状态 | APPROVED → DELIVERED（T-09） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "deliverRemark": "已交付客户，客户确认满意",
  "version": 6
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| deliverRemark | string | 否 | ≤100 字符，XSS 清洗 | 交付备注 |
| version | integer | 是 | 必须等于当前版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "DELIVERED",
  "deliveredAt": "2026-10-25T15:00:00",
  "isArchived": true,
  "version": 7
}
```

---

### 3.10 PUT /orders/{id}/cancel — 作废订单

| 项目 | 内容 |
|---|---|
| Domain Action | CancelOrder |
| 可访问角色 | BL |
| 数据范围 | 全部订单 |
| 前置状态 | CREATED/ASSIGNED/DESIGNING/WAIT_APPROVE → CANCELLED（T-10） |
| 是否需 version | 是 |

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 ID |

**请求体：**
```json
{
  "cancelReason": "客户取消需求",
  "version": 3
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| cancelReason | string | 是 | 1~200 字符，XSS 清洗 | 作废原因 |
| version | integer | 是 | 必须等于当前版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "version": 4
}
```

---

## 四、设计稿版本接口

### 4.1 POST /design-versions — 上传设计稿版本

> **说明：** 该接口同时承载两个 Domain Action，由订单当前状态自动区分：
> - 订单状态为 DESIGNING 且无历史版本 → **UploadFirstDesign**（首版上传）
> - 订单状态为 DESIGNING 且存在待处理意见 → **UploadNewVersion**（迭代上传）

| 项目 | 内容 |
|---|---|
| Domain Action | UploadFirstDesign / UploadNewVersion |
| 可访问角色 | DS |
| 数据范围 | 本人负责的订单 |
| 前置状态 | DESIGNING → WAIT_APPROVE（T-04 / T-07） |
| 是否需 version | 是 |

**请求体：**
```json
{
  "orderId": 1,
  "fileId": "dsn_xyz789",
  "fileName": "海报设计稿V2.psd",
  "fileSize": 15728640,
  "fileType": "psd",
  "versionNote": "已按意见调整标题字体和Logo位置",
  "version": 4
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| orderId | long | 是 | 必须为有效订单 ID | 关联订单 ID |
| fileId | string | 是 | presign 接口返回的 fileId | 设计稿文件 ID |
| fileName | string | 是 | ≤200 字符 | 原始文件名 |
| fileSize | long | 是 | 设计稿源文件 ≤100MB | 文件大小（字节） |
| fileType | string | 是 | psd / ai / jpg / png / pdf / webp | 文件类型（扩展名） |
| versionNote | string | 是 | 1~200 字符，XSS 清洗 | 版本说明 |
| version | integer | 是 | 必须等于当前订单版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 5,
  "versionNo": "V2",
  "fileUrl": "https://.../v2.psd",
  "fileName": "海报设计稿V2.psd",
  "fileSize": 15728640,
  "versionNote": "已按意见调整标题字体和Logo位置",
  "isFinal": false,
  "uploadedAt": "2026-10-22T15:30:00",
  "orderStatus": "WAIT_APPROVE",
  "orderVersion": 5
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 设计稿版本 ID |
| versionNo | string | 版本号（V1 / V2 / ...） |
| fileUrl | string | 文件访问 URL |
| fileName | string | 原始文件名 |
| fileSize | long | 文件大小（字节） |
| versionNote | string | 版本说明 |
| isFinal | boolean | 是否为最终版 |
| uploadedAt | string | 上传时间 |
| orderStatus | string | 更新后的订单状态（WAIT_APPROVE） |
| orderVersion | integer | 更新后的订单版本号 |

---

## 五、修改意见接口

### 5.1 POST /order-feedbacks — 提交修改意见

| 项目 | 内容 |
|---|---|
| Domain Action | SubmitFeedback |
| 可访问角色 | AM / BL |
| 数据范围 | AM：本人创建；BL：全部 |
| 前置状态 | WAIT_APPROVE → DESIGNING（T-05） |
| 是否需 version | 是 |

**请求体：**
```json
{
  "orderId": 1,
  "content": "主标题字体改为粗体，底部Logo向右移动20px",
  "version": 3
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| orderId | long | 是 | 必须为有效订单 ID | 关联订单 ID |
| content | string | 是 | 1~500 字符，XSS 清洗 | 修改意见内容 |
| version | integer | 是 | 必须等于当前订单版本号 | 乐观锁版本号 |

**响应 data：**
```json
{
  "id": 8,
  "content": "主标题字体改为粗体，底部Logo向右移动20px",
  "status": "PENDING",
  "versionId": 4,
  "versionNo": "V1",
  "submittedAt": "2026-10-22T10:00:00",
  "orderStatus": "DESIGNING",
  "orderVersion": 4
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 修改意见 ID |
| content | string | 意见内容 |
| status | string | 意见状态（FeedbackStatus 枚举：PENDING） |
| versionId | long | 关联的设计稿版本 ID |
| versionNo | string | 关联的设计稿版本号 |
| submittedAt | string | 提交时间 |
| orderStatus | string | 更新后的订单状态（DESIGNING） |
| orderVersion | integer | 更新后的订单版本号 |

---

## 六、错误码字典

### 6.1 HTTP 状态码总览

| HTTP 状态码 | 含义 | 典型场景 |
|---|---|---|
| 200 OK | 请求成功 | 正常查询、创建、更新成功 |
| 400 Bad Request | 请求参数错误 | 参数缺失、格式错误、校验失败 |
| 401 Unauthorized | 未认证 | Token 缺失、Token 过期、Token 无效 |
| 403 Forbidden | 无权限 | 角色权限不足、数据越权访问 |
| 404 Not Found | 资源不存在 | 订单 ID 不存在、用户不存在 |
| 409 Conflict | 版本冲突 | 乐观锁冲突、并发操作冲突 |
| 422 Unprocessable Entity | 业务规则不满足 | 状态流转前置条件不满足、重复操作 |
| 500 Internal Server Error | 服务器内部错误 | 数据库异常、未预期的运行时错误 |

### 6.2 业务错误码明细表

| 错误码 code | HTTP 状态码 | message 模板 | 触发场景 |
|---|---|---|---|
| SUCCESS | 200 | 操作成功 | 请求成功 |
| PARAM_VALIDATION_ERROR | 400 | 参数校验失败：{field} {message} | 请求参数格式或值不合法 |
| MISSING_REQUIRED_FIELD | 400 | 缺少必填字段：{field} | 必填字段为空或未传 |
| INVALID_PHONE_FORMAT | 400 | 手机号码格式不正确 | 联系电话不符合 1[3-9]\d{9} |
| INVALID_QUANTITY | 400 | 数量必须大于 0 且不超过 99999 | 数量为 0 / 负数 / 超上限 |
| INVALID_DELIVERY_DATE | 400 | 交付日期不能早于今天 | 交付日期已过期 |
| INVALID_FILE_FORMAT | 400 | 不支持的文件格式：{format} | 文件格式不在白名单内 |
| FILE_TOO_LARGE | 400 | 文件大小不能超过 {size} | 文件大小超出限制 |
| UNAUTHORIZED | 401 | 未登录或登录已过期 | Token 缺失 / 无效 / 过期 |
| TOKEN_EXPIRED | 401 | 登录已过期，请重新登录 | Token 超过有效期 |
| ACCOUNT_LOCKED | 401 | 账号已被锁定，请 {minutes} 分钟后重试 | 登录失败次数过多被锁定 |
| PERMISSION_DENIED | 403 | 无操作权限 | 角色无该接口访问权限 |
| DATA_PERMISSION_DENIED | 403 | 无权访问该数据 | 尝试访问不属于自己的订单 |
| ORDER_NOT_FOUND | 404 | 订单不存在 | 订单 ID 无效或已被逻辑删除 |
| USER_NOT_FOUND | 404 | 用户不存在 | 用户 ID 或账号无效 |
| DESIGN_VERSION_NOT_FOUND | 404 | 设计稿版本不存在 | 设计稿版本 ID 无效 |
| ORDER_VERSION_CONFLICT | 409 | 订单已被他人修改，请刷新后重试 | 乐观锁版本号不匹配，并发冲突 |
| ORDER_STATUS_CONFLICT | 422 | 订单当前状态不允许此操作 | 状态流转前置条件不满足 |
| ORDER_ALREADY_ASSIGNED | 422 | 订单已被分派，不可重复分派 | 对已分派订单再次分派 |
| ORDER_ALREADY_DELIVERED | 422 | 订单已交付，不可再修改 | 对已交付订单执行编辑操作 |
| ORDER_NOT_APPROVED | 422 | 订单尚未定稿，不可交付 | 在非 APPROVED 状态下尝试交付 |
| NO_PENDING_FEEDBACK | 422 | 当前没有待处理的修改意见 | DESIGNING 状态下无待处理意见却上传新版本 |
| DESIGN_NOT_SUBMITTED | 422 | 尚未提交设计稿 | WAIT_APPROVE 状态前尝试提交意见 |
| INTERNAL_SERVER_ERROR | 500 | 服务器内部错误，请稍后重试 | 未预期的系统异常 |

---

> **文档结束**  
> 所有字段命名、枚举值严格遵循 GLOSSARY.md 统一领域语言词汇表。  
> 文件上传契约详见 FILE_CONTRACT.md；状态流转详见 STATE_MACHINE.md；权限矩阵详见 PERMISSION_MATRIX.md。
