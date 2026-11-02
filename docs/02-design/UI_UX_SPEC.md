# UI/UX 设计规范

> 文档版本：v1.0  
> 适用范围：广告制作公司宣传物料订单管理系统 MVP  
> 设计风格：浅色企业后台 / 简洁专业 / 高效操作  
> 主色调：#2563EB（品牌蓝）  
> 前置文档：PRD.md / USER_STORIES.md / GLOSSARY.md / PERMISSION_MATRIX.md  
> 组件库：Element Plus（Vue 3）

---

## 一、Design Token

> 以下 Token 可零改造转为 `variables.css`（CSS Custom Properties）。命名规则：`--{类别}-{层级}-{修饰}`。

### 1.1 颜色体系

#### 品牌色

| Token 名 | 色值 | 用途 |
|---|---|---|
| `--color-primary` | `#2563EB` | 主按钮、链接、选中态、品牌标识 |
| `--color-primary-hover` | `#1D4ED8` | 主按钮 hover 态 |
| `--color-primary-active` | `#1E40AF` | 主按钮按下态 |
| `--color-primary-light` | `#DBEAFE` | 主色浅背景（选中行、标签底色） |
| `--color-primary-lighter` | `#EFF6FF` | 主色极浅背景（hover 行） |

#### 中性色

| Token 名 | 色值 | 用途 |
|---|---|---|
| `--color-gray-50` | `#F9FAFB` | 页面背景、卡片底色 |
| `--color-gray-100` | `#F3F4F6` | 分隔线、禁用背景 |
| `--color-gray-200` | `#E5E7EB` | 边框、分割线 |
| `--color-gray-300` | `#D1D5DB` | 次级边框、输入框边框 |
| `--color-gray-500` | `#6B7280` | 次要文字、说明文字 |
| `--color-gray-700` | `#374151` | 正文文字 |
| `--color-gray-900` | `#111827` | 标题文字、重要信息 |

#### 功能色

| Token 名 | 色值 | 用途 |
|---|---|---|
| `--color-success` | `#10B981` | 成功状态、已交付徽章 |
| `--color-success-light` | `#ECFDF5` | 成功状态浅背景 |
| `--color-warning` | `#F59E0B` | 警告状态、设计中徽章 |
| `--color-warning-light` | `#FFFBEB` | 警告状态浅背景 |
| `--color-danger` | `#EF4444` | 错误状态、删除按钮 |
| `--color-danger-light` | `#FEF2F2` | 错误状态浅背景 |
| `--color-info` | `#3B82F6` | 信息状态、已分派徽章 |
| `--color-info-light` | `#EFF6FF` | 信息状态浅背景 |

#### 状态徽章专属色

| Token 名 | 色值 | 对应状态 |
|---|---|---|
| `--badge-created-bg` | `#F3F4F6` | CREATED（灰） |
| `--badge-created-text` | `#4B5563` | CREATED 文字 |
| `--badge-assigned-bg` | `#DBEAFE` | ASSIGNED（蓝） |
| `--badge-assigned-text` | `#1E40AF` | ASSIGNED 文字 |
| `--badge-designing-bg` | `#FFEDD5` | DESIGNING（橙） |
| `--badge-designing-text` | `#C2410C` | DESIGNING 文字 |
| `--badge-wait-approve-bg` | `#EDE9FE` | WAIT_APPROVE（紫） |
| `--badge-wait-approve-text` | `#5B21B6` | WAIT_APPROVE 文字 |
| `--badge-approved-bg` | `#CCFBF1` | APPROVED（青） |
| `--badge-approved-text` | `#0F766E` | APPROVED 文字 |
| `--badge-delivered-bg` | `#D1FAE5` | DELIVERED（绿） |
| `--badge-delivered-text` | `#065F46` | DELIVERED 文字 |
| `--badge-cancelled-bg` | `#374151` | CANCELLED（深灰） |
| `--badge-cancelled-text` | `#F9FAFB` | CANCELLED 文字 |

---

### 1.2 字号与行高

| Token 名 | 字号 / 行高 | 用途 |
|---|---|---|
| `--font-size-h1` | `24px / 32px` | 页面标题 |
| `--font-size-h2` | `20px / 28px` | 卡片标题、模块标题 |
| `--font-size-h3` | `18px / 26px` | 次级标题 |
| `--font-size-body` | `14px / 22px` | 正文、表单标签、表格内容（基准） |
| `--font-size-small` | `12px / 18px` | 辅助文字、说明、时间戳 |
| `--font-size-mini` | `11px / 16px` | 徽章文字、角标 |

**字体族：**
```css
--font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", Roboto, sans-serif;
```

---

### 1.3 间距体系

8px 栅格基准。

| Token 名 | 数值 | 用途 |
|---|---|---|
| `--space-1` | `4px` | 微间距（图标与文字、内联元素） |
| `--space-2` | `8px` | 小组件间距 |
| `--space-3` | `12px` | 表单元素间距、卡片内边距小 |
| `--space-4` | `16px` | 标准内边距、模块间距 |
| `--space-6` | `24px` | 卡片内边距、大模块间距 |
| `--space-8` | `32px` | 页面左右边距、区块间距 |
| `--space-10` | `40px` | 页面顶部间距 |
| `--space-section` | `48px` | 大章节间距 |

