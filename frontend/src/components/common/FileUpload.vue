<template>
  <!--
    文件上传组件
    功能：拖拽上传、多文件、进度条、格式校验、大小限制
    v-model: 已上传文件列表 [{ fileId, fileUrl, fileName, fileSize }]
  -->
  <div class="file-upload">
    <!-- 拖拽上传区域 -->
    <div
      class="upload-dropzone"
      :class="{ 'is-dragover': isDragover, 'is-disabled': disabled }"
      @click="handleClick"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInputRef"
        type="file"
        class="upload-input"
        :multiple="multiple"
        :accept="acceptStr"
        @change="handleFileChange"
      />
      <div class="dropzone-icon">
        <el-icon :size="40" color="var(--primary-color)"><UploadFilled /></el-icon>
      </div>
      <div class="dropzone-text">
        <p class="dropzone-title">
          将文件拖到此处，或
          <span class="dropzone-link">点击上传</span>
        </p>
        <p class="dropzone-tip">
          支持 {{ acceptLabel }}，
          单文件不超过 {{ maxSizeLabel }}
        </p>
      </div>
    </div>

    <!-- 文件列表 -->
    <div v-if="fileList.length > 0" class="file-list">
      <div
        v-for="(file, index) in fileList"
        :key="file.uid || file.fileId || index"
        class="file-item"
        :class="{ 'is-success': file.status === 'success', 'is-error': file.status === 'error' }"
      >
        <div class="file-info">
          <el-icon class="file-icon"><Document /></el-icon>
          <div class="file-meta">
            <span class="file-name" :title="file.name || file.fileName">{{ file.name || file.fileName }}</span>
            <span class="file-size">{{ formatFileSize(file.size || file.fileSize) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <!-- 上传中：显示进度 -->
          <template v-if="file.status === 'uploading'">
            <span class="progress-text">{{ file.percent || 0 }}%</span>
            <el-button type="danger" link size="small" @click="handleAbort(file)">
              取消
            </el-button>
          </template>
          <!-- 上传成功 -->
          <template v-else-if="file.status === 'success'">
            <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
            <el-button v-if="!disabled" type="danger" link size="small" @click="handleRemove(index)">
              删除
            </el-button>
          </template>
          <!-- 上传失败 -->
          <template v-else-if="file.status === 'error'">
            <span class="error-text">{{ file.errorMsg || '上传失败' }}</span>
            <el-button type="primary" link size="small" @click="handleRetry(file)">
              重试
            </el-button>
          </template>
        </div>
        <!-- 进度条 -->
        <div v-if="file.status === 'uploading'" class="progress-bar">
          <div class="progress-inner" :style="{ width: (file.percent || 0) + '%' }"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, CircleCheckFilled } from '@element-plus/icons-vue'

// ---------- Props / Emits ----------
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  // 是否多选
  multiple: {
    type: Boolean,
    default: true
  },
  // 上传场景：reference(参考图) / design(设计稿)
  scene: {
    type: String,
    default: 'reference'
  },
  // 允许的文件类型白名单
  acceptTypes: {
    type: Array,
    default: () => ['psd', 'ai', 'jpg', 'jpeg', 'png', 'pdf', 'webp']
  },
  // 最大文件大小（字节），参考图 5MB，设计稿 100MB
  maxSize: {
    type: Number,
    default: 5 * 1024 * 1024 // 5MB
  },
  // 最大文件数量
  maxCount: {
    type: Number,
    default: 10
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'change'])

// ---------- 状态 ----------
const fileInputRef = ref(null)
const isDragover = ref(false)
const fileList = ref([]) // 内部文件列表（包含上传中、成功、失败状态）

// ---------- 计算属性 ----------
const acceptStr = computed(() => {
  return props.acceptTypes.map(t => '.' + t).join(',')
})

const acceptLabel = computed(() => {
  return props.acceptTypes.map(t => t.toUpperCase()).join('/')
})

const maxSizeLabel = computed(() => {
  if (props.maxSize >= 1024 * 1024 * 1024) {
    return (props.maxSize / 1024 / 1024 / 1024).toFixed(0) + 'GB'
  }
  if (props.maxSize >= 1024 * 1024) {
    return (props.maxSize / 1024 / 1024).toFixed(0) + 'MB'
  }
  return (props.maxSize / 1024).toFixed(0) + 'KB'
})

