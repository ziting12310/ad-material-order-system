<template>
  <!--
    归档查询页
    功能：筛选 + 归档订单列表 + 游标分页 + 共用 OrderDetailDrawer 详情抽屉
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
            placeholder="公司名称"
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
        <el-form-item label="物料截止">
          <el-date-picker
            v-model="filters.taskDeadlineRange"
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
          <!-- 已归档公司数：与首页【已归档】卡片共用同一统计接口与筛选条件 -->
          <span class="table-count">
            已归档公司 {{ archivedCompanyCount }} 家 · 共 {{ tableData.length }} 条
          </span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="pagedTableData"
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
        <el-table-column label="物料类型" width="100" align="center">
          <template #default="{ row }">
            {{ getMaterialLabel(row.materialType) }}
          </template>
        </el-table-column>
        <el-table-column label="物料数量" width="100" align="center">
          <template #default="{ row }">
            <span class="material-count">
              {{ (row.tasks && row.tasks.length) || row.taskCount || 0 }} 项
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.archiveStatus === '已归档' ? 'success' : 'info'" effect="light" round size="small">
              {{ row.archiveStatus || '未归档' }}
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

      <!-- 分页（前端本地分页） -->
      <div class="load-more-wrap">
        <el-empty
          v-if="!loading && tableData.length === 0"
          :description="hasFilters ? '暂无符合条件的数据' : '暂无数据'"
          :image-size="100"
        />
        <ClientPagination
          v-else
          :current-page="page"
          :page-size="archivePageSize"
          :total="archiveTotal"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉：复用 OrderDetailDrawer（与客户经理工作台、设计师工作台完全一致） -->
    <OrderDetailDrawer
      v-model="detailDrawerVisible"
      :order-id="currentViewOrderId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { listArchivedOrders, countArchived } from '@/api/audit'
import { useClientPagination } from '@/composables/useClientPagination'
import ClientPagination from '@/components/common/ClientPagination.vue'
import OrderDetailDrawer from '@/views/order/OrderDetailDrawer.vue'
import { MATERIAL_OPTIONS, getMaterialLabel } from '@/constants'

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
  deliveryDateRange: [],
  taskDeadlineRange: []
})

const hasFilters = computed(() => {
  return !!(filters.keyword || filters.materialType ||
    (filters.deliveryDateRange && filters.deliveryDateRange.length === 2) ||
    (filters.taskDeadlineRange && filters.taskDeadlineRange.length === 2))
})

// ---------- 表格 & 前端本地分页 ----------
const loading = ref(false)
const tableData = ref([])
// 已归档公司数（与首页【已归档】卡片同一统计口径）
const archivedCompanyCount = ref(0)

const {
  pagedData: pagedTableData,
  currentPage: page,
  total: archiveTotal,
  pageSize: archivePageSize,
  resetPage: resetArchivePage,
  handlePageChange
} = useClientPagination(tableData)

// ---------- 详情抽屉（共用组件） ----------
const detailDrawerVisible = ref(false)
const currentViewOrderId = ref(null)

// ---------- 工具函数 ----------
function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
    + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// 归档筛选参数（列表与统计接口共用，保证筛选条件完全一致）
function buildFilterParams() {
  const params = {}
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.materialType) params.materialType = filters.materialType
  if (filters.deliveryDateRange && filters.deliveryDateRange.length === 2) {
    params.deliveredDateFrom = filters.deliveryDateRange[0]
    params.deliveredDateTo = filters.deliveryDateRange[1]
  }
  if (filters.taskDeadlineRange && filters.taskDeadlineRange.length === 2) {
    params.taskDeadlineFrom = filters.taskDeadlineRange[0]
    params.taskDeadlineTo = filters.taskDeadlineRange[1]
  }
  return params
}

// ---------- 数据加载（循环游标接口拉全量） ----------
async function fetchList() {
  loading.value = true
  try {
    const baseParams = buildFilterParams()
    const allRows = []
    let cursor = null
    let hasMore = true
    while (hasMore) {
      const params = { ...baseParams, limit: PAGE_SIZE }
      if (cursor) params.cursor = cursor
      const res = await listArchivedOrders(params)
      allRows.push(...(res.list || []))
      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }
    tableData.value = allRows
    resetArchivePage()

    // 同一套筛选条件 + 同一统计接口（按公司去重），与首页卡片数字保持一致
    try {
      const count = await countArchived(baseParams)
      archivedCompanyCount.value = Number(count) || 0
    } catch (e) {
      archivedCompanyCount.value = 0
    }
  } catch (e) {
    tableData.value = []
    archivedCompanyCount.value = 0
    resetArchivePage()
  } finally {
    loading.value = false
  }
}

// ---------- 搜索 / 重置 ----------
function handleSearch() { fetchList() }

function handleReset() {
  filters.keyword = ''
  filters.materialType = ''
  filters.deliveryDateRange = []
  filters.taskDeadlineRange = []
  fetchList()
}

// ---------- 查看详情 ----------
function handleViewDetail(row) {
  currentViewOrderId.value = row.id
  detailDrawerVisible.value = true
}

onMounted(() => { fetchList() })
</script>

<style scoped>
.archive-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

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

.filter-card {
  border-radius: var(--border-radius);
}

.filter-card :deep(.el-card__body) {
  padding: 20px 24px 4px;
}

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

.load-more-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  border-top: 1px solid var(--gray-100);
  min-height: 60px;
}
</style>
