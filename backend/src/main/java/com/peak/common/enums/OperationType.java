package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationType {

    ORDER_CREATE("ORDER_CREATE", "创建订单"),
    ORDER_CONFIRM("ORDER_CONFIRM", "确认物料清单"),
    ORDER_EDIT("ORDER_EDIT", "编辑订单信息"),
    ORDER_CANCEL("ORDER_CANCEL", "作废订单"),
    ORDER_DELIVER("ORDER_DELIVER", "交付归档"),

    TASK_ADD("TASK_ADD", "新增物料任务"),
    TASK_EDIT("TASK_EDIT", "修改物料任务"),
    TASK_REMOVE("TASK_REMOVE", "删除物料任务"),
    TASK_ASSIGN("TASK_ASSIGN", "分派设计人员"),
    TASK_ACCEPT("TASK_ACCEPT", "设计师接单"),
    TASK_UPLOAD_VERSION("TASK_UPLOAD_VERSION", "上传设计版本"),
    TASK_FEEDBACK("TASK_FEEDBACK", "提交修改意见"),
    TASK_APPROVE("TASK_APPROVE", "确认定稿"),
    TASK_REJECT("TASK_REJECT", "驳回定稿");

    private final String code;
    private final String label;

    public static OperationType of(String code) {
        for (OperationType t : values()) if (t.code.equals(code)) return t;
        return null;
    }
}