// ---------- 同步外部 v-model ----------
watch(
  () => props.modelValue,
  (val) => {
    if (val && val.length > 0) {
      // 外部传入已上传文件，标记为 success
      const existingIds = fileList.value.filter(f => f.status === 'success').map(f => f.fileId)
      val.forEach(item => {
        if (!existingIds.includes(item.fileId)) {
          fileList.value.push({
            ...item,
            name: item.fileName,
            size: item.fileSize,
            status: 'success',
            percent: 100
          })
        }
      })
    }
  },
  { immediate: true, deep: true }
)

// ---------- 工具函数 ----------
function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

function getFileExt(filename) {
  const dot = filename.lastIndexOf('.')
  return dot > -1 ? filename.slice(dot + 1).toLowerCase() : ''
}

function generateUid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}

// ---------- 校验 ----------
function validateFile(file) {
  // 类型校验
  const ext = getFileExt(file.name)
  if (!props.acceptTypes.includes(ext)) {
    ElMessage.error(`不支持的文件类型：${file.name}，仅支持 ${acceptLabel.value}`)
    return false
  }
  // 大小校验
  if (file.size > props.maxSize) {
    ElMessage.error(`文件 ${file.name} 超过最大限制 ${maxSizeLabel.value}`)
    return false
  }
  return true
}

// ---------- 事件处理 ----------
function handleClick() {
  if (props.disabled) return
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

function handleDragOver(e) {
  if (props.disabled) return
  isDragover.value = true
}

function handleDragLeave(e) {
  isDragover.value = false
}

function handleDrop(e) {
  isDragover.value = false
  if (props.disabled) return
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    handleFiles(Array.from(files))
  }
}

function handleFileChange(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    handleFiles(Array.from(files))
  }
  e.target.value = ''
}

function handleFiles(files) {
  if (props.disabled) return

  // 数量限制校验
  const successCount = fileList.value.filter(f => f.status === 'success').length
  const remaining = props.maxCount - successCount
  if (remaining <= 0) {
    ElMessage.warning(`最多上传 ${props.maxCount} 个文件`)
    return
  }
  if (files.length > remaining) {
    ElMessage.warning(`还可上传 ${remaining} 个文件，已自动截取`)
    files = files.slice(0, remaining)
  }

  // 逐个校验并上传
  files.forEach(file => {
    if (!validateFile(file)) return

    const fileItem = {
      uid: generateUid(),
      raw: file,
      name: file.name,
      size: file.size,
      status: 'uploading',
      percent: 0,
      errorMsg: ''
    }
    fileList.value.push(fileItem)
    emit('change', fileList.value)

    // 开始上传
    uploadFile(fileItem)
  })
}

// ---------- 上传逻辑（模拟，等后端 OSS 配置好再替换） ----------
async function uploadFile(fileItem) {
  try {
    // TODO: 等后端配置好 OSS 后，替换为真实上传流程：
    // 1. 调 presign 接口拿上传凭证
    // 2. 前端直传 OSS，监听进度
    // 3. 上传成功后通知后端，拿到 fileId

    // 模拟上传进度
    await simulateUpload(fileItem)

    // 模拟上传成功，生成 mock 返回值
    const mockResult = {
      fileId: 'file_' + generateUid(),
      fileUrl: URL.createObjectURL(fileItem.raw), // 临时预览 URL
      fileName: fileItem.name,
      fileSize: fileItem.size
    }

    // 更新文件状态
    const idx = fileList.value.findIndex(f => f.uid === fileItem.uid)
    if (idx > -1) {
      fileList.value[idx] = {
        ...fileList.value[idx],
        ...mockResult,
        status: 'success',
        percent: 100
      }
    }

    // 同步 v-model（只传成功的文件）
    syncModelValue()
    emit('success', mockResult)
    emit('change', fileList.value)
  } catch (e) {
    const idx = fileList.value.findIndex(f => f.uid === fileItem.uid)
    if (idx > -1) {
      fileList.value[idx].status = 'error'
      fileList.value[idx].errorMsg = e.message || '上传失败'
    }
    emit('error', { file: fileItem, error: e })
    emit('change', fileList.value)
  }
}