---

### 1.4 圆角体系

基准圆角 8px。

| Token 名 | 数值 | 用途 |
|---|---|---|
| `--radius-sm` | `4px` | 徽章、标签、小按钮 |
| `--radius-md` | `8px` | **基准圆角**，按钮、输入框、卡片、弹窗 |
| `--radius-lg` | `12px` | 大卡片、页面容器 |
| `--radius-full` | `9999px` | 圆形头像、胶囊按钮 |

---

### 1.5 阴影体系

| Token 名 | 值 | 用途 |
|---|---|---|
| `--shadow-sm` | `0 1px 2px 0 rgba(0, 0, 0, 0.05)` | 输入框聚焦、悬浮卡片微阴影 |
| `--shadow-md` | `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)` | 弹窗、下拉菜单、卡片悬浮 |
| `--shadow-lg` | `0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)` | 全局遮罩弹窗、抽屉 |
| `--shadow-focus` | `0 0 0 3px rgba(37, 99, 235, 0.2)` | 输入框聚焦外环（与主色呼应） |

---

### 1.6 动效曲线

| Token 名 | 值 | 用途 |
|---|---|---|
| `--transition-fast` | `0.15s ease-out` | 按钮 hover、颜色变化、小动效 |
| `--transition-normal` | `0.25s ease-in-out` | 页面切换、弹窗出现、抽屉滑入 |
| `--transition-slow` | `0.4s cubic-bezier(0.4, 0, 0.2, 1)` | 大组件展开、数据加载过渡 |

---

### 1.7 完整 CSS 变量输出

```css
/* ============================================================
   AMOMS Design Tokens
   ============================================================ */
:root {
  /* 品牌色 */
  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-primary-active: #1E40AF;
  --color-primary-light: #DBEAFE;
  --color-primary-lighter: #EFF6FF;

  /* 中性色 */
  --color-gray-50: #F9FAFB;
  --color-gray-100: #F3F4F6;
  --color-gray-200: #E5E7EB;
  --color-gray-300: #D1D5DB;
  --color-gray-500: #6B7280;
  --color-gray-700: #374151;
  --color-gray-900: #111827;

  /* 功能色 */
  --color-success: #10B981;
  --color-success-light: #ECFDF5;
  --color-warning: #F59E0B;
  --color-warning-light: #FFFBEB;
  --color-danger: #EF4444;
  --color-danger-light: #FEF2F2;
  --color-info: #3B82F6;
  --color-info-light: #EFF6FF;

  /* 状态徽章色 */
  --badge-created-bg: #F3F4F6;
  --badge-created-text: #4B5563;
  --badge-assigned-bg: #DBEAFE;
  --badge-assigned-text: #1E40AF;
  --badge-designing-bg: #FFEDD5;
  --badge-designing-text: #C2410C;
  --badge-wait-approve-bg: #EDE9FE;
  --badge-wait-approve-text: #5B21B6;
  --badge-approved-bg: #CCFBF1;
  --badge-approved-text: #0F766E;
  --badge-delivered-bg: #D1FAE5;
  --badge-delivered-text: #065F46;
  --badge-cancelled-bg: #374151;
  --badge-cancelled-text: #F9FAFB;

  /* 字号 */
  --font-size-h1: 24px;
  --font-size-h2: 20px;
  --font-size-h3: 18px;
  --font-size-body: 14px;
  --font-size-small: 12px;
  --font-size-mini: 11px;

  /* 行高 */
  --line-height-h1: 32px;
  --line-height-h2: 28px;
  --line-height-h3: 26px;
  --line-height-body: 22px;
  --line-height-small: 18px;
  --line-height-mini: 16px;

  /* 字体族 */
  --font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", Roboto, sans-serif;

  /* 间距 */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-section: 48px;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  /* 阴影 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
  --shadow-focus: 0 0 0 3px rgba(37, 99, 235, 0.2);

  /* 动效 */
  --transition-fast: 0.15s ease-out;
  --transition-normal: 0.25s ease-in-out;
  --transition-slow: 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
```

---

## 二、页面原型说明

### 2.1 全局布局规范

```
┌───────────────────────────────────────────────────────────────┐
│  Logo  AMOMS          搜索(可选)    通知·消息   用户头像▼     │  ← Header (64px)
├──────────┬────────────────────────────────────────────────────┤
│          │  面包屑 / 页面标题 + 操作区                         │  ← PageHeader
│  侧边栏   ├────────────────────────────────────────────────────┤
│  (220px) │                                                    │
│  · 订单   │              主内容区                              │
│  · 我的   │              (padding: 24px)                      │
│    任务   │                                                    │
│  · 全部   │                                                    │
│    订单   │                                                    │
│          │                                                    │
└──────────┴────────────────────────────────────────────────────┘
```

**布局规则：**
- Header 高度 64px，白色背景 + 底部 1px 边框 `--color-gray-200`
- 侧边栏宽度 220px，浅灰背景 `--color-gray-50`
- 主内容区 padding: 24px
- 内容最大宽度：自适应（最小 1024px 可用）
- 面包屑字号 `--font-size-small`，颜色 `--color-gray-500`

