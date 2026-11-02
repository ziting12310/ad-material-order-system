package com.peak.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

/**
 * 全局统一返回对象 — API.md §1.1
 *
 * <pre>
 * { "code": "SUCCESS", "message": "操作成功", "data": {}, "traceId": "abc123" }
 * </pre>
 *
 * <p>静态工厂方法：success() / success(data) / fail(code, msg)。
 * 全局异常处理器将所有异常转换为 ResultVO 返回。</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVO<T> {

    /** 业务状态码（SUCCESS / PARAM_VALIDATION_ERROR / ORDER_VERSION_CONFLICT 等） */
    private String code;
    /** 人类可读提示信息 */
    private String message;
    /** 响应数据（失败时为 null） */
    private T data;
    /** 全链路追踪 ID（MDC 注入） */
    private String traceId;

    // ---------- 静态工厂 ----------
    public static <T> ResultVO<T> success() {
        return build("SUCCESS", "操作成功", null);
    }

    public static <T> ResultVO<T> success(T data) {
        return build("SUCCESS", "操作成功", data);
    }

    public static <T> ResultVO<T> success(String message, T data) {
        return build("SUCCESS", message, data);
    }

    public static <T> ResultVO<T> fail(String code, String message) {
        return build(code, message, null);
    }

    public static <T> ResultVO<T> fail(String message) {
        return build("FAILED", message, null);
    }

    private static <T> ResultVO<T> build(String code, String message, T data) {
        ResultVO<T> r = new ResultVO<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.traceId = MDC.get("traceId");
        return r;
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(code);
    }
}
