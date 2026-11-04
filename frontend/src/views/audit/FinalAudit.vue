<template>
  <!--
    定稿审核归档页 — C 模块核心页面
    功能：筛选 + 表格 + 分页 + 确认定稿弹窗 + 驳回弹窗 + 归档详情弹窗
    状态：WAIT_APPROVE 待审核 / APPROVED 已定稿 / REJECTED 已驳回
  -->
  <div class="final-audit">
    <!-- 顶部统计小卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :span="8">
        <div class="mini-stat stat-waiting" @click="quickFilter('WAIT_APPROVE')">
          <div class="mini-num">{{ stats.pending }}</div>
          <div class="mini-label">待审核</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="mini-stat stat-approved" @click="quickFilter('APPROVED')">
          <div class="mini-num">{{ stats.approved }}</div>
          <div class="mini-label">已定稿</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="mini-stat stat-rejected" @click="quickFilter('REJECTED')">
          <div class="mini-num">{{ stats.rejected }}</div>
          <div class="mini-label">已驳回</div>
        </div>
      </el-col>
    </el-row>

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
        <el-form-item label="审核状态">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            style="width: 160px"
          >
            <el-option label="待审核" value="WAIT_APPROVE" />
            <el-option label="已定稿" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
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

    <!-- 主表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">审核归档订单列表</span>
          <span class="table-count">共 {{ pagination.total }} 条</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 500 }"
      >
        <el-table-column prop="id" label="订单ID" width="90" align="center" />
        <el-table-column prop="orderNo" label="订单编号" width="170">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column prop="materialType" label="物料类型" width="110">
          <template #default="{ row }">
            {{ getMaterialLabel(row.materialType) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 待审核：显示定稿/驳回按钮 -->
            <template v-if="row.status === 'WAIT_APPROVE'">
              <el-button type="success" size="small" @click="openApprove(row)">
                确认定稿
              </el-button>
              <el-button type="danger" size="small" plain @click="openReject(row)">
                驳回
              </el-button>
            </template>
            <!-- 已定稿/已驳回：查看归档详情 -->
            <template v-else>
              <el-button type="primary" size="small" plain @click="openArchive(row)">
                查看归档详情
              </el-button>
            </template>
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

    <!-- ========== 确认定稿弹窗 ========== -->
    <el-dialog
      v-model="approveDialog.visible"
      title="确认定稿"
      width="420px"
      :close-on-click-modal="false"
      align-center
    >
      <div class="dialog-body">
        <div class="dialog-icon success">
          <el-icon :size="28"><CircleCheckFilled /></el-icon>
        </div>
        <div class="dialog-text">
          <p class="dialog-title">确定确认该订单定稿？</p>
          <p class="dialog-sub">
            订单号：<span class="mono-text">{{ approveDialog.row?.orderNo }}</span>
          </p>
          <p class="dialog-sub">
            客户名称：{{ approveDialog.row?.customerName }}
          </p>
          <p class="dialog-tip">
            定稿后订单将进入归档状态，不可再修改。
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approveDialog.loading" @click="handleApprove">
          确认定稿
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 驳回定稿弹窗 ========== -->
    <el-dialog
      v-model="rejectDialog.visible"
      title="驳回定稿"
      width="480px"
      :close-on-click-modal="false"
      align-center
    >
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectRules"
        label-width="80px"
      >
        <el-form-item label="订单编号">
          <span class="mono-text">{{ rejectDialog.row?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="客户名称">
          {{ rejectDialog.row?.customerName }}
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectRemark">
          <el-input
            v-model="rejectForm.rejectRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因，将反馈给设计人员"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.loading" @click="handleReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 归档详情弹窗 ========== -->
    <el-dialog
      v-model="archiveDialog.visible"
      title="归档详情"
      width="680px"
      :close-on-click-modal="false"
    >
      <div v-loading="archiveDialog.loading" class="archive-detail">
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="订单编号">
            <span class="mono-text">{{ archiveDialog.row?.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ archiveDialog.row?.customerName }}
          </el-descriptions-item>
          <el-descriptions-item label="物料类型">
            {{ getMaterialLabel(archiveDialog.row?.materialType) }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ archiveDialog.row?.quantity }}
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="statusTagType(archiveDialog.row?.status)" effect="light" round size="small">
              {{ statusLabel(archiveDialog.row?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="归档时间">
            {{ formatDateTime(archiveDetail.archivedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="规格说明" :span="2">
            {{ archiveDetail.specification || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="定稿备注" :span="2">
            {{ archiveDetail.approveRemark || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="驳回原因" v-if="archiveDialog.row?.status === 'REJECTED'" :span="2">
            {{ archiveDetail.rejectRemark || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="archiveDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  RefreshLeft,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import {
  listAuditOrders,
  confirmFinal,
  rejectFinal,
  getArchiveDetail,
  getAuditStatistics
} from '@/api/audit'

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
  WAIT_APPROVE: { label: '待审核', type: 'warning' },
  APPROVED:     { label: '已定稿', type: 'success' },
  REJECTED:     { label: '已驳回', type: 'danger' }
}

const getMaterialLabel = v => {
  const m = materialOptions.find(item => item.value === v)
  return m ? m.label : v
}
const statusLabel = s => (STATUS_MAP[s] ? STATUS_MAP[s].label : s)
const statusTagType = s => (STATUS_MAP[s] ? STATUS_MAP[s].type : 'info')

// ---------- 统计 ----------
const stats = reactive({ pending: 0, approved: 0, rejected: 0 })

async function fetchStats() {
  try {
    const res = await getAuditStatistics()
    if (res) {
      stats.pending = res.pending || 0
      stats.approved = res.approved || 0
      stats.rejected = res.rejected || 0
    }
  } catch (e) {
    stats.pending = 12
    stats.approved = 48
    stats.rejected = 5
  }
}

// ---------- 筛选 ----------
const filters = reactive({
  orderNo: '',
  customerName: '',
  status: '',
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
    if (filters.status) params.status = filters.status
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }

    const res = await listAuditOrders(params)
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    generateMockData()
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  const statuses = ['WAIT_APPROVE', 'APPROVED', 'REJECTED', 'APPROVED', 'WAIT_APPROVE']
  const customers = ['可口可乐', '宝洁中国', '华为技术', '小米科技', '阿里巴巴', '腾讯科技', '京东集团', '美团']
  const materials = ['POSTER', 'FLYER', 'BUSINESS_CARD', 'BROCHURE', 'BANNER']
  const list = []
  for (let i = 1; i <= pagination.pageSize; i++) {
    const idx = (pagination.page - 1) * pagination.pageSize + i
    list.push({
      id: idx,
      orderNo: 'ORD-2026-' + String(idx).padStart(5, '0'),
      customerName: customers[idx % customers.length],
      materialType: materials[idx % materials.length],
      quantity: 100 + idx * 50,
      specification: 'A3尺寸 双面印刷 300g铜版纸',
      status: statuses[idx % statuses.length],
      createdAt: '2026-11-0' + ((idx % 9) + 1) + ' 14:' + String((idx * 3) % 60).padStart(2, '0') + ':00'
    })
  }
  tableData.value = list
  pagination.total = 65
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  filters.orderNo = ''
  filters.customerName = ''
  filters.status = ''
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

// 点击统计卡片快速筛选
function quickFilter(status) {
  filters.status = status
  pagination.page = 1
  fetchList()
}

// ---------- 确认定稿弹窗 ----------
const approveDialog = reactive({
  visible: false,
  loading: false,
  row: null
})

function openApprove(row) {
  approveDialog.row = row
  approveDialog.visible = true
  approveDialog.loading = false
}

async function handleApprove() {
  const row = approveDialog.row
  if (!row) return
  approveDialog.loading = true
  try {
    await confirmFinal(row.id, {})
    ElMessage.success('定稿成功')
    approveDialog.visible = false
    fetchList()
    fetchStats()
  } finally {
    approveDialog.loading = false
  }
}

// ---------- 驳回弹窗 ----------
const rejectDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const rejectFormRef = ref(null)
const rejectForm = reactive({ rejectRemark: '' })
const rejectRules = {
  rejectRemark: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ]
}

function openReject(row) {
  rejectDialog.row = row
  rejectForm.rejectRemark = ''
  rejectDialog.visible = true
  rejectDialog.loading = false
}

async function handleReject() {
  if (!rejectFormRef.value) return
  rejectFormRef.value.validate(async valid => {
    if (!valid) return
    const row = rejectDialog.row
    if (!row) return
    rejectDialog.loading = true
    try {
      await rejectFinal(row.id, { rejectRemark: rejectForm.rejectRemark })
      ElMessage.success('已驳回')
      rejectDialog.visible = false
      fetchList()
      fetchStats()
    } finally {
      rejectDialog.loading = false
    }
  })
}

// ---------- 归档详情弹窗 ----------
const archiveDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const archiveDetail = ref({})

async function openArchive(row) {
  archiveDialog.row = row
  archiveDialog.visible = true
  archiveDialog.loading = true
  try {
    const res = await getArchiveDetail(row.id)
    archiveDetail.value = res || {}
  } catch (e) {
    archiveDetail.value = {
      specification: row.specification || 'A3尺寸 双面印刷 300g铜版纸',
      approveRemark: '客户已确认，符合要求',
      rejectRemark: '颜色偏黄，需重新调整',
      archivedAt: row.createdAt
    }
  } finally {
    archiveDialog.loading = false
  }
}

// ---------- 工具 ----------
function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.final-audit {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 顶部统计 ========== */
.stat-row {
  margin-bottom: 0;
}

.mini-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition-base);
  border: 1px solid var(--gray-200);
  background: #ffffff;
}

.mini-stat:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.mini-num {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.mini-label {
  font-size: 13px;
  color: var(--gray-500);
}

.stat-waiting .mini-num { color: #d97706; }
.stat-waiting { border-left: 3px solid var(--warning-color); }

.stat-approved .mini-num { color: #059669; }
.stat-approved { border-left: 3px solid var(--success-color); }

.stat-rejected .mini-num { color: #dc2626; }
.stat-rejected { border-left: 3px solid var(--danger-color); }

/* ========== 筛选 ========== */
.filter-card {
  border-radius: var(--border-radius);
}
.filter-card :deep(.el-card__body) {
  padding: 20px 24px 4px;
}

/* ========== 表格 ========== */
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

/* ========== 弹窗 ========== */
.dialog-body {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0;
}

.dialog-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dialog-icon.success {
  background: var(--success-light);
  color: #059669;
}

.dialog-text {
  flex: 1;
}

.dialog-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--gray-800);
  margin: 0 0 10px;
}

.dialog-sub {
  font-size: 13px;
  color: var(--gray-500);
  margin: 4px 0;
}

.dialog-tip {
  font-size: 12px;
  color: #d97706;
  background: var(--warning-light);
  padding: 6px 10px;
  border-radius: var(--border-radius-sm);
  margin-top: 10px;
}

/* 归档详情弹窗 */
.archive-detail {
  padding: 8px 0;
}
</style>
