<template>
  <!--
    归档查询页
    功能：筛选 + 归档订单列表 + 游标分页 + 详情抽屉（只读）
    所有操作按钮禁用，只读状态
  -->
  <div class="archive-list">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h2 class="page-title">归档查询</h2>
      <div class="page-sub">历史订单归档记录，只读查看</div>
    </div>

    <!-- 筛选卡片 -->
    <el-card class="filter-card" shadow="never">
      <el-form :modenl="filters" inline label-width="80px" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="订单号/公司名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="物料类型">
          <el-select
            v-model="filters.materialType"
            placeholder="全部类型"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in MATERIAL_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="交付日期">
          <el-date-picker
            v-model="filters.deliveryDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">归档订单列表</span>
          <span class="table-count">共加载 {{ tableData.length }} 条</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        :header-cell-style="headerCellStyle"
      >
        <el-table-column prop="orderNo" label="订单号" width="170">
          <template #default="{ row }">
            <span class="order-no-text">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="公司名称" min-width="140" />
        <el-table-column label="物料数量" width="100" align="center">
          <template #default="{ row }">
            <span class="material-count">
              {{ (row.tasks && row.tasks.length) || row.taskCount || 0 }} 项
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success" effect="light" round size="small">
              已归档
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryDate" label="交付日期" width="120" />
        <el-table-column prop="archivedAt" label="归档时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.archivedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
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
        <div v-else-if="tableData.length > 0" class="no-more">
          没有更多了
        </div>
        <el-empty
          v-else-if="!loading"
          :description="hasFilters ? '暂无符合条件的数据' : '暂无数据'"
          :image-size="100"
        />
      </div>
    </el-card>

    <!-- 详情抽屉（只读） -->
    <el-drawer
      v-model="detailDrawer.visible"
      :title="''"
      direction="rtl"
      size="800px"
      :close-on-click-modal="false"
      class="archive-detail-drawer"
      @open="handleDrawerOpen"
    >
      <template #header>
        <div class="drawer-header">
          <div class="header-left">
            <span class="order-no">{{ detail.orderNo || '—' }}</span>
            <el-tag type="success" effect="light" round size="small">已归档</el-tag>
          </div>
          <div class="header-right">
            <el-tag type="info" effect="plain" size="small">只读</el-tag>
          </div>
        </div>
      </template>

      <div v-loading="detailDrawer.loading" class="drawer-body">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="default" class="detail-descriptions">
              <el-descriptions-item label="订单编号">
                <span class="mono-text">{{ detail.orderNo || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="归档状态">
                <el-tag type="success" effect="light" round size="small">已归档</el-tag>
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
              <el-descriptions-item label="归档时间">
                {{ formatDateTime(detail.archivedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="归档人">
                {{ detail.archiverName || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="物料数量">
                {{ (detail.tasks && detail.tasks.length) || detail.taskCount || 0 }} 项
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
              <el-table-column label="规格" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.specification || '—' }}
                </template>
              </el-table-column>
              <el-table-column label="设计师" width="100">
                <template #default="{ row }">
                  {{ row.designerName || '—' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
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
              <el-table-column label="截止日期" width="110">
                <template #default="{ row }">
                  {{ row.deadline || '—' }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-if="!detail.tasks || detail.tasks.length === 0"
              description="暂无物料任务"
              :image-size="80"
            />
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
                    :disabled="!row.fileUrl && !row.url"
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
                    :disabled="!row.fileUrl && !row.url && !row.previewUrl"
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

          <!-- 审核记录 -->
          <el-tab-pane label="审核记录" name="audit">
            <el-timeline v-if="detail.auditLogs && detail.auditLogs.length > 0" class="audit-timeline">
              <el-timeline-item
                v-for="(log, index) in detail.auditLogs"
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
                  <div v-if="log.remark || log.note" class="log-remark">
                    {{ log.remark || log.note }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty
              v-if="!detail.auditLogs || detail.auditLogs.length === 0"
              description="暂无审核记录"
              :image-size="80"
            />
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <el-button type="primary" @click="detailDrawer.visible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Document } from '@element-plus/icons-vue'
import { listArchivedOrders, getArchivedOrder } from '@/api/audit'
import {
  MATERIAL_OPTIONS,
  getMaterialLabel,
  getTaskStatusLabel,
  getTaskStatusType
} from '@/constants'

// ---------- 常量 ----------
const PAGE_SIZE = 20
const headerCellStyle = {
  background: 'var(--gray-50)',
  color: 'var(--gray-600)',
  fontWeight: 500
}

// ---------- 筛选 ----------
const filters = reactive({
  keyword: '',
  materialType: '',
  deliveryDateRange: []
})

// 是否有筛选条件
const hasFilters = computed(() => {
  return !!(filters.keyword || filters.materialType ||
    (filters.deliveryDateRange && filters.deliveryDateRange.length === 2))
})

// ---------- 表格 & 游标分页 ----------
const loading = ref(false)
const loadingMore = ref(false)
const tableData = ref([])
const nextCursor = ref(null)
const hasMore = ref(true)

// ---------- 详情抽屉 ----------
const detailDrawer = reactive({
  visible: false,
  loading: false,
  row: null
})
const detail = ref({})
const activeTab = ref('basic')

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

function logTypeColor(action) {
  const map = {
    CREATE: 'primary',
    UPDATE: 'warning',
    DELETE: 'danger',
    APPROVE: 'success',
    REJECT: 'danger',
    ASSIGN: 'primary',
    ACCEPT: 'primary',
    DELIVER: 'success',
    CANCEL: 'danger',
    ARCHIVE: 'success'
  }
  if (!action) return 'info'
  const upper = String(action).toUpperCase()
  for (const key of Object.keys(map)) {
    if (upper.includes(key)) return map[key]
  }
  return 'info'
}

// ---------- 数据加载（游标分页） ----------
async function fetchList(isLoadMore = false) {
  if (isLoadMore) {
    if (!hasMore.value || loadingMore.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const params = {
      limit: PAGE_SIZE
    }

    // 游标
    if (isLoadMore && nextCursor.value) {
      params.cursor = nextCursor.value
    }

    // 筛选条件
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.materialType) params.materialType = filters.materialType
    if (filters.deliveryDateRange && filters.deliveryDateRange.length === 2) {
      params.deliveryDateFrom = filters.deliveryDateRange[0]
      params.deliveryDateTo = filters.deliveryDateRange[1]
    }

    const res = await listArchivedOrders(params)

    if (isLoadMore) {
      // 追加模式
      if (res.list && res.list.length > 0) {
        tableData.value = [...tableData.value, ...res.list]
      }
    } else {
      // 重置
      tableData.value = res.list || []
    }

    nextCursor.value = res.nextCursor || null
    hasMore.value = !!res.hasMore
  } catch (e) {
    if (!isLoadMore) {
      tableData.value = []
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
  fetchList(true)
}

// ---------- 搜索 / 重置 ----------
function handleSearch() {
  nextCursor.value = null
  hasMore.value = true
  fetchList(false)
}

function handleReset() {
  filters.keyword = ''
  filters.materialType = ''
  filters.deliveryDateRange = []
  nextCursor.value = null
  hasMore.value = true
  fetchList(false)
}

// ---------- 查看详情 ----------
function handleViewDetail(row) {
  detailDrawer.row = row
  detailDrawer.visible = true
}

async function handleDrawerOpen() {
  activeTab.value = 'basic'
  detail.value = {}
  detailDrawer.loading = true
  try {
    const row = detailDrawer.row
    if (row?.id) {
      const res = await getArchivedOrder(row.id)
      detail.value = res || {}
    }
  } catch (e) {
    // 用行数据兜底
    detail.value = { ...detailDrawer.row }
  } finally {
    detailDrawer.loading = false
  }
}

// ---------- 操作（只读，仅下载/预览） ----------
function downloadFile(row) {
  const url = row.fileUrl || row.url
  if (url) {
    window.open(url, '_blank')
  } else {
    ElMessage.info('文件下载链接不可用')
  }
}

function previewVersion(row) {
  const url = row.previewUrl || row.fileUrl || row.url
  if (url) {
    window.open(url, '_blank')
  } else {
    ElMessage.info('预览链接不可用')
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchList(false)
})
</script>

<style scoped>
.archive-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

/* ========== 顶部标题 ========== */
.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--gray-900);
}

.page-sub {
  font-size: 13px;
  color: var(--gray-400);
}

/* ========== 筛选卡片 ========== */
.filter-card {
  border-radius: var(--border-radius);
}

.filter-card :deep(.el-card__body) {
  padding: 20px 24px 4px;
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: var(--border-radius);
}

.table-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--gray-800);
}

.table-count {
  font-size: 13px;
  color: var(--gray-400);
}

.order-no-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 500;
}

.material-count {
  font-size: 13px;
  color: var(--gray-700);
}

/* ========== 加载更多 ========== */
.load-more-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  border-top: 1px solid var(--gray-100);
  min-height: 60px;
}

.no-more {
  font-size: 13px;
  color: var(--gray-400);
}

/* ========== 详情抽屉 ========== */
.archive-detail-drawer :deep(.el-drawer) {
  border-radius: var(--border-radius) 0 0 var(--border-radius);
}

.archive-detail-drawer :deep(.el-drawer__header) {
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
  gap: 12px;
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

/* ========== 审核记录时间线 ========== */
.audit-timeline {
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

.log-remark {
  margin-top: 4px;
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.5;
}
</style>
