package com.peak.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/v1/orders/{id}/assign 入参（分派设计人员）— API.md §3.5 */
@Data
public class OrderAssignRequest {
    /** 设计负责人 ID（FK → users.id，必须是 DS 角色） */
    @NotNull(message = "设计人员不能为空")
    private Long designerId;
    /** 分派备注（选填，≤100 字符） */
    @Size(max = 100, message = "分派备注不能超过 100 字符")
    private String assignRemark;
    /** 乐观锁版本号 */
    @NotNull(message = "version 不能为空")
    private Integer version;
}
