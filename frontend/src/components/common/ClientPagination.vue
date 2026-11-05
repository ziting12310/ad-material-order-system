<template>
  <!-- 前端本地分页统一组件：样式与项目原有分页保持一致 -->
  <el-pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    :pager-count="5"
    layout="total, prev, pager, next, jumper"
    background
    @current-change="handleCurrentChange"
  />
</template>

<script setup>
defineProps({
  // 当前页码（由 useClientPagination 的 currentPage 提供）
  currentPage: { type: Number, default: 1 },
  // 总条数（由 useClientPagination 的 total 提供）
  total: { type: Number, default: 0 },
  // 每页条数（固定 10）
  pageSize: { type: Number, default: 10 }
})

const emit = defineEmits(['current-change'])

function handleCurrentChange(page) {
  emit('current-change', page)
}
</script>
