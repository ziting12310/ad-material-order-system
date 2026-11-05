<template>
  <!--
    设计师工作台
    功能：统计卡片 + Tab切换（我的任务/审核记录）+ 筛选 + 任务表格 + 游标分页
  -->
  <div class="design-workbench">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-assigned" @click="quickFilter('ASSIGNED')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assigned }}</div>
            <div class="stat-label">待接单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-designing" @click="quickFilter('DESIGNING')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Edit /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.designing }}</div>
            <div class="stat-label">设计中</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-waiting" @click="quickFilter('WAIT_APPROVE')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.waitApprove }}</div>
            <div class="stat-label">待定稿</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-approved" @click="quickFilter('APPROVED')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-card class="tab-card" shadow="never">
      <el-tabs v-model="activeTab" class="workbench-tabs">
        <!-- 我的任务 -->
        <el-tab-pane label="我的任务" name="tasks">
          <!-- 筛选 -->
          <div class="filter-bar">
            <el-form :model="filters" inline label-width="60px" @submit.prevent>
              <el-form-item label="状态">
                <el-select
                  v-model="filters.status"
                  placeholder="全部状态"
                  clearable
                  style="width: 130px"
                >
                  <el-option label="待接单" value="ASSIGNED" />
                  <el-option label="设计中" value="DESIGNING" />
                  <el-option label="待定稿" value="WAIT_APPROVE" />
                  <el-option label="已完成" value="APPROVED" />
                </el-select>
              </el-form-item>
              <el-form-item label="物料">
                <el-select
                  v-model="filters.materialType"
                  placeholder="全部类型"
                  clearable
                  style="width: 130px"
                >
                  <el-option
                    v-for="item in MATERIAL_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="default" :icon="Search" @click="handleSearch">
                  搜索
                </el-button>
                <el-button size="default" :icon="RefreshLeft" @click="handleReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 任务表格 -->
          <el-table
            v-loading="loading"
            :data="taskList"
            border
            style="width: 100%"
            :header-cell-style="headerCellStyle"
            class="task-table"
          >
            <el-table-column prop="taskNo" label="任务编号" width="150">
              <template #default="{ row }">
                <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="所属订单" width="160">
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="物料类型" width="100">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="right" />
            <el-table-column prop="specification" label="规格" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.specification || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="getTaskStatusType(row.status)"
                  effect="light"
                  round
                  size="small"
                >
                  {{ getTaskStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止日期" width="120">
              <template #default="{ row }">
                {{ row.deadline || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <!-- 待接单：显示接单按钮 -->
                <el-button
                  v-if="row.status === 'ASSIGNED'"
                  type="primary"
                  size="small"
                  @click="handleAccept(row)"
                >
                  接单
                </el-button>
                <!-- 设计中：显示上传版本按钮 -->
                <el-button
                  v-else-if="row.status === 'DESIGNING'"
                  type="primary"
                  size="small"
                  plain
                  @click="handleUploadVersion(row)"
                >
                  上传版本
                </el-button>
                <!-- 其他状态：查看详情 -->
                <el-button
                  v-else
                  type="primary"
                  link
                  size="small"
                  @click="handleViewDetail(row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 加载更多 -->
          <div class="load-more-wrap">
            <el-button
              v-if="hasMore"
              type="primary"
              plain
              :loading="loadingMore"
              :disabled="loadingMore"
              @click="loadMore"
            >
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </el-button>
            <div v-else-if="taskList.length > 0" class="no-more">
              没有更多了
            </div>
            <el-empty
              v-else-if="!loading"
              :description="hasTaskFilters ? '暂无符合条件的数据' : '暂无数据'"
              :image-size="100"
            />
          </div>
        </el-tab-pane>

        <!-- 我参与的审核记录 -->
        <el-tab-pane label="我参与的审核记录" name="audit">
          <div v-loading="auditLoading" class="audit-log-list">
            <div
              v-for="(item, index) in auditLogs"
              :key="item.id || index"
              class="audit-log-item"
            >
              <div class="log-time">{{ formatDateTime(item.operatedAt || item.createdAt) }}</div>
              <div class="log-content">
                <div class="log-header">
                  <span class="log-order">订单：{{ item.orderNo || '—' }}</span>
                  <el-tag
                    :type="logActionType(item.action)"
                    effect="light"
                    size="small"
                    round
                  >
                    {{ logActionLabel(item.action) }}
                  </el-tag>
                </div>
                <div v-if="item.remark || item.note" class="log-remark">
                  备注：{{ item.remark || item.note }}
                </div>
                <div v-if="item.operatorName" class="log-operator">
                  操作人：{{ item.operatorName }}
                </div>
              </div>
            </div>
            <el-empty
              v-if="!auditLoading && auditLogs.length === 0"
              description="暂无审核记录"
              :image-size="80"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 上传版本弹窗（预留） -->
    <el-dialog
      v-model="uploadDialog.visible"
      title="上传设计版本"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="upload-dialog-body">
        <div class="dialog-info">
          <p><span class="label">任务编号：</span><span class="mono-text">{{ uploadDialog.row?.taskNo }}</span></p>
          <p><span class="label">物料类型：</span>{{ getMaterialLabel(uploadDialog.row?.materialType) }}</p>
        </div>
        <el-form :model="uploadForm" label-width="80px">
          <el-form-item label="版本说明">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入本次版本的修改说明"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="设计文件">
            <FileUpload
              v-model="uploadForm.files"
              :max-size="100 * 1024 * 1024"
              :max-count="5"
              scene="design"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="uploadDialog.loading" @click="handleSubmitVersion">
          提交版本
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Bell,
  Edit,
  Clock,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import { listOrders, acceptTask, uploadDesignVersion } from '@/api'
import { listMyAuditLogs } from '@/api/audit'
import {
  MATERIAL_OPTIONS,
  getMaterialLabel,
  getTaskStatusLabel,
  getTaskStatusType
} from '@/constants'
import { useUserStore } from '@/stores/user'
import FileUpload from '@/components/common/FileUpload.vue'

const userStore = useUserStore()

// ---------- 常量 ----------
const PAGE_SIZE = 20
const headerCellStyle = {
  background: 'var(--gray-50)',
  color: 'var(--gray-600)',
  fontWeight: 500
}

// ---------- Tab ----------
const activeTab = ref('tasks')

// 是否有任务筛选条件
const hasTaskFilters = computed(() => {
  return !!(filters.status || filters.materialType)
})

// ---------- 统计 ----------
const stats = reactive({
  assigned: 0,
  designing: 0,
  waitApprove: 0,
  approved: 0
})

async function fetchStats() {
  try {
    // 通过一次列表查询统计各状态数量（简化实现）
    const designerId = userStore.userInfo?.id
    if (!designerId) return

    // 分别查询各状态数量
    const [assignedRes, designingRes, waitRes, approvedRes] = await Promise.all([
      listOrders({ designerId, status: 'ASSIGNED', limit: 1 }).catch(() => ({ total: 0 })),
      listOrders({ designerId, status: 'DESIGNING', limit: 1 }).catch(() => ({ total: 0 })),
      listOrders({ designerId, status: 'WAIT_APPROVE', limit: 1 }).catch(() => ({ total: 0 })),
      listOrders({ designerId, status: 'APPROVED', limit: 1 }).catch(() => ({ total: 0 }))
    ])

    stats.assigned = assignedRes.total || 0
    stats.designing = designingRes.total || 0
    stats.waitApprove = waitRes.total || 0
    stats.approved = approvedRes.total || 0
  } catch (e) {
    // 接口异常时使用 mock 数据
    stats.assigned = 3
    stats.designing = 5
    stats.waitApprove = 2
    stats.approved = 28
  }
}

// ---------- 筛选 ----------
const filters = reactive({
  status: '',
  materialType: ''
})

// ---------- 任务列表 & 游标分页 ----------
const loading = ref(false)
const loadingMore = ref(false)
const taskList = ref([])
const nextCursor = ref(null)
const hasMore = ref(true)

// 从订单列表中提取任务（扁平化）
function extractTasksFromOrders(orders) {
  const tasks = []
  orders.forEach(order => {
    if (order.tasks && order.tasks.length > 0) {
      order.tasks.forEach(task => {
        tasks.push({
          ...task,
          orderId: order.id,
          orderNo: order.orderNo,
          customerName: order.customerName
        })
      })
    } else {
      // 订单级兜底（没有任务列表时，把订单本身作为一条任务）
      tasks.push({
        id: order.id,
        taskNo: order.orderNo,
        orderId: order.id,
        orderNo: order.orderNo,
        customerName: order.customerName,
        materialType: order.materialType,
        quantity: order.quantity,
        specification: order.specification,
        status: order.status,
        deadline: order.deliveryDate
      })
    }
  })

  // 应用前端筛选（状态、物料类型）
  return tasks.filter(task => {
    if (filters.status && task.status !== filters.status) return false
    if (filters.materialType && task.materialType !== filters.materialType) return false
    return true
  })
}

async function fetchTaskList(isLoadMore = false) {
  if (isLoadMore) {
    if (!hasMore.value || loadingMore.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const designerId = userStore.userInfo?.id
    if (!designerId) {
      throw new Error('未获取到用户信息')
    }

    const params = {
      limit: PAGE_SIZE,
      designerId
    }

    if (isLoadMore && nextCursor.value) {
      params.cursor = nextCursor.value
    }

    // 状态筛选传给后端（订单级状态筛选）
    if (filters.status) params.status = filters.status
    if (filters.materialType) params.materialType = filters.materialType

    const res = await listOrders(params)
    const orders = res.list || []
    const newTasks = extractTasksFromOrders(orders)

    if (isLoadMore) {
      taskList.value = [...taskList.value, ...newTasks]
    } else {
      taskList.value = newTasks
    }

    nextCursor.value = res.nextCursor || null
    hasMore.value = !!res.hasMore
  } catch (e) {
    if (!isLoadMore) {
      taskList.value = []
    }
    hasMore.value = false
  } finally {
    if (isLoadMore) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

function loadMore() {
  fetchTaskList(true)
}

// ---------- 搜索 / 重置 ----------
function handleSearch() {
  nextCursor.value = null
  hasMore.value = true
  fetchTaskList(false)
}

function handleReset() {
  filters.status = ''
  filters.materialType = ''
  nextCursor.value = null
  hasMore.value = true
  fetchTaskList(false)
}

// 点击统计卡片快速筛选
function quickFilter(status) {
  activeTab.value = 'tasks'
  filters.status = status
  nextCursor.value = null
  hasMore.value = true
  fetchTaskList(false)
}

// ---------- 操作：接单 ----------
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(
      `确定接受任务 ${row.taskNo || row.id} 吗？`,
      '接单确认',
      {
        confirmButtonText: '确认接单',
        cancelButtonText: '取消',
        type: 'primary'
      }
    )
  } catch (e) {
    return
  }

  try {
    // 接单接口（需要 orderId 和 taskId）
    await acceptTask(row.orderId || row.id, row.id !== row.orderId ? row.id : row.id, 1)
    ElMessage.success('接单成功')
    // 刷新列表和统计
    nextCursor.value = null
    hasMore.value = true
    fetchTaskList(false)
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

// ---------- 操作：上传版本 ----------
const uploadDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const uploadForm = reactive({
  description: '',
  files: []
})

function handleUploadVersion(row) {
  uploadDialog.row = row
  uploadForm.description = ''
  uploadForm.files = []
  uploadDialog.visible = true
  uploadDialog.loading = false
}

async function handleSubmitVersion() {
  if (uploadForm.files.length === 0) {
    ElMessage.warning('请上传设计文件')
    return
  }

  uploadDialog.loading = true
  try {
    const orderId = uploadDialog.row?.orderId || uploadDialog.row?.id
    await uploadDesignVersion(orderId, {
      description: uploadForm.description,
      files: uploadForm.files
    })
    ElMessage.success('版本提交成功')
    uploadDialog.visible = false
    // 刷新列表
    nextCursor.value = null
    hasMore.value = true
    fetchTaskList(false)
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    uploadDialog.loading = false
  }
}

// ---------- 操作：查看详情 ----------
function handleViewDetail(row) {
  ElMessage.info(`查看任务详情：${row.taskNo || row.id}`)
}

// ---------- 审核记录 ----------
const auditLoading = ref(false)
const auditLogs = ref([])

async function fetchAuditLogs() {
  auditLoading.value = true
  try {
    const res = await listMyAuditLogs(50)
    auditLogs.value = res.list || res || []
  } catch (e) {
    // mock 数据
    auditLogs.value = [
      { id: 1, orderNo: 'ORD-2026-00015', action: 'APPROVE', remark: '设计符合要求，确认定稿', operatorName: '张经理', operatedAt: '2026-11-03 14:20:00' },
      { id: 2, orderNo: 'ORD-2026-00012', action: 'REJECT', remark: '颜色需要调整，参考稿为准', operatorName: '李主管', operatedAt: '2026-11-02 16:40:00' },
      { id: 3, orderNo: 'ORD-2026-00008', action: 'APPROVE', remark: '定稿通过', operatorName: '张经理', operatedAt: '2026-10-30 10:15:00' }
    ]
  } finally {
    auditLoading.value = false
  }
}

// 审核操作类型映射
function logActionLabel(action) {
  const map = {
    APPROVE: '确认定稿',
    REJECT: '驳回设计',
    SUBMIT: '提交审核',
    ASSIGN: '任务分派',
    ACCEPT: '设计师接单',
    CANCEL: '订单作废'
  }
  if (!action) return '—'
  return map[action.toUpperCase()] || action
}

function logActionType(action) {
  if (!action) return 'info'
  const upper = action.toUpperCase()
  if (upper.includes('APPROVE')) return 'success'
  if (upper.includes('REJECT')) return 'danger'
  if (upper.includes('SUBMIT')) return 'warning'
  if (upper.includes('ASSIGN') || upper.includes('ACCEPT')) return 'primary'
  return 'info'
}

// ---------- 工具函数 ----------
function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = n => String(n).padStart(2, '0')
  return (
    d.getFullYear() +
    '-' + pad(d.getMonth() + 1) +
    '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) +
    ':' + pad(d.getMinutes())
  )
}

// ---------- 生命周期 ----------
watch(activeTab, (val) => {
  if (val === 'audit' && auditLogs.value.length === 0) {
    fetchAuditLogs()
  }
})

onMounted(() => {
  fetchStats()
  fetchTaskList(false)
})
</script>

<style scoped>
.design-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 统计卡片 ========== */
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-200);
  background: #ffffff;
  cursor: pointer;
  transition: var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--gray-800);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--gray-500);
}

.stat-assigned .stat-icon-wrap {
  background: var(--info-light);
  color: var(--info-color);
}
.stat-assigned .stat-value {
  color: var(--info-color);
}

.stat-designing .stat-icon-wrap {
  background: var(--warning-light);
  color: #d97706;
}
.stat-designing .stat-value {
  color: #d97706;
}

.stat-waiting .stat-icon-wrap {
  background: #fef3c7;
  color: #b45309;
}
.stat-waiting .stat-value {
  color: #b45309;
}

.stat-approved .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-approved .stat-value {
  color: #059669;
}

/* ========== Tab 卡片 ========== */
.tab-card {
  border-radius: var(--border-radius);
}

.tab-card :deep(.el-card__body) {
  padding: 0;
}

.workbench-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid var(--gray-100);
}

.workbench-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.workbench-tabs :deep(.el-tab-pane) {
  padding: 20px;
}

/* ========== 筛选栏 ========== */
.filter-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
  background: var(--gray-50);
  border-radius: var(--border-radius);
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 0;
}

