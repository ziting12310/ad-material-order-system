package com.peak.service;

import com.peak.dto.TaskAssignDTO;
import com.peak.dto.TaskCreateItem;
import com.peak.dto.TaskUpdateDTO;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OrderTaskVO;

import java.util.List;

public interface OrderTaskService {

    void batchCreateTasks(Long orderId, List<TaskCreateItem> items, Long creatorId);

    List<OrderTaskVO> listTasks(Long orderId, Long userId, String userRole);

    OrderTaskVO getTask(Long taskId, Long userId, String userRole);

    OrderTaskVO addTask(Long orderId, TaskCreateItem item, Long operatorId);

    OrderTaskVO updateTask(Long taskId, TaskUpdateDTO dto, Long operatorId);

    void removeTask(Long taskId, Long operatorId);

    OrderTaskVO assignTask(Long taskId, TaskAssignDTO dto, Long operatorId);

    OrderStatusUpdateResponseVO acceptTask(Long taskId, Integer version, Long designerId);

    void onVersionUploaded(Long taskId, Long versionId, Long uploadedBy);

    OrderTaskVO approveTask(Long taskId, String remark, Integer version, Long operatorId);

    OrderTaskVO rejectTask(Long taskId, String rejectNote, Integer version, Long operatorId);

    boolean allTasksApproved(Long orderId);
}
