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
    @NotNull @Positive
    private Integer quantity;
    @NotBlank @Size(min = 5, max = 200)
    private String specification;
    @NotNull @Future
    private LocalDate deliveryDate;
    @Size(max = 500)
    private String remark;
    private List<String> referenceFileIds;

    /** 乐观锁版本号 — 必须等于当前订单 version，冲突返回 409 */
    @NotNull(message = "version 不能为空")
    private Integer version;
}
