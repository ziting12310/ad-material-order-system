package com.peak.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;

/** POST /api/v1/orders 入参（创建订单）— API.md §3.2 */
@Data
public class OrderCreateRequest {
    @NotBlank(message = "客户名称不能为空")
    @Size(min = 2, max = 100, message = "客户名称长度 2~100 字符")
    private String customerName;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(min = 2, max = 20, message = "联系人姓名长度 2~20 字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /** @deprecated 多物料模型下请使用 tasks 列表，此字段兼容旧版仅单物料时有效 */
    @Deprecated
    private String materialType;

    /** @deprecated 多物料模型下请使用 tasks 列表 */
    @Deprecated
    private Integer quantity;

    /** @deprecated 多物料模型下请使用 tasks 列表 */
    @Deprecated
    private String specification;

    /** 物料任务列表（多物料模型） */
    @Valid
    private List<TaskCreateItem> tasks;

    @NotNull(message = "交付日期不能为空")
    @Future(message = "交付日期必须晚于今天")
    private LocalDate deliveryDate;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    /** 参考文件 ID 列表（最多 5 个） */
    private List<String> referenceFileIds;
}
