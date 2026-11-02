# 统一领域语言词汇表（Glossary）

> 文档版本：v1.0  
> 适用范围：广告制作公司宣传物料订单管理系统 MVP  
> 关联文档：PRD.md / USER_STORIES.md  
> 用途：作为 MySQL 建模、Java/TS 后端实体、Vue 前端组件、API 文档的唯一命名基线，禁止任何角色自行命名。

---

## 一、命名总则表

| 规则编号 | 命名对象 | 命名规范 | 示例 |
|---|---|---|---|
| NM-001 | MySQL 表名 | snake_case 复数形式 | `orders`、`design_versions`、`order_feedbacks` |
| NM-002 | MySQL 字段名 | snake_case 单数形式 | `order_no`、`customer_name`、`created_at` |
| NM-003 | MySQL 主键 | `id`（自增 BIGINT），外键为 `{entity_singular}_id` | `id`、`order_id`、`designer_id` |
| NM-004 | MySQL 时间字段 | `_at` 后缀 | `created_at`、`assigned_at`、`delivered_at` |
| NM-005 | MySQL 布尔字段 | `is_` 前缀，TINYINT(1) 存储 | `is_archived`、`is_deleted` |
| NM-006 | MySQL 枚举字段 | 存储 VARCHAR 或 TINYINT，对应枚举类编码 | `status` 存 `WAIT_APPROVE` |
| NM-007 | Java 实体类名 | PascalCase 单数形式 | `Order`、`DesignVersion`、`OrderFeedback` |
| NM-008 | Java 枚举类名 | PascalCase，Enum 后缀可选 | `OrderStatus`、`MaterialType` |
| NM-009 | Java 枚举常量 | UPPER_SNAKE_CASE | `WAIT_APPROVE`、`CREATED`、`DELIVERED` |
| NM-010 | Java 字段 / 方法 | camelCase | `orderNo`、`customerName`、`getOrderNo()` |
| NM-011 | Java 时间字段 | `At` 后缀 | `createdAt`、`assignedAt`、`deliveredAt` |
| NM-012 | Java 布尔字段 | 不使用 `is` 前缀（避免与 getter 冲突） | `archived`、`deleted` |
| NM-013 | TypeScript 类型 / 接口 / 组件名 | PascalCase | `Order`、`OrderDetailProps`、`OrderList.vue` |
| NM-014 | TypeScript 字段 / 函数 / 变量 | camelCase | `orderNo`、`customerName`、`fetchOrder()` |
| NM-015 | TypeScript 布尔字段 | `is` 前缀 | `isArchived`、`isDeleted` |
| NM-016 | TypeScript 时间字段 | `At` 后缀 | `createdAt`、`assignedAt` |
| NM-017 | API 路径 | 小写复数 + kebab-case | `/api/v1/orders`、`/api/v1/design-versions` |
| NM-018 | API 路径参数 | kebab-case | `/orders/{order-id}` → 实际用 `orderId` 变量 |
| NM-019 | API 查询参数 | camelCase | `pageSize`、`status`、`customerName` |
| NM-020 | Vue 组件文件名 | PascalCase + .vue 后缀 | `OrderList.vue`、`DesignUploader.vue` |
| NM-021 | Vue 组合式函数 | camelCase，use 前缀 | `useOrderList()`、`useDesignUpload()` |
| NM-022 | 前端状态管理 Store | camelCase，use 前缀 + Store 后缀 | `useOrderStore()` |

---

## 二、核心领域实体（Domain Entities）

| 中文领域概念 | 统一英文标识符 | MySQL 表名 | Java 实体类 | TypeScript 接口 | API 路径前缀 | 业务含义与边界定义 |
|---|---|---|---|---|---|---|
| 订单 | Order | `orders` | `Order` | `Order` | `/api/v1/orders` | 一笔宣传物料订单的主记录，包含客户信息、物料规格、状态、交付信息等全量业务字段。MVP 中客户信息为内联字段，不独立建表。 |
| 设计稿版本 | DesignVersion | `design_versions` | `DesignVersion` | `DesignVersion` | `/api/v1/design-versions` | 某订单下的一个设计稿版本，包含文件 URL、版本号、版本说明、上传人等。每个订单可对应多个版本，版本号从 V1 递增。 |
| 订单修改意见 | OrderFeedback | `order_feedbacks` | `OrderFeedback` | `OrderFeedback` | `/api/v1/order-feedbacks` | 针对某订单某版本设计稿的修改意见记录，包含意见内容、提交人、关联版本、意见状态。一条意见对应一个版本。 |
| 用户 | User | `users` | `User` | `User` | `/api/v1/users` | 系统内部用户，包含账号、姓名、角色等。MVP 仅 3 类业务角色 + 1 类访客（非实体）。 |
| 参考文件 | ReferenceFile | `reference_files` | `ReferenceFile` | `ReferenceFile` | （内联在订单内） | 订单登记时上传的参考资料文件，直传 OSS，仅存 URL。MVP 中作为订单的附属实体，不单独提供 CRUD 接口。 |

