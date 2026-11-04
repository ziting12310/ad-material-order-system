package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定稿审核 DTO — 用于 confirmFinal（确认定稿）和 rejectFinal（驳回定稿）
 *
 * <p>C 模块 M5 定稿审核专用。orderId 从路径变量传入，remark 和 version 从请求体传入。
 * 同一个 DTO 复用于确认和驳回，减少重复类；业务语义由 Controller 路由区分。</p>
 */
@Data
@Schema(description = "定稿审核请求（确认/驳回共用）")
public class FinalAuditDTO {

    /**
     * 定稿/驳回备注
     * <p>确认定稿时可选（记录定稿意见），驳回时必填（说明驳回原因）</p>
     */
    @Schema(description = "审核备注（驳回时必填）", example = "设计稿符合要求，同意定稿")
    private String remark;

    /**
     * 乐观锁版本号
     * <p>必须与订单当前 version 一致，否则返回 409 版本冲突</p>
     */
    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "3")
    private Integer version;
}
