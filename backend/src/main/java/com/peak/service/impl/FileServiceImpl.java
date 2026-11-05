package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.context.UserContext;
import com.peak.common.enums.UserRole;
import com.peak.dto.PresignRequest;
import com.peak.dto.ReferenceFileConfirmRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderReferenceFile;
import com.peak.entity.OrderTask;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderReferenceFileMapper;
import com.peak.mapper.OrderTaskMapper;
import com.peak.service.FileService;
import com.peak.vo.PresignResponseVO;
import com.peak.vo.ReferenceFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件系统文件服务实现（模拟 OSS）。
 *
 * <p>存储目录默认 ./uploads，可通过 amoms.file.storage-dir 覆盖；
 * 每个文件对应一个同名 .meta JSON，保存原始文件名 / 大小 / MIME，供回填参考文件使用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String TYPE_REFERENCE = "reference";
    private static final String TYPE_DESIGN = "design";

    private static final Set<String> DESIGN_EXTENSIONS = Set.of("psd", "ai", "jpg", "jpeg", "png", "pdf", "webp");
    private static final Set<String> REFERENCE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf", "webp");

    private static final long MAX_REFERENCE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_DESIGN_SIZE = 100L * 1024 * 1024;

    private final OrderReferenceFileMapper orderReferenceFileMapper;
    private final DesignVersionMapper designVersionMapper;
    private final OrderMapper orderMapper;
    private final OrderTaskMapper orderTaskMapper;
    private final ObjectMapper objectMapper;

    @Value("${amoms.file.storage-dir:./uploads}")
    private String storageDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public PresignResponseVO presign(PresignRequest dto) {
        String type = dto.getFileType() == null ? "" : dto.getFileType().toLowerCase(Locale.ROOT);
        if (!TYPE_REFERENCE.equals(type) && !TYPE_DESIGN.equals(type)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "fileType 仅支持 reference / design");
        }

        String fileName = StringUtils.getFilename(dto.getFileName());
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件名缺少扩展名");
        }
        extension = extension.toLowerCase(Locale.ROOT);

        Set<String> allowed = TYPE_DESIGN.equals(type) ? DESIGN_EXTENSIONS : REFERENCE_EXTENSIONS;
        if (!allowed.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "不支持的扩展名 [%s]，允许：%s", extension, allowed);
        }

        long maxSize = TYPE_DESIGN.equals(type) ? MAX_DESIGN_SIZE : MAX_REFERENCE_SIZE;
        if (dto.getFileSize() == null || dto.getFileSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "fileSize 必须大于 0");
        }
        if (dto.getFileSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "文件大小超过限制（最大 %dMB）", maxSize / 1024 / 1024);
        }

        String fileId = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        PresignResponseVO vo = new PresignResponseVO();
        vo.setFileId(fileId);
        vo.setUploadUrl(apiPrefix() + "/upload?fileId=" + fileId);
        vo.setAccessUrl(resolveAccessUrl(fileId));
        vo.setFormData(Map.of());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PresignResponseVO upload(String fileId, MultipartFile file,
                                    Long orderId, Long taskId, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_DESIGN_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "文件大小超过 100MB");
        }

        Path path = filePath(fileId);
        if (Files.exists(path)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "文件 [%s] 已上传，请重新 presign", fileId);
        }

        String originalName = StringUtils.getFilename(file.getOriginalFilename());
        if (originalName != null) {
            String originalExtension = StringUtils.getFilenameExtension(originalName);
            String expectedExtension = StringUtils.getFilenameExtension(fileId);
            if (originalExtension != null && expectedExtension != null
                    && !expectedExtension.equalsIgnoreCase(originalExtension)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                        "上传文件扩展名与 presign 不一致");
            }
        } else {
            originalName = fileId;
        }

        try {
            Files.createDirectories(storageRoot());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, path);
            }
            writeMeta(fileId, new FileMeta(originalName, file.getSize(), file.getContentType()));
        } catch (IOException e) {
            log.error("文件保存失败 fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件保存失败");
        }

        // 传了 orderId 说明是参考文件，上传成功即回填 URL / 文件名 / 大小
        if (orderId != null) {
            ReferenceFileConfirmRequest confirm = new ReferenceFileConfirmRequest();
            confirm.setOrderId(orderId);
            confirm.setTaskId(taskId);
            confirm.setFileId(fileId);
            upsertReferenceFile(confirm, userId);
        }

        PresignResponseVO vo = new PresignResponseVO();
        vo.setFileId(fileId);
        vo.setUploadUrl(null);
        vo.setAccessUrl(resolveAccessUrl(fileId));
        vo.setFormData(Map.of());
        return vo;
    }

    @Override
    public Resource load(String fileId, Long userId, String userRole) {
        Path path = filePath(fileId);
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "文件 [%s] 不存在或尚未上传", fileId);
        }

        // 数据范围校验：按文件绑定的订单 / 任务判断
        DesignVersion version = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getFileId, fileId)
                        .orderByAsc(DesignVersion::getId)
                        .last("LIMIT 1"));
        Long orderId;
        Long taskId;
        if (version != null) {
            orderId = version.getOrderId();
            taskId = version.getTaskId();
        } else {
            OrderReferenceFile ref = orderReferenceFileMapper.selectOne(
                    new LambdaQueryWrapper<OrderReferenceFile>()
                            .eq(OrderReferenceFile::getFileId, fileId)
                            .orderByAsc(OrderReferenceFile::getId)
                            .last("LIMIT 1"));
            if (ref == null) {
                // 未绑定任何业务记录：仅 BL 可读取
                if (!UserRole.BL.getCode().equals(userRole)) {
                    throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权读取该文件");
                }
                return new FileSystemResource(path);
            }
            orderId = ref.getOrderId();
            taskId = ref.getTaskId();
        }

        // 匿名预览（浏览器直接打开 URL，无 Authorization）：只放行已绑定订单/任务的文件；
        // 未绑定业务记录的文件仍然仅 BL 可读（见上方 ref == null 分支）。
        // 携带令牌访问时 userId/userRole 不为空，继续走下方数据范围校验。
        if (userId == null && userRole == null) {
            return new FileSystemResource(path);
        }

        checkFileScope(orderId, taskId, userId, userRole);
        return new FileSystemResource(path);
    }

    /** 文件读取数据范围：BL 全量；AM 本人创建订单；DS 本人被分派的任务 */
    private void checkFileScope(Long orderId, Long taskId, Long userId, String userRole) {
        if (UserRole.BL.getCode().equals(userRole)) {
            return;
        }
        Order order = orderId != null ? orderMapper.selectById(orderId) : null;
        if (order == null) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权读取该文件");
        }
        if (UserRole.AM.getCode().equals(userRole) && userId != null
                && userId.equals(order.getCreatedBy())) {
            return;
        }
        if (UserRole.DS.getCode().equals(userRole) && userId != null) {
            if (taskId != null) {
                OrderTask task = orderTaskMapper.selectById(taskId);
                if (task != null && userId.equals(task.getDesignerId())) {
                    return;
                }
            } else {
                Long assigned = orderTaskMapper.selectCount(
                        new LambdaQueryWrapper<OrderTask>()
                                .eq(OrderTask::getOrderId, orderId)
                                .eq(OrderTask::getDesignerId, userId));
                if (assigned != null && assigned > 0) {
                    return;
                }
            }
        }
        throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权读取该文件");
    }

    @Override
    public String resolveAccessUrl(String fileId) {
        return apiPrefix() + "/" + fileId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReferenceFileVO confirmReferenceFile(ReferenceFileConfirmRequest dto, Long userId) {
        return upsertReferenceFile(dto, userId);
    }

    private ReferenceFileVO upsertReferenceFile(ReferenceFileConfirmRequest dto, Long userId) {
        checkOrderWritePermission(dto.getOrderId(), userId);

        Path path = filePath(dto.getFileId());
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "文件 [%s] 不存在或尚未上传", dto.getFileId());
        }

        FileMeta meta = readMeta(dto.getFileId());
        long fileSize = meta != null ? meta.fileSize() : sizeOf(path);
        String fileName = meta != null ? meta.fileName() : dto.getFileId();
        Long taskId = dto.getTaskId();

        List<OrderReferenceFile> existing = orderReferenceFileMapper.selectList(
                new LambdaQueryWrapper<OrderReferenceFile>()
                        .eq(OrderReferenceFile::getOrderId, dto.getOrderId())
                        .eq(OrderReferenceFile::getFileId, dto.getFileId()));

        OrderReferenceFile target;
        if (existing.isEmpty()) {
            target = new OrderReferenceFile();
            target.setOrderId(dto.getOrderId());
            target.setFileId(dto.getFileId());
            target.setTaskId(taskId);
            target.setUploadedBy(userId);
            target.setUploadedAt(LocalDateTime.now());
            fillReferenceFile(target, fileSize, fileName);
            orderReferenceFileMapper.insert(target);
        } else {
            for (OrderReferenceFile ref : existing) {
                if (ref.getTaskId() == null) {
                    ref.setTaskId(taskId);
                }
                fillReferenceFile(ref, fileSize, fileName);
                orderReferenceFileMapper.updateById(ref);
            }
            target = existing.get(0);
        }

        return toReferenceFileVO(target);
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 参考文件写入的数据范围校验：仅订单创建人可写，BL 负责全部订单可跳过。
     * upload 带 orderId 与 confirm 都会经过 upsertReferenceFile，因此在此统一拦截。
     */
    private void checkOrderWritePermission(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (UserRole.BL.getCode().equals(UserContext.getRole())) {
            return;
        }
        if (!order.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权操作他人订单");
        }
    }

    private void fillReferenceFile(OrderReferenceFile ref, long fileSize, String fileName) {
        ref.setFileUrl(resolveAccessUrl(ref.getFileId()));
        ref.setFileName(fileName);
        ref.setFileSize(fileSize);
    }

    private ReferenceFileVO toReferenceFileVO(OrderReferenceFile ref) {
        ReferenceFileVO vo = new ReferenceFileVO();
        vo.setId(ref.getId());
        vo.setFileId(ref.getFileId());
        vo.setTaskId(ref.getTaskId());
        vo.setFileUrl(ref.getFileUrl());
        vo.setFileName(ref.getFileName());
        vo.setFileSize(ref.getFileSize());
        return vo;
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "读取文件大小失败");
        }
    }

    private Path storageRoot() {
        return Paths.get(storageDir).toAbsolutePath().normalize();
    }

    private Path filePath(String fileId) {
        validateFileId(fileId);
        Path root = storageRoot();
        Path path = root.resolve(fileId).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "非法文件路径");
        }
        return path;
    }

    private Path metaPath(String fileId) {
        validateFileId(fileId);
        Path root = storageRoot();
        Path path = root.resolve(".meta").resolve(fileId + ".json").normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "非法文件路径");
        }
        return path;
    }

    private void validateFileId(String fileId) {
        // 仅保留非空校验（原文件 ID 正则已移除）；
        // 路径穿越仍由 filePath / metaPath 的 normalize + startsWith(root) 兜底拦截。
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "非法文件 ID");
        }
    }

    private void writeMeta(String fileId, FileMeta meta) throws IOException {
        Path path = metaPath(fileId);
        Files.createDirectories(path.getParent());
        objectMapper.writeValue(path.toFile(), meta);
    }

    private FileMeta readMeta(String fileId) {
        Path path = metaPath(fileId);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return objectMapper.readValue(path.toFile(), FileMeta.class);
        } catch (IOException e) {
            log.warn("读取文件元数据失败 fileId={}", fileId, e);
            return null;
        }
    }

    private String apiPrefix() {
        String ctx = contextPath == null ? "" : contextPath.trim();
        if (ctx.endsWith("/")) {
            ctx = ctx.substring(0, ctx.length() - 1);
        }
        return ctx + "/v1/files";
    }

    /** 文件元数据 sidecar */
    private record FileMeta(String fileName, long fileSize, String contentType) {
    }
}
