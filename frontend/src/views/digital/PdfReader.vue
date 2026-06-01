<template>
  <div class="pdf-reader">
    <div class="pdf-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="prevPage" :disabled="currentPage <= 1">上一页</el-button>
        <el-input-number
          v-model="currentPage"
          :min="1"
          :max="totalPages"
          size="small"
          style="width: 100px; margin: 0 8px"
          @change="handlePageChange"
        />
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <el-button :icon="ArrowRight" @click="nextPage" :disabled="currentPage >= totalPages">下一页</el-button>
      </div>
      <div class="toolbar-right">
        <el-button :icon="ZoomOut" @click="zoomOut" :disabled="scale <= 0.5">缩小</el-button>
        <span class="zoom-info">{{ Math.round(scale * 100) }}%</span>
        <el-button :icon="ZoomIn" @click="zoomIn" :disabled="scale >= 3">放大</el-button>
        <el-button @click="resetZoom">重置</el-button>
        <el-button :icon="isFullscreen ? Close : FullScreen" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </el-button>
      </div>
    </div>

    <div class="pdf-content" ref="contentRef">
      <div v-if="loading" class="pdf-loading">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>正在加载PDF文档...</p>
      </div>
      <div v-else-if="error" class="pdf-error">
        <el-icon :size="40" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <p>PDF加载失败</p>
        <el-button type="primary" @click="loadPdf">重新加载</el-button>
      </div>
      <div v-else class="pdf-pages" ref="pagesRef">
        <vue-pdf-embed
          ref="pdfRef"
          :source="pdfSource"
          :page="currentPage"
          :scale="scale"
          @rendered="handleRendered"
          @loading-failed="handleError"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ArrowLeft, ArrowRight, ZoomIn, ZoomOut, FullScreen, Close, Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import VuePdfEmbed from 'vue-pdf-embed'

const props = defineProps({
  resourceId: { type: Number, required: true },
  fileUrl: { type: String, required: true },
  title: { type: String, default: '' }
})

const pdfRef = ref(null)
const contentRef = ref(null)
const pagesRef = ref(null)
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1)
const isFullscreen = ref(false)

const pdfSource = computed(() => props.fileUrl)

function handleRendered() {
  loading.value = false
  if (pdfRef.value) {
    totalPages.value = pdfRef.value.pageCount || 0
  }
}

function handleError() {
  loading.value = false
  error.value = true
}

function loadPdf() {
  loading.value = true
  error.value = false
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

function handlePageChange(val) {
  if (val >= 1 && val <= totalPages.value) {
    currentPage.value = val
  }
}

function zoomIn() {
  if (scale.value < 3) {
    scale.value = Math.min(3, scale.value + 0.25)
  }
}

function zoomOut() {
  if (scale.value > 0.5) {
    scale.value = Math.max(0.5, scale.value - 0.25)
  }
}

function resetZoom() {
  scale.value = 1
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    contentRef.value?.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

$dark-bg: #1e1e1e;
$dark-surface: #2c2c2c;
$dark-border: #3c3c3c;
$dark-text: #ccc;
$dark-text-muted: #aaa;

.pdf-reader {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $dark-bg;
}

.pdf-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-2 $space-4;
  background: $dark-surface;
  border-bottom: 1px solid $dark-border;
  flex-shrink: 0;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: $space-1;
  }

  .page-info,
  .zoom-info {
    color: $dark-text;
    font-size: $font-size-base;
    min-width: 60px;
    text-align: center;
  }
}

.pdf-content {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: $space-5;
}

.pdf-pages {
  display: flex;
  justify-content: center;
}

.pdf-loading,
.pdf-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: $dark-text;
  gap: $space-4;
}

.pdf-error {
  p {
    color: $danger;
  }
}

@include mobile {
  .pdf-toolbar {
    flex-wrap: wrap;
    gap: $space-2;
    padding: $space-2 $space-3;
  }
  .pdf-content {
    padding: $space-3;
  }
}
</style>
