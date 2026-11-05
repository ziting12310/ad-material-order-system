package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/v1/order-feedbacks 入参（提交修改意见）— API.md §5.1 */
@Data
public class OrderFeedbackCreateRequest {
    /** 关联订单 ID */
    @NotNull(message = "orderId 不能为空")
    private Long orderId;
    /** 任务 ID（多物料模型下必填，必须属于 orderId） */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;
    /** 修改意见内容（1~500 字符，XSS 清洗） */
    @NotBlank(message = "修改意见不能为空")
    @Size(min = 1, max = 500, message = "修改意见长度 1~500 字符")
    private String content;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
