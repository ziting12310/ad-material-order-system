/**
 * 文件模块接口
 * 后端路径：/api/v1/files
 * 流程：presign 获取 fileId → upload 上传文件 → 获取 accessUrl
 */
import request from '@/utils/request'

/**
 * 申请上传凭证（presign）
 * POST /api/v1/files/presign
 * @param {Object} data - { fileName, fileSize, fileType }
 *   - fileType: "reference"（参考图）| "design"（设计稿）
 * @returns {Object} { fileId, uploadUrl, accessUrl, formData }
 */
export function presignFile(data) {
    return request.post('/files/presign', data)
}

/**
 * 上传文件到本地存储
 * POST /api/v1/files/upload
 * @param {string} fileId - presign 返回的 fileId
 * @param {File} file - 文件对象
 * @param {Object} [options] - 可选 { orderId, taskId, onProgress }
 * @returns {Object} { fileId, accessUrl }
 */
export function uploadFile(fileId, file, options = {}) {
    const formData = new FormData()
    formData.append('file', file)
    if (options.orderId != null) formData.append('orderId', options.orderId)
    if (options.taskId != null) formData.append('taskId', options.taskId)

    // 注意：不要手动设置 Content-Type: multipart/form-data
    // 浏览器会自动设置并带上正确的 boundary
    return request.post('/files/upload', formData, {
        params: { fileId },
        onUploadProgress: options.onProgress
    })
}

/**
 * 获取文件访问 URL（仅供展示，实际加载请用 fetchFileBlob）
 * 注：直接使用此 URL 时浏览器不会携带 Authorization，依赖后端匿名预览放行。
 * @param {string} fileId
 * @returns {string}
 */
export function getFileUrl(fileId) {
    return `/api/v1/files/${fileId}`
}

/**
 * 以 Blob 形式下载文件（带 Authorization，走正常权限校验）
 * GET /api/v1/files/{fileId}
 *
 * 为什么不用直接 URL + img/a 标签：
 * 1. 浏览器 img/a 标签请求不带 Authorization，依赖匿名放行，跨环境不稳定
 * 2. 权限校验必须走后端正常的数据范围逻辑（BL/AM/DS）
 * 3. 拿到 Blob 后用 URL.createObjectURL 生成本地 URL，预览和下载都可用
 *
 * @param {string} fileId - 文件 ID
 * @param {function} [onProgress] - 进度回调 (percent: number) => void
 * @returns {Promise<Blob>} 文件 Blob
 */
export function fetchFileBlob(fileId, onProgress) {
    return request.get(`/files/${encodeURIComponent(fileId)}`, {
        responseType: 'blob',
        onDownloadProgress: onProgress
            ? (e) => {
                if (e.total) {
                    onProgress(Math.round((e.loaded / e.total) * 100))
                }
            }
            : undefined
    })
}
