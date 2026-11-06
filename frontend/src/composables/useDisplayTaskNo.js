/**
 * 全局工具：给任务行数组算 displayTaskNo（公司名_NNN 格式）
 *
 * 用法：
 *   import { applyDisplayTaskNo } from '@/composables/useDisplayTaskNo'
 *   const tasksWithNo = applyDisplayTaskNo(tasks, { customerKey: 'customerName' })
 *
 * 输出示例：
 *   [
 *     { customerName: '恒信汽车', createdAt: '2026-10-01', ..., displayTaskNo: '恒信汽车_001' },
 *     { customerName: '恒信汽车', createdAt: '2026-10-03', ..., displayTaskNo: '恒信汽车_002' },
 *     { customerName: '星光传媒', createdAt: '2026-09-20', ..., displayTaskNo: '星光传媒_001' },
 *   ]
 */

/**
 * 给一批任务行追加 displayTaskNo 字段（**就地修改 + 返回同一数组**）
 *
 * @param {Array} tasks - 已 flatten 的任务行数组，每行必须带 customerName
 *                        字段（或由 options.customerKey 指定的字段）
 * @param {Object} [options]
 * @param {string} [options.customerKey='customerName'] - 公司名字段
 * @param {string} [options.sortBy='createdAt']          - 同公司内排序字段
 * @returns {Array} 同一数组（副作用式），每行多一个 displayTaskNo
 */
export function applyDisplayTaskNo(tasks, options = {}) {
  if (!tasks || !Array.isArray(tasks) || tasks.length === 0) return tasks

  const customerKey = options.customerKey || 'customerName'
  const sortBy = options.sortBy || 'createdAt'

  // 先按公司名 + 时间稳定排序 —— 同公司的排在一起、按创建先后
  tasks.sort((a, b) => {
    const nameA = a[customerKey] || ''
    const nameB = b[customerKey] || ''
    if (nameA !== nameB) return nameA.localeCompare(nameB, 'zh-Hans-CN')
    const timeA = new Date(a[sortBy] || 0).getTime()
    const timeB = new Date(b[sortBy] || 0).getTime()
    return timeA - timeB
  })

  // 分组计数 → 分配序号
  const companyCount = {}
  for (const task of tasks) {
    const key = task[customerKey] || '未知公司'
    companyCount[key] = (companyCount[key] || 0) + 1
    task.displayTaskNo = key + '_' + String(companyCount[key]).padStart(3, '0')
  }

  return tasks
}

/**
 * 把订单列表 flatten 成任务行数组，每行带 displayTaskNo
 *
 * @param {Array} orders - 订单列表（OrderListItemVO[]），每个订单含 tasks[] 子数组
 * @returns {Array} 任务行数组，每行多 orderId/orderNo/customerName/displayTaskNo
 */
export function flattenOrdersWithTaskNo(orders) {
  const tasks = []
  if (orders && Array.isArray(orders)) {
    for (const order of orders) {
      if (order.status === 'CANCELLED') continue
      if (!order.tasks || order.tasks.length === 0) continue
      for (const task of order.tasks) {
        tasks.push({
          ...task,
          orderId: order.id,
          orderNo: order.orderNo,
          customerName: order.customerName || task.customerName || '未知公司'
        })
      }
    }
  }
  return applyDisplayTaskNo(tasks)
}