// 模拟上传进度（真实场景替换为 OSS 直传）
function simulateUpload(fileItem) {
  return new Promise((resolve, reject) => {
    let percent = 0
    const totalDuration = 1500 + Math.random() * 1500 // 1.5~3 秒
    const steps = 30
    const stepDuration = totalDuration / steps

    const timer = setInterval(() => {
      if (fileItem.status === 'error' || fileItem.status === 'aborted') {
        clearInterval(timer)
        reject(new Error('已取消'))
        return
      }
      percent += 100 / steps
      if (percent >= 100) {
        percent = 100
        clearInterval(timer)
        fileItem.percent = 100
        setTimeout(resolve, 100)
      } else {
        fileItem.percent = Math.floor(percent)
      }
    }, stepDuration)
  })
}

// ---------- 操作 ----------
function handleRemove(index) {
  fileList.value.splice(index, 1)
  syncModelValue()
  emit('change', fileList.value)
}

function handleAbort(fileItem) {
  const idx = fileList.value.findIndex(f => f.uid === fileItem.uid)
  if (idx > -1) {
    fileList.value[idx].status = 'aborted'
    fileList.value.splice(idx, 1)
    syncModelValue()
    emit('change', fileList.value)
  }
}

function handleRetry(fileItem) {
  const idx = fileList.value.findIndex(f => f.uid === fileItem.uid)
  if (idx > -1 && fileItem.raw) {
    fileList.value[idx].status = 'uploading'
    fileList.value[idx].percent = 0
    fileList.value[idx].errorMsg = ''
    uploadFile(fileList.value[idx])
  }
}

// ---------- 同步 v-model ----------
function syncModelValue() {
  const successFiles = fileList.value
    .filter(f => f.status === 'success')
    .map(f => ({
      fileId: f.fileId,
      fileUrl: f.fileUrl,
      fileName: f.fileName || f.name,
      fileSize: f.fileSize || f.size
    }))
  emit('update:modelValue', successFiles)
}
</script>

<style scoped>
.file-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

/* ---------- 拖拽区 ---------- */
.upload-dropzone {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  border: 2px dashed var(--gray-300);
  border-radius: var(--border-radius);
  background: var(--gray-50);
  cursor: pointer;
  transition: var(--transition-base);
  text-align: center;
}

.upload-dropzone:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}

.upload-dropzone.is-dragover {
  border-color: var(--primary-color);
  background: var(--primary-light);
  transform: scale(1.01);
}

.upload-dropzone.is-disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.upload-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  display: none;
}

.dropzone-icon {
  margin-bottom: 12px;
  line-height: 1;
}

.dropzone-text {
  line-height: 1.5;
}

.dropzone-title {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--gray-600);
}

.dropzone-link {
  color: var(--primary-color);
  font-weight: 500;
}

.dropzone-tip {
  margin: 0;
  font-size: 12px;
  color: var(--gray-400);
}

/* ---------- 文件列表 ---------- */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
  border: 1px solid var(--gray-200);
  border-radius: var(--border-radius);
  background: #ffffff;
  transition: var(--transition-base);
}

.file-item:hover {
  border-color: var(--gray-300);
}

.file-item.is-success {
  border-color: var(--success-light);
  background: #f0fdf4;
}

.file-item.is-error {
  border-color: var(--danger-light);
  background: #fef2f2;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: var(--primary-color);
  font-size: 18px;
  flex-shrink: 0;
}

.file-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: var(--gray-800);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: var(--gray-400);
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.progress-text {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 500;
  min-width: 36px;
  text-align: right;
}

.success-icon {
  color: var(--success-color);
  font-size: 16px;
}

.error-text {
  font-size: 12px;
  color: var(--danger-color);
}

/* ---------- 进度条 ---------- */
.progress-bar {
  margin-top: 8px;
  height: 4px;
  background: var(--gray-100);
  border-radius: 2px;
  overflow: hidden;
}

.progress-inner {
  height: 100%;
  background: var(--primary-color);
  border-radius: 2px;
  transition: width 0.2s ease;
}

/* 调整 info 行布局：让左侧文件信息和右侧操作在同一行 */
.file-item {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.file-item .progress-bar {
  width: 100%;
  margin-top: 4px;
}
</style>
