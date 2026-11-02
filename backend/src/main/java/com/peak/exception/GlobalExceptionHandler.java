package com.peak.exception;

import com.peak.common.constants.ErrorCode;
import com.peak.vo.ResultVO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 拦截所有异常并转换为统一 ResultVO 返回
 *
 * <p>职责：
 *  - BusinessException → 预期业务分支，WARN 日志
 *  - Jakarta Validation 校验异常 → 422 PARAM_VALIDATION_ERROR
 *  - 其他 RuntimeException → ERROR 日志 + INTERNAL_ERROR
 *  - 所有响应 traceId 从 MDC 注入（链路追踪）
 *
 * <p>Controller 层不 try-catch，业务异常全部向上抛出由本处理器统一处理。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- 业务异常（预期分支，不打印 ERROR 堆栈）----------
    @ExceptionHandler(BusinessException.class)
    public ResultVO<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [{}] traceId={} -> {}", e.getCode(), MDC.get("traceId"), e.getMessage());
        return ResultVO.fail(e.getCode(), e.getMessage());
    }

    // ---------- Jakarta Validation（@Valid @RequestBody DTO 校验失败）----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 拼接所有字段校验错误："customerName 不能为空, contactPhone 格式不正确"
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, msg);
    }

    // ---------- 表单/Query 参数校验失败 ----------
    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public ResultVO<Void> handleBindException(Exception e) {
        // Java 17 switch 模式匹配（预览语法）在无 --enable-preview 编译下不可用，
        // 这里用传统 if-instanceof 多态分发，所有 JDK 版本均可编译运行。
        String msg;
        if (e instanceof BindException be) {
            // 表单参数 @Valid 校验失败（如 @PostMapping + @Validated 表单提交）
            msg = be.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else if (e instanceof ConstraintViolationException ce) {
            // 方法级 @Validated（如 Query 参数 @Min / @Pattern 校验失败）
            msg = ce.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
        } else {
            msg = e.getMessage();
        }
        log.warn("参数绑定失败 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, msg);
    }

    // ---------- JSON 解析失败（比如字段类型不匹配）----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultVO<Void> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("JSON 解析失败 traceId={}", MDC.get("traceId"), e);
        return ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, "请求体格式错误，请检查 JSON 字段类型");
    }

    // ---------- 兜底：未捕获的系统异常（ERROR 级别 + 完整堆栈）----------
    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception e) {
        log.error("系统异常 traceId={}", MDC.get("traceId"), e);
        return ResultVO.fail(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }
}
