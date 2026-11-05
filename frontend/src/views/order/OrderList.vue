<template>
  <div class="order-list-page">
    <!-- 顶部栏 -->
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        新建订单
      </el-button>
    </div>

    <!-- 筛选卡片 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filters" inline label-width="72px" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="订单号/客户名称"
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
        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in ORDER_STATUS_OPTIONS"
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
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="never">
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
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column label="物料" width="120" align="center">
          <template #default="{ row }">
            <span class="material-count">{{ (row.tasks && row.tasks.length) || row.taskCount || 0 }} 项物料</span>
          </template>
        </el-table-column>
        <el-table-column label="任务进度" width="140" align="center">
          <template #default="{ row }">
            <span class="task-progress">
              <span class="progress-done">{{ getApprovedCount(row) }}</span>
              <span class="progress-sep">/</span>
              <span class="progress-total">{{ getTotalCount(row) }}</span>
              <span class="progress-label">已定稿</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getOrderStatusType(row.status)"
              effect="light"
              round
              size="small"
            >
              {{ getOrderStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryDate" label="交付日期" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
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
          description="暂无订单数据"
          :image-size="100"
        />
      </div>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <OrderCreateDialog
      v-model="createDialogVisible"
      :edit-data="currentEditRow"
      @success="handleCreateSuccess"
    />

    <!-- 详情抽屉 -->
    <OrderDetailDrawer
      v-model="detailDrawerVisible"
      :order-id="currentViewId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { listOrders, deleteOrder } from '@/api'
import {
  MATERIAL_OPTIONS,
  ORDER_STATUS_OPTIONS,
  getOrderStatusLabel,
  getOrderStatusType
} from '@/constants'
import OrderCreateDialog from './OrderCreateDialog.vue'
import OrderDetailDrawer from './OrderDetailDrawer.vue'

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
  status: '',
  deliveryDateRange: []
})

// ---------- 表格 & 游标分页 ----------
const loading = ref(false)
const loadingMore = ref(false)
const tableData = ref([])
const nextCursor = ref(null)
const hasMore = ref(true)

// ---------- 弹窗 / 抽屉 ----------
const createDialogVisible = ref(false)
const currentEditRow = ref(null)
const detailDrawerVisible = ref(false)
const currentViewId = ref(null)

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

function getTotalCount(row) {
  if (row.taskCount != null) return row.taskCount
  if (row.tasks && row.tasks.length) return row.tasks.length
  return 0
}

function getApprovedCount(row) {
  if (row.approvedTaskCount != null) return row.approvedTaskCount
  if (row.tasks && row.tasks.length) {
    return row.tasks.filter(t => t.status === 'APPROVED').length
  }
  return 0
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
    if (filters.status) params.status = filters.status
    if (filters.deliveryDateRange && filters.deliveryDateRange.length === 2) {
      params.deliveryDateFrom = filters.deliveryDateRange[0]
      params.deliveryDateTo = filters.deliveryDateRange[1]
    }

    const res = await listOrders(params)

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
    // 错误已由拦截器统一处理
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

// ---------- 上拉加载 ----------
let scrollContainer = null

function handleScroll() {
  if (!scrollContainer) return
  const { scrollTop, scrollHeight, clientHeight } = scrollContainer
  // 距离底部 100px 时触发加载更多
  if (scrollHeight - scrollTop - clientHeight < 100) {
    if (hasMore.value && !loadingMore.value && !loading.value) {
      loadMore()
    }
  }
}

function setupScrollListener() {
  // 查找内容区域滚动容器
  scrollContainer = document.querySelector('.main-content') ||
                    document.querySelector('.app-main') ||
                    document.querySelector('.layout-content')
  if (scrollContainer) {
    scrollContainer.addEventListener('scroll', handleScroll, { passive: true })
  }
}

function removeScrollListener() {
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleScroll)
    scrollContainer = null
  }
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
  filters.status = ''
  filters.deliveryDateRange = []
  nextCursor.value = null
  hasMore.value = true
  fetchList(false)
}

// ---------- 操作 ----------
function handleCreate() {
  currentEditRow.value = null
  createDialogVisible.value = true
}

function handleEdit(row) {
  currentEditRow.value = row
  createDialogVisible.value = true
}

function handleView(row) {
  currentViewId.value = row.id
  detailDrawerVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除订单 ${row.orderNo} 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (e) {
    return
  }

  try {
    await deleteOrder(row.id)
    ElMessage.success('删除成功')
    // 重新加载列表
    nextCursor.value = null
    hasMore.value = true
    fetchList(false)
  } catch (e) {
    // 错误已由拦截器处理
  }
}

function handleCreateSuccess() {
  // 创建/编辑成功后刷新列表
  nextCursor.value = null
  hasMore.value = true
  fetchList(false)
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchList(false)
  // 延迟绑定滚动监听，确保 DOM 已渲染
  setTimeout(setupScrollListener, 100)
})

onBeforeUnmount(() => {
  removeScrollListener()
})
</script>

<style scoped>
.order-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--gray-900);
}

.filter-card {
  border-radius: var(--border-radius);
}

.filter-card :deep(.el-card__body) {
  padding: 20px 24px 4px;
}

.table-card {
  border-radius: var(--border-radius);
}

.table-card :deep(.el-card__body) {
  padding: 0;
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

.task-progress {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-size: 13px;
}

.progress-done {
  font-weight: 600;
  color: var(--success-color);
  font-size: 14px;
}

.progress-sep {
  color: var(--gray-400);
}

.progress-total {
  font-weight: 500;
  color: var(--gray-700);
}

.progress-label {
  margin-left: 4px;
  color: var(--gray-500);
  font-size: 12px;
}

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
</style>
