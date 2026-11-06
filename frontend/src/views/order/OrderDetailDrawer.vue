<template>
  <el-drawer
    v-model="visible"
    :title="''"
    direction="rtl"
    size="800px"
    :close-on-click-modal="false"
    class="order-detail-drawer"
    @open="handleOpen"
  >
    <template #header>
      <div class="drawer-header">
        <div class="header-left">
          <span class="order-no">{{ detail.orderNo || '—' }}</span>
          <el-tag
            v-if="detail.status"
            :type="getOrderStatusType(detail.status)"
            effect="light"
            round
            size="small"
          >
            {{ getOrderStatusLabel(detail.status) }}
          </el-tag>
        </div>
        <div class="header-right">
          <span class="customer-name">{{ detail.customerName || '—' }}</span>
          <span class="delivery-date">交付：{{ detail.deliveryDate || '—' }}</span>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="drawer-body">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border size="default" class="detail-descriptions">
            <el-descriptions-item label="订单编号">
              <span class="mono-text">{{ detail.orderNo || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag
                v-if="detail.status"
                :type="getOrderStatusType(detail.status)"
                effect="light"
                round
                size="small"
              >
                {{ getOrderStatusLabel(detail.status) }}
              </el-tag>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="公司名称">
              {{ detail.customerName || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ detail.contactName || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ detail.contactPhone || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="交付日期">
              {{ detail.deliveryDate || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="物料数量">
              {{ (detail.tasks && detail.tasks.length) || 0 }} 项
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(detail.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ detail.remark || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- 物料任务 -->
        <el-tab-pane label="物料任务" name="tasks">
          <el-table
            :data="detail.tasks || []"
            border
            size="default"
            class="task-table"
          >
            <el-table-column label="订单号" width="170">
              <template #default>
                <span class="mono-text">{{ detail.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="物料" width="100">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <el-table-column label="数量" width="80" align="right">
              <template #default="{ row }">
                {{ row.quantity || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.status"
                  :type="getTaskStatusType(row.status)"
                  effect="light"
                  round
                  size="small"
                >
                  {{ getTaskStatusLabel(row.status) }}
                </el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="设计师" width="100">
              <template #default="{ row }">
                {{ row.designerName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="截止日期" width="110">
              <template #default="{ row }">
                {{ row.deadline || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="用途说明" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.usageDesc || '—' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 设计版本 -->
        <el-tab-pane label="设计版本" name="versions">
          <el-table
            :data="detail.designVersions || []"
            border
            size="default"
            class="version-table"
          >
            <el-table-column label="版本号" width="100">
              <template #default="{ row }">
                <span class="version-tag">V{{ row.version || row.versionNo || '1' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="上传人" width="100">
              <template #default="{ row }">
                {{ row.uploaderName || row.uploader || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.uploadedAt || row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="版本说明" min-width="200">
              <template #default="{ row }">
                {{ row.versionNote || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="previewVersion(row)"
                >
                  预览
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="!detail.designVersions || detail.designVersions.length === 0"
            description="暂无设计版本"
            :image-size="80"
          />
        </el-tab-pane>

        <!-- 修改意见 -->
        <el-tab-pane label="修改意见" name="feedbacks">
          <!-- 提交意见按钮（仅 WAIT_APPROVE 状态且角色为 AM/BL 时显示） -->
          <div v-if="canSubmitFeedback" class="feedback-toolbar">
            <el-button type="primary" :icon="EditPen" @click="openFeedbackDialog">
              提交意见
            </el-button>
          </div>

          <div class="feedback-list">
            <div
              v-for="(item, index) in detail.feedbacks || []"
              :key="item.id || index"
              class="feedback-item"
            >
              <div class="feedback-header">
                <span class="feedback-author">{{ item.submitterName || item.author || '匿名' }}</span>
                <span class="feedback-time">{{ formatDateTime(item.submittedAt || item.createdAt) }}</span>
                <el-tag
                  v-if="item.status"
                  :type="feedbackStatusType(item.status)"
                  effect="light"
                  size="small"
                  round
                >
                  {{ feedbackStatusLabel(item.status) }}
                </el-tag>
              </div>
              <div v-if="item.version || item.versionNo" class="feedback-version">
                版本：V{{ item.version || item.versionNo }}
              </div>
              <div class="feedback-content">{{ item.content || item.description || '—' }}</div>
            </div>
          </div>
          <el-empty
            v-if="!detail.feedbacks || detail.feedbacks.length === 0"
            description="暂无修改意见"
            :image-size="80"
          />
        </el-tab-pane>

        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="logs">
          <el-timeline v-if="sortedLogs.length > 0" class="log-timeline">
            <el-timeline-item
              v-for="(log, index) in sortedLogs"
              :key="log.id || index"
              :timestamp="formatDateTime(log.operatedAt || log.createdAt)"
              :type="logTypeColor(log.operationType)"
              placement="top"
            >
              <div class="log-item">
                <div class="log-title">
                  <el-tag
                    :type="logTypeColor(log.operationType)"
                    effect="light"
                    size="small"
                    round
                  >
                    {{ log.operationTypeName || '其他' }}
                  </el-tag>
                  <span class="log-operator">{{ log.operatorName || log.operator || '系统' }}</span>
                </div>
                <div v-if="log.detail || log.remark || log.note" class="log-detail">
                  {{ log.detail || log.remark || log.note }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty
            v-if="!detail.operationLogs || detail.operationLogs.length === 0"
            description="暂无操作日志"
            :image-size="80"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 提交修改意见弹窗 -->
    <el-dialog
      v-model="feedbackDialog.visible"
      title="提交修改意见"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="feedbackFormRef"
        :model="feedbackForm"
        :rules="feedbackRules"
        label-width="80px"
      >
        <el-form-item label="订单编号">
          <span class="mono-text">{{ detail.orderNo || '—' }}</span>
        </el-form-item>
        <el-form-item label="选择任务" prop="taskId">
          <el-select
            v-model="feedbackForm.taskId"
            placeholder="请选择意见针对的任务"
            style="width: 100%"
            @change="syncFeedbackTaskVersion"
          >
            <el-option
              v-for="t in detail.tasks || []"
              :key="t.id"
              :label="(t.taskNo || t.id) + ' - ' + getMaterialLabel(t.materialType)"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="意见内容" prop="content">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入修改意见内容（1-500字）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackDialog.loading" @click="handleSubmitFeedback">
          提交
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { getOrderDetail, submitFeedback, listFeedbacks } from '@/api'
import { useUserStore } from '@/stores/user'
import {
  MATERIAL_OPTIONS,
  getOrderStatusLabel,
  getOrderStatusType,
  getMaterialLabel,
  TASK_STATUS_MAP
} from '@/constants'

const userStore = useUserStore()

// ---------- Props / Emits ----------
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  orderId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

// ---------- 计算属性 ----------
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

// ---------- 状态 ----------
const loading = ref(false)
const activeTab = ref('basic')
const detail = ref({})

// ---------- 角色/权限判断 ----------
const canSubmitFeedback = computed(() => {
  const role = userStore.userInfo?.role
  const status = detail.value?.status
  // 提交修改意见 AM/BL 均可（审核职责已从 BL 移交 AM，但意见提交仍保留 BL 权限）
  return status === 'WAIT_APPROVE' && (role === 'AM' || role === 'BL')
})

// ---------- 操作日志按时间倒序排列 ----------
const sortedLogs = computed(() => {
  const logs = detail.value.operationLogs || []
  return [...logs].sort((a, b) => {
    const timeA = new Date(a.operatedAt || a.createdAt).getTime()
    const timeB = new Date(b.operatedAt || b.createdAt).getTime()
    return timeB - timeA
  })
})

// ---------- 任务状态辅助函数 ----------
function getTaskStatusLabel(status) {
  return TASK_STATUS_MAP[status]?.label || status || '—'
}

function getTaskStatusType(status) {
  return TASK_STATUS_MAP[status]?.type || 'info'
}

// ---------- 反馈状态 ----------
function feedbackStatusLabel(status) {
  const map = {
    PENDING: '待处理',
    PROCESSED: '已处理',
    RESOLVED: '已解决'
  }
  return map[status] || status || '—'
}

function feedbackStatusType(status) {
  const map = {
    PENDING: 'warning',
    PROCESSED: 'primary',
    RESOLVED: 'success'
  }
  return map[status] || 'info'
}

// ---------- 操作日志类型映射 ----------
function logActionLabel(action) {
  const map = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    APPROVE: '定稿',
    REJECT: '驳回',
    ASSIGN: '分派',
    ACCEPT: '接单',
    SUBMIT: '提交审核',
    DELIVER: '交付',
    CANCEL: '作废',
    ARCHIVE: '归档',
    FEEDBACK: '意见反馈'
  }
  if (!action) return '其他'
  const upper = String(action).toUpperCase()
  for (const key of Object.keys(map)) {
    if (upper.includes(key)) return map[key]
  }
  return '其他'
}

function logTypeColor(action) {
  if (!action) return 'info'
  const upper = String(action).toUpperCase()
  // 创建：info
  if (upper.includes('CREATE')) return 'info'
  // 分派：primary
  if (upper.includes('ASSIGN')) return 'primary'
  // 接单：warning
  if (upper.includes('ACCEPT')) return 'warning'
  // 定稿：success
  if (upper.includes('APPROVE')) return 'success'
  // 驳回：danger
  if (upper.includes('REJECT')) return 'danger'
  // 交付：success
  if (upper.includes('DELIVER') || upper.includes('ARCHIVE')) return 'success'
  // 作废：danger
  if (upper.includes('CANCEL') || upper.includes('DELETE')) return 'danger'
  // 提交：warning
  if (upper.includes('UPLOAD') || upper.includes('FEEDBACK')) return 'warning'
  // 其他：info
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

// ---------- 数据加载 ----------
async function fetchDetail() {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await getOrderDetail(props.orderId)
    detail.value = res || {}
  } catch (e) {
    // 错误已由拦截器处理
    detail.value = {}
  } finally {
    loading.value = false
  }
}

// ---------- 刷新意见列表 ----------
async function refreshFeedbacks() {
  try {
    const res = await listFeedbacks(props.orderId)
    detail.value.feedbacks = res.list || res || []
  } catch (e) {
    // 保持原状
  }
}

function handleOpen() {
  activeTab.value = 'basic'
  detail.value = {}
  if (props.orderId) {
    fetchDetail()
  }
}

// ---------- 操作 ----------
function previewVersion(row) {
  if (row.previewUrl || row.fileUrl || row.url) {
    window.open(row.previewUrl || row.fileUrl || row.url, '_blank')
  } else {
    ElMessage.info('预览功能开发中')
  }
}

// ========== 提交修改意见 ==========
const feedbackDialog = ref({
  visible: false,
  loading: false
})
const feedbackFormRef = ref(null)
// version 是所选任务的乐观锁版本号（随任务选择自动带出，无需手填）
const feedbackForm = ref({
  taskId: null,
  version: null,
  content: ''
})
const feedbackRules = {
  taskId: [
    { required: true, message: '请选择任务', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入意见内容', trigger: 'blur' },
    { min: 1, max: 500, message: '长度在 1 到 500 个字符', trigger: 'blur' }
  ]
}

function openFeedbackDialog() {
  // 默认选中第一个任务，version 取该任务的乐观锁版本号
  const tasks = detail.value.tasks || []
  feedbackForm.value.taskId = tasks.length > 0 ? tasks[0].id : null
  syncFeedbackTaskVersion()
  feedbackForm.value.content = ''
  feedbackDialog.value.visible = true
  feedbackDialog.value.loading = false
}

// 任务切换时同步乐观锁 version
function syncFeedbackTaskVersion() {
  const tasks = detail.value.tasks || []
  const task = tasks.find(t => t.id === feedbackForm.value.taskId)
  feedbackForm.value.version = task ? task.version : null
}

async function handleSubmitFeedback() {
  if (!feedbackFormRef.value) return
  feedbackFormRef.value.validate(async valid => {
    if (!valid) return
    // 后端 OrderFeedbackCreateRequest 要求 orderId + taskId + version(任务乐观锁)
    if (feedbackForm.value.version == null) {
      ElMessage.error('所选任务缺少乐观锁 version，请刷新详情后重试')
      return
    }

    feedbackDialog.value.loading = true
    try {
      await submitFeedback({
        orderId: props.orderId,
        taskId: feedbackForm.value.taskId,
        version: feedbackForm.value.version,
        content: feedbackForm.value.content
      })
      ElMessage.success('意见提交成功')
      feedbackDialog.value.visible = false
      // 刷新意见列表 + 订单详情
      refreshFeedbacks()
      fetchDetail()
      emit('refresh')
    } catch (e) {
      // 错误已由拦截器处理
    } finally {
      feedbackDialog.value.loading = false
    }
  })
}
</script>

<style scoped>
.order-detail-drawer :deep(.el-drawer) {
  border-radius: var(--border-radius) 0 0 var(--border-radius);
}

.order-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 0;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-no {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--gray-600);
}

.customer-name {
  font-weight: 500;
  color: var(--gray-800);
}

.delivery-date {
  color: var(--gray-500);
}

.drawer-body {
  padding: 0 20px 20px;
  height: calc(100vh - 65px);
  overflow-y: auto;
}

.detail-tabs {
  margin-top: 8px;
}

.detail-descriptions {
  margin-top: 12px;
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
  font-size: 13px;
}

.task-table,
.version-table {
  margin-top: 12px;
  --el-table-header-bg-color: var(--gray-50);
  --el-table-border-color: var(--gray-200);
}

.version-tag {
  display: inline-block;
  padding: 2px 8px;
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--border-radius-sm);
  font-size: 12px;
  font-weight: 600;
  font-family: 'SF Mono', Monaco, monospace;
}

/* ========== 修改意见 ========== */
.feedback-toolbar {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--gray-100);
}

.feedback-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-item {
  padding: 14px 16px;
  background: var(--gray-50);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-100);
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.feedback-author {
  font-weight: 600;
  color: var(--gray-800);
  font-size: 14px;
}

.feedback-time {
  font-size: 12px;
  color: var(--gray-500);
  flex: 1;
}

.feedback-version {
  font-size: 12px;
  color: var(--primary-color);
  margin-bottom: 6px;
  font-family: 'SF Mono', Monaco, monospace;
}

.feedback-content {
  color: var(--gray-700);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* ========== 操作日志时间线 ========== */
.log-timeline {
  margin-top: 12px;
  padding-left: 8px;
}

.log-item {
  padding-bottom: 4px;
}

.log-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--gray-800);
  margin-bottom: 6px;
}

.log-action {
  font-weight: 500;
}

.log-operator {
  font-size: 12px;
  color: var(--gray-500);
}

.log-detail {
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.5;
  padding-left: 2px;
}
</style>
