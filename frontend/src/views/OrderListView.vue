<template>
  <!-- 订单列表 — Cursor 游标分页 + 状态筛选 + 关键词搜索（US-001/002/003） -->
  <div class="space-y-4">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <select v-model="filters.status" class="input h-9 w-36">
          <option value="">全部状态</option>
          <option v-for="s in STATUS_OPTIONS" :key="s.code" :value="s.code">{{ s.label }}</option>
        </select>
        <input v-model.trim="filters.keyword" class="input h-9 w-64" placeholder="搜索订单号 / 客户名称" />
      </div>
      <!-- 创建订单仅 AM 可见（PRD §2 RBAC） -->
      <button v-if="userStore.role === 'AM'" class="btn-primary" @click="router.push('/orders/create')">
        ＋ 创建订单
      </button>
    </div>

    <!-- 表格 -->
    <div class="card overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-gray-500">
          <tr>
            <th class="text-left font-medium px-4 py-3">订单号</th>
            <th class="text-left font-medium px-4 py-3">客户名称</th>
            <th class="text-left font-medium px-4 py-3">物料</th>
            <th class="text-left font-medium px-4 py-3">数量</th>
            <th class="text-left font-medium px-4 py-3">状态</th>
            <th class="text-left font-medium px-4 py-3">设计师</th>
            <th class="text-left font-medium px-4 py-3">交付日期</th>
            <th class="text-left font-medium px-4 py-3">创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && list.length === 0" class="text-gray-400 text-center">
            <td colspan="8" class="py-10">加载中…</td>
          </tr>
          <tr v-else-if="list.length === 0" class="text-gray-400 text-center">
            <td colspan="8" class="py-10">暂无订单</td>
          </tr>
          <tr
            v-for="item in list"
            :key="item.id"
            class="border-t border-gray-100 hover:bg-gray-50 cursor-pointer"
            @click="router.push(`/orders/${item.id}`)"
          >
            <td class="px-4 py-3 font-mono text-brand">{{ item.orderNo }}</td>
            <td class="px-4 py-3">{{ item.customerName }}</td>
            <td class="px-4 py-3">{{ MATERIAL_LABEL[item.materialType] || item.materialType }}</td>
            <td class="px-4 py-3">{{ item.quantity }}</td>
            <td class="px-4 py-3"><StatusBadge :status="item.status" /></td>
            <td class="px-4 py-3">{{ item.designerName || '—' }}</td>
            <td class="px-4 py-3">{{ item.deliveryDate }}</td>
            <td class="px-4 py-3 text-gray-500">{{ formatTime(item.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Cursor 分页 -->
    <div class="flex items-center justify-between text-sm text-gray-500">
      <span>共 {{ total ?? '—' }} 条</span>
      <button :disabled="loading || !hasMore" class="btn" @click="fetchList">
        {{ loading ? '加载中…' : hasMore ? '加载更多' : '没有更多了' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api'
import { useUserStore } from '@/stores/user'
import StatusBadge from '@/components/StatusBadge.vue'

const router = useRouter()
const userStore = useUserStore()

// 状态枚举（GLOSSARY §3.1）
const STATUS_OPTIONS = [
  { code: 'CREATED', label: '待分派' },
  { code: 'ASSIGNED', label: '已分派' },
  { code: 'DESIGNING', label: '设计中' },
  { code: 'WAIT_APPROVE', label: '待定稿' },
  { code: 'APPROVED', label: '已定稿' },
  { code: 'DELIVERED', label: '已交付' },
  { code: 'CANCELLED', label: '已作废' }
]
const MATERIAL_LABEL = { POSTER: '海报', FLYER: '宣传单', BUSINESS_CARD: '名片', BROCHURE: '画册', BANNER: '喷绘', OTHER: '其他' }

const list = ref([])
const hasMore = ref(false)
const loading = ref(false)
const total = ref(null)
let nextCursor = null

const filters = reactive({ status: '', keyword: '' })

onMounted(fetchList)

// 筛选条件变化 → 重置分页重新拉取
watch(() => [filters.status, filters.keyword], () => {
  list.value = []
  nextCursor = null
  hasMore.value = false
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const params = { limit: 10 }
    if (nextCursor) params.cursor = nextCursor
    if (filters.status) params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword
    // request.js 已解包 ResultVO，res = { list, nextCursor, hasMore, total }
    const res = await listOrders(params)
    list.value = [...list.value, ...(res.list || [])]
    nextCursor = res.nextCursor
    hasMore.value = res.hasMore
    if (res.total != null) total.value = res.total
  } finally {
    loading.value = false
  }
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>