---

## 三、核心领域枚举（Domain Enums）

### 3.1 订单状态 OrderStatus

| 中文领域概念 | 统一英文标识符（枚举常量） | MySQL 存储值 | Java 枚举类 | TS 枚举名 | 业务含义与边界定义 |
|---|---|---|---|---|---|
| 待登记 | WAIT_CREATE | `WAIT_CREATE` | `OrderStatus.WAIT_CREATE` | `OrderStatus.WaitCreate` | 客户经理进入新建订单页但尚未提交的草稿状态。MVP 中可前端态，不一定落库。 |
| 已登记（待分派） | CREATED | `CREATED` | `OrderStatus.CREATED` | `OrderStatus.Created` | 客户经理已提交订单，等待业务负责人分派设计人员。**业务口语"待分派"= CREATED，数据库必须存 CREATED。** |
| 已分派 | ASSIGNED | `ASSIGNED` | `OrderStatus.ASSIGNED` | `OrderStatus.Assigned` | 业务负责人已指定设计人员，设计人员尚未确认接收或尚未上传首版。**USER_STORIES 中"设计中"起始态，数据库独立存 ASSIGNED。** |
| 设计中 | DESIGNING | `DESIGNING` | `OrderStatus.DESIGNING` | `OrderStatus.Designing` | 设计人员正在进行设计制作。**"修改中"不是独立状态码，归入 DESIGNING。** 只要订单在设计人员手中迭代，均为 DESIGNING。 |
| 待定稿 | WAIT_APPROVE | `WAIT_APPROVE` | `OrderStatus.WAIT_APPROVE` | `OrderStatus.WaitApprove` | 设计稿已提交，等待业务负责人审核确认。**"待审核"统一为 WAIT_APPROVE / 待定稿，禁止使用 PENDING_REVIEW 等别名。** |
| 已定稿 | APPROVED | `APPROVED` | `OrderStatus.APPROVED` | `OrderStatus.Approved` | 业务负责人已确认终稿，待交付归档。**"确认终稿并交付"拆解为 ApproveFinal + MarkDelivered 两步，APPROVED 为中间态，不可跳过。** |
| 已交付 | DELIVERED | `DELIVERED` | `OrderStatus.DELIVERED` | `OrderStatus.Delivered` | 订单已完成交付，归档状态，所有编辑操作禁用。 |
| 已作废 | CANCELLED | `CANCELLED` | `OrderStatus.CANCELLED` | `OrderStatus.Cancelled` | 订单因客户取消或其他原因作废，终止流程。**禁止使用 DELETED / INVALID 等别名。** |

### 3.2 物料类型 MaterialType

| 中文领域概念 | 统一英文标识符（枚举常量） | MySQL 存储值 | Java 枚举类 | TS 枚举名 | 业务含义与边界定义 |
|---|---|---|---|---|---|
| 海报 | POSTER | `POSTER` | `MaterialType.POSTER` | `MaterialType.Poster` | 纸质海报类宣传物料。 |
| 宣传单 | FLYER | `FLYER` | `MaterialType.FLYER` | `MaterialType.Flyer` | 宣传单页、折页类物料。 |
| 名片 | BUSINESS_CARD | `BUSINESS_CARD` | `MaterialType.BUSINESS_CARD` | `MaterialType.BusinessCard` | 个人名片设计。 |
| 画册 | BROCHURE | `BROCHURE` | `MaterialType.BROCHURE` | `MaterialType.Brochure` | 企业画册、产品手册类。 |
| 喷绘 | BANNER | `BANNER` | `MaterialType.BANNER` | `MaterialType.Banner` | 大型喷绘、背景板、X 展架类。 |
| 其他 | OTHER | `OTHER` | `MaterialType.OTHER` | `MaterialType.Other` | 未分类的其他物料类型。 |

