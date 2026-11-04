<template>
  <!--
    订单管理列表页
    功能：筛选 + 表格 + 分页 + 查看详情
  -->
  <div class="order-list">
    <!-- 筛选表单 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filters" inline label-width="80px" @submit.prevent>
        <el-form-item label="订单编号">
          <el-input
            v-model="filters.orderNo"
            placeholder="请输入订单编号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input
            v-model="filters.customerName"
            placeholder="请输入客户名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="物料类型">
          <el-select
            v-model="filters.materialType"
            placeholder="全部类型"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="item in materialOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="filters.dateRange"
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

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 500 }"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="orderNo" label="订单编号" width="160">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column prop="contactName" label="联系人" width="110" />
        <el-table-column prop="materialType" label="物料类型" width="110">
          <template #default="{ row }">
            {{ getMaterialLabel(row.materialType) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryDate" label="交付日期" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-loading="detailLoading" class="detail-content">
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="订单编号">
            <span class="mono-text">{{ detail.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ detail.customerName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ detail.contactName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ detail.contactPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="物料类型">
            {{ getMaterialLabel(detail.materialType) }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ detail.quantity }}
          </el-descriptions-item>
          <el-descriptions-item label="规格说明" :span="2">
            {{ detail.specification || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="交付日期">
            {{ detail.deliveryDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(detail.status)" effect="light" round size="small">
              {{ getStatusLabel(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ detail.remark || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDateTime(detail.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { listOrders, getOrderDetail } from '@/api/order'

// ---------- 枚举常量 ----------
const materialOptions = [
  { value: 'POSTER', label: '海报' },
  { value: 'FLYER', label: '宣传单' },
  { value: 'BUSINESS_CARD', label: '名片' },
  { value: 'BROCHURE', label: '画册' },
  { value: 'BANNER', label: '喷绘' },
  { value: 'OTHER', label: '其他' }
]

const STATUS_MAP = {
  CREATED:      { label: '待分派', type: 'info' },
  ASSIGNED:     { label: '已分派', type: 'primary' },
  DESIGNING:    { label: '设计中', type: 'warning' },
  WAIT_APPROVE: { label: '待定稿', type: 'warning' },
  APPROVED:     { label: '已定稿', type: 'success' },
  DELIVERED:    { label: '已交付', type: 'success' },
  CANCELLED:    { label: '已作废', type: 'info' }
}

const getMaterialLabel = v => {
  const m = materialOptions.find(item => item.value === v)
  return m ? m.label : v
}
const getStatusLabel = s => (STATUS_MAP[s] ? STATUS_MAP[s].label : s)
const statusTagType = s => (STATUS_MAP[s] ? STATUS_MAP[s].type : 'info')

// ---------- 筛选 ----------
const filters = reactive({
  orderNo: '',
  customerName: '',
  materialType: '',
  dateRange: []
})

// ---------- 表格 & 分页 ----------
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// ---------- 详情 ----------
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

// ---------- 数据加载 ----------
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.pageSize
    }
    if (filters.orderNo) params.orderNo = filters.orderNo
    if (filters.customerName) params.customerName = filters.customerName
    if (filters.materialType) params.materialType = filters.materialType
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }

    const res = await listOrders(params)
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    // 接口不通时用 mock 数据
    generateMockData()
  } finally {
    loading.value = false
  }
}

// 模拟数据
function generateMockData() {
  const statuses = ['CREATED', 'ASSIGNED', 'DESIGNING', 'WAIT_APPROVE', 'APPROVED']
  const customers = ['可口可乐', '宝洁中国', '华为技术', '小米科技', '阿里巴巴', '腾讯科技', '京东集团', '美团']
  const materials = ['POSTER', 'FLYER', 'BUSINESS_CARD', 'BROCHURE', 'BANNER']
  const list = []
  for (let i = 1; i <= pagination.pageSize; i++) {
    const idx = (pagination.page - 1) * pagination.pageSize + i
    list.push({
      id: idx,
      orderNo: 'ORD-2026-' + String(idx).padStart(5, '0'),
      customerName: customers[idx % customers.length],
      contactName: ['张三', '李四', '王五', '赵六'][idx % 4],
      contactPhone: '138' + String(10000000 + idx).slice(-8),
      materialType: materials[idx % materials.length],
      quantity: 100 + idx * 50,
      specification: 'A3尺寸 双面印刷 300g铜版纸',
      status: statuses[idx % statuses.length],
      deliveryDate: '2026-11-15',
      createdAt: '2026-11-0' + ((idx % 9) + 1) + ' 10:' + String((idx * 3) % 60).padStart(2, '0') + ':00',
      remark: '加急处理，注意颜色校准'
    })
  }
  tableData.value = list
  pagination.total = 86
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  filters.orderNo = ''
  filters.customerName = ''
  filters.materialType = ''
  filters.dateRange = []
  pagination.page = 1
  fetchList()
}

function handlePageChange(page) {
  pagination.page = page
  fetchList()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  fetchList()
}

// ---------- 详情 ----------
async function viewDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getOrderDetail(row.id)
    detail.value = res || {}
  } catch (e) {
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

// ---------- 工具 ----------
function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

onMounted(fetchList)
</script>

<style scoped>
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.order-no {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
  font-size: 13px;
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--gray-100);
}

.detail-content {
  padding: 8px 0;
}
</style>
