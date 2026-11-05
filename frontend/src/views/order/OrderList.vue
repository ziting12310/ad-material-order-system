<template>
  <div class="order-list-page">
    <!-- 顶部栏 -->
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
      <div class="header-actions">
        <el-alert
          v-if="isBL"
          type="info"
          :closable="false"
          show-icon
          title="待分派提醒"
          class="bl-reminder"
        />
        <el-button v-if="isAM" type="primary" :icon="Plus" @click="handleCreate">
          新建订单
        </el-button>
      </div>
    </div>

    <!-- 筛选卡片 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filters" inline label-width="72px" @submit.prevent>
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
        :data="pagedData"
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
        <el-table-column label="操作" :width="actionColWidth" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              查看详情
            </el-button>
            <el-button
              v-if="isAM && row.status === 'CREATED'"
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <!-- 删除：仅客户经理可见，且只对待分派（CREATED）订单展示 -->
            <el-button
              v-if="isAM && row.status === 'CREATED'"
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

      <!-- 分页 -->
      <div class="load-more-wrap">
        <el-empty
          v-if="!loading && tableData.length === 0"
          :description="emptyText"
          :image-size="100"
        />
        <ClientPagination
          v-else
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
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

    <!-- 分派弹窗 -->
    <el-dialog
      v-model="assignDialog.visible"
      title="分派订单"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="assignFormRef"
        :model="assignForm"
        :rules="assignRules"
        label-width="80px"
      >
        <el-form-item label="订单编号">
          <span class="mono-text">{{ assignDialog.row?.orderNo || '—' }}</span>
        </el-form-item>
        <el-form-item label="公司名称">
          {{ assignDialog.row?.customerName || '—' }}
        </el-form-item>
        <el-form-item label="选择设计师" prop="designerId">
          <el-select
            v-model="assignForm.designerId"
            placeholder="请选择设计师"
            filterable
            style="width: 100%"
            :loading="designerLoading"
          >
            <el-option
              v-for="d in designerList"
              :key="d.id"
              :label="d.name + ' (' + (d.username || d.id) + ')'"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分派备注" prop="assignRemark">
          <el-input
            v-model="assignForm.assignRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入分派备注（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="assignDialog.loading" @click="handleAssignSubmit">
          确认分派
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { listOrders, deleteOrder, listDesigners, assignTask, assignOrder, getOrderDetail } from '@/api'
import { useClientPagination } from '@/composables/useClientPagination'
import ClientPagination from '@/components/common/ClientPagination.vue'
import {
  MATERIAL_OPTIONS,
  ORDER_STATUS_OPTIONS,
  getOrderStatusLabel,
  getOrderStatusType
} from '@/constants'
import { useUserStore } from '@/stores/user'
import OrderCreateDialog from './OrderCreateDialog.vue'
import OrderDetailDrawer from './OrderDetailDrawer.vue'

const userStore = useUserStore()
const isAM = computed(() => userStore.userInfo?.role === 'AM')
const isBL = computed(() => userStore.userInfo?.role === 'BL')
const isDS = computed(() => userStore.userInfo?.role === 'DS')

// 是否有筛选条件
const hasFilters = computed(() => {
  return !!(filters.keyword || filters.materialType || filters.status ||
    (filters.deliveryDateRange && filters.deliveryDateRange.length === 2))
})

// 操作列宽度
const actionColWidth = computed(() => {
  if (isAM.value) return 200   // AM：查看 + 编辑 + 删除（删除仅待分派订单可见）
  if (isBL.value) return 120   // BL：仅查看详情（只读）
  return 120                   // DS / 其他：查看
})

// 空状态文案
const emptyText = computed(() => {
  if (hasFilters.value) return '暂无符合条件的数据'
  if (isAM.value) return '还没有订单，点击右上角"新建订单"开始吧'
  if (isBL.value) return '暂无订单数据'
  return '暂无数据'
})

// ---------- 常量 ----------
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

// ---------- 表格 & 前端本地分页 ----------
const loading = ref(false)
const tableData = ref([])   // 全量订单（后端一次性拉完，不新增后端分页接口）

const { pagedData, currentPage, total, pageSize, resetPage, handlePageChange } =
  useClientPagination(tableData)

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