### 3.3 用户角色 UserRole

| 中文领域概念 | 统一英文标识符（枚举常量） | MySQL 存储值 | Java 枚举类 | TS 枚举名 | 业务含义与边界定义 |
|---|---|---|---|---|---|
| 客户经理 | AM | `AM` | `UserRole.AM` | `UserRole.AM` | Account Manager，订单发起人，负责客户对接与订单登记。 |
| 业务负责人 | BL | `BL` | `UserRole.BL` | `UserRole.BL` | Business Lead，订单管理者，负责分派、审核、交付。 |
| 设计制作人员 | DS | `DS` | `UserRole.DS` | `UserRole.DS` | Designer，设计执行者，负责设计稿上传与迭代。 |
| 访客 | GUEST | — | — | — | 未登录用户，不落库，前端权限判断用。 |

### 3.4 修改意见状态 FeedbackStatus

| 中文领域概念 | 统一英文标识符（枚举常量） | MySQL 存储值 | Java 枚举类 | TS 枚举名 | 业务含义与边界定义 |
|---|---|---|---|---|---|
| 待处理 | PENDING | `PENDING` | `FeedbackStatus.PENDING` | `FeedbackStatus.Pending` | 修改意见已提交，设计人员尚未处理。 |
| 已处理 | PROCESSED | `PROCESSED` | `FeedbackStatus.PROCESSED` | `FeedbackStatus.Processed` | 设计人员已上传新版本，该意见标记为已处理。 |

---

## 四、业务操作动词（Domain Actions）

| 中文领域概念 | 统一英文标识符（Action） | Java 方法名 / Service 方法 | TS 函数名 / API 名 | API 端点 | 触发前状态 | 触发后状态 | 操作角色 | 业务含义与边界定义 |
|---|---|---|---|---|---|---|---|---|
| 创建订单 | CreateOrder | `createOrder()` | `createOrder()` | `POST /api/v1/orders` | WAIT_CREATE | CREATED | AM | 客户经理提交新订单，必填项校验通过后生成订单记录。 |
| 分派订单 | AssignOrder | `assignOrder()` | `assignOrder()` | `PUT /api/v1/orders/{id}/assign` | CREATED | ASSIGNED | BL | 业务负责人为订单指定设计人员。 |
| 确认接收任务 | AcceptTask | `acceptTask()` | `acceptTask()` | `PUT /api/v1/orders/{id}/accept` | ASSIGNED | DESIGNING | DS | 设计人员确认接收任务，正式开始设计。 |
| 上传首版设计稿 | UploadFirstDesign | `uploadFirstDesign()` | `uploadFirstDesign()` | `POST /api/v1/design-versions` | DESIGNING | WAIT_APPROVE | DS | 设计人员上传第一版设计稿，版本号 V1。 |
| 提交修改意见 | SubmitFeedback | `submitFeedback()` | `submitFeedback()` | `POST /api/v1/order-feedbacks` | WAIT_APPROVE | DESIGNING | AM / BL | 客户经理或业务负责人提交修改意见，订单退回设计中。 |
| 上传新版本设计稿 | UploadNewVersion | `uploadNewVersion()` | `uploadNewVersion()` | `POST /api/v1/design-versions` | DESIGNING | WAIT_APPROVE | DS | 设计人员根据修改意见上传新版本，版本号递增。 |
| 驳回重做 | RejectDesign | `rejectDesign()` | `rejectDesign()` | `PUT /api/v1/orders/{id}/reject` | WAIT_APPROVE | DESIGNING | BL | 业务负责人驳回设计稿，需填写驳回意见。 |
| 确认终稿 | ApproveFinal | `approveFinal()` | `approveFinal()` | `PUT /api/v1/orders/{id}/approve` | WAIT_APPROVE | APPROVED | BL | 业务负责人确认设计稿为最终版。**禁止与交付合并为一步操作。** |
| 标记交付 | MarkDelivered | `markDelivered()` | `markDelivered()` | `PUT /api/v1/orders/{id}/deliver` | APPROVED | DELIVERED | BL | 业务负责人标记订单已交付，订单归档。 |
| 作废订单 | CancelOrder | `cancelOrder()` | `cancelOrder()` | `PUT /api/v1/orders/{id}/cancel` | CREATED/ASSIGNED/DESIGNING/WAIT_APPROVE | CANCELLED | BL | 业务负责人作废订单，终止流程。 |
| 查询订单列表 | ListOrders | `listOrders()` | `listOrders()` | `GET /api/v1/orders` | — | — | 全部已登录 | 按角色权限查询对应范围内的订单列表，Cursor 游标分页。 |
| 查询订单详情 | GetOrder | `getOrder()` | `getOrder()` | `GET /api/v1/orders/{id}` | — | — | 全部已登录（按权限） | 查询单条订单详情及关联设计稿版本、修改意见。 |

