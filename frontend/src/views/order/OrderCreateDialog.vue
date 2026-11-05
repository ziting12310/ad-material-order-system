<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑订单' : '新建订单'"
    width="1000px"
    :close-on-click-modal="false"
    class="order-create-dialog"
    @closed="handleClosed"
  >
    <div class="dialog-body">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        label-position="right"
      >
        <!-- 基本信息 -->
        <div class="section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司名称" prop="customerName">
                <el-input v-model="form.customerName" placeholder="请输入公司名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交付日期" prop="deliveryDate">
              <el-date-picker
                v-model="form.deliveryDate"
                type="date"
                placeholder="请选择交付日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注（选填）"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 物料任务列表（卡片式） -->
        <div class="section-title">
          <span>物料任务</span>
          <span class="task-count">共 {{ form.tasks.length }} 项</span>
        </div>

        <div class="task-card-list">
          <div
            v-for="(task, index) in form.tasks"
            :key="index"
            class="task-card"
          >
            <div class="task-card-header">
              <span class="task-index">物料 {{ index + 1 }}</span>
              <el-button
                v-if="form.tasks.length > 1"
                type="danger"
                link
                size="small"
                @click="removeTask(index)"
              >
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>

            <div class="task-card-body">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item
                    :prop="`tasks.${index}.materialType`"
                    :rules="taskRules.materialType"
                    label="物料类型"
                  >
                    <el-select
                      v-model="task.materialType"
                      placeholder="请选择物料类型"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="item in MATERIAL_OPTIONS"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item
                    :prop="`tasks.${index}.quantity`"
                    :rules="taskRules.quantity"
                    label="数量"
                  >
                    <el-input-number
                      v-model="task.quantity"
                      :min="1"
                      :max="99999"
                      :step="1"
                      :precision="0"
                      controls-position="right"
                      style="width: 100%"
                      @input="val => handleQuantityInput(task, val)"
                      @change="val => handleQuantityInput(task, val)"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item
                    :prop="`tasks.${index}.deadline`"
                    :rules="taskRules.deadline"
                    label="截止日期"
                  >
                    <el-date-picker
                      v-model="task.deadline"
                      type="date"
                      placeholder="选择截止日期"
                      value-format="YYYY-MM-DD"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :prop="`tasks.${index}.specification`"
                    label="规格说明"
                  >
                    <el-input v-model="task.specification" placeholder="如：60x90cm、300dpi 等" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :prop="`tasks.${index}.usageDesc`"
                    label="用途说明"
                  >
                    <el-input v-model="task.usageDesc" placeholder="如：门店开业宣传、展会展示等" />
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item
                    :prop="`tasks.${index}.remark`"
                    label="备注"
                  >
                    <el-input v-model="task.remark" placeholder="其他要求或说明（选填）" />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </div>
        </div>

        <div class="add-task-row">
          <el-button type="primary" plain :icon="Plus" @click="addTask">
            添加物料
          </el-button>
        </div>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { createOrder, updateOrder, getOrderDetail } from '@/api'
import { MATERIAL_OPTIONS } from '@/constants'

// ---------- Props / Emits ----------
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  editData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

// ---------- 计算属性 ----------
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const isEdit = computed(() => !!props.editData && !!props.editData.id)

// ---------- 表单 ----------
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)

function createEmptyTask() {
  return {
    materialType: '',
    quantity: 1,
    specification: '',
    usageDesc: '',
    deadline: '',
    remark: ''
  }
}

function createEmptyForm() {
  return {
    id: null,
    version: null,
    customerName: '',
    contactName: '',
    contactPhone: '',
    deliveryDate: '',
    remark: '',
    // 订单级遗留字段（后端 DTO 还校验，必须带上）
    materialType: '',
    quantity: 1,
    specification: '',
    tasks: [createEmptyTask()]
  }
}

const form = reactive(createEmptyForm())

// ---------- 校验规则 ----------
const rules = {
  customerName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { max: 100, message: '公司名称不能超过 100 个字符', trigger: 'blur' }
  ],
  contactName: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
    { max: 50, message: '联系人不能超过 50 个字符', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/,
      message: '请输入正确的联系电话',
      trigger: 'blur'
    }
  ],
  deliveryDate: [
    { required: true, message: '请选择交付日期', trigger: 'change' }
  ]
}