---

### 2.2 页面 1：Login（登录页）

**页面路径：** `/login`  
**访问角色：** 未登录用户（Guest）

#### 布局线框

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                                                             │
│              ┌─────────────────────────────┐                │
│              │   [品牌Logo] AMOMS          │                │
│              │   广告物料订单管理系统        │                │
│              │                             │                │
│              │   ┌─────────────────────┐   │                │
│              │   │  账号               │   │                │
│              │   └─────────────────────┘   │                │
│              │   ┌─────────────────────┐   │                │
│              │   │  密码               │   │                │
│              │   └─────────────────────┘   │                │
│              │                             │                │
│              │   [       登 录       ]    │                │
│              │                             │                │
│              │   测试账号提示（小字）       │                │
│              └─────────────────────────────┘                │
│                                                             │
│                    © 2026 AMOMS v1.0                        │
└─────────────────────────────────────────────────────────────┘
```

#### 字段清单

| 字段名 | 组件类型 | 占位符 | 校验规则 | GLOSSARY 对应 |
|---|---|---|---|---|
| username | 输入框 | 请输入账号 | 必填 | username |
| password | 密码输入框 | 请输入密码 | 必填 | password |

#### 操作按钮

| 按钮 | 类型 | 动作 |
|---|---|---|
| 登录 | Primary Button（全宽） | 提交登录表单 |

#### 交互说明

1. 页面加载时自动聚焦账号输入框
2. 按 Enter 键可直接提交登录
3. 登录失败时，密码框下方显示错误提示（红色文字），输入框抖动反馈（`--transition-fast`）
4. 登录成功后，根据角色跳转：
   - AM / BL → `/orders`
   - DS → `/my-tasks`
5. 底部小字展示测试账号提示（灰色 `--color-gray-500`）
6. 账号锁定时弹窗提示"账号已被锁定，请 N 分钟后重试"

#### 关联 BDD 场景

| 场景 | 页面元素 |
|---|---|
| 登录成功 | 登录按钮 → 跳转首页 |
| 密码错误 | 错误提示 + 抖动 |
| 账号锁定 | 锁定提示弹窗 |

---

### 2.3 页面 2：OrderList（订单列表）

**页面路径：** `/orders`  
**访问角色：** AM / BL / DS  
**数据范围：** AM 看本人创建 / BL 看全部 / DS 看本人负责  
**Domain Action：** ListOrders

#### 布局线框

```
┌─ 面包屑：首页 / 订单列表 ──────────────────────────────────────┐
│                                                               │
│  订单列表                                  [+ 新建订单] [刷新] │  ← PageHeader + 操作区
│                                                               │
├───────────────────────────────────────────────────────────────┤
│  [状态筛选▼ 全部]  [搜索订单编号/客户名称__________]           │  ← 筛选区
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ 订单编号  │ 客户名称  │ 物料类型 │ 数量 │ 状态 │ 交付日期 │  │  ← 表格头
│  ├─────────────────────────────────────────────────────────┤  │
│  │ ORD-...   │ 星辰科技  │ 海报     │ 500  │ [徽章]│ 11-15  │  │
│  │ ...                                                  │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
│  共 128 条                    [上一页]  1 / 7  [下一页]       │  ← 分页（Cursor）
│                                                               │
```

#### 列表字段清单

| 列名 | 字段名 | 宽度 | 排序 | GLOSSARY 对应 |
|---|---|---|---|---|
| 订单编号 | orderNo | 160px | 否 | order_no |
| 客户名称 | customerName | 200px | 否 | customer_name |
| 物料类型 | materialType | 100px | 否 | material_type |
| 数量 | quantity | 80px | 否 | quantity |
| 状态 | status | 100px | 否（状态筛选） | status |
| 设计负责人 | designerName | 100px | 否 | designer_id → name |
| 交付日期 | deliveryDate | 120px | 是 | delivery_date |
| 创建时间 | createdAt | 160px | 否（Cursor 基准） | created_at |
| 操作 | actions | 160px | — | — |

#### 筛选区字段

| 筛选器 | 类型 | 选项/占位 | GLOSSARY 对应 |
|---|---|---|---|
| 状态筛选 | 下拉选择 | 全部 / CREATED / ASSIGNED / DESIGNING / WAIT_APPROVE / APPROVED / DELIVERED / CANCELLED | status |
| 关键词搜索 | 输入框 | 搜索订单编号/客户名称 | order_no + customer_name |

#### 操作区按钮

| 按钮 | 角色可见性 | 类型 | 动作 | Domain Action |
|---|---|---|---|---|
| + 新建订单 | 仅 AM | Primary | 跳转到 OrderCreate 页 | CreateOrder |
| 刷新 | 全部 | Default | 重新加载列表 | — |

#### 行操作按钮

| 按钮 | 角色可见性 | 类型 | 动作 |
|---|---|---|---|
| 查看详情 | 全部 | Text | 跳转到 OrderDetail 页 |
| 分派 | 仅 BL（CREATED 状态） | Primary（小） | 打开分派弹窗 | AssignOrder |
| 作废 | 仅 BL（非终态） | Danger Text | 打开作废确认弹窗 | CancelOrder |

#### 交互说明

1. 列表使用 Cursor 游标分页，"上一页/下一页"按钮，无页码输入
2. 状态列使用状态徽章（见第 3.1 节）
3. 交付日期逾期未交付的订单，日期标红 `--color-danger`
4. 行 hover 时背景 `--color-primary-lighter`
5. 搜索框支持回车搜索，输入时防抖 300ms
6. 列表加载时显示骨架屏（Skeleton）

#### 关联 BDD 场景

| 场景 | 页面元素 |
|---|---|
| US-002 场景 1 分派成功 | 分派按钮 → 状态变更 → 列表刷新 |
| US-002 场景 3 并发冲突 | 分派弹窗 → 409 错误 Toast |
| US-003 场景 1 查看详情 | 查看详情按钮 → 跳转 |
| US-003 场景 2 XSS 搜索 | 搜索框输入特殊字符 → 空结果 |
| US-003 场景 3 已交付移除 | 列表筛选 → 已交付订单不显示在进行中 |

---

### 2.4 页面 3：OrderCreate（新建订单）

**页面路径：** `/orders/create`  
**访问角色：** AM  
**Domain Action：** CreateOrder

#### 布局线框

```
┌─ 面包屑：首页 / 订单列表 / 新建订单 ──────────────────────────┐
│                                                               │
│  新建订单                              [取消] [提交订单]       │  ← PageHeader
│                                                               │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─ 基本信息 ───────────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  客户名称 *   [________________________]                  │ │
│  │  联系人姓名 * [________]  联系电话 * [___________]       │ │
│  │                                                          │ │
│  │  物料类型 *   [海报 ▼]     数量 *   [___]                 │ │
│  │                                                          │ │
│  │  规格说明 *   [________________________________]          │ │
│  │              （5~200字）                                 │ │
│  │                                                          │ │
│  │  交付日期 *   [2026-11-15 ▼]                              │ │
│  │                                                          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 参考资料 ───────────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  [上传参考文件] （最多5个，支持 jpg/png/webp/pdf）         │ │
│  │                                                          │ │
│  │  ┌────┐ ┌────┐ ┌────┐                                   │ │
│  │  │图1 │ │图2 │ │ +  │                                   │ │
│  │  └────┘ └────┘ └────┘                                   │ │
│  │                                                          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 备注说明 ───────────────────────────────────────────────┐ │
│  │  [_____________________________________________________] │ │
│  │  （选填，最多500字）                                       │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│                                   [取消]  [提交订单]          │  ← 底部操作栏
│                                                               │
```

#### 表单字段清单

| 字段名 | 组件类型 | 必填 | 校验规则 | 错误提示 | GLOSSARY 对应 |
|---|---|---|---|---|---|
| customerName | 输入框 | 是 | 2~100字符，不可全空格 | 请输入客户名称 | customer_name |
| contactName | 输入框 | 是 | 2~20字符，仅中文/英文 | 请输入联系人姓名 | contact_name |
| contactPhone | 输入框 | 是 | 11位手机号格式 | 请输入正确的手机号码 | contact_phone |
| materialType | 下拉选择 | 是 | 枚举值 | 请选择物料类型 | material_type |
| quantity | 数字输入框 | 是 | 1~99999 正整数 | 数量必须大于 0 | quantity |
| specification | 输入框 | 是 | 5~200字符 | 请填写规格说明（5~200字） | specification |
| deliveryDate | 日期选择器 | 是 | 晚于今天，早于今天+365天 | 交付日期不能早于今天 | delivery_date |
| referenceFileIds | 文件上传 | 否 | 最多5个，格式白名单，单文件≤5MB | 文件格式不支持 / 文件过大 | reference_files |
| remark | 文本域 | 否 | ≤500字符 | — | remark |

#### 操作按钮

| 按钮 | 类型 | 动作 |
|---|---|---|
| 取消 | Default | 返回上一页，未保存内容二次确认 |
| 提交订单 | Primary | 提交表单，校验通过后创建订单 |

#### 交互说明

1. 实时校验：失焦时触发，错误提示显示在输入框下方（红色文字 `--font-size-small`）
2. 提交时按钮立即置为 loading 状态 + 禁用，防止重复提交（US-001 场景 3）
3. 文件上传使用拖拽区 + 点击上传，支持多文件，格式错误的文件显示红色叉号
4. 图片上传后自动压缩转 WebP（见 FILE_CONTRACT.md）
5. 提交成功后跳转到订单详情页，Toast 提示"订单提交成功"
6. 取消按钮点击时，如有未保存内容，弹出确认对话框

#### 关联 BDD 场景

| 场景 | 页面元素 |
|---|---|
| US-001 场景 1 正常提交 | 全部字段填写 → 提交成功 → 跳转详情 |
| US-001 场景 2 必填项为空 | 空值提交 → 红色错误提示 |
| US-001 场景 3 重复提交 | 连续点击提交 → 按钮禁用 + loading |

---

### 2.5 页面 4：OrderDetail（订单详情）

**页面路径：** `/orders/{id}`  
**访问角色：** AM / BL / DS  
**Domain Action：** GetOrder（主）+ 多个写操作

#### 布局线框

```
┌─ 面包屑：首页 / 订单列表 / 订单详情 ──────────────────────────┐
│                                                               │
│  ORD-2026-00001  [状态徽章]         [返回列表] [操作按钮组 ▼]   │  ← PageHeader
│                                                               │
├─── 左栏 (65%) ──────────────────────┬─── 右栏 (35%) ──────────┤
│                                      │                         │
│  ┌─ 客户与订单信息 ───────────────┐ │  ┌─ 订单进度 ──────────┐ │
│  │ 客户名称：星辰科技有限公司      │ │  │  ○→○→○→●→○→○→○     │ │
│  │ 联系人：王经理 / 138****8000   │ │  │  已登记→已分派→...  │ │
│  │ 物料类型：海报    数量：500    │ │  └─────────────────────┘ │
│  │ 规格：A2铜版纸双面彩印         │ │                         │
│  │ 交付日期：2026-11-15           │ │  ┌─ 参考资料 ──────────┐ │
│  │ 备注：客户要求风格偏科技感      │ │  │ [图1] [图2] [图3]   │ │
│  └────────────────────────────────┘ │  └─────────────────────┘ │
│                                      │                         │
│  ┌─ 设计稿版本 ───────────────────┐ │  ┌─ 修改意见 ──────────┐ │
│  │  [V2 ● 最新版]  2026-10-22     │ │  │                     │ │
│  │  ┌─────────────────────────┐  │ │  │ 张三（AM） 10-22    │ │
│  │  │                         │  │ │  │ 主标题字体改为粗体...│ │
│  │  │      设计稿预览图        │  │ │  │                     │ │
│  │  │                         │  │ │  ├─────────────────────┤ │
│  │  └─────────────────────────┘  │ │  │                     │ │
│  │  版本说明：已按意见调整...     │ │  │ [输入修改意见...]   │ │
│  │  [下载源文件]                 │ │  │        [提交意见]   │ │
│  └────────────────────────────────┘ │  └─────────────────────┘ │
│                                      │                         │
│  ┌─ 版本历史 ─────────────────────┐ │                         │
│  │  V2  2026-10-22  李明  [查看] │ │                         │
│  │  V1  2026-10-21  李明  [查看] │ │                         │
│  └────────────────────────────────┘ │                         │
│                                      │                         │
└──────────────────────────────────────┴─────────────────────────┘
```

#### 左栏模块与字段

**1. 客户与订单信息卡片**

| 字段 | GLOSSARY 对应 | 显示规则 |
|---|---|---|
| customerName | customer_name | — |
| contactName | contact_name | — |
| contactPhone | contact_phone | 详情页完整显示（列表页脱敏） |
| materialType | material_type | 显示中文名称 |
| quantity | quantity | — |
| specification | specification | — |
| deliveryDate | delivery_date | 逾期未交付标红 |
| remark | remark | 无则显示"—" |
| createdByName | created_by | 创建人姓名 |
| createdAt | created_at | 格式化显示 |
| designerName | designer_id | 未分派显示"待分派" |
| assignedAt | assigned_at | 未分派显示"—" |

**2. 设计稿版本卡片**

| 字段 | GLOSSARY 对应 | 显示规则 |
|---|---|---|
| versionNo | version_no | 最新版标注"● 最新版"徽章 |
| fileUrl | file_url | 预览图展示 + 下载按钮 |
| fileName | file_name | — |
| fileSize | file_size | 格式化显示（KB/MB） |
| versionNote | version_note | — |
| isFinal | is_final | 最终版显示金色"最终版"徽章 |
| uploadedByName | uploaded_by | — |
| uploadedAt | uploaded_at | — |

**3. 版本历史列表**

| 字段 | 说明 |
|---|---|
| versionNo | 版本号 |
| uploadedAt | 上传时间 |
| uploadedByName | 上传人 |
| 操作 | 查看按钮 → 切换左侧预览 |

#### 右栏模块与字段

**1. 订单进度条**
- 7 个节点水平排列：CREATED → ASSIGNED → DESIGNING → WAIT_APPROVE → APPROVED → DELIVERED
- 已完成节点：实心蓝色 `--color-primary`
- 当前节点：蓝色外圈 + 白色内圈 + 脉冲动画
- 未完成节点：灰色空心 `--color-gray-300`

**2. 参考资料卡片**
- 缩略图网格展示，点击放大预览
- 参考文件名 + 大小

**3. 修改意见卡片**

| 字段 | GLOSSARY 对应 | 显示规则 |
|---|---|---|
| submittedByName | submitted_by | 提交人姓名 + 角色标签 |
| submittedAt | submitted_at | 格式化显示 |
| content | content | 意见内容 |
| status | status | PENDING 显示橙色圆点，PROCESSED 显示绿色对勾 |

**意见输入区（仅特定角色+状态可见）：**
- 文本域 + 提交按钮
- 占位符："请输入修改意见..."
- 字数统计：x/500

#### 操作按钮矩阵（按状态 × 角色动态显示）

> 权限逻辑引用 PERMISSION_MATRIX.md，以下为视觉呈现规则。

| 状态 | AM（创建者） | BL（业务负责人） | DS（负责人） |
|---|---|---|---|
| **CREATED** | 编辑订单 / （意见区禁用） | 编辑订单 / 分派 / 作废 | （不可见订单） |
| **ASSIGNED** | （仅查看） | 作废 | 接收任务 / （意见区禁用） |
| **DESIGNING** | （仅查看） | 作废 | 上传设计稿 / （意见区禁用） |
| **WAIT_APPROVE** | 提交修改意见 | 驳回 / 确认终稿 / 作废 | （按钮禁用 + tooltip） |
| **APPROVED** | （仅查看，意见区禁用） | 标记交付 / 作废 | （仅查看，按钮禁用） |
| **DELIVERED** | （仅查看，意见区禁用，顶部归档横幅） | （仅查看，归档横幅） | （仅查看，归档横幅） |
| **CANCELLED** | （仅查看，顶部作废横幅） | （仅查看，作废横幅） | （不可见 / 仅查看） |

**按钮类型规则：**
- 主要操作（推进流程）：Primary 按钮 → AssignOrder / ApproveFinal / MarkDelivered / UploadFirstDesign
- 次要操作：Default 按钮 → 编辑、提交意见
- 危险操作：Danger Text 按钮 → 作废
- 禁用操作：Disabled 按钮 + tooltip 说明原因

#### 交互说明

1. 设计稿版本点击切换预览，左右箭头可快速切换版本
2. 修改意见提交后，列表顶部新增一条，订单状态徽章实时更新为 DESIGNING
3. 上传设计稿按钮点击后弹出上传对话框（拖拽 + 点击上传）
4. 确认终稿 / 标记交付 / 作废 等关键操作均有二次确认弹窗
5. 版本冲突（409）时弹出提示"订单已被他人修改，请刷新页面"，并提供刷新按钮
6. 已交付订单顶部显示绿色归档横幅；已作废订单顶部显示灰色作废横幅

#### 关联 BDD 场景

21 个场景中除登录与列表类外，全部在本页面触发：
- US-002 分派订单（BL + CREATED 状态）
- US-003 查看详情（全部角色）
- US-004 上传设计稿（DS + DESIGNING 状态）
- US-005 提交修改意见（AM + WAIT_APPROVE 状态）
- US-006 上传新版本（DS + DESIGNING + 待处理意见）
- US-007 确认终稿与交付（BL + WAIT_APPROVE / APPROVED）

---

### 2.6 页面 5：MyTasks（我的任务）

**页面路径：** `/my-tasks`  
**访问角色：** DS（设计制作人员）  
**Domain Action：** ListOrders（按 designer_id 过滤）

#### 布局线框

```
┌─ 面包屑：首页 / 我的任务 ─────────────────────────────────────┐
│                                                               │
│  我的任务                                                [刷新] │
│                                                               │
├───────────────────────────────────────────────────────────────┤
│  [状态筛选▼ 全部]  [搜索________________]                      │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  状态卡片视图（Card Layout）                              │  │
│  │                                                          │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ [徽章] 设计中 │  │ [徽章] 待审核│  │ [徽章] 已交付│   │  │
│  │  │              │  │              │  │              │   │  │
│  │  │ ORD-001      │  │ ORD-002      │  │ ORD-003      │   │  │
│  │  │ 星辰科技海报 │  │ 明月宣传册   │  │ 旭日名片     │   │  │
│  │  │              │  │              │  │              │   │  │
│  │  │ V1 待处理意见│  │ 等待审核中   │  │ 已归档       │   │  │
│  │  │              │  │              │  │              │   │  │
│  │  │ [查看详情]   │  │ [查看详情]   │  │ [查看详情]   │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  │                                                          │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
│                      [加载更多]                               │  ← 滚动加载更多（Cursor）
│                                                               │
```

#### 卡片字段清单

| 字段 | GLOSSARY 对应 | 位置 |
|---|---|---|
| status（徽章） | status | 卡片顶部 |
| orderNo | order_no | 卡片标题 |
| customerName + materialType | customer_name + material_type | 卡片副标题 |
| 待处理意见数 | —（计算字段） | 卡片中部状态提示 |
| 最新版本号 | version_no | 设计中状态显示 |
| 查看详情按钮 | — | 卡片底部 |

#### 筛选区

| 筛选器 | 类型 | 选项 |
|---|---|---|
| 状态筛选 | 标签式切换 | 全部 / 待处理 / 设计中 / 待审核 / 已交付 |
| 搜索 | 输入框 | 搜索订单编号/客户名称 |

#### 交互说明

1. 默认展示为卡片视图（设计师更关注任务视觉化），也可切换为列表视图
2. 卡片背景色随状态变化（设计中橙边、待审核紫边、已交付灰底）
3. 有未处理意见的卡片显示红色小圆点角标 + "N 条待处理意见"提示
4. 滚动到底部自动加载更多（Cursor 分页）
5. 卡片点击可进入订单详情页

#### 关联 BDD 场景

| 场景 | 页面元素 |
|---|---|
| US-003 场景 1 查看任务 | 任务卡片 → 跳转详情 |
| US-003 场景 3 已交付移除 | 已交付筛选标签下查看 |

---

### 2.7 页面 6：NotFound（404 页面）

**页面路径：** 任意未匹配路由  
**访问角色：** 全部已登录用户

#### 布局线框

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│                                                               │
│                    .----.                                      │
│                   /  o   \    404                             │
│                   \   ^__/   页面不存在                        │
│                    `-=='                                     │
│                                                               │
│               抱歉，您访问的页面不存在或已被移除                │
│                                                               │
│                  [返回首页]  [返回上一页]                      │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