---

## 五、关键字段命名映射表

### 5.1 orders 表（订单主表）

| 中文含义 | MySQL 字段名 | Java 字段名 | TypeScript 字段名 | 字段类型 | 命名说明 |
|---|---|---|---|---|---|
| 主键 ID | `id` | `id` | `id` | BIGINT | 自增主键，统一命名为 id。 |
| 订单编号 | `order_no` | `orderNo` | `orderNo` | VARCHAR(20) | 业务唯一标识，格式 ORD-YYYY-NNNNN。 |
| 客户名称 | `customer_name` | `customerName` | `customerName` | VARCHAR(100) | **客户不建独立实体，仅保留此字段。** 禁止使用 client / company 别名。 |
| 联系人姓名 | `contact_name` | `contactName` | `contactName` | VARCHAR(20) | 客户对接人姓名。禁止使用 contact_person 别名。 |
| 联系电话 | `contact_phone` | `contactPhone` | `contactPhone` | VARCHAR(11) | 中国大陆手机号，列表页脱敏显示。禁止使用 mobile / tel 别名。 |
| 物料类型 | `material_type` | `materialType` | `materialType` | VARCHAR(20) | 对应 MaterialType 枚举存储值。禁止使用 category / type 模糊命名。 |
| 数量 | `quantity` | `quantity` | `quantity` | INT | 物料数量，正整数。禁止使用 amount / count 别名。 |
| 规格说明 | `specification` | `specification` | `specification` | VARCHAR(200) | 尺寸、材质、工艺等描述。禁止使用 spec / desc 缩写。 |
| 交付日期 | `delivery_date` | `deliveryDate` | `deliveryDate` | DATE | 约定的交付日期。禁止使用 deadline / due_date 别名。 |
| 备注说明 | `remark` | `remark` | `remark` | VARCHAR(500) | 补充说明。禁止使用 note / comment 别名。 |
| 订单状态 | `status` | `status` | `status` | VARCHAR(20) | 对应 OrderStatus 枚举存储值。禁止使用 state / order_status 别名。 |
| 设计负责人 ID | `designer_id` | `designerId` | `designerId` | BIGINT | 关联 users 表。禁止使用 designer_user_id 冗余命名。 |
| 创建人 ID | `created_by` | `createdBy` | `createdBy` | BIGINT | 关联 users 表，客户经理。禁止使用 creator_id 别名。 |
| 创建时间 | `created_at` | `createdAt` | `createdAt` | DATETIME | 订单提交时间。_at 后缀规范。 |
| 分派时间 | `assigned_at` | `assignedAt` | `assignedAt` | DATETIME | 业务负责人分派时间。_at 后缀规范。 |
| 分派人 ID | `assigned_by` | `assignedBy` | `assignedBy` | BIGINT | 关联 users 表，业务负责人。 |
| 定稿时间 | `approved_at` | `approvedAt` | `approvedAt` | DATETIME | 终稿确认时间。_at 后缀规范。 |
| 定稿人 ID | `approved_by` | `approvedBy` | `approvedBy` | BIGINT | 关联 users 表，业务负责人。 |
| 定稿版本 ID | `final_version_id` | `finalVersionId` | `finalVersionId` | BIGINT | 关联 design_versions 表。禁止使用 final_design_id 别名。 |
| 交付时间 | `delivered_at` | `deliveredAt` | `deliveredAt` | DATETIME | 标记交付时间。_at 后缀规范。 |
| 交付人 ID | `delivered_by` | `deliveredBy` | `deliveredBy` | BIGINT | 关联 users 表，业务负责人。 |
| 交付备注 | `deliver_remark` | `deliverRemark` | `deliverRemark` | VARCHAR(100) | 交付时的备注信息。 |
| 是否已归档 | `is_archived` | `archived` | `isArchived` | TINYINT(1) | 交付后自动设为 true。**DB 用 is_ 前缀，Java 不用 is 前缀，TS 用 is 前缀。** |
| 乐观锁版本号 | `version` | `version` | `version` | INT | 乐观锁控制并发冲突。 |
| 逻辑删除标识 | `is_deleted` | `deleted` | `isDeleted` | TINYINT(1) | 逻辑删除标记。**DB 用 is_ 前缀，Java 不用 is 前缀，TS 用 is 前缀。** |