// ---------- 数据加载（循环游标接口拉全量，后端不改） ----------
async function fetchList() {
  loading.value = true
  try {
    const allRows = []
    let cursor = null
    let hasMore = true

    while (hasMore) {
      const params = { limit: 100 }
      if (cursor) params.cursor = cursor

      // 筛选条件
      if (filters.keyword) params.keyword = filters.keyword
      if (filters.materialType) params.materialType = filters.materialType
      if (filters.status) params.status = filters.status
      if (filters.deliveryDateRange && filters.deliveryDateRange.length === 2) {
        params.deliveryDateFrom = filters.deliveryDateRange[0]
        params.deliveryDateTo = filters.deliveryDateRange[1]
      }

      const res = await listOrders(params)

      allRows.push(...(res.list || []))
      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }

    tableData.value = allRows
    resetPage()   // 数据刷新后回到第一页
  } catch (e) {
    // 错误已由拦截器统一处理
    tableData.value = []
    resetPage()
  } finally {
    loading.value = false
  }
}

// 回到第一页并重新加载（搜索/重置/增删改/分派成功后调用）
function reloadFirstPage() {
  fetchList()
}

// ---------- 搜索 / 重置 ----------
function handleSearch() {
  reloadFirstPage()
}

function handleReset() {
  filters.keyword = ''
  filters.materialType = ''
  filters.status = ''
  filters.deliveryDateRange = []
  reloadFirstPage()
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
  // 前端二次校验：仅待分派（CREATED）订单允许删除
  if (row.status !== 'CREATED') {
    ElMessage.warning('该订单已经分派，不可删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认删除订单 ${row.orderNo}？`,
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
    reloadFirstPage()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

function handleCreateSuccess() {
  // 创建/编辑成功后刷新列表
  reloadFirstPage()
}

// ---------- 分派操作 ----------
const assignDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const assignFormRef = ref(null)
const assignForm = reactive({
  designerId: null,
  assignRemark: '',
  version: null           // 订单级乐观锁，详情接口返回
})
const assignRules = {
  designerId: [
    { required: true, message: '请选择设计师', trigger: 'change' }
  ]
}
const designerList = ref([])
const designerLoading = ref(false)

async function fetchDesigners() {
  designerLoading.value = true
  try {
    const res = await listDesigners()
    designerList.value = res.list || res || []
  } catch (e) {
    designerList.value = []
  } finally {
    designerLoading.value = false
  }
}

async function handleAssign(row) {
  assignDialog.row = row
  assignForm.designerId = null
  assignForm.assignRemark = ''
  assignForm.version = null

  // 打开弹窗前必须拉详情：列表 VO 不含订单 version 和 tasks 实体
  let orderDetail = null
  try {
    assignDialog.loading = true
    orderDetail = await getOrderDetail(row.id)
    if (orderDetail?.version == null) {
      ElMessage.error('后端订单详情未返回 version，无法分派，请检查 OrderDetailVO')
      return
    }
    assignForm.version = orderDetail.version
    // 把真实 tasks 挂到 row 上，供提交时使用
    row.tasks = orderDetail.tasks || []
  } catch (e) {
    ElMessage.error('加载订单详情失败，请重试')
    return
  } finally {
    assignDialog.loading = false
  }

  assignDialog.visible = true
  // 加载设计师列表
  if (designerList.value.length === 0) {
    fetchDesigners()
  }
}

async function handleAssignSubmit() {
  if (!assignFormRef.value) return
  assignFormRef.value.validate(async valid => {
    if (!valid) return

    const row = assignDialog.row
    if (!row) return

    assignDialog.loading = true
    try {
      // 任务级分派：逐个分派，每个 task 带自己的 version
      const tasks = row.tasks || []
      if (tasks.length > 0) {
        const assignPromises = tasks.map(task => {
          if (task.version == null) {
            throw new Error(`任务 ${task.id} 未返回 version，无法分派`)
          }
          return assignTask(row.id, task.id, {
            designerId: assignForm.designerId,
            assignRemark: assignForm.assignRemark,
            version: task.version
          })
        })
        await Promise.all(assignPromises)
      } else {
        // 订单级分派（无任务时）：用订单本体的 version
        if (assignForm.version == null) {
          throw new Error('订单 version 缺失，无法分派')
        }
        await assignOrder(row.id, {
          designerId: assignForm.designerId,
          assignRemark: assignForm.assignRemark,
          version: assignForm.version
        })
      }

      ElMessage.success('分派成功')
      assignDialog.visible = false
      // 刷新列表
      reloadFirstPage()
    } catch (e) {
      // 错误已由拦截器处理
    } finally {
      assignDialog.loading = false
    }
  })
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchList()
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bl-reminder {
  --el-alert-padding-y: 8px;
  --el-alert-padding-x: 12px;
}

.bl-reminder :deep(.el-alert__content) {
  padding: 0;
}

.bl-reminder :deep(.el-alert__title) {
  font-size: 13px;
  line-height: 1.4;
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
