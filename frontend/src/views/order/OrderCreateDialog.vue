<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑订单' : '新建订单'"
    width="900px"
    :close-on-click-modal="false"
    class="order-create-dialog"
    @closed="handleClosed"
  >
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
          <el-form-item label="客户名称" prop="customerName">
            <el-input v-model="form.customerName" placeholder="请输入客户名称" clearable />
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

      <!-- 物料任务列表 -->
      <div class="section-title">
        <span>物料任务</span>
        <span class="task-count">共 {{ form.tasks.length }} 项</span>
      </div>

      <div class="task-table-wrapper">
        <el-table :data="form.tasks" border size="default" class="task-table">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="物料类型" min-width="140">
            <template #default="{ row, $index }">
              <el-form-item
                :prop="`tasks.${$index}.materialType`"
                :rules="taskRules.materialType"
                class="table-form-item"
              >
                <el-select
                  v-model="row.materialType"
                  placeholder="请选择"
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
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row, $index }">
              <el-form-item
                :prop="`tasks.${$index}.quantity`"
                :rules="taskRules.quantity"
                class="table-form-item"
              >
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="99999"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="规格说明" min-width="160">
            <template #default="{ row, $index }">
              <el-form-item
                :prop="`tasks.${$index}.specification`"
                class="table-form-item"
              >
                <el-input v-model="row.specification" placeholder="规格说明" />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="用途说明" min-width="140">
            <template #default="{ row, $index }">
              <el-form-item
                :prop="`tasks.${$index}.purpose`"
                class="table-form-item"
              >
                <el-input v-model="row.purpose" placeholder="用途说明" />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="140">
            <template #default="{ row, $index }">
              <el-form-item
                :prop="`tasks.${$index}.deadline`"
                :rules="taskRules.deadline"
                class="table-form-item"
              >
                <el-date-picker
                  v-model="row.deadline"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注（选填）" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button
                type="danger"
                link
                size="small"
                :disabled="form.tasks.length <= 1"
                @click="removeTask($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="add-task-row">
        <el-button type="primary" plain :icon="Plus" @click="addTask">
          添加物料
        </el-button>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createOrder, updateOrder } from '@/api'
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

function createEmptyTask() {
  return {
    materialType: '',
    quantity: 1,
    specification: '',
    purpose: '',
    deadline: '',
    remark: ''
  }
}

function createEmptyForm() {
  return {
    customerName: '',
    contactName: '',
    contactPhone: '',
    deliveryDate: '',
    remark: '',
    tasks: [createEmptyTask()]
  }
}

const form = reactive(createEmptyForm())

// ---------- 校验规则 ----------
const rules = {
  customerName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { max: 100, message: '客户名称不能超过 100 个字符', trigger: 'blur' }
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

// ---------- 监听编辑数据 ----------
watch(
  () => props.editData,
  val => {
    if (val && val.id) {
      // 编辑模式：填充表单
      form.customerName = val.customerName || ''
      form.contactName = val.contactName || ''
      form.contactPhone = val.contactPhone || ''
      form.deliveryDate = val.deliveryDate || ''
      form.remark = val.remark || ''
      if (val.tasks && val.tasks.length > 0) {
        form.tasks = val.tasks.map(t => ({
          id: t.id,
          materialType: t.materialType || '',
          quantity: t.quantity || 1,
          specification: t.specification || '',
          purpose: t.purpose || '',
          deadline: t.deadline || '',
          remark: t.remark || ''
        }))
      } else {
        form.tasks = [createEmptyTask()]
      }
    }
  },
  { immediate: true }
)

// ---------- 提交 ----------
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateOrder(props.editData.id, { ...form })
      ElMessage.success('订单更新成功')
    } else {
      await createOrder({ ...form })
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
    formRef.value.resetFields()
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

.task-table-wrapper {
  border-radius: var(--border-radius-sm);
  overflow: hidden;
}

.task-table {
  --el-table-header-bg-color: var(--gray-50);
  --el-table-border-color: var(--gray-200);
}

.table-form-item {
  margin: 0;
  width: 100%;
}

.table-form-item :deep(.el-form-item__content) {
  line-height: 1;
}

.add-task-row {
  margin-top: 12px;
}
</style>
