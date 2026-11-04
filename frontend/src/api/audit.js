/**
 * 定稿审核 & 归档模块接口
 * 后端路径：/api/v1/final-audit
 */
import request from '@/utils/request'

/**
 * 查询定稿/归档订单列表
 * GET /api/v1/final-audit/orders
 * @param {Object} params { cursor, limit, orderNo, customerName, status, startDate, endDate }
 */
export function listAuditOrders(params) {
    return request.get('/final-audit/orders', { params })
}

/**
 * 确认定稿
 * POST /api/v1/final-audit/orders/{id}/confirm
 */
export function confirmFinal(id, data) {
    return request.post(`/final-audit/orders/${id}/confirm`, data)
}

/**
 * 驳回定稿
 * POST /api/v1/final-audit/orders/{id}/reject
 * @param {Object} data { rejectRemark }
 */
export function rejectFinal(id, data) {
    return request.post(`/final-audit/orders/${id}/reject`, data)
}

/**
 * 查询归档详情
 * GET /api/v1/final-audit/orders/{id}
 */
export function getArchiveDetail(id) {
    return request.get(`/final-audit/orders/${id}`)
}

/**
 * 统计数据（待审核/已定稿/已驳回数量）
 * GET /api/v1/final-audit/statistics
 */
export function getAuditStatistics() {
    return request.get('/final-audit/statistics')
}