### 5.2 design_versions 表（设计稿版本表）

| 中文含义 | MySQL 字段名 | Java 字段名 | TypeScript 字段名 | 字段类型 | 命名说明 |
|---|---|---|---|---|---|
| 主键 ID | `id` | `id` | `id` | BIGINT | 自增主键。 |
| 关联订单 ID | `order_id` | `orderId` | `orderId` | BIGINT | 外键关联 orders 表。 |
| 版本号 | `version_no` | `versionNo` | `versionNo` | VARCHAR(10) | V1、V2、V3... 字符串递增。禁止使用 version / version_code 别名。 |
| 设计文件 URL | `file_url` | `fileUrl` | `fileUrl` | VARCHAR(500) | OSS 文件访问地址。**数据库只存 URL，不存二进制。** 禁止使用 file_path / file_name 混淆。 |
| 文件名称 | `file_name` | `fileName` | `fileName` | VARCHAR(200) | 原始文件名。 |
| 文件大小（字节） | `file_size` | `fileSize` | `fileSize` | BIGINT | 单位：字节。禁止使用 size / file_length 别名。 |
| 文件类型 | `file_type` | `fileType` | `fileType` | VARCHAR(20) | 文件扩展名/ MIME 类型。 |
| 版本说明 | `version_note` | `versionNote` | `versionNote` | VARCHAR(200) | 本版修改说明。禁止使用 description / note 模糊命名。 |
| 上传人 ID | `uploaded_by` | `uploadedBy` | `uploadedBy` | BIGINT | 关联 users 表。 |
| 上传时间 | `uploaded_at` | `uploadedAt` | `uploadedAt` | DATETIME | 上传完成时间。_at 后缀规范。 |
| 是否最终版 | `is_final` | `finalVersion` | `isFinal` | TINYINT(1) | 是否为最终交付版本。**DB 用 is_ 前缀，Java 用 finalVersion（final 为关键字），TS 用 is 前缀。** |

### 5.3 order_feedbacks 表（订单修改意见表）

| 中文含义 | MySQL 字段名 | Java 字段名 | TypeScript 字段名 | 字段类型 | 命名说明 |
|---|---|---|---|---|---|
| 主键 ID | `id` | `id` | `id` | BIGINT | 自增主键。 |
| 关联订单 ID | `order_id` | `orderId` | `orderId` | BIGINT | 外键关联 orders 表。 |
| 关联版本 ID | `version_id` | `versionId` | `versionId` | BIGINT | 外键关联 design_versions 表，对应哪版设计稿的意见。 |
| 意见内容 | `content` | `content` | `content` | TEXT | 修改意见正文。禁止使用 feedback / comment / message 别名。 |
| 意见状态 | `status` | `status` | `status` | VARCHAR(20) | 对应 FeedbackStatus 枚举：PENDING / PROCESSED。禁止使用 feedback_status 冗余命名。 |
| 提交人 ID | `submitted_by` | `submittedBy` | `submittedBy` | BIGINT | 关联 users 表。 |
| 提交时间 | `submitted_at` | `submittedAt` | `submittedAt` | DATETIME | 意见提交时间。_at 后缀规范。 |
| 处理时间 | `processed_at` | `processedAt` | `processedAt` | DATETIME | 上传新版本后自动写入。_at 后缀规范。 |

### 5.4 users 表（用户表）

