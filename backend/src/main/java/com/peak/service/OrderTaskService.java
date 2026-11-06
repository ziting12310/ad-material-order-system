package com.peak.service;

import com.peak.dto.TaskAssignDTO;
import com.peak.dto.TaskCreateItem;
import com.peak.dto.TaskUpdateDTO;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OrderTaskStatsVO;
import com.peak.vo.OrderTaskVO;

import java.util.List;

public interface OrderTaskService {

    void batchCreateTasks(Long orderId, List<TaskCreateItem> items, Long creatorId);

    List<OrderTaskVO> listTasks(Long orderId, Long userId, String userRole);

    OrderTaskVO getTask(Long taskId, Long userId, String userRole);

    OrderTaskVO addTask(Long orderId, TaskCreateItem item, Long operatorId);

    OrderTaskVO updateTask(Long taskId, TaskUpdateDTO dto, Long operatorId);

    void removeTask(Long taskId, Long operatorId);

    /** 分派设计人员（CREATED → ASSIGNED）；orderId 用于校验任务是否属于当前订单 */
    OrderTaskVO assignTask(Long orderId, Long taskId, TaskAssignDTO dto, Long operatorId);

    OrderStatusUpdateResponseVO acceptTask(Long taskId, Integer version, Long designerId);

    /** 重新开始设计（REJECTED → DESIGNING，仅任务所属设计师可操作） */
    OrderStatusUpdateResponseVO restartDesign(Long taskId, Integer version, Long designerId);

    void onVersionUploaded(Long taskId, Long versionId, Long uploadedBy);

    /** 客户提交修改意见后回退任务状态（WAIT_APPROVE → DESIGNING） */
    void onFeedbackSubmitted(Long taskId, Long feedbackId, Long operatorId);

    OrderTaskVO approveTask(Long taskId, String remark, Integer version, Long operatorId);

    OrderTaskVO rejectTask(Long taskId, String rejectNote, Integer version, Long operatorId);

    boolean allTasksApproved(Long orderId);

    /** 工作台任务状态统计（与工作台任务列表同口径：可见订单 + 任务归属 + 有效状态） */
    OrderTaskStatsVO getTaskStats(Long userId, String userRole);
}
