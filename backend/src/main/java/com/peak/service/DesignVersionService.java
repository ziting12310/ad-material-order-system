package com.peak.service;

import com.peak.dto.DesignVersionCreateRequest;
import com.peak.vo.DesignVersionResponseVO;

/**
 * 设计稿版本服务接口 — API.md §4.1 UploadFirstDesign / UploadNewVersion
 *
 * <p>职责：版本号自增 + 上传人校验 + 状态推进（订单状态 → WAIT_APPROVE）</p>
 * <p>版本号规则：当前订单最新版本号 + 1；格式 V1 / V2 / V3...</p>
 */
public interface DesignVersionService {

    /**
     * 上传设计稿（首版 / 新版本共用）
     * <p>
     * 伪代码流程：
     *  ① 查订单 → 不存在 → ORDER_NOT_FOUND
     *  ② 状态校验（DESIGNING 才能上传）→ 冲突 → ORDER_STATUS_CONFLICT
     *  ③ 乐观锁校验（dto.version vs order.version）→ 冲突 → ORDER_VERSION_CONFLICT
     *  ④ 计算新版本号（查 design_versions 取当前 orderId 最大 versionNo + 1）
     *  ⑤ 插入 DesignVersion + 更新 Order 状态为 WAIT_APPROVE（同事务）
     *  ⑥ 组装 DesignVersionResponseVO
     * </p>
     */
    DesignVersionResponseVO uploadDesignVersion(DesignVersionCreateRequest dto, Long uploaderId);
}