| 中文含义 | MySQL 字段名 | Java 字段名 | TypeScript 字段名 | 字段类型 | 命名说明 |
|---|---|---|---|---|---|
| 主键 ID | `id` | `id` | `id` | BIGINT | 自增主键。 |
| 登录账号 | `username` | `username` | `username` | VARCHAR(50) | 登录用账号，唯一。禁止使用 account / login 别名。 |
| 姓名 | `name` | `name` | `name` | VARCHAR(20) | 用户真实姓名。禁止使用 real_name / full_name 别名。 |
| 密码哈希 | `password_hash` | `passwordHash` | — | VARCHAR(100) | bcrypt 哈希值，前端 TS 接口不返回此字段。 |
| 角色 | `role` | `role` | `role` | VARCHAR(10) | 对应 UserRole 枚举：AM / BL / DS。禁止使用 role_type / user_role 冗余命名。 |
| 手机号 | `phone` | `phone` | `phone` | VARCHAR(11) | 用户联系电话。禁止使用 mobile / telephone 别名。 |
| 创建时间 | `created_at` | `createdAt` | `createdAt` | DATETIME | 用户创建时间。 |
| 最后登录时间 | `last_login_at` | `lastLoginAt` | `lastLoginAt` | DATETIME | 最近一次登录时间。 |
| 账号是否锁定 | `is_locked` | `locked` | `isLocked` | TINYINT(1) | 登录失败锁定用。**DB 用 is_ 前缀，Java 不用 is 前缀，TS 用 is 前缀。** |

### 5.5 reference_files 表（参考文件表）

| 中文含义 | MySQL 字段名 | Java 字段名 | TypeScript 字段名 | 字段类型 | 命名说明 |
|---|---|---|---|---|---|
| 主键 ID | `id` | `id` | `id` | BIGINT | 自增主键。 |
| 关联订单 ID | `order_id` | `orderId` | `orderId` | BIGINT | 外键关联 orders 表。 |
| 文件 URL | `file_url` | `fileUrl` | `fileUrl` | VARCHAR(500) | OSS 文件访问地址。 |
| 文件名称 | `file_name` | `fileName` | `fileName` | VARCHAR(200) | 原始文件名。 |
| 文件大小 | `file_size` | `fileSize` | `fileSize` | BIGINT | 单位：字节。 |
| 上传时间 | `uploaded_at` | `uploadedAt` | `uploadedAt` | DATETIME | 上传完成时间。 |

---

## 六、别名与冲突消解表

> 本系统所有命名以本表裁决结果为准，PRD、USER_STORIES 中出现的别名/口语化表达一律按本表映射到统一标识符。

| 序号 | 别名 / 口语表达 | 出现文档 | 统一标识符（裁决结果） | 裁决理由 |
|---|---|---|---|---|
| AL-01 | "待分派" | USER_STORIES、PRD 口语 | `CREATED`（OrderStatus） | "待分派"是业务视角描述，数据库状态码为 CREATED（已登记），两者指同一状态。 |
| AL-02 | "待审核" | USER_STORIES | `WAIT_APPROVE`（OrderStatus） | 统一为"待定稿"状态码 WAIT_APPROVE，禁止使用 PENDING_REVIEW / TO_BE_REVIEWED 等变体。 |
| AL-03 | "修改中" | USER_STORIES | `DESIGNING`（OrderStatus） | "修改中"不是独立状态码，归入 DESIGNING。只要设计人员在处理，就是 DESIGNING。 |
| AL-04 | "设计中"（分派后初始态） | USER_STORIES | `ASSIGNED`（OrderStatus） | 分派后设计人员尚未开始工作时，状态为 ASSIGNED，不是 DESIGNING。USER_STORIES 中口语化"设计中"需区分。 |
| AL-05 | "确认终稿并交付" | USER_STORIES US-007 | `ApproveFinal` + `MarkDelivered`（两步操作） | 拆解为两个独立操作，中间保留 APPROVED 状态，保证状态机完整性。 |
| AL-06 | "客户"（独立实体） | 日常口语 | `customer_name`（订单内联字段） | MVP 不建客户实体表，客户信息仅为订单的字段，禁止创建 Customer 实体。 |
| AL-07 | "设计师" / "设计员" / "美工" | 日常口语 | `DS` / `Designer`（角色与人员） | 统一角色编码为 DS，代码中实体用 User（role=DS），不建独立 Designer 实体。 |
| AL-08 | "订单列表" / "我的任务" | 不同角色视角 | `OrderList`（同一组件，数据过滤不同） | 前端统一组件名 OrderList，根据角色和过滤条件展示不同数据。 |
| AL-09 | "提交审核" / "送审" | 口语 | `UploadFirstDesign` / `UploadNewVersion` | 上传设计稿即进入待审核状态，提交审核不是独立操作。 |
| AL-10 | "项目" / "单子" / "工单" | 日常口语 | `Order` | 统一为 Order，禁止混用 Project / Ticket 等术语。 |
| AL-11 | "稿件" / "作品" / "设计文件" | 口语 | `DesignVersion` | 统一为设计稿版本实体 DesignVersion。 |
| AL-12 | "反馈" / "意见" / "评论" | 口语 | `OrderFeedback` | 统一为 OrderFeedback，禁止使用 Comment / Review 等别名。 |
| AL-13 | "定稿" / "确认" / "审核通过" | 口语 | `ApproveFinal` | 统一为确认终稿操作 ApproveFinal。 |
| AL-14 | "交片" / "交稿" / "出片" | 行业口语 | `MarkDelivered` | 统一为标记交付操作 MarkDelivered。 |
| AL-15 | "作废" / "取消" / "撤销" | 口语 | `CancelOrder` / `CANCELLED` | 统一为作废操作 CancelOrder，状态码 CANCELLED。 |

