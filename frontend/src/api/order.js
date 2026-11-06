/**
 * 订单模块接口
 * 后端路径：/api/v1/orders
 * 分页方式：Cursor 游标分页（{ list, nextCursor, hasMore, total }）
 */
import request from '@/utils/request'

/**
 * 订单列表（Cursor 游标分页）
 * GET /api/v1/orders
 * @param {Object} params - 查询参数
 *   - cursor:    游标（首页不传）
 *   - limit:     每页数量
 *   - materialType: 物料类型
 *   - status:    订单状态
 *   - keyword:   关键字（订单号/客户名称）
 *   - designerId: 设计师ID
 *   - creatorId: 创建人ID
 *   - deliveryDateFrom: 交付日期起
 *   - deliveryDateTo:   交付日期止
 * @returns {Object} { list, nextCursor, hasMore, total }
 */
export function listOrders(params) {
    return request.get('/orders', { params, silent: true })
}

/**
 * 订单列表（页码分页）
 * GET /api/v1/orders/page
 * @param {Object} params - { pageNum(默认1), pageSize(默认20), materialType, status, keyword, ... }
 * @returns {Object} { list, total, pages, pageNum, pageSize }
 */
export function pageOrders(params) {
    return request.get('/orders/page', { params, silent: true })
}

/**
 * 订单详情
 * GET /api/v1/orders/{id}
 */
export function getOrderDetail(id) {
    return request.get(`/orders/${id}`, { silent: true })
}

/**
 * 创建订单
 * POST /api/v1/orders
 */
export function createOrder(data) {
    return request.post('/orders', data)
}

/**
 * 更新订单
 * PUT /api/v1/orders/{id}
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

/**
 * 确认定稿（订单级）
 * PUT /api/v1/orders/{id}/approve
 */
export function approveOrder(id, data) {
    return request.put(`/orders/${id}/approve`, data)
}

/**
 * 驳回设计（订单级）
 * PUT /api/v1/orders/{id}/reject
 */
export function rejectOrder(id, data) {
    return request.put(`/orders/${id}/reject`, data)
}

/**
 * 订单交付
 * PUT /api/v1/orders/{id}/deliver
 */
export function deliverOrder(id, data) {
    return request.put(`/orders/${id}/deliver`, data)
}

/**
 * 订单作废
 * PUT /api/v1/orders/{id}/cancel
 */
export function cancelOrder(id, data) {
    return request.put(`/orders/${id}/cancel`, data)
}

// ==================== 物料任务 ====================

/**
 * 工作台任务状态统计
 * GET /api/v1/order-tasks/stats
 * 与工作台任务列表同一数据口径：仅当前用户可见订单、本人任务、有效状态
 */
export function getTaskStats() {
    return request.get('/order-tasks/stats', { silent: true })
}

/**
 * 重新开始设计（已驳回 → 设计中，仅任务所属设计师）
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}/restart-design
 */
export function restartDesign(orderId, taskId, version) {
    return request.put(`/orders/${orderId}/tasks/${taskId}/restart-design`, { version }, { silent: true })
}

/**
 * 查询订单下所有物料任务
 * GET /api/v1/orders/{orderId}/tasks
 */
export function listOrderTasks(orderId) {
    return request.get(`/orders/${orderId}/tasks`, { silent: true })
}

/**
 * 查询单个物料任务详情
 * GET /api/v1/orders/{orderId}/tasks/{taskId}
 */
export function getOrderTask(orderId, taskId) {
    return request.get(`/orders/${orderId}/tasks/${taskId}`)
}

/**
 * 新增物料任务
 * POST /api/v1/orders/{orderId}/tasks
 */
export function addOrderTask(orderId, data) {
    return request.post(`/orders/${orderId}/tasks`, data)
}

/**
 * 修改物料任务
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}
 */
export function updateOrderTask(orderId, taskId, data) {
    return request.put(`/orders/${orderId}/tasks/${taskId}`, data)
}

/**
 * 删除物料任务
 * DELETE /api/v1/orders/{orderId}/tasks/{taskId}
 */
export function deleteOrderTask(orderId, taskId) {
    return request.delete(`/orders/${orderId}/tasks/${taskId}`)
}

/**
 * 分派设计人员
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}/assign
 */
export function assignTask(orderId, taskId, data) {
    return request.put(`/orders/${orderId}/tasks/${taskId}/assign`, data)
}

