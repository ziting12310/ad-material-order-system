package com.peak.exception;

import com.peak.common.constants.BusinessErrorCode;
import com.peak.common.constants.ErrorCode;
import com.peak.vo.ResultVO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 拦截所有异常并转换为统一 ResultVO + 语义化 HTTP 状态码
 *
 * <p>响应约定（不再统一返回 200）：
 * <pre>
 * 场景                          HTTP  业务码
 * 未登录 / Token 缺失           401   UNAUTHORIZED
 * Token 过期                   401   TOKEN_EXPIRED
 * Token 非法                   401   TOKEN_INVALID
 * 账号或密码错误                401   LOGIN_FAILED
 * 账号已锁定                    401   ACCOUNT_LOCKED
 * 角色权限不足                  403   PERMISSION_DENIED
 * 数据范围越权                  403   DATA_SCOPE_DENIED
 * 资源不存在                    404   ORDER_NOT_FOUND / USER_NOT_FOUND / ...
 * 请求方法不支持                405   METHOD_NOT_ALLOWED
 * 乐观锁/编号冲突               409   ORDER_VERSION_CONFLICT / ORDER_NO_DUPLICATE
 * 参数校验失败                  400   PARAM_VALIDATION_ERROR
 * 缺少必填参数                  400   MISSING_REQUIRED_FIELD
 * 上传文件超限                  413   FILE_TOO_LARGE
 * 状态机冲突                    422   ORDER_STATUS_CONFLICT / ORDER_STATUS_ILLEGAL_TRANSITION
 * 系统异常                      500   INTERNAL_ERROR
 * </pre>
 *
 * <p>Controller 层不 try-catch，业务异常全部向上抛出由本处理器统一处理。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务错误码 → HTTP 状态码映射（未命中时默认 400 Bad Request） */
    private static final Map<String, HttpStatus> CODE_STATUS = Map.ofEntries(
            // 400
            Map.entry(ErrorCode.PARAM_VALIDATION_ERROR, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.MISSING_REQUIRED_FIELD, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.FILE_TYPE_NOT_SUPPORTED, HttpStatus.BAD_REQUEST),
            // 401
            Map.entry(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.LOGIN_FAILED, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.ACCOUNT_LOCKED, HttpStatus.UNAUTHORIZED),
            // 403
            Map.entry(ErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.DATA_SCOPE_DENIED, HttpStatus.FORBIDDEN),
            // 404
            Map.entry(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.DESIGN_VERSION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.FEEDBACK_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
            // 405
            Map.entry(ErrorCode.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED),
            // 409
            Map.entry(ErrorCode.ORDER_VERSION_CONFLICT, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.ORDER_NO_DUPLICATE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.ORDER_ALREADY_ASSIGNED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.USERNAME_DUPLICATE, HttpStatus.CONFLICT),
            // 413
            Map.entry(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE),
            // 422
            Map.entry(ErrorCode.ORDER_STATUS_CONFLICT, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.DESIGNER_ROLE_INVALID, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.NO_PENDING_FEEDBACK, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(), HttpStatus.UNPROCESSABLE_ENTITY),
            // 500
            Map.entry(ErrorCode.BUSINESS_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
    );

    /** 根据业务错误码解析 HTTP 状态码 */
    private HttpStatus resolveStatus(String code) {
        return CODE_STATUS.getOrDefault(code, HttpStatus.BAD_REQUEST);
    }

    // ---------- 业务异常（预期分支，不打印 ERROR 堆栈）----------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultVO<Void>> handleBusinessException(BusinessException e) {
        HttpStatus status = resolveStatus(e.getCode());
        log.warn("业务异常 [{}] -> {} traceId={} -> {}",
                e.getCode(), status.value(), MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(status)
                .body(ResultVO.fail(e.getCode(), e.getMessage()));
    }

    // ---------- Jakarta Validation（@Valid @RequestBody DTO 校验失败）→ 400 ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultVO<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResponseEntity.badRequest()
                .body(ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, msg));
    }

    // ---------- 表单 / Query 参数校验失败 → 400 ----------
    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ResultVO<Void>> handleBindException(Exception e) {
        String msg;
        if (e instanceof BindException be) {
            msg = be.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else if (e instanceof ConstraintViolationException ce) {
            msg = ce.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
        } else {
            msg = e.getMessage();
        }
        log.warn("参数绑定失败 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResponseEntity.badRequest()
                .body(ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, msg));
    }

    // ---------- JSON 解析失败（字段类型不匹配等）→ 400 ----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultVO<Void>> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("JSON 解析失败 traceId={}", MDC.get("traceId"), e);
        return ResponseEntity.badRequest()
                .body(ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, "请求体格式错误，请检查 JSON 字段类型"));
    }

    // ---------- 缺少必填 Query 参数 / 请求头 → 400 ----------
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ResultVO<Void>> handleMissingParam(Exception e) {
        String msg;
        if (e instanceof MissingServletRequestParameterException me) {
            msg = "缺少必填参数：" + me.getParameterName();
        } else if (e instanceof MissingRequestHeaderException he) {
            msg = "缺少必填请求头：" + he.getHeaderName();
        } else {
            msg = e.getMessage();
        }
        log.warn("缺少必填项 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResponseEntity.badRequest()
                .body(ResultVO.fail(ErrorCode.MISSING_REQUIRED_FIELD, msg));
    }

    // ---------- 参数类型不匹配（如 id 传了非数字）→ 400 ----------
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResultVO<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = "参数 " + e.getName() + " 类型不正确";
        log.warn("参数类型不匹配 traceId={} -> {}", MDC.get("traceId"), msg);
        return ResponseEntity.badRequest()
                .body(ResultVO.fail(ErrorCode.PARAM_VALIDATION_ERROR, msg));
    }

    // ---------- 上传文件超过大小限制 → 413 ----------
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultVO<Void>> handleFileTooLarge(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制 traceId={} -> {}", MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ResultVO.fail(ErrorCode.FILE_TOO_LARGE, "上传文件超过大小限制"));
    }

    // ---------- 请求方法不支持（如 GET 调 POST 接口）→ 405 ----------
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultVO<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持 traceId={} -> {}", MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResultVO.fail(ErrorCode.METHOD_NOT_ALLOWED, "请求方法不支持：" + e.getMethod()));
    }

    // ---------- 路由 / 静态资源不存在 → 404 ----------
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ResultVO<Void>> handleNotFound(Exception e) {
        log.warn("资源不存在 traceId={} -> {}", MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResultVO.fail(ErrorCode.NOT_FOUND, "请求的资源不存在"));
    }

    // ---------- 兜底：未捕获的系统异常（ERROR 级别 + 完整堆栈）→ 500 ----------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO<Void>> handleException(Exception e) {
        log.error("系统异常 traceId={}", MDC.get("traceId"), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultVO.fail(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试"));
    }
}
