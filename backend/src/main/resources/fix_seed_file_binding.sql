-- ============================================================================
-- 修复 seed 数据 file_id / file_url 与 uploads/ 物理文件不匹配
--
-- 问题根因：
--   原 seed 用的是无扩展名的伪 file_id（如 'file_demo_poster_v1'、
--   'file_ord5_banner_v1'、'ref_brief_0001' 等），但 uploads/ 目录
--   里只有 UUID+扩展名 风格的真实上传文件，导致：
--     1) FileServiceImpl.load() 返回 "文件不存在或尚未上传"（400）
--     2) 或文件未绑定任何业务记录 → DATA_SCOPE_DENIED（403）
--     3) JwtInterceptor 的匿名预览放行也救不了，因为根本找不到绑定
--     4) 前端 <img src> 加载失败 → 预览弹窗只有黑条 + 文件名
--
-- 修复方案（本次实际执行）：
--   把 design_versions 和 order_reference_files 的所有记录
--   统一指向 uploads/ 中真实存在的文件：
--     file_id = '5a320f43b3664e1fa77893dc6bb3df1b.png'
--     file_url = '/api/v1/files/5a320f43b3664e1fa77893dc6bb3df1b.png'
--
-- 前提：
--   ./uploads/5a320f43b3664e1fa77893dc6bb3df1b.png 必须存在
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- design_versions：全部 15 条记录统一指向真实文件
UPDATE design_versions
SET
    file_id  = '5a320f43b3664e1fa77893dc6bb3df1b.png',
    file_url = CONCAT('/api/v1/files/', file_id);

-- order_reference_files：全部 8 条记录同理
UPDATE order_reference_files
SET
    file_id  = '5a320f43b3664e1fa77893dc6bb3df1b.png',
    file_url = CONCAT('/api/v1/files/', file_id);

SET FOREIGN_KEY_CHECKS = 1;

-- 验证
SELECT 'design_versions' AS tbl, COUNT(*) AS total_files
FROM design_versions WHERE file_id = '5a320f43b3664e1fa77893dc6bb3df1b.png'
UNION ALL
SELECT 'order_reference_files', COUNT(*)
FROM order_reference_files WHERE file_id = '5a320f43b3664e1fa77893dc6bb3df1b.png';

-- 修复后：
-- 1) GET /api/v1/files/5a320f43b3664e1fa77893dc6bb3df1b.png 匿名返回 200 + image/png
-- 2) 订单详情抽屉 → 设计版本 → 预览按钮 → <img src> 正常加载图片