#### 元素清单

| 元素 | 说明 |
|---|---|
| 404 插画 | 简约线稿风格，灰色 `--color-gray-300` |
| 标题 | "404 页面不存在"，字号 `--font-size-h1`，颜色 `--color-gray-700` |
| 说明文字 | "抱歉，您访问的页面不存在或已被移除"，`--color-gray-500` |
| 返回首页按钮 | Primary 按钮，跳转至 `/orders` 或 `/my-tasks`（按角色） |
| 返回上一页按钮 | Default 按钮，调用 `router.back()` |

#### 交互说明

1. 页面进入时有淡入动画（`--transition-slow`）
2. 按 ESC 键触发返回上一页
3. 插画可使用 CSS 绘制或 SVG，不使用图片资源

---

## 三、组件视觉规范

### 3.1 状态徽章（Status Badge）

**7 种状态对应 7 种配色：**

| 状态枚举 | 中文名称 | 背景色 | 文字色 | 视觉示意 |
|---|---|---|---|---|
| CREATED | 已登记（待分派） | `#F3F4F6` 灰 | `#4B5563` 深灰 | ▢ 灰色实心 |
| ASSIGNED | 已分派 | `#DBEAFE` 浅蓝 | `#1E40AF` 深蓝 | ▢ 蓝色实心 |
| DESIGNING | 设计中 | `#FFEDD5` 浅橙 | `#C2410C` 深橙 | ▢ 橙色实心 |
| WAIT_APPROVE | 待定稿 | `#EDE9FE` 浅紫 | `#5B21B6` 深紫 | ▢ 紫色实心 |
| APPROVED | 已定稿 | `#CCFBF1` 浅青 | `#0F766E` 深青 | ▢ 青色实心 |
| DELIVERED | 已交付 | `#D1FAE5` 浅绿 | `#065F46` 深绿 | ▢ 绿色实心 |
| CANCELLED | 已作废 | `#374151` 深灰 | `#F9FAFB` 白色 | ▢ 深灰实心（反白字） |

