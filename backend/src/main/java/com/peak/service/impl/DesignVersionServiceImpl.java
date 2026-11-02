package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.OrderStatus;
import com.peak.dto.DesignVersionCreateRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderMapper;
import com.peak.service.DesignVersionService;
import com.peak.vo.DesignVersionResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设计稿版本服务实现 — API.md §4.1 UploadFirstDesign / UploadNewVersion
 *
 * <p>伪代码流程（每次上传均执行）：
 *  ① 查订单 → 不存在 → ORDER_NOT_FOUND 404
 *  ② 状态校验（DESIGNING 才能上传）→ 冲突 → ORDER_STATUS_CONFLICT 422
 *  ③ 乐观锁校验（dto.version vs order.version）→ 冲突 → ORDER_VERSION_CONFLICT 409
 *  ④ 计算新版本号（查 design_versions 取当前 orderId 最大 versionNo → 数字部分 + 1 → V+数字）
 *  ⑤ 插入 DesignVersion + 更新 Order 状态为 WAIT_APPROVE（同事务）
 *  ⑥ 组装 DesignVersionResponseVO（含订单更新后状态 + 版本号）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignVersionServiceImpl implements DesignVersionService {

    private final OrderMapper orderMapper;
    private final DesignVersionMapper designVersionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignVersionResponseVO uploadDesignVersion(DesignVersionCreateRequest dto, Long uploaderId) {
        // ① 查订单
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // ② 状态校验：DESIGNING 才能上传首版 / 新版本
        if (!OrderStatus.DESIGNING.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，上传设计稿需要状态为 [设计中]",
                    OrderStatus.of(order.getStatus()) != null
                            ? OrderStatus.of(order.getStatus()).getLabel() : order.getStatus());
        }

        // ③ 乐观锁校验
        if (!order.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), dto.getVersion());
        }

        // ④ 计算新版本号（查当前订单最新版本号 → 数字部分 + 1）
        String newVersionNo = generateNextVersionNo(order.getId());

        // ⑤ 插入 DesignVersion
        // 注：fileId 存 OSS 对象 Key（presign 返回），fileUrl 存完整访问 URL
        // MVP 阶段前端直传后回填完整 URL；后续可对接 PresignResponseVO.accessUrl
        DesignVersion version = new DesignVersion();
        version.setOrderId(order.getId());
        version.setVersionNo(newVersionNo);
        version.setFileId(dto.getFileId());              // OSS 对象 Key（业务定位用）
        version.setFileUrl(dto.getFileId());             // 占位：fileId = fileUrl，后续前端回填完整 URL
        version.setFileName(StringEscapeUtils.escapeHtml4(dto.getFileName()));
        version.setFileSize(dto.getFileSize());
        version.setFileType(dto.getFileType());
        version.setVersionNote(StringEscapeUtils.escapeHtml4(dto.getVersionNote()));
        version.setFinalVersion(0);                   // 新上传都不是 final
        version.setUploadedBy(uploaderId);
        version.setUploadedAt(LocalDateTime.now());
        designVersionMapper.insert(version);

        // ⑥ 更新订单状态为 WAIT_APPROVE（触发 BL 定稿流程）
        order.setStatus(OrderStatus.WAIT_APPROVE.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑦ 组装响应
        DesignVersionResponseVO resp = new DesignVersionResponseVO();
        resp.setId(version.getId());
        resp.setVersionNo(version.getVersionNo());
        resp.setFileUrl(version.getFileUrl());
        resp.setFileName(version.getFileName());
        resp.setFileSize(version.getFileSize());
        resp.setVersionNote(version.getVersionNote());
        resp.setIsFinal(false);
        resp.setUploadedAt(version.getUploadedAt());
        resp.setOrderStatus(order.getStatus());
        resp.setOrderVersion(order.getVersion());
        return resp;
    }

    /**
     * 版本号自增：查当前订单最新 versionNo（格式 V1 / V2）→ 提取数字部分 → +1
     * 无历史版本 → 返回 V1
     */
    private String generateNextVersionNo(Long orderId) {
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, orderId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            return "V1";
        }
        // 提取数字部分（V12 → 12）
        Pattern p = Pattern.compile("V(\\d+)");
        Matcher m = p.matcher(latest.getVersionNo());
        int next = 1;
        if (m.find()) {
            next = Integer.parseInt(m.group(1)) + 1;
        }
        return "V" + next;
    }
}
