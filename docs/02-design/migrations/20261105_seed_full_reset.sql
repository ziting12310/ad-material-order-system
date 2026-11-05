-- ============================================================================
-- 20261105_seed_full_reset.sql
-- 方案一：全量重置（清空 8 张表 + 一次性导入全部种子数据）
-- 要点：DELETE 与 INSERT 在同一事务内，任一条失败整体回滚，不会出现半套数据
-- 前置：10 条物理外键已存在
-- ============================================================================
USE `amoms`;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

DELETE FROM order_operation_logs;
DELETE FROM order_feedbacks;
DELETE FROM order_reference_files;
DELETE FROM design_versions;
DELETE FROM order_tasks;
DELETE FROM orders;
DELETE FROM users;
DELETE FROM roles;

-- ============================================================================
-- 种子数据部分（原 20261105_seed_test_data.sql，已合并进本文件）
-- 项目：ad-material-order-system / MySQL InnoDB
-- 用途：物理外键约束下的配套种子数据（开发测试环境）
-- 前置：10 条物理外键已就绪（20261105_11_rebuild_tables.sql 或 20261105_12_add_foreign_keys.sql）
-- 插入顺序：roles → users → orders(final_version_id=NULL) → order_tasks
--           → design_versions → UPDATE 回填 final_version_id
--           → order_feedbacks / order_reference_files / order_operation_logs
-- 说明：订单状态仅使用 4 态 CREATED / ASSIGNED / REVIEWING / FINISHED
-- ============================================================================