**规格：**
- 高度：22px
- 左右内边距：8px
- 圆角：`--radius-sm`（4px）
- 字号：`--font-size-mini`（11px）
- 字重：500

---

### 3.2 按钮三态

#### Primary Button（主按钮）

| 状态 | 背景色 | 文字色 | 边框 | 其他 |
|---|---|---|---|---|
| Default | `--color-primary` #2563EB | white | 无 | — |
| Hover | `--color-primary-hover` #1D4ED8 | white | 无 | 阴影 `--shadow-sm` |
| Active | `--color-primary-active` #1E40AF | white | 无 | — |
| Disabled | `--color-gray-200` #E5E7EB | `--color-gray-500` | 无 | 鼠标 `not-allowed` |
| Loading | `--color-primary`（带透明度） | white | 无 | 旋转加载图标 |

#### Default Button（次按钮）

| 状态 | 背景色 | 文字色 | 边框 |
|---|---|---|---|
| Default | white | `--color-gray-700` | 1px `--color-gray-300` |
| Hover | `--color-gray-50` | `--color-primary` | 1px `--color-primary` |
| Active | `--color-gray-100` | `--color-primary-active` | 1px `--color-primary-active` |
| Disabled | `--color-gray-50` | `--color-gray-300` | 1px `--color-gray-200` |

