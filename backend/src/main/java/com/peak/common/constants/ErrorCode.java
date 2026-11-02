package com.peak.common.constants;

/**
 * 全局错误码常量 — API.md 错误码字典章节
 *
 * <p>错误码命名规则：HTTP_状态码_业务域_具体错误</p>
 * 所有错误码在 ResultVO.code 字段返回，与前端约定对齐。
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ---------- 200 Success ----------
    public static final String SUCCESS = "SUCCESS";

    // ---------- 400 Bad Request ----------
    /** 参数校验失败（Jakarta Validation 注解触发） */
    public static final String PARAM_VALIDATION_ERROR = "PARAM_VALIDATION_ERROR";
    /** 文件格式不支持 */
    public static final String FILE_TYPE_NOT_SUPPORTED = "FILE_TYPE_NOT_SUPPORTED";

    // ---------- 401 Unauthorized ----------
    /** 未登录 / Token 缺失 */
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    /** Token 过期 */
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    /** Token 非法（签名错误 / 格式错误） */
    public static final String TOKEN_INVALID = "TOKEN_INVALID";

    // ---------- 403 Forbidden ----------
    /** 账号密码错误 */
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    /** 账号已锁定 */
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    /** 无角色权限访问 */
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";

    // ---------- 404 Not Found ----------
    /** 订单不存在 */
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    /** 设计稿版本不存在 */
    public static final String DESIGN_VERSION_NOT_FOUND = "DESIGN_VERSION_NOT_FOUND";
    /** 修改意见不存在 */
    public static final String FEEDBACK_NOT_FOUND = "FEEDBACK_NOT_FOUND";
    /** 用户不存在 */
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    // ---------- 409 Conflict ----------
    /** 乐观锁版本冲突（并发操作拦截） */
    public static final String ORDER_VERSION_CONFLICT = "ORDER_VERSION_CONFLICT";
    /** 订单编号重复 */
    public static final String ORDER_NO_DUPLICATE = "ORDER_NO_DUPLICATE";

    // ---------- 422 Unprocessable Entity ----------
    /** 订单状态冲突（当前状态不允许该操作） */
    public static final String ORDER_STATUS_CONFLICT = "ORDER_STATUS_CONFLICT";
    /** 设计人员角色不符合（分派的用户不是 DS 角色） */
    public static final String DESIGNER_ROLE_INVALID = "DESIGNER_ROLE_INVALID";
    /** 数据范围越权（非本人创建/非本人负责的订单） */
    public static final String DATA_SCOPE_DENIED = "DATA_SCOPE_DENIED";

    // ---------- 500 Internal Server Error ----------
    /** 通用业务异常 */
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    /** 系统内部错误 */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
