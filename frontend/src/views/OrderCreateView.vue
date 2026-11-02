<template>
  <!-- 创建订单页（AM 角色）— US-001 -->
  <div class="max-w-2xl">
    <OrderForm :model="form" :loading="loading" button-text="创建订单" @submit="onSubmit" @cancel="router.back()" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createOrder } from '@/api'
import OrderForm from '@/components/OrderForm.vue'

const router = useRouter()

// 表单字段对齐 OrderCreateRequest DTO（API.md §3.2）
const form = reactive({
  customerName: '',
  contactName: '',
  contactPhone: '',
  materialType: '',
  quantity: 1,
  specification: '',
  deliveryDate: '',
  remark: ''
})
const loading = ref(false)

async function onSubmit(payload) {
  loading.value = true
  try {
    const res = await createOrder(payload)
    // 创建成功 → 跳订单详情（返回 { id, orderNo, status }）
    router.push(`/orders/${res.id}`)
  } finally {
    loading.value = false
  }
}
</script>
