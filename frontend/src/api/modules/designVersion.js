/**
 * 设计稿版本接口模块 — 对齐 API.md §4.1
 * 注意：modules 下统一使用 import request from '../request'
 *
 * 首版与新版共用同一接口，versionNo 由后端生成（V1/V2/V3...）
 * 上传后订单状态 DESIGNING → WAIT_APPROVE
 */
import request from '../request'

/**
 * 上传设计稿（DS）
 * POST /api/v1/design-versions
 * @param {Object} data { orderId, fileId, fileName, fileSize, fileType, versionNote, version }
 */
export const uploadDesignVersion = data => request.post('/design-versions', data)