#### Danger Button（危险按钮）

| 状态 | 背景色 | 文字色 |
|---|---|---|
| Default | `--color-danger` #EF4444 | white |
| Hover | `#DC2626` | white |
| Active | `#B91C1C` | white |

**按钮尺寸：**
- Large：高度 40px，左右 padding 20px，字号 14px
- Default：高度 32px，左右 padding 16px，字号 14px（基准）
- Small：高度 24px，左右 padding 12px，字号 12px
- 圆角：`--radius-md`（8px）

---

### 3.3 空 / 加载 / 错误三态

#### 空状态（Empty）

```
    ┌─────────┐
    │  (图标)  │
    │         │
    │ 暂无数据 │
    │         │
    │  [操作按钮（可选）]
    └─────────┘
```

- 图标：线性风格，`--color-gray-300`
- 主文案："暂无数据"，字号 `--font-size-h3`，颜色 `--color-gray-700`
- 副文案：灰色说明文字，`--font-size-body`，`--color-gray-500`
- 操作按钮：可选（如"去创建"），Primary 小按钮
- 场景：空搜索结果、空任务列表、空参考文件

#### 加载状态（Loading）

- **列表加载：** 骨架屏（Skeleton），灰色渐变动画闪烁
- **按钮加载：** 按钮内 spinner + 文字变"加载中..."，按钮禁用
- **页面加载：** 顶部进度条（NProgress 风格，主色 `--color-primary`）
- 动画：`1.4s ease-in-out infinite` 渐变闪烁