---

## 七、禁用词清单

> 以下命名在本系统中**严格禁止使用**，违反即视为代码质量问题。

| 序号 | 禁用词（禁止出现在代码/数据库/API中） | 禁用原因 | 替代词（统一命名） |
|---|---|---|---|
| FW-01 | `project`（项目） | MVP 核心概念是订单，不是项目，避免混淆工程项目管理 | `order` |
| FW-02 | `ticket`（工单） | 工单是 IT 服务管理术语，不适用于广告物料业务 | `order` |
| FW-03 | `client`（客户） | 易与 HTTP client 技术术语混淆 | `customer_name`（字段级），无客户实体 |
| FW-04 | `review`（审核/评审） | 含义模糊，既可能指审核通过也可能指审核驳回 | `approve`（通过）/ `reject`（驳回）/ `feedback`（意见） |
| FW-05 | `status_code` / `state` | 状态字段统一为 `status`，禁止加冗余后缀或同义词 | `status` |
| FW-06 | `creator` / `updater` / `deleter` | 操作人字段统一为 `{action}_by` 格式 | `created_by` / `updated_by` / `deleted_by` |
| FW-07 | `create_time` / `update_time` | 时间字段统一 `_at` 后缀，不用 `_time` | `created_at` / `updated_at` |
| FW-08 | `is_createtime` 等奇葩组合 | 时间字段不用 is_ 前缀，布尔字段才用 is_ | 按类型区分 |
| FW-09 | `order_info` / `user_info` | 表名/实体名不加 info 后缀，冗余 | `orders` / `users` / `Order` / `User` |
| FW-10 | `designer_user_id` | 外键命名 `{entity}_id` 即可，不用加 `user` | `designer_id` |
| FW-11 | `deleted_flag` / `del_flag` | 逻辑删除字段统一 `is_deleted` | `is_deleted` |
| FW-12 | `mobile_no` / `tel` / `phone_num` | 电话字段统一 `contact_phone` / `phone` | `contact_phone`（订单联系人）/ `phone`（用户） |
| FW-13 | `qty` / `amt` / `desc` | 禁止使用缩写，必须使用完整英文单词 | `quantity` / `amount` / `description` |
| FW-14 | `id_` / `_id`（前后加下划线） | 主键直接用 `id`，外键 `{entity}_id` | `id` / `order_id` |
| FW-15 | `order_status`（表内冗余前缀） | 表内字段无需加表名前缀，`status` 即可 | `status` |
| FW-16 | `task`（任务） | 设计人员视角的"我的任务"本质是订单列表，不建 Task 实体 | `order`（通过角色筛选） |
| FW-17 | `material_order` / `advertising_order` | 表名无需加业务前缀，系统本身就是物料订单系统 | `orders` |
| FW-18 | `feedback_comment` / `comment_feedback` | 修改意见和评论是两个不同概念，本系统只有意见无评论 | `order_feedbacks` / `content` |
| FW-19 | `submit` 作为独立操作名 | submit 含义模糊，必须明确提交什么 | `createOrder` / `submitFeedback` / `uploadFirstDesign` |
| FW-20 | `finish` / `complete` / `done` | 完成含义模糊，必须明确是定稿还是交付 | `approveFinal`（定稿）/ `markDelivered`（交付） |

---

> **文档结束**  
> 本词汇表为系统唯一命名基线。任何新增命名必须先更新本文档后方可进入代码。  
> 命名争议以本文档为准，文档维护权归领域建模师 / 架构师所有。
