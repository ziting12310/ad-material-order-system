import { computed, ref } from 'vue'

// 前端本地分页固定每页 10 条
export const CLIENT_PAGE_SIZE = 10

/**
 * 前端本地分页组合式函数（不请求后端，纯内存切片）
 *
 * @param {import('vue').Ref<Array>} sourceRef 全量数据源（页面先把后端数据一次性拉满再传入）
 * @param {number} pageSize 每页条数，默认固定 10
 * @returns {{
 *   pagedData: import('vue').ComputedRef<Array>,   // 当前页要展示的表格数据
 *   currentPage: import('vue').Ref<number>,        // 当前页码
 *   total: import('vue').ComputedRef<number>,      // 总条数
 *   pageSize: number,                              // 每页条数
 *   resetPage: () => void,                         // 重置回第 1 页（筛选/搜索/新增/审核后调用）
 *   handlePageChange: (page: number) => void       // 换页
 * }}
 */
export function useClientPagination(sourceRef, pageSize = CLIENT_PAGE_SIZE) {
  const currentPage = ref(1)

  // 总条数 = 全量数组长度
  const total = computed(() => (sourceRef.value || []).length)

  // 当前页数据 = 全量数组内存切片
  const pagedData = computed(() => {
    const list = sourceRef.value || []
    const start = (currentPage.value - 1) * pageSize
    return list.slice(start, start + pageSize)
  })

  // 重置回第 1 页
  function resetPage() {
    currentPage.value = 1
  }

  // 换页（纯前端，不重新请求接口）
  function handlePageChange(page) {
    currentPage.value = page
  }

  return { pagedData, currentPage, total, pageSize, resetPage, handlePageChange }
}
