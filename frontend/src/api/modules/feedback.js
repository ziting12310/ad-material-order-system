/**
 * 修改意见接口模块 — 对齐 API.md §5.1
 * 注意：modules 下统一使用 import request from '../request'
 *
 * 提交后订单状态 WAIT_APPROVE → DESIGNING（设计师按意见改稿）
 */
import request from '../request'

/**
 * 提交修改意见（AM/BL）
 * POST /api/v1/order-feedbacks
 * @param {Object} data { orderId, content, version }
 */
export const submitFeedback = data => request.post('/order-feedbacks', data)
