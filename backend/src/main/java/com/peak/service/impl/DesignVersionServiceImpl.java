package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.dto.DesignVersionCreateRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.DesignVersionService;
import com.peak.service.FileService;
import com.peak.vo.DesignVersionDiffVO;
import com.peak.vo.DesignVersionResponseVO;
import com.peak.vo.DesignVersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final UserMapper userMapper;
    private final FileService fileService;

    private static final Pattern VERSION_NO_PATTERN = Pattern.compile("V(\\d+)");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignVersionResponseVO uploadDesignVersion(DesignVersionCreateRequest dto, Long uploaderId) {
        // ① 查订单
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // ② 归属校验：只有被分派的设计制作人员（DS）才能上传
        User uploader = userMapper.selectById(uploaderId);
        if (uploader == null || !UserRole.DS.getCode().equals(uploader.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "仅设计制作人员（DS）可以上传设计稿");
        }
        if (order.getDesignerId() == null || !order.getDesignerId().equals(uploaderId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "只能上传分派给自己的订单设计稿");
        }

        // ③ 状态校验：DESIGNING 才能上传首版 / 新版本
        if (!OrderStatus.DESIGNING.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，上传设计稿需要状态为 [设计中]",
                    OrderStatus.of(order.getStatus()) != null
                            ? OrderStatus.of(order.getStatus()).getLabel() : order.getStatus());
        }

        // ④ 乐观锁校验
        if (!order.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), dto.getVersion());
        }

        // ⑤ 插入 DesignVersion（版本号取数字最大值 +1，唯一键冲突时重试）
        Long taskId = dto.getTaskId() != null ? dto.getTaskId() : order.getId();
        DesignVersion version = new DesignVersion();
        version.setOrderId(order.getId());
        version.setTaskId(taskId);
        version.setFileId(dto.getFileId());
        version.setFileUrl(fileService.resolveAccessUrl(dto.getFileId()));
        version.setFileName(StringEscapeUtils.escapeHtml4(dto.getFileName()));
        version.setFileSize(dto.getFileSize());
        version.setFileType(dto.getFileType());
        version.setVersionNote(StringEscapeUtils.escapeHtml4(dto.getVersionNote()));
        version.setFinalVersion(0);                   // 新上传都不是 final
        version.setUploadedBy(uploaderId);
        version.setUploadedAt(LocalDateTime.now());

        int attempts = 0;
        while (true) {
            version.setVersionNo(generateNextVersionNo(order.getId()));
            try {
                designVersionMapper.insert(version);
                break;
            } catch (DuplicateKeyException e) {
                if (++attempts >= 3) {
                    throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                            "版本号生成冲突，请刷新后重试");
                }
                version.setId(null);
            }
        }

        // ⑥ 更新订单状态为 WAIT_APPROVE（触发 BL 定稿流程）
        order.setStatus(OrderStatus.WAIT_APPROVE.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑦ 组装响应
        DesignVersionResponseVO resp = new DesignVersionResponseVO();
        resp.setId(version.getId());
        resp.setTaskId(version.getTaskId());
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

    @Override
    public List<DesignVersionVO> listVersions(Long orderId, Long taskId, Long userId, String userRole) {
        Long resolvedOrderId = orderId;
        if (resolvedOrderId == null && taskId != null) {
            DesignVersion sample = designVersionMapper.selectOne(
                    new LambdaQueryWrapper<DesignVersion>()
                            .eq(DesignVersion::getTaskId, taskId)
                            .orderByAsc(DesignVersion::getId)
                            .last("LIMIT 1"));
            if (sample == null) {
                return List.of();
            }
            resolvedOrderId = sample.getOrderId();
        }
        Order order = requireOrderForRead(resolvedOrderId, userId, userRole);

        LambdaQueryWrapper<DesignVersion> wrapper = new LambdaQueryWrapper<DesignVersion>()
                .eq(DesignVersion::getOrderId, order.getId());
        if (taskId != null) {
            wrapper.eq(DesignVersion::getTaskId, taskId);
        }
        List<DesignVersion> versions = designVersionMapper.selectList(wrapper);
        versions.sort(Comparator.comparingInt((DesignVersion v) -> parseVersionNo(v.getVersionNo())));

        Map<Long, String> userNames = loadUserNames(
                versions.stream().map(DesignVersion::getUploadedBy).collect(Collectors.toSet()));

        return versions.stream()
                .map(v -> toVersionVO(v, userNames))
                .collect(Collectors.toList());
    }

    @Override
    public DesignVersionDiffVO diffVersions(Long orderId, Long taskId, Integer v1, Integer v2,
                                            Long userId, String userRole) {
        if (v1 == null || v2 == null || v1 <= 0 || v2 <= 0) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "v1 / v2 必须为正整数");
        }

        LambdaQueryWrapper<DesignVersion> wrapper = new LambdaQueryWrapper<DesignVersion>()
                .in(DesignVersion::getVersionNo, "V" + v1, "V" + v2);
        if (orderId != null) {
            wrapper.eq(DesignVersion::getOrderId, orderId);
        }
        if (taskId != null) {
            wrapper.eq(DesignVersion::getTaskId, taskId);
        }
        if (orderId == null && taskId == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "orderId 或 taskId 至少传一个");
        }

        List<DesignVersion> versions = designVersionMapper.selectList(wrapper);
        if (versions.isEmpty()) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND,
                    "版本 V%d / V%d 不存在", v1, v2);
        }

        Long resolvedOrderId = orderId != null ? orderId : versions.get(0).getOrderId();
        Order order = requireOrderForRead(resolvedOrderId, userId, userRole);

        DesignVersion version1 = findByVersionNo(versions, "V" + v1);
        DesignVersion version2 = findByVersionNo(versions, "V" + v2);
        if (version1 == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND,
                    "版本 V%d 不存在", v1);
        }
        if (version2 == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND,
                    "版本 V%d 不存在", v2);
        }

        DesignVersionDiffVO result = new DesignVersionDiffVO();
        result.setOrderId(order.getId());
        result.setTaskId(taskId != null ? taskId : order.getId());
        result.setV1(toDiffItem(version1));
        result.setV2(toDiffItem(version2));
        return result;
    }

    /**
     * 版本号自增：查当前订单全部版本号 → 取数字最大值 +1
     * 无历史版本 → 返回 V1
     */
    private String generateNextVersionNo(Long orderId) {
        List<DesignVersion> versions = designVersionMapper.selectList(
                new LambdaQueryWrapper<DesignVersion>().eq(DesignVersion::getOrderId, orderId));
        int max = versions.stream().mapToInt(v -> parseVersionNo(v.getVersionNo())).max().orElse(0);
        return "V" + (max + 1);
    }

    private static int parseVersionNo(String versionNo) {
        if (versionNo == null) {
            return 0;
        }
        Matcher matcher = VERSION_NO_PATTERN.matcher(versionNo);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private Order requireOrderForRead(Long orderId, Long userId, String userRole) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "orderId 或 taskId 至少传一个");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        checkViewScope(order, userId, userRole);
        return order;
    }

    private void checkViewScope(Order order, Long userId, String userRole) {
        if (UserRole.BL.getCode().equals(userRole)) {
            return;
        }
        if (UserRole.AM.getCode().equals(userRole) && userId != null
                && userId.equals(order.getCreatedBy())) {
            return;
        }
        if (UserRole.DS.getCode().equals(userRole) && userId != null
                && userId.equals(order.getDesignerId())) {
            return;
        }
        throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看该订单的设计稿版本");
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private DesignVersionVO toVersionVO(DesignVersion version, Map<Long, String> userNames) {
        DesignVersionVO vo = new DesignVersionVO();
        vo.setId(version.getId());
        vo.setTaskId(version.getTaskId());
        vo.setVersionNo(version.getVersionNo());
        vo.setFileUrl(version.getFileUrl());
        vo.setFileName(version.getFileName());
        vo.setFileSize(version.getFileSize());
        vo.setVersionNote(version.getVersionNote());
        vo.setIsFinal(version.getFinalVersion() != null && version.getFinalVersion() == 1);
        vo.setUploadedByName(userNames.get(version.getUploadedBy()));
        vo.setUploadedAt(version.getUploadedAt());
        return vo;
    }

    private DesignVersion findByVersionNo(List<DesignVersion> versions, String versionNo) {
        return versions.stream()
                .filter(v -> versionNo.equals(v.getVersionNo()))
                .findFirst()
                .orElse(null);
    }

    private DesignVersionDiffVO.VersionItem toDiffItem(DesignVersion version) {
        DesignVersionDiffVO.VersionItem item = new DesignVersionDiffVO.VersionItem();
        item.setId(version.getId());
        item.setVersionNo(version.getVersionNo());
        item.setFileUrl(version.getFileUrl());
        item.setFileName(version.getFileName());
        item.setFileSize(version.getFileSize());
        item.setVersionNote(version.getVersionNote());
        item.setUploadedAt(version.getUploadedAt());
        return item;
    }
}
