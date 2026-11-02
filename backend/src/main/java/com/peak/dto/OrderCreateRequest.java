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

    @NotBlank(message = "物料类型不能为空")
    private String materialType;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于 0")
    private Integer quantity;

    @NotBlank(message = "规格说明不能为空")
    @Size(min = 5, max = 200, message = "规格说明长度 5~200 字符")
    private String specification;

    @NotNull(message = "交付日期不能为空")
    @Future(message = "交付日期必须晚于今天")
    private LocalDate deliveryDate;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    /** 参考文件 ID 列表（最多 5 个） */
    private List<String> referenceFileIds;
}
