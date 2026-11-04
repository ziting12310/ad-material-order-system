package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单交付归档 DTO — C 模块 M6 交付归档专用
 *
 * <p>归档前置条件：订单状态为 APPROVED（已定稿）。
 * 归档后订单进入只读状态，所有修改操作（定稿、驳回、上传版本、提交意见）均被拒绝。</p>
 */
@Data
@Schema(description = "订单交付归档请求")
public class ArchiveOrderDTO {

    /**
     * 归档备注
     * <p>可选，记录交付说明、客户反馈等信息</p>
     */
    @Schema(description = "归档/交付备注", example = "已交付客户，确认无问题")
    private String remark;

    /**
     * 乐观锁版本号
     * <p>必须与订单当前 version 一致，否则返回 409 版本冲突</p>
     */
    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "5")
    private Integer version;
}
