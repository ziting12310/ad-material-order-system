package com.peak.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** PUT /api/v1/orders/{id} 入参（编辑订单）— API.md §3.4；仅 CREATED 状态可编辑 */
@Data
public class OrderUpdateRequest {
    @NotBlank @Size(min = 2, max = 100)
    private String customerName;
    @NotBlank @Size(min = 2, max = 20)
    private String contactName;
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String contactPhone;
    @NotBlank
    private String materialType;
    @NotNull
    @Positive(message = "数量必须为正整数")
    @Max(value = 99999, message = "数量需在 1-99999 之间")
    private Integer quantity;
    @Size(max = 200, message = "规格说明最长200字符")
    private String specification;
    @NotNull @Future
    private LocalDate deliveryDate;
    @Size(max = 500)
    private String remark;
    private List<String> referenceFileIds;

    /**
     * 物料任务明细（可选）：前端编辑弹窗按任务修改数量/规格/截止日期等。
     * 传入时按任务 id 逐条更新，并携带任务自身 version 做乐观锁校验。
     */
    @Valid
    private List<TaskItem> tasks;

    /** 乐观锁版本号 — 必须等于当前订单 version，冲突返回 409 */
    @NotNull(message = "version 不能为空")
    private Integer version;

    /** 编辑时同步的单条物料任务 */
    @Data
    public static class TaskItem {
        @NotNull(message = "任务ID不能为空")
        private Long id;

        @NotNull(message = "任务版本号不能为空")
        private Integer version;

        private String materialType;

        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须为正整数")
        @Max(value = 99999, message = "数量需在 1-99999 之间")
        private Integer quantity;

        @Size(max = 200, message = "规格说明最长200字符")
        private String specification;

        @Size(max = 200, message = "用途说明最多200字符")
        private String usageDesc;

        private LocalDate deadline;

        @Size(max = 500, message = "任务备注最多500字符")
        private String remark;
    }
}
