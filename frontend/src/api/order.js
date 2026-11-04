/**
 * 订单模块接口
 * 后端路径：/api/v1/orders
 */
import request from '@/utils/request'

/**
 * 订单列表（分页）
 * GET /api/v1/orders
 * @param {Object} params { page, limit, orderNo, customerName, materialType, status, startDate, endDate }
 * @returns { list, total, page, limit }
 */
export function listOrders(params) {
    return request.get('/orders', { params })
}

/**
 * 订单详情
 * GET /api/v1/orders/{id}
 */
export function getOrderDetail(id) {
    return request.get(`/orders/${id}`)
}

/**
 * 创建订单
 * POST /api/v1/orders
 * @param {Object} data 订单表单数据
 */
export function createOrder(data) {
    return request.post('/orders', data)
}

/**
 * 更新订单
 * PUT /api/v1/orders/{id}
 * @param {Number} id 订单ID
 * @param {Object} data 订单表单数据
 */
export function updateOrder(id, data) {
    return request.put(`/orders/${id}`, data)
}

/**
 * 删除订单
 * DELETE /api/v1/orders/{id}
 */
export function deleteOrder(id) {
    return request.delete(`/orders/${id}`)
}
