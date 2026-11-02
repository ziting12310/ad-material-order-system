package com.peak.exception;

/**
 * 自定义业务异常 — Service 层业务校验失败时抛出
 *
 * <p>由 GlobalExceptionHandler（阶段 5）统一捕获并转换为 ResultVO.fail(code, message) 返回前端。
 * 与系统 RuntimeException 区分：BusinessException 是预期业务分支，不打印 ERROR 堆栈。</p>
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码 — 对应 ErrorCode 常量 */
    private final String code;

    /** 错误消息（可包含动态参数） */
    private final String message;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String code, String message, Object... args) {
        super(String.format(message, args));
        this.code = code;
        this.message = String.format(message, args);
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