#### 错误状态（Error）

```
    ┌─────────┐
    │  ⚠ 图标  │
    │         │
    │ 加载失败 │
    │ 错误描述 │
    │         │
    │  [重新加载]
    └─────────┘
```

- 图标：警告/错误图标，`--color-danger`
- 主文案："加载失败" / "操作失败"
- 副文案：错误描述，支持展开查看详情
- 操作按钮："重新加载"，Primary 按钮

---

### 3.4 Toast 消息提示

**4 类 Toast：**

| 类型 | 图标 | 颜色 | 背景色 | 场景 |
|---|---|---|---|---|
| Success | ✓ | `--color-success` | `--color-success-light` | 操作成功、提交成功、保存成功 |
| Warning | ⚠ | `--color-warning` | `--color-warning-light` | 注意事项、即将逾期 |
| Error | ✕ | `--color-danger` | `--color-danger-light` | 操作失败、网络错误、权限不足 |
| Info | ℹ | `--color-info` | `--color-info-light` | 普通提示、系统通知 |

**规格：**
- 位置：右上角（距顶部 16px，距右侧 16px）
- 宽度：最大 360px，最小 240px
- 圆角：`--radius-md`（8px）
- 阴影：`--shadow-md`
- 持续时间：Success/Info 3秒自动关闭；Warning/Error 不自动关闭（手动关闭）
- 动画：从右侧滑入（`translateX(100%) → 0`），`--transition-normal`

