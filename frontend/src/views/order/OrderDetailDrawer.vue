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
            <el-descriptions-item label="客户名称">
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
            <el-table-column label="任务编号" width="130">
              <template #default="{ row }">
                <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
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
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewTask(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 参考文件 -->
        <el-tab-pane label="参考文件" name="files">
          <el-table
            :data="detail.referenceFiles || []"
            border
            size="default"
            class="file-table"
          >
            <el-table-column label="文件名" min-width="200">
              <template #default="{ row }">
                <el-icon class="file-icon"><Document /></el-icon>
                <span class="file-name">{{ row.fileName || row.name || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100" align="right">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize || row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.uploadedAt || row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="downloadFile(row)"
                >
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="!detail.referenceFiles || detail.referenceFiles.length === 0"
            description="暂无参考文件"
            :image-size="80"
          />
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
                {{ row.description || row.remark || '—' }}
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
          <el-timeline class="log-timeline">
            <el-timeline-item
              v-for="(log, index) in detail.operationLogs || []"
              :key="log.id || index"
              :timestamp="formatDateTime(log.operatedAt || log.createdAt)"
              :type="logTypeColor(log.action)"
              placement="top"
            >
              <div class="log-item">
                <div class="log-title">
                  <span class="log-action">{{ log.actionName || log.action }}</span>
                  <span class="log-operator">{{ log.operatorName || log.operator || '系统' }}</span>
                </div>
                <div v-if="log.detail || log.remark" class="log-detail">
                  {{ log.detail || log.remark }}
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
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api'
import {
  MATERIAL_OPTIONS,
  getOrderStatusLabel,
  getOrderStatusType,
  getMaterialLabel,
  TASK_STATUS_MAP
} from '@/constants'

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

const emit = defineEmits(['update:modelValue'])

// ---------- 计算属性 ----------
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

// ---------- 状态 ----------
const loading = ref(false)
const activeTab = ref('basic')
const detail = ref({})

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

// ---------- 日志颜色 ----------
function logTypeColor(action) {
  const map = {
    CREATE: 'primary',
    UPDATE: 'warning',
    DELETE: 'danger',
    APPROVE: 'success',
    REJECT: 'danger',
    ASSIGN: 'primary',
    DELIVER: 'success',
    CANCEL: 'danger'
  }
  if (!action) return 'info'
  const upper = String(action).toUpperCase()
  for (const key of Object.keys(map)) {
    if (upper.includes(key)) return map[key]
  }
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

function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
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

function handleOpen() {
  activeTab.value = 'basic'
  detail.value = {}
  if (props.orderId) {
    fetchDetail()
  }
}

// ---------- 操作 ----------
function viewTask(row) {
  ElMessage.info(`查看任务：${row.taskNo || row.id}`)
}

function downloadFile(row) {
  if (row.fileUrl || row.url) {
    window.open(row.fileUrl || row.url, '_blank')
  } else {
    ElMessage.info('文件下载功能开发中')
  }
}

function previewVersion(row) {
  if (row.previewUrl || row.fileUrl || row.url) {
    window.open(row.previewUrl || row.fileUrl || row.url, '_blank')
  } else {
    ElMessage.info('预览功能开发中')
  }
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
.file-table,
.version-table {
  margin-top: 12px;
  --el-table-header-bg-color: var(--gray-50);
  --el-table-border-color: var(--gray-200);
}

.file-icon {
  margin-right: 6px;
  color: var(--primary-color);
  vertical-align: -2px;
}

.file-name {
  color: var(--gray-800);
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
  margin-bottom: 8px;
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

.feedback-content {
  color: var(--gray-700);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

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
}

.log-action {
  font-weight: 500;
}

.log-operator {
  font-size: 12px;
  color: var(--gray-500);
}

.log-detail {
  margin-top: 4px;
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.5;
}
</style>
