<template>
  <!-- 我的任务（DS 角色）— US-003：查看分派给我的订单 -->
  <div class="space-y-4">
    <div v-if="loading" class="text-gray-400 text-center py-10">加载中…</div>
    <div v-else-if="list.length === 0" class="text-gray-400 text-center py-20">暂无分派给你的任务</div>

    <div v-else class="space-y-3">
      <div
        v-for="item in list"
        :key="item.id"
        class="card p-4 hover:border-brand/40 cursor-pointer transition-colors"
        @click="router.push(`/orders/${item.id}`)"
      >
        <div class="flex items-center justify-between mb-2">
          <span class="font-mono text-brand text-sm">{{ item.orderNo }}</span>
          <StatusBadge :status="item.status" />
        </div>
        <div class="text-sm text-gray-600 mb-1">
          <span class="mr-4">客户：{{ item.customerName }}</span>
          <span>交付日期：{{ item.deliveryDate }}</span>
        </div>
        <p class="text-sm text-gray-500 truncate">{{ item.specification }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api'
import { useUserStore } from '@/stores/user'
import StatusBadge from '@/components/StatusBadge.vue'

const router = useRouter()
const userStore = useUserStore()
const list = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    // 后端按登录态注入 designerId 数据范围（PRD §2：DS 仅见分派给自己的订单）
    // 进行中任务 = ASSIGNED / DESIGNING / WAIT_APPROVE
    const res = await listOrders({ limit: 100 })
    list.value = (res.list || []).filter(o =>
      ['ASSIGNED', 'DESIGNING', 'WAIT_APPROVE'].includes(o.status)
    )
  } finally {
    loading.value = false
  }
})
</script>