const taskRules = {
  materialType: [
    { required: true, message: '请选择物料类型', trigger: 'change' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止日期', trigger: 'change' }
  ]
}

// ---------- 物料任务操作 ----------
function addTask() {
  form.tasks.push(createEmptyTask())
}

function removeTask(index) {
  if (form.tasks.length <= 1) return
  form.tasks.splice(index, 1)
}

/**
 * 数量输入框自动取整：小数四舍五入，并收敛到 1~99999 的正整数
 */
function handleQuantityInput(task, val) {
  const n = Number(val)
  if (!Number.isFinite(n)) return
  const rounded = Math.min(99999, Math.max(1, Math.round(n)))
  if (task.quantity !== rounded) {
    task.quantity = rounded
  }
}

/**
 * 提交前兜底：确保所有任务数量都是正整数（防止绕过输入框的异常值）
 */
function normalizeAllQuantities() {
  form.tasks.forEach(task => {
    const n = Number(task.quantity)
    task.quantity = Number.isFinite(n)
      ? Math.min(99999, Math.max(1, Math.round(n)))
      : 1
  })
}

// ---------- 加载编辑数据 ----------
async function loadEditData(id) {
  loading.value = true
  try {
    const detail = await getOrderDetail(id)
    if (detail) {
      form.id = detail.id
      form.version = detail.version
      form.customerName = detail.customerName || ''
      form.contactName = detail.contactName || ''
      form.contactPhone = detail.contactPhone || ''
      form.deliveryDate = detail.deliveryDate || ''
      form.remark = detail.remark || ''
      // 订单级遗留字段（后端 DTO 还校验，必须带回）
      form.materialType = detail.materialType || ''
      form.quantity = detail.quantity || 1
      form.specification = detail.specification || ''
      if (detail.tasks && detail.tasks.length > 0) {
        form.tasks = detail.tasks.map(t => ({
          id: t.id,
          version: t.version,
          materialType: t.materialType || '',
          quantity: t.quantity || 1,
          specification: t.specification || '',
          usageDesc: t.usageDesc || '',
          deadline: t.deadline || '',
          remark: t.remark || ''
        }))
      } else {
        form.tasks = [createEmptyTask()]
      }
    }
  } catch (e) {
    // 详情加载失败，使用列表数据兜底
    const val = props.editData
    if (val) {
      form.id = val.id
      form.version = val.version
      form.customerName = val.customerName || ''
      form.contactName = val.contactName || ''
      form.contactPhone = val.contactPhone || ''
      form.deliveryDate = val.deliveryDate || ''
      form.remark = val.remark || ''
      form.materialType = val.materialType || ''
      form.quantity = val.quantity || 1
      form.specification = val.specification || ''
      form.tasks = [createEmptyTask()]
    }
  } finally {
    loading.value = false
  }
}

// 监听弹窗打开
watch(
  () => props.modelValue,
  val => {
    if (val && isEdit.value && props.editData?.id) {
      // 编辑模式：先加载完整详情（含 tasks）
      nextTick(() => {
        loadEditData(props.editData.id)
      })
    }
  }
)

// ---------- 提交 ----------
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  // 数量统一取整，避免小数被后端 Integer 反序列化拒绝
  normalizeAllQuantities()

  // 空字符串日期/文本转 null，避免 LocalDate 反序列化失败
  form.tasks.forEach(t => {
    if (t.deadline === '') t.deadline = null
    if (t.usageDesc === '') t.usageDesc = null
    if (t.remark === '') t.remark = null
  })

  // 同步订单级遗留字段（后端 DTO 还校验，取第一个任务的值）
  if (form.tasks && form.tasks.length > 0) {
    const firstTask = form.tasks[0]
    form.materialType = firstTask.materialType || ''
    form.quantity = firstTask.quantity || 1
    form.specification = firstTask.specification || ''
  }

  // 编辑必须携带乐观锁 version，缺失时直接提示，避免无意义请求
  if (isEdit.value && form.version == null) {
    ElMessage.error('订单版本号缺失，请刷新页面后重试')
    return
  }

  const payload = { ...form }
  if (isEdit.value) {
    // 只提交已存在的任务（带 id 才能按任务更新）；新增任务走任务接口
    payload.tasks = form.tasks.filter(t => t.id != null && t.id !== '')
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateOrder(props.editData.id, payload)
      ElMessage.success('订单更新成功')
    } else {
      await createOrder(payload)
      ElMessage.success('订单创建成功')
    }
    emit('success')
    visible.value = false
  } catch (e) {
    // 错误已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  visible.value = false
}

function handleClosed() {
  // 弹窗关闭后重置表单
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  Object.assign(form, createEmptyForm())
}
</script>

<style scoped>
.order-create-dialog {
  --el-border-radius-base: var(--border-radius);
}

.order-create-dialog :deep(.el-dialog) {
  border-radius: var(--border-radius);
}

.dialog-body {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
  margin: 8px 0 16px;
  padding-left: 10px;
  border-left: 3px solid var(--primary-color);
  line-height: 1;
}

.section-title .task-count {
  font-size: 13px;
  font-weight: 400;
  color: var(--gray-500);
}

/* 物料任务卡片 */
.task-card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  border: 1px solid var(--gray-200);
  border-radius: var(--border-radius);
  background: #fff;
  transition: var(--transition-base);
}

.task-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.task-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--gray-50);
  border-bottom: 1px solid var(--gray-200);
  border-radius: var(--border-radius) var(--border-radius) 0 0;
}

.task-index {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
}

.task-card-body {
  padding: 16px;
}

.add-task-row {
  margin-top: 16px;
}
</style>
