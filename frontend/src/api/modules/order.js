/**
 * 订单接口模块 — 对齐 API.md §3（10 个订单动作）
 * 注意：modules 下统一使用 import request from '../request'
 *
 * 所有写接口携带 version（乐观锁），冲突时后端返回 409 ORDER_VERSION_CONFLICT
 */
import request from '../request'

// ---------- 查询 ----------

/**
 * 订单列表（Cursor 游标分页，禁止 offset）
 * GET /api/v1/orders
 * @param {Object} params { cursor, limit, status, keyword, materialType }
 * @returns { list, nextCursor, hasMore, total }
 */
export const listOrders = params => request.get('/orders', { params })

/**
 * 订单详情（含 referenceFiles / designVersions / feedbacks）
 * GET /api/v1/orders/:id
 */
export const getOrderDetail = id => request.get(`/orders/${id}`)

// ---------- 写入（全部携带 version 乐观锁）----------

/**
 * 创建订单（AM）— 状态 CREATED
 * POST /api/v1/orders
 * @param {Object} data { customerName, contactName, contactPhone, materialType, quantity, specification, deliveryDate, remark }
 */
export const createOrder = data => request.post('/orders', data)

/**
 * 编辑订单（AM，仅 CREATED 状态）
 * PUT /api/v1/orders/:id
 */
export const updateOrder = (id, data) => request.put(`/orders/${id}`, data)

/**
 * 分派设计人员（BL）— CREATED → ASSIGNED
 * PUT /api/v1/orders/:id/assign
 * @param {Object} data { designerId, version }
 */
export const assignOrder = (id, data) => request.put(`/orders/${id}/assign`, data)

/**
 * 接单开始设计（DS）— ASSIGNED → DESIGNING
 * PUT /api/v1/orders/:id/accept
 * @param {Object} data { version }
 */
export const acceptTask = (id, data) => request.put(`/orders/${id}/accept`, data)

/**
 * 驳回设计稿（BL）— WAIT_APPROVE → DESIGNING
 * PUT /api/v1/orders/:id/reject
 * @param {Object} data { rejectNote, version }
 */
export const rejectDesign = (id, data) => request.put(`/orders/${id}/reject`, data)

/**
 * 确认终稿（BL）— WAIT_APPROVE → APPROVED
 * PUT /api/v1/orders/:id/approve
 * @param {Object} data { approveNote, version }
 */
export const approveFinal = (id, data) => request.put(`/orders/${id}/approve`, data)

/**
 * 标记交付（BL）— APPROVED → DELIVERED（归档 is_archived=1）
 * PUT /api/v1/orders/:id/deliver
 * @param {Object} data { deliverRemark, version }
 */
export const markDelivered = (id, data) => request.put(`/orders/${id}/deliver`, data)

/**
 * 作废订单（AM/BL，非终态）— → CANCELLED
 * PUT /api/v1/orders/:id/cancel
 * @param {Object} data { cancelReason, version }
 */
export const cancelOrder = (id, data) => request.put(`/orders/${id}/cancel`, data)