/**
 * 订单级分派（CREATED 状态无任务时使用）
 * PUT /api/v1/orders/{orderId}/assign
 * 入参：{ designerId, assignRemark, version }
 */
export function assignOrder(orderId, data) {
    return request.put(`/orders/${orderId}/assign`, data)
}

/**
 * 设计师接单
 * PUT /api/v1/orders/{orderId}/tasks/{taskId}/accept
 */
export function acceptTask(orderId, taskId, version) {
    return request.put(`/orders/${orderId}/tasks/${taskId}/accept`, { version })
}

// ==================== 设计稿版本 ====================

/**
 * 查询订单设计稿版本列表
 * GET /api/v1/design-versions?orderId={orderId}
 */
export function listDesignVersions(orderId) {
    return request.get('/design-versions', { params: { orderId }, silent: true })
}

/**
 * 上传设计稿新版本（单文件）
 * POST /api/v1/design-versions
 * body: { orderId, taskId, fileId, fileName, fileSize, fileType, versionNote, version(任务乐观锁) }
 */
export function uploadDesignVersion(data) {
    return request.post('/design-versions', data)
}

// ==================== 修改意见 ====================

/**
 * 查询修改意见列表
 * GET /api/v1/order-feedbacks?orderId={orderId}
 */
export function listFeedbacks(orderId) {
    return request.get('/order-feedbacks', { params: { orderId }, silent: true })
}

/**
 * 提交修改意见
 * POST /api/v1/order-feedbacks
 * body: { orderId, taskId, content, version(任务乐观锁) }
 */
export function submitFeedback(data) {
    return request.post('/order-feedbacks', data)
}

// ==================== 参考文件 ====================

/**
 * 查询参考文件列表
 * GET /api/v1/orders/{orderId}/reference-files
 */
export function listReferenceFiles(orderId) {
    return request.get(`/orders/${orderId}/reference-files`, { silent: true })
}

/**
 * 添加参考文件
 * POST /api/v1/orders/{orderId}/reference-files
 */
export function addReferenceFile(orderId, data) {
    return request.post(`/orders/${orderId}/reference-files`, data)
}

/**
 * 删除参考文件
 * DELETE /api/v1/orders/{orderId}/reference-files/{fileId}
 */
export function deleteReferenceFile(orderId, fileId) {
    return request.delete(`/orders/${orderId}/reference-files/${fileId}`)
}

// ==================== 文件上传底层 API（供 FileUpload 组件使用） ====================

/**
 * 获取上传凭证（模拟 OSS presign）
 * POST /api/v1/files/presign
 * body: { fileType: "design|reference", fileName, fileSize, contentType }
 * 返回: { fileId, uploadUrl, accessUrl }
 */
export function presignFile(data) {
    return request.post('/files/presign', data)
}

/**
 * 执行文件落盘（multipart/form-data）
 * POST /api/v1/files/upload?fileId={fileId}
 * form: file(文件), orderId(可选), taskId(可选)
 * 返回: { fileId, accessUrl }
 * 注意：必须直接用 axios，不要用 request 实例的 post，避免 Content-Type 被覆盖
 */
import axios from 'axios'
export async function uploadFileMultipart(fileId, rawFile, { orderId = null, taskId = null } = {}) {
    const form = new FormData()
    form.append('file', rawFile)
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    if (orderId != null) params.append('orderId', orderId)
    if (taskId != null) params.append('taskId', taskId)

    // baseURL 已在 request.js 设为 '/api/v1'，但 axios 实例不共享，用绝对路径
    const base = request.defaults.baseURL || '/api/v1'
    const resp = await axios.post(`${base}/files/upload?${params.toString()}`, form, {
        headers: {
            // 让浏览器自动设置 Content-Type: multipart/form-data + boundary
        },
        // 复用 JWT token
        transformRequest: [(data, headers) => {
            const token = localStorage.getItem('amoms_token')
            if (token) headers['Authorization'] = `Bearer ${token}`
            return data
        }]
    })
    // 与 request 拦截器保持一致：解包 ResultVO → data
    const res = resp.data
    if (res && typeof res === 'object' && 'code' in res) {
        if (res.code === 'SUCCESS' || res.code === 0) return res.data
        throw new Error(res.message || '上传失败')
    }
    return res
}
