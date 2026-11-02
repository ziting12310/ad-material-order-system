<template>
  <!--
    订单表单 — 创建/编辑复用
    字段对齐 OrderCreateRequest DTO（API.md §3.2 / GLOSSARY §5 orders 表）
  -->
  <form @submit.prevent="onSubmit" class="space-y-4 bg-white p-6 rounded-lg border border-gray-100">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- 客户名称（2~100 字符） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>客户名称</label>
        <input v-model.trim="model.customerName" class="input" placeholder="2-100 字符" maxlength="100" />
      </div>
      <!-- 联系人姓名（2~20 字符） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>联系人姓名</label>
        <input v-model.trim="model.contactName" class="input" placeholder="2-20 字符" maxlength="20" />
      </div>
      <!-- 联系电话（11 位手机号） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>联系电话</label>
        <input v-model.trim="model.contactPhone" class="input" placeholder="11 位手机号" maxlength="11" />
      </div>
      <!-- 物料类型（MaterialType 枚举） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>物料类型</label>
        <select v-model="model.materialType" class="input">
          <option value="">请选择</option>
          <option v-for="t in MATERIAL_OPTIONS" :key="t.code" :value="t.code">{{ t.label }}</option>
        </select>
      </div>
      <!-- 数量（1~99999） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>数量</label>
        <input v-model.number="model.quantity" type="number" min="1" max="99999" class="input" />
      </div>
      <!-- 交付日期（必须晚于今天） -->
      <div>
        <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>交付日期</label>
        <input v-model="model.deliveryDate" type="date" class="input" :min="today" />
      </div>
    </div>

    <!-- 规格说明（5~200 字符） -->
    <div>
      <label class="block text-sm text-gray-600 mb-1"><span class="text-red-500 mr-0.5">*</span>规格说明</label>
      <textarea v-model.trim="model.specification" class="input h-20" placeholder="5-200 字符" maxlength="200" />
    </div>

    <!-- 备注（选填，≤500 字符） -->
    <div>
      <label class="block text-sm text-gray-600 mb-1">备注</label>
      <textarea v-model.trim="model.remark" class="input h-16" placeholder="选填，≤500 字符" maxlength="500" />
    </div>

    <!-- 操作按钮 -->
    <div class="flex justify-end gap-2 pt-2 border-t border-gray-100">
      <button type="button" class="btn" @click="$emit('cancel')">取消</button>
      <button type="submit" :disabled="loading" class="btn-primary">
        {{ loading ? '提交中…' : buttonText }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  model: { type: Object, required: true },       // reactive 表单对象
  loading: Boolean,
  buttonText: { type: String, default: '提交' }
})
const emit = defineEmits(['submit', 'cancel'])

// 物料类型枚举（GLOSSARY §3.2 MaterialType）
const MATERIAL_OPTIONS = [
  { code: 'POSTER',        label: '海报' },
  { code: 'FLYER',         label: '宣传单' },
  { code: 'BUSINESS_CARD', label: '名片' },
  { code: 'BROCHURE',      label: '画册' },
  { code: 'BANNER',        label: '喷绘' },
  { code: 'OTHER',         label: '其他' }
]

// 交付日期不能早于今天（后端 @Future 校验）
const today = computed(() => new Date().toISOString().slice(0, 10))

function onSubmit() {
  emit('submit', { ...props.model })
}
</script>
