/**
 * 全局常量 — 枚举、字典、选项列表
 * 所有页面共用，避免重复定义
 */

// 物料类型
export const MATERIAL_OPTIONS = [
    { value: 'POSTER', label: '海报' },
    { value: 'FLYER', label: '宣传单' },
    { value: 'BUSINESS_CARD', label: '名片' },
    { value: 'BROCHURE', label: '画册' },
    { value: 'BANNER', label: '喷绘' },
    { value: 'OTHER', label: '其他' }
]

// 订单状态
export const ORDER_STATUS_MAP = {
    CREATED:      { label: '待分派', type: 'info' },
    ASSIGNED:     { label: '已分派', type: 'primary' },
    DESIGNING:    { label: '设计中', type: 'warning' },
    WAIT_APPROVE: { label: '待审核', type: 'warning' },
    APPROVED:     { label: '已定稿', type: 'success' },
    DELIVERED:    { label: '已交付', type: 'success' },
    CANCELLED:    { label: '已作废', type: 'danger' }
}

// 任务状态（订单状态子集）
export const TASK_STATUS_MAP = {
    CREATED:      { label: '待分派', type: 'info' },
    ASSIGNED:     { label: '已分派', type: 'primary' },
    DESIGNING:    { label: '设计中', type: 'warning' },
    WAIT_APPROVE: { label: '待审核', type: 'warning' },
    APPROVED:     { label: '已定稿', type: 'success' },
    REJECTED:     { label: '已驳回', type: 'danger' }
}

// 用户角色
export const ROLE_MAP = {
    AM: { label: '客户经理', type: '' },
    BL: { label: '业务负责人', type: 'primary' },
    DS: { label: '设计人员', type: 'success' }
}

// 物料类型名称
export function getMaterialLabel(value) {
    const m = MATERIAL_OPTIONS.find(item => item.value === value)
    return m ? m.label : value
}

// 订单状态名称
export function getOrderStatusLabel(status) {
    return ORDER_STATUS_MAP[status]?.label || status
}

// 订单状态标签类型
export function getOrderStatusType(status) {
    return ORDER_STATUS_MAP[status]?.type || 'info'
}

// 任务状态名称
export function getTaskStatusLabel(status) {
    return TASK_STATUS_MAP[status]?.label || status
}

// 任务状态标签类型
export function getTaskStatusType(status) {
    return TASK_STATUS_MAP[status]?.type || 'info'
}

// 角色名称
export function getRoleLabel(role) {
    return ROLE_MAP[role]?.label || role
}

// 订单状态筛选选项
export const ORDER_STATUS_OPTIONS = Object.entries(ORDER_STATUS_MAP).map(([value, item]) => ({
    value,
    label: item.label
}))
