package com.peak.util;

import com.peak.common.enums.OperationType;
import com.peak.entity.OrderOperationLog;
import com.peak.mapper.OrderOperationLogMapper;
import com.peak.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogUtil {

    private final OrderOperationLogMapper logMapper;
    private final UserMapper userMapper;

    public void logOrder(Long orderId, OperationType opType, String description,
                         Long operatorId, String before, String after) {
        log(orderId, null, opType, description, operatorId, before, after);
    }

    public void logTask(Long orderId, Long taskId, OperationType opType, String description,
                        Long operatorId, String before, String after) {
        log(orderId, taskId, opType, description, operatorId, before, after);
    }

    private void log(Long orderId, Long taskId, OperationType opType, String description,
                     Long operatorId, String before, String after) {
        try {
            OrderOperationLog entry = new OrderOperationLog();
            entry.setOrderId(orderId);
            entry.setTaskId(taskId);
            entry.setOperationType(opType.getCode());
            entry.setDescription(description);
            entry.setBeforeValue(before);
            entry.setAfterValue(after);
            entry.setOperatorId(operatorId);

            if (operatorId != null) {
                var user = userMapper.selectById(operatorId);
                if (user != null) {
                    entry.setOperatorName(user.getName());
                    entry.setOperatorRole(user.getRole());
                }
            }

            entry.setOperatedAt(LocalDateTime.now());
            entry.setClientIp(currentClientIp());
            logMapper.insert(entry);
        } catch (Exception e) {
            log.warn("操作日志写入失败：orderId={}, taskId={}, opType={}, error={}",
                    orderId, taskId, opType, e.getMessage());
        }
    }

    /** 取当前请求的真实客户端 IP（支持 X-Forwarded-For） */
    private String currentClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
