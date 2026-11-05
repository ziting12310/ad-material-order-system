/**
 * 定稿审核 + 归档模块接口
 * 后端路径：
 *   - /api/v1/audit    （审核域：待审核列表、审核记录）
 *   - /api/v1/archive  （归档域：已归档订单列表/详情）
 * 分页方式：Cursor 游标分页
 */
import request from '@/utils/request'

// ==================== 审核域 ====================

/**
 * 待审核任务列表（我的待办）
 * GET /api/v1/audit/pending
 *
 * BL 看到所有待审核任务；AM 看到自己订单下的；DS 无权限
 * 返回的是物料任务级（OrderTaskVO），不是订单级
 */
export function listPendingTasks(params) {
    return request.get('/audit/pending', { params, silent: true })
}

/**
 * 已定稿任务列表（APPROVED / DELIVERED 状态）
 * GET /api/v1/audit/approved
 */
export function listApprovedTasks(params) {
    return request.get('/audit/approved', { params, silent: true })
}

/**
 * 已驳回任务列表（最近一次审核操作为 TASK_REJECT，后续无 TASK_APPROVE）
 * GET /api/v1/audit/rejected
 */
export function listRejectedTasks(params) {
    return request.get('/audit/rejected', { params, silent: true })
}

/**
 * 审核统计：待审核 / 已定稿 / 驳回次数
 * GET /api/v1/audit/stats
 */
export function getAuditStats() {
    return request.get('/audit/stats', { silent: true })
}

/**
 * 我参与的审核记录
 * GET /api/v1/audit/my-logs
 */
export function listMyAuditLogs(limit = 20) {
    return request.get('/audit/my-logs', { params: { limit }, silent: true })
}

/**
 * 查询订单的审核记录
 * GET /api/v1/audit/orders/{orderId}/logs
 */
export function listAuditLogsByOrder(orderId) {
    return request.get(`/audit/orders/${orderId}/logs`, { silent: true })
}

// ==================== 归档域 ====================

/**
 * 已归档订单列表（Cursor 分页）
 * GET /api/v1/archive/orders
 */
export function listArchivedOrders(params) {
    return request.get('/archive/orders', { params, silent: true })
}

/**
 * 归档订单详情（只读）
 * GET /api/v1/archive/orders/{id}
 */
export function getArchivedOrder(id) {
    return request.get(`/archive/orders/${id}`, { silent: true })
}

/**
 * 归档订单数量统计
 * GET /api/v1/archive/count
 */
export function countArchived() {
    return request.get('/archive/count', { silent: true })
}

// ==================== 任务级定稿/驳回 ====================
// 注意：任务级的定稿/驳回后端 OrderTaskService 有实现，
// 但目前 Controller 只有订单级的 approve/reject（/orders/{id}/approve）。
// 以下接口名预留，等后端补全任务级接口后直接替换路径。

/**
 * 任务级确认定稿（预留，当前走订单级接口）
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}/approve
 */
export function approveTask(orderId, taskId, data) {
    return request.put(`/orders/${orderId}/tasks/${taskId}/approve`, data)
}

/**
 * 任务级驳回（预留，当前走订单级接口）
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}/reject
 */
export function rejectTask(orderId, taskId, data) {
    return request.put(`/orders/${orderId}/tasks/${taskId}/reject`, data)
}