-- ----------------------------------------------------------------------------
-- 1. roles（角色字典）
-- ----------------------------------------------------------------------------
INSERT INTO roles (id, role_code, role_name, description, created_at, updated_at) VALUES
  (1, 'GUEST', '访客',       '未登录用户，无任何操作权限',       '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (2, 'AM',    '客户经理',   '订单发起人，负责客户对接与登记',   '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (3, 'BL',    '业务负责人', '负责分派、审核与交付',             '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (4, 'DS',    '设计制作人员', '设计执行者，负责设计稿上传与迭代', '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- ----------------------------------------------------------------------------
-- 2. users（8 个账号：AM×2 / BL×2 / DS×4；密码统一 123456 的 bcrypt 哈希）
--    role 保持 VARCHAR 角色编码，不引入 role_id
-- ----------------------------------------------------------------------------
INSERT INTO users (id, username, password_hash, name, role, phone, locked, last_login_at, created_at, updated_at, is_deleted) VALUES
  (1, 'am_zhangsan', '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '张三', 'AM', '13800138001', 0, '2026-11-03 09:20:00', '2026-08-01 09:10:00', '2026-11-03 09:20:00', 0),
  (2, 'am_lisi',     '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '李四', 'AM', '13800138002', 0, '2026-11-04 14:05:00', '2026-08-01 09:15:00', '2026-11-04 14:05:00', 0),
  (3, 'bl_wangwu',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '王五', 'BL', '13800138003', 0, '2026-11-05 08:40:00', '2026-08-01 09:20:00', '2026-11-05 08:40:00', 0),
  (4, 'bl_zhaoliu',  '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '赵六', 'BL', '13800138004', 0, '2026-11-02 16:30:00', '2026-08-01 09:25:00', '2026-11-02 16:30:00', 0),
  (5, 'ds_liming',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '李明', 'DS', '13800138005', 0, '2026-10-30 10:00:00', '2026-08-01 09:30:00', '2026-10-30 10:00:00', 0),
  (6, 'ds_qianqi',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '钱七', 'DS', '13800138006', 0, '2026-10-28 18:10:00', '2026-08-01 09:35:00', '2026-10-28 18:10:00', 0),
  (7, 'ds_sunba',    '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '孙八', 'DS', '13800138007', 0, '2026-10-29 11:25:00', '2026-08-01 09:40:00', '2026-10-29 11:25:00', 0),
  (8, 'ds_zhoujiu',  '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '周九', 'DS', '13800138008', 1, NULL,                  '2026-08-01 09:45:00', '2026-09-01 09:45:00', 0);

-- ----------------------------------------------------------------------------
-- 3. orders（10 单，覆盖 4 态；final_version_id 先全部为 NULL，版本插完再回填）
--    CREATED: 1,2  ASSIGNED: 3,4  REVIEWING: 5,6,7,10  FINISHED: 8,9
-- ----------------------------------------------------------------------------
INSERT INTO orders
  (id, order_no, customer_name, contact_name, contact_phone, material_type, quantity,
   specification, delivery_date, remark, status, designer_id, created_by,
   assigned_by, assigned_at, approved_by, approved_at, final_version_id,
   delivered_by, delivered_at, deliver_remark, is_archived, version, created_at, updated_at, is_deleted)
VALUES
  (1, 'ORD-2026-00001', '星光传媒', '陈晨', '13800138100', 'POSTER', 500,
   'A1 海报，300g 铜版纸，四色印刷，覆哑膜', '2026-09-20', '客户要求蓝色主色调', 'CREATED',
   NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
   '2026-08-12 10:00:00', '2026-08-12 10:00:00', 0),

  (2, 'ORD-2026-00002', '蓝海地产', '周敏', '13900139100', 'FLYER', 2000,
   'A5 宣传单，157g 铜版纸，双面彩印', '2026-09-28', '楼盘开盘派发用', 'CREATED',
   NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
   '2026-08-20 09:30:00', '2026-08-20 09:30:00', 0),

  (3, 'ORD-2026-00003', '云栖咖啡', '林可', '13700137100', 'BUSINESS_CARD', 300,
   '90×54mm 名片，300g 白卡，双面印刷', '2026-09-25', '新店开业物料', 'ASSIGNED',
   5, 1, 3, '2026-09-02 11:20:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
   '2026-08-28 15:40:00', '2026-09-02 11:20:00', 0),

  (4, 'ORD-2026-00004', '未来教育', '王强', '13600136100', 'BROCHURE', 800,
   'A4 画册 16 页，200g 铜版纸，骑马钉', '2026-10-10', '秋季课程招生', 'ASSIGNED',
   6, 2, 4, '2026-09-08 10:05:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
   '2026-09-01 14:10:00', '2026-09-08 10:05:00', 0),

  (5, 'ORD-2026-00005', '恒信汽车', '赵磊', '13500135100', 'BANNER', 12,
   '3×1.5m 喷绘布，户外高精写真，穿绳打扣', '2026-10-15', '车展展位背景', 'REVIEWING',
   5, 1, 3, '2026-09-10 09:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1,
   '2026-09-05 16:20:00', '2026-09-18 17:30:00', 0),

  (6, 'ORD-2026-00006', '悦动健身', '孙悦', '13400134100', 'POSTER', 300,
   'A2 海报，200g 铜版纸，单面四色', '2026-10-20', '会员招募活动', 'REVIEWING',
   7, 1, 4, '2026-09-15 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1,
   '2026-09-09 11:00:00', '2026-09-22 15:00:00', 0),

  (7, 'ORD-2026-00007', '绿源食品', '吴芳', '13300133100', 'FLYER', 5000,
   'A4 宣传单，128g 铜版纸，双面彩印', '2026-10-25', '商超堆头派发', 'REVIEWING',
   6, 2, 3, '2026-09-20 13:10:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 2,
   '2026-09-14 09:15:00', '2026-09-28 16:40:00', 0),

  (8, 'ORD-2026-00008', '星辰酒店', '郑浩', '13200132100', 'BROCHURE', 600,
   'A4 画册 24 页，250g 哑粉纸，胶装', '2026-10-08', '酒店品牌宣传册', 'FINISHED',
   5, 1, 3, '2026-08-18 09:40:00', 3, '2026-10-05 10:20:00', NULL,
   4, '2026-10-08 14:00:00', '客户已签收，交付纸质成品', 1, 3,
   '2026-08-05 10:30:00', '2026-10-08 14:00:00', 0),

  (9, 'ORD-2026-00009', '汉唐科技', '刘洋', '13100131100', 'BUSINESS_CARD', 1000,
   '90×54mm 名片，350g 特种纸，烫金工艺', '2026-10-18', '年会嘉宾名片', 'FINISHED',
   6, 2, 4, '2026-08-25 15:00:00', 3, '2026-10-12 09:50:00', NULL,
   4, '2026-10-15 16:30:00', '已交付电子稿 + 印刷成品', 0, 2,
   '2026-08-15 11:20:00', '2026-10-15 16:30:00', 0),

  (10, 'ORD-2026-00010', '远方文旅', '何静', '13000130100', 'OTHER', 99999,
   '定制文创礼盒（含书签、明信片），按样品打样', '2026-11-30', '边界用例：超大数量 + 定制物料', 'REVIEWING',
   7, 1, 3, '2026-10-02 10:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1,
   '2026-09-25 17:00:00', '2026-10-02 10:00:00', 0);

-- ----------------------------------------------------------------------------
-- 4. order_tasks（15 条；订单 10 无任务，用于验证"0 任务订单"边界）
--    CREATED 订单任务未分派；ASSIGNED/REVIEWING/FINISHED 任务已分派
--    ✅ 修复：approved_by 全部由 0 改为 NULL
-- ----------------------------------------------------------------------------
INSERT INTO order_tasks
  (id, order_id, task_no, material_type, quantity, specification, usage_desc, deadline,
   remark, status, designer_id, assigned_by, assigned_at, assign_remark,
   approved_by, approved_at, final_version_id, approve_remark, sort_order,
   version, created_at, updated_at, is_deleted)
VALUES
  (1, 1, 'T-001', 'POSTER', 300, 'A1 海报，300g 铜版纸，覆哑膜', '门店橱窗主视觉', '2026-09-15',
   '优先出首版', 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0,
   '2026-08-12 10:00:00', '2026-08-12 10:00:00', 0),
  (2, 1, 'T-002', 'POSTER', 200, 'A2 海报，200g 铜版纸', '店内活动区海报', '2026-09-18',
   NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, 0,
   '2026-08-12 10:00:00', '2026-08-12 10:00:00', 0),
  (3, 2, 'T-001', 'FLYER', 2000, 'A5 宣传单，157g 铜版纸，双面彩印', '开盘现场派发', '2026-09-26',
   NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0,
   '2026-08-20 09:30:00', '2026-08-20 09:30:00', 0),
  (4, 3, 'T-001', 'BUSINESS_CARD', 150, '90×54mm 名片，300g 白卡', '门店店长名片', '2026-09-22',
   NULL, 'ASSIGNED', 5, 3, '2026-09-02 11:20:00', '注意LOGO留白', NULL,
   NULL, NULL, NULL, 1, 0, '2026-08-28 15:40:00', '2026-09-02 11:20:00', 0),
  (5, 3, 'T-002', 'BUSINESS_CARD', 150, '90×54mm 名片，300g 白卡', '咖啡师名片', '2026-09-22',
   NULL, 'ASSIGNED', 5, 3, '2026-09-02 11:20:00', '与店长名片同版式', NULL,
   NULL, NULL, NULL, 2, 0, '2026-08-28 15:40:00', '2026-09-02 11:20:00', 0),
  (6, 4, 'T-001', 'BROCHURE', 800, 'A4 画册 16 页，200g 铜版纸，骑马钉', '招生简章', '2026-10-05',
   NULL, 'ASSIGNED', 6, 4, '2026-09-08 10:05:00', '按课程分类排版', NULL,
   NULL, NULL, NULL, 1, 0, '2026-09-01 14:10:00', '2026-09-08 10:05:00', 0),
  (7, 5, 'T-001', 'BANNER', 6, '3×1.5m 喷绘布，户外高精写真', '主展台背景', '2026-10-12',
   NULL, 'REVIEWING', 5, 3, '2026-09-10 09:00:00', '主视觉优先', NULL,
   NULL, NULL, NULL, 1, 1, '2026-09-05 16:20:00', '2026-09-18 17:30:00', 0),
  (8, 5, 'T-002', 'BANNER', 6, '2×1m 喷绘布，户外高精写真', '车型展示区背景', '2026-10-12',
   NULL, 'REVIEWING', 5, 3, '2026-09-10 09:00:00', NULL, NULL,
   NULL, NULL, NULL, 2, 1, '2026-09-05 16:20:00', '2026-09-18 17:30:00', 0),
  (9, 6, 'T-001', 'POSTER', 300, 'A2 海报，200g 铜版纸，单面四色', '前台展示', '2026-10-16',
   NULL, 'REVIEWING', 7, 4, '2026-09-15 10:30:00', NULL, NULL,
   NULL, NULL, NULL, 1, 1, '2026-09-09 11:00:00', '2026-09-22 15:00:00', 0),
  (10, 7, 'T-001', 'FLYER', 2000, 'A4 宣传单，128g 铜版纸，双面彩印', '商超堆头', '2026-10-20',
   NULL, 'REVIEWING', 6, 3, '2026-09-20 13:10:00', NULL, NULL,
   NULL, NULL, NULL, 1, 2, '2026-09-14 09:15:00', '2026-09-28 16:40:00', 0),
  (11, 7, 'T-002', 'FLYER', 1500, 'A5 宣传单，157g 铜版纸', '社区地推', '2026-10-20',
   NULL, 'REVIEWING', 6, 3, '2026-09-20 13:10:00', NULL, NULL,
   NULL, NULL, NULL, 2, 2, '2026-09-14 09:15:00', '2026-09-28 16:40:00', 0),
  (12, 7, 'T-003', 'POSTER', 1500, 'A3 促销海报，200g 铜版纸', '店内促销区', '2026-10-22',
   NULL, 'REVIEWING', 6, 3, '2026-09-20 13:10:00', NULL, NULL,
   NULL, NULL, NULL, 3, 1, '2026-09-14 09:15:00', '2026-09-28 16:40:00', 0),
  (13, 8, 'T-001', 'BROCHURE', 300, 'A4 画册 24 页，250g 哑粉纸，胶装', '酒店品牌册', '2026-10-03',
   NULL, 'FINISHED', 5, 3, '2026-08-18 09:40:00', '含酒店实景图', 3,
   '2026-10-05 10:20:00', NULL, '一次通过', 1, 2, '2026-08-05 10:30:00', '2026-10-05 10:20:00', 0),
  (14, 8, 'T-002', 'BROCHURE', 300, 'A5 折页，250g 哑粉纸，双面覆膜', '客房服务指南', '2026-10-03',
   NULL, 'FINISHED', 5, 3, '2026-08-18 09:40:00', NULL, 3,
   '2026-10-05 10:20:00', NULL, '按VI规范调整', 2, 1, '2026-08-05 10:30:00', '2026-10-05 10:20:00', 0),
  (15, 9, 'T-001', 'BUSINESS_CARD', 1000, '90×54mm 名片，350g 特种纸，烫金', '年会嘉宾用', '2026-10-14',
   NULL, 'FINISHED', 6, 4, '2026-08-25 15:00:00', NULL, 3,
   '2026-10-12 09:50:00', NULL, '烫金位置已确认', 1, 2, '2026-08-15 11:20:00', '2026-10-12 09:50:00', 0);

-- ----------------------------------------------------------------------------
-- 5. design_versions（10 条；仅 REVIEWING / FINISHED 订单有版本）
--    注意：真实库唯一键为 uk_order_version(order_id, version_no)
--          因此同一订单内版本号按订单维度全局递增（V1/V2/V3）
-- ----------------------------------------------------------------------------
INSERT INTO design_versions
  (id, order_id, task_id, version_no, file_id, file_url, file_name, file_size,
   file_type, version_note, is_final, uploaded_by, uploaded_at, created_at, updated_at, is_deleted)
VALUES
  (1, 5, 7, 'V1', 'file_ord5_banner_v1',  '/api/v1/files/file_ord5_banner_v1',  'banner_main_v1.jpg',   5242880, 'jpg', '主展台背景首版',       0, 5, '2026-09-16 14:20:00', '2026-09-16 14:20:00', '2026-09-16 14:20:00', 0),
  (2, 5, 8, 'V2', 'file_ord5_banner_v2',  '/api/v1/files/file_ord5_banner_v2',  'banner_car_v2.psd',   15728640, 'psd', '车型展示区背景首版',   0, 5, '2026-09-18 16:00:00', '2026-09-18 16:00:00', '2026-09-18 16:00:00', 0),
  (3, 6, 9, 'V1', 'file_ord6_poster_v1',  '/api/v1/files/file_ord6_poster_v1',  'poster_gym_v1.png',   2097152, 'png', '会员招募海报首版',     0, 7, '2026-09-22 11:30:00', '2026-09-22 11:30:00', '2026-09-22 11:30:00', 0),
  (4, 7, 10, 'V1', 'file_ord7_flyer_v1',  '/api/v1/files/file_ord7_flyer_v1',  'flyer_super_v1.ai',   8388608, 'ai',  '商超堆头单页首版',     0, 6, '2026-09-24 10:10:00', '2026-09-24 10:10:00', '2026-09-24 10:10:00', 0),
  (5, 7, 11, 'V2', 'file_ord7_flyer_v2',  '/api/v1/files/file_ord7_flyer_v2',  'flyer_community_v1.pdf', 3145728, 'pdf', '社区地推单页首版',  0, 6, '2026-09-26 15:40:00', '2026-09-26 15:40:00', '2026-09-26 15:40:00', 0),
  (6, 7, 10, 'V3', 'file_ord7_flyer_v3',  '/api/v1/files/file_ord7_flyer_v3',  'flyer_super_v2.ai',   8912896, 'ai',  '按意见调整主色与字号', 0, 6, '2026-09-28 16:40:00', '2026-09-28 16:40:00', '2026-09-28 16:40:00', 0),
  (7, 8, 13, 'V1', 'file_ord8_brochure_v1','/api/v1/files/file_ord8_brochure_v1','brochure_brand_v1.psd', 20971520, 'psd', '酒店品牌册首版',     0, 5, '2026-08-20 11:00:00', '2026-08-20 11:00:00', '2026-08-20 11:00:00', 0),
  (8, 8, 14, 'V2', 'file_ord8_fold_v1',   '/api/v1/files/file_ord8_fold_v1',   'folder_service_v1.pdf', 4194304, 'pdf', '客房服务折页首版',   0, 5, '2026-08-22 14:30:00', '2026-08-22 14:30:00', '2026-08-22 14:30:00', 0),
  (9, 8, 13, 'V3', 'file_ord8_brochure_v2','/api/v1/files/file_ord8_brochure_v2','brochure_brand_v2.psd', 22020096, 'psd', '定稿：替换实景图并调整版式', 1, 5, '2026-08-30 17:20:00', '2026-08-30 17:20:00', '2026-08-30 17:20:00', 0),
  (10, 9, 15, 'V1', 'file_ord9_card_v1',  '/api/v1/files/file_ord9_card_v1',  'card_gold_v1.ai',      5242880, 'ai',  '烫金名片定稿',         1, 6, '2026-08-26 09:40:00', '2026-08-26 09:40:00', '2026-08-26 09:40:00', 0);

-- ----------------------------------------------------------------------------
-- 6. 回填 final_version_id（版本落库后再 UPDATE，规避循环依赖）
--    orders.final_version_id 无物理外键，仅逻辑关联 design_versions.id
-- ----------------------------------------------------------------------------
UPDATE orders SET final_version_id = 9 WHERE id = 8;   -- 星辰酒店定稿版本 V3
UPDATE orders SET final_version_id = 10 WHERE id = 9;  -- 汉唐科技定稿版本 V1

UPDATE order_tasks SET final_version_id = 9  WHERE id = 13;
UPDATE order_tasks SET final_version_id = 8  WHERE id = 14;
UPDATE order_tasks SET final_version_id = 10 WHERE id = 15;

-- ----------------------------------------------------------------------------
-- 7. order_feedbacks（6 条；PENDING / PROCESSED 各覆盖）
--    外键：order_id → orders、task_id → order_tasks、version_id → design_versions
-- ----------------------------------------------------------------------------
INSERT INTO order_feedbacks
  (id, order_id, task_id, version_id, content, status, submitted_by,
   submitted_at, processed_at, created_at, updated_at, is_deleted)
VALUES
  (1, 5, 7, 1, '主展台背景主色偏暗，请提亮到品牌蓝；车型名称字号放大一级。', 'PENDING',
   1, '2026-09-19 10:15:00', NULL, '2026-09-19 10:15:00', '2026-09-19 10:15:00', 0),
  (2, 5, 8, 2, '车型展示区留白过多，请压缩边距并补充车型卖点文案。', 'PROCESSED',
   2, '2026-09-22 09:30:00', '2026-09-26 16:00:00', '2026-09-22 09:30:00', '2026-09-26 16:00:00', 0),
  (3, 7, 10, 4, '主标题字体过于花哨，请改为品牌指定字体；优惠信息需加粗。', 'PROCESSED',
   1, '2026-09-25 15:20:00', '2026-09-30 11:40:00', '2026-09-25 15:20:00', '2026-09-30 11:40:00', 0),
  (4, 7, 10, 6, '调整后主色仍偏冷，请再暖一点，并补一个门店地址二维码。', 'PENDING',
   2, '2026-10-02 14:00:00', NULL, '2026-10-02 14:00:00', '2026-10-02 14:00:00', 0),
  (5, 8, 13, 7, '品牌册 P6 实景图分辨率不足，需更换高清原图。', 'PROCESSED',
   3, '2026-08-27 09:10:00', '2026-08-28 10:00:00', '2026-08-27 09:10:00', '2026-08-28 10:00:00', 0),
  (6, 6, 9, 3, '海报底部需要留出活动二维码区域，尺寸不小于 30×30mm。', 'PENDING',
   1, '2026-09-23 16:45:00', NULL, '2026-09-23 16:45:00', '2026-09-23 16:45:00', 0);

-- ----------------------------------------------------------------------------
-- 8. order_reference_files（8 条；task_id 可空，表示订单级参考文件）
-- ----------------------------------------------------------------------------
INSERT INTO order_reference_files
  (id, order_id, task_id, file_id, file_url, file_name, file_size,
   uploaded_by, uploaded_at, created_at, updated_at, is_deleted)
VALUES
  (1, 1, NULL, 'ref_brief_0001',  '/api/v1/files/ref_brief_0001',  '客户需求简报_星光传媒.pdf', 524288, 1, '2026-08-12 09:50:00', '2026-08-12 09:50:00', '2026-08-12 09:50:00', 0),
  (2, 1, 1,    'ref_layout_0001', '/api/v1/files/ref_layout_0001', '门店橱窗参考图.jpg',     1048576, 1, '2026-08-12 09:55:00', '2026-08-12 09:55:00', '2026-08-12 09:55:00', 0),
  (3, 3, 4,    'ref_logo_0003',   '/api/v1/files/ref_logo_0003',   '云栖咖啡LOGO.ai',         2097152, 2, '2026-08-28 15:30:00', '2026-08-28 15:30:00', '2026-08-28 15:30:00', 0),
  (4, 5, 7,    'ref_vi_0005',     '/api/v1/files/ref_vi_0005',     '恒信汽车品牌VI规范.pdf',  3145728, 1, '2026-09-05 16:10:00', '2026-09-05 16:10:00', '2026-09-05 16:10:00', 0),
  (5, 7, 10,   'ref_pkg_0007',    '/api/v1/files/ref_pkg_0007',    '包装竞品参考.png',         838860, 2, '2026-09-14 09:05:00', '2026-09-14 09:05:00', '2026-09-14 09:05:00', 0),
  (6, 7, 11,   'ref_copy_0007',   '/api/v1/files/ref_copy_0007',   '地推文案初稿.docx',        65536, 2, '2026-09-14 09:10:00', '2026-09-14 09:10:00', '2026-09-14 09:10:00', 0),
  (7, 8, 13,   'ref_photo_0008',  '/api/v1/files/ref_photo_0008',  '酒店实景照片.zip',         5242880, 1, '2026-08-05 10:20:00', '2026-08-05 10:20:00', '2026-08-05 10:20:00', 0),
  (8, 10, NULL, 'ref_route_0010', '/api/v1/files/ref_route_0010', '文旅路线图.pdf',           1572864, 2, '2026-09-25 16:50:00', '2026-09-25 16:50:00', '2026-09-25 16:50:00', 0);

-- ----------------------------------------------------------------------------
-- 9. order_operation_logs（30 条；仅 order_id 有物理外键，task_id 为逻辑关联）
-- ----------------------------------------------------------------------------
INSERT INTO order_operation_logs
  (id, order_id, task_id, operation_type, description, before_value, after_value,
   operator_id, operator_name, operator_role, operated_at, client_ip)
VALUES
  (1,  1, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00001', NULL, 'CREATED', 1, '张三', 'AM', '2026-08-12 10:00:00', '192.168.1.10'),
  (2,  1, 1,    'TASK_ADD',     '新增物料任务 T-001（A1 海报）', NULL, 'CREATED', 1, '张三', 'AM', '2026-08-12 10:02:00', '192.168.1.10'),
  (3,  1, 2,    'TASK_ADD',     '新增物料任务 T-002（A2 海报）', NULL, 'CREATED', 1, '张三', 'AM', '2026-08-12 10:03:00', '192.168.1.10'),

  (4,  2, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00002', NULL, 'CREATED', 2, '李四', 'AM', '2026-08-20 09:30:00', '192.168.1.11'),
  (5,  2, 3,    'TASK_ADD',     '新增物料任务 T-001（A5 宣传单）', NULL, 'CREATED', 2, '李四', 'AM', '2026-08-20 09:32:00', '192.168.1.11'),

  (6,  3, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00003', NULL, 'CREATED', 1, '张三', 'AM', '2026-08-28 15:40:00', '192.168.1.10'),
  (7,  3, 4,    'TASK_ASSIGN',  '分派 T-001 给设计师李明', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-09-02 11:20:00', '192.168.1.12'),
  (8,  3, 5,    'TASK_ASSIGN',  '分派 T-002 给设计师李明', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-09-02 11:20:00', '192.168.1.12'),

  (9,  4, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00004', NULL, 'CREATED', 2, '李四', 'AM', '2026-09-01 14:10:00', '192.168.1.11'),
  (10, 4, 6,    'TASK_ASSIGN',  '分派 T-001 给设计师钱七', 'CREATED', 'ASSIGNED', 4, '赵六', 'BL', '2026-09-08 10:05:00', '192.168.1.13'),

  (11, 5, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00005', NULL, 'CREATED', 1, '张三', 'AM', '2026-09-05 16:20:00', '192.168.1.10'),
  (12, 5, 7,    'TASK_ASSIGN',  '分派主展台背景任务给李明', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-09-10 09:00:00', '192.168.1.12'),
  (13, 5, 7,    'TASK_UPLOAD_VERSION', '上传设计稿 V1（主展台背景）', NULL, 'V1', 5, '李明', 'DS', '2026-09-16 14:20:00', '192.168.1.14'),
  (14, 5, 8,    'TASK_UPLOAD_VERSION', '上传设计稿 V2（车型展示区）', NULL, 'V2', 5, '李明', 'DS', '2026-09-18 16:00:00', '192.168.1.14'),

  (15, 6, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00006', NULL, 'CREATED', 1, '张三', 'AM', '2026-09-09 11:00:00', '192.168.1.10'),
  (16, 6, 9,    'TASK_ASSIGN',  '分派会员招募海报给孙八', 'CREATED', 'ASSIGNED', 4, '赵六', 'BL', '2026-09-15 10:30:00', '192.168.1.13'),
  (17, 6, 9,    'TASK_UPLOAD_VERSION', '上传设计稿 V1（会员招募海报）', NULL, 'V1', 7, '孙八', 'DS', '2026-09-22 11:30:00', '192.168.1.15'),

  (18, 7, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00007', NULL, 'CREATED', 2, '李四', 'AM', '2026-09-14 09:15:00', '192.168.1.11'),
  (19, 7, 10,   'TASK_ASSIGN',  '分派商超堆头单页给钱七', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-09-20 13:10:00', '192.168.1.12'),
  (20, 7, 10,   'TASK_FEEDBACK', '客户对 V1 提出修改意见（字体与优惠信息）', NULL, 'PENDING', 1, '张三', 'AM', '2026-09-25 15:20:00', '192.168.1.10'),
  (21, 7, 10,   'TASK_UPLOAD_VERSION', '上传设计稿 V3（按意见调整）', 'V1', 'V3', 6, '钱七', 'DS', '2026-09-28 16:40:00', '192.168.1.16'),

  (22, 8, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00008', NULL, 'CREATED', 1, '张三', 'AM', '2026-08-05 10:30:00', '192.168.1.10'),
  (23, 8, 13,   'TASK_ASSIGN',  '分派酒店品牌册给李明', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-08-18 09:40:00', '192.168.1.12'),
  (24, 8, 13,   'TASK_APPROVE', '确认品牌册终稿', 'REVIEWING', 'FINISHED', 3, '王五', 'BL', '2026-10-05 10:20:00', '192.168.1.12'),
  (25, 8, NULL, 'ORDER_DELIVER', '订单交付归档', 'FINISHED', 'FINISHED', 4, '赵六', 'BL', '2026-10-08 14:00:00', '192.168.1.13'),

  (26, 9, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00009', NULL, 'CREATED', 2, '李四', 'AM', '2026-08-15 11:20:00', '192.168.1.11'),
  (27, 9, 15,   'TASK_APPROVE', '确认烫金名片终稿', 'REVIEWING', 'FINISHED', 3, '王五', 'BL', '2026-10-12 09:50:00', '192.168.1.12'),
  (28, 9, NULL, 'ORDER_DELIVER', '订单交付（电子稿 + 成品）', 'FINISHED', 'FINISHED', 4, '赵六', 'BL', '2026-10-15 16:30:00', '192.168.1.13'),

  (29, 10, NULL, 'ORDER_CREATE', '创建订单 ORD-2026-00010（定制文创礼盒）', NULL, 'CREATED', 1, '张三', 'AM', '2026-09-25 17:00:00', '192.168.1.10'),
  (30, 10, NULL, 'TASK_ASSIGN', '订单级分派给设计师孙八', 'CREATED', 'ASSIGNED', 3, '王五', 'BL', '2026-10-02 10:00:00', '192.168.1.12');



-- ============================================================================
-- 导入后自检
-- SELECT 'roles', COUNT(*) FROM roles UNION ALL
-- SELECT 'users', COUNT(*) FROM users UNION ALL
-- SELECT 'orders', COUNT(*) FROM orders UNION ALL
-- SELECT 'order_tasks', COUNT(*) FROM order_tasks UNION ALL
-- SELECT 'design_versions', COUNT(*) FROM design_versions UNION ALL
-- SELECT 'order_feedbacks', COUNT(*) FROM order_feedbacks UNION ALL
-- SELECT 'order_reference_files', COUNT(*) FROM order_reference_files UNION ALL
-- SELECT 'order_operation_logs', COUNT(*) FROM order_operation_logs;
-- ============================================================================

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- 导入后核查：期望 4 / 8 / 10 / 15 / 10 / 6 / 8 / 30
SELECT 'roles' AS 表名, COUNT(*) AS 行数 FROM roles
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_tasks', COUNT(*) FROM order_tasks
UNION ALL SELECT 'design_versions', COUNT(*) FROM design_versions
UNION ALL SELECT 'order_feedbacks', COUNT(*) FROM order_feedbacks
UNION ALL SELECT 'order_reference_files', COUNT(*) FROM order_reference_files
UNION ALL SELECT 'order_operation_logs', COUNT(*) FROM order_operation_logs;
