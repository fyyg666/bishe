<template>
  <div class="digital-reader">
    <div class="reader-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      <h3 class="reader-title">{{ resource?.title || '在线阅读' }}</h3>
      <div style="width: 80px"></div>
    </div>

    <div class="reader-body">
      <div v-if="pageLoading" class="reader-loading">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>正在加载资源信息...</p>
      </div>

      <div v-else-if="pageError" class="reader-error">
        <el-icon :size="40" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <p>{{ pageError }}</p>
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </div>

      <div v-else-if="unsupportedFormat" class="reader-error">
        <el-icon :size="40" color="#e6a23c"><WarningFilled /></el-icon>
        <p>不支持在线阅读该格式（{{ resource?.format }}），请下载后阅读</p>
        <el-button type="primary" @click="handleDownload">下载资源</el-button>
      </div>

      <PdfReader
        v-else-if="resource?.format === 'PDF'"
        :resource-id="resource.id"
        :file-url="fileUrl"
        :title="resource.title"
      />

      <EpubReader
        v-else-if="resource?.format === 'EPUB'"
        :resource-id="resource.id"
        :file-url="fileUrl"
        :title="resource.title"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading, CircleCloseFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDigitalResourceDetail, incrementViewCount, getDownloadUrl } from '@/api/digitalResource'
import PdfReader from './PdfReader.vue'
import EpubReader from './EpubReader.vue'

const route = useRoute()
const router = useRouter()

const resource = ref(null)
const fileUrl = ref('')
const pageLoading = ref(true)
const pageError = ref('')

const unsupportedFormat = computed(() => {
  if (!resource.value) return false
  return !['PDF', 'EPUB'].includes(resource.value.format)
})

function goBack() {
  router.push('/digital-resources')
}

async function loadResource() {
  const id = route.params.id
  if (!id) {
    pageError.value = '资源ID无效'
    pageLoading.value = false
    return
  }

  try {
    const res = await getDigitalResourceDetail(id)
    resource.value = res.data

    if (resource.value.accessUrl) {
      fileUrl.value = resource.value.accessUrl
    } else {
      fileUrl.value = `/api/v1/digital-resources/${id}/download`
    }

    incrementViewCount(id).catch(() => {})
  } catch {
    pageError.value = '加载资源信息失败'
  } finally {
    pageLoading.value = false
  }
}

async function handleDownload() {
  try {
    const res = await getDownloadUrl(resource.value.id)
    const url = res.data?.url || res.data
    if (url) {
      window.open(url, '_blank')
    } else {
      ElMessage.warning('无法获取下载地址')
    }
  } catch {
    ElMessage.error('获取下载地址失败')
  }
}

onMounted(() => {
  loadResource()
})
</script>

<style lang="scss" scoped>
.digital-reader {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #1e1e1e;
}

.reader-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #2c2c2c;
  border-bottom: 1px solid #3c3c3c;
  flex-shrink: 0;

  .reader-title {
    color: #eee;
    font-size: 16px;
    font-weight: 500;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
    flex: 1;
  }
}

.reader-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.reader-loading,
.reader-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
  gap: 16px;

  p {
    font-size: 16px;
  }
}

.reader-error {
  p {
    color: #f56c6c;
  }
}
</style>