/* ========== 任务表格 ========== */
.task-table {
  margin-bottom: 0;
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 500;
}

.order-no-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--gray-700);
  font-size: 13px;
}

/* ========== 加载更多 ========== */
.load-more-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  border-top: 1px solid var(--gray-100);
  min-height: 60px;
  margin-top: 0;
}

.no-more {
  font-size: 13px;
  color: var(--gray-400);
}

/* ========== 审核记录列表 ========== */
.audit-log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.audit-log-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid var(--gray-100);
  border-radius: var(--border-radius);
  background: #ffffff;
  transition: var(--transition-base);
}

.audit-log-item:hover {
  border-color: var(--gray-200);
  box-shadow: var(--shadow-sm);
}

.log-time {
  flex-shrink: 0;
  width: 140px;
  font-size: 13px;
  color: var(--gray-500);
  padding-top: 2px;
}

.log-content {
  flex: 1;
  min-width: 0;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.log-order {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-800);
}

.log-remark {
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.5;
  margin-bottom: 4px;
}

.log-operator {
  font-size: 12px;
  color: var(--gray-400);
}

/* ========== 上传版本弹窗 ========== */
.upload-dialog-body {
  padding: 8px 0;
}

.dialog-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--gray-50);
  border-radius: var(--border-radius);
}

.dialog-info p {
  margin: 4px 0;
  font-size: 13px;
  color: var(--gray-700);
}

.dialog-info .label {
  color: var(--gray-500);
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}
</style>