---

### 3.5 响应式断点

| 断点 | 名称 | 布局变化 | 适用设备 |
|---|---|---|---|
| **≥ 1200px** | Desktop（桌面端） | 完整侧边栏 + 多列表格 + 左右分栏 | 桌面电脑 |
| **768 ~ 1199px** | Tablet（平板端） | 侧边栏收起为图标模式；详情页上下布局替换左右布局 | 平板 / 小屏笔记本 |
| **< 768px** | Mobile（移动端） | 侧边栏变为底部 Tab 或抽屉菜单；表格变为卡片列表；单列布局 | 手机（MVP 仅保证可用，不做深度优化） |

**响应式规则：**
- MVP 以 Desktop（≥1200px）为主要设计目标，Tablet 级保证可用，Mobile 级仅做基础适配
- 表格在 < 768px 时自动转为卡片式列表（每行一个卡片，字段纵向排列）
- 侧边栏在 768~1199px 宽度变为 64px（图标模式），hover 展开
- 字体大小在移动端整体缩小一级（body 从 14px → 13px）

---

## 四、按钮操作权限视觉矩阵

> **逻辑依据：** PERMISSION_MATRIX.md，本处仅做视觉呈现规则，不重复定义权限规则。

### 矩阵总览

| 操作按钮 | DOMAIN ACTION | AM | BL | DS |
|---|---|:---:|:---:|:---:|
| 新建订单 | CreateOrder | ✅ Primary | ❌ — | ❌ — |
| 编辑订单 | UpdateOrder | ✅ Default（CREATED，本人） | ✅ Default（CREATED） | ❌ — |
| 分派订单 | AssignOrder | ❌ — | ✅ Primary（CREATED） | ❌ — |
| 接收任务 | AcceptTask | ❌ — | ❌ — | ✅ Primary（ASSIGNED） |
| 上传设计稿 | UploadFirstDesign | ❌ — | ❌ — | ✅ Primary（DESIGNING，首版） |
| 上传新版本 | UploadNewVersion | ❌ — | ❌ — | ✅ Primary（DESIGNING，有意见） |
| 提交修改意见 | SubmitFeedback | ✅ Default（WAIT_APPROVE，本人） | ✅ Default（WAIT_APPROVE） | ❌ — |
| 驳回设计 | RejectDesign | ❌ — | ✅ Warning（WAIT_APPROVE） | ❌ — |
| 确认终稿 | ApproveFinal | ❌ — | ✅ Primary（WAIT_APPROVE） | ❌ — |
| 标记交付 | MarkDelivered | ❌ — | ✅ Success（APPROVED） | ❌ — |
| 作废订单 | CancelOrder | ❌ — | ✅ Danger Text（非终态） | ❌ — |

**图例：** ✅ 显示（按钮类型）　　❌ 不显示　　🔍 仅查看

### 按钮显示优先级

同一页面的操作按钮按以下优先级排序（从右到左，最右侧为主要操作）：

```
[作废] [驳回] ... [提交意见] [确认终稿] [标记交付]
                                          ↑
                                     最主要操作在最右
```

- 主要推进操作（Primary / Success）：最右侧
- 次要操作（Default）：中间
- 危险操作（Danger Text）：最左侧

---

> **文档结束**  
> 所有命名严格遵循 GLOSSARY.md 统一领域语言词汇表。  
> 状态徽章色、按钮三态、空/加载/错误三态、Toast 类型、响应式断点均可直接落地 Element Plus 自定义主题。
