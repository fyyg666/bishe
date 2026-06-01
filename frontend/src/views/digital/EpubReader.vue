<template>
  <div class="epub-reader" :class="themeClass">
    <div class="epub-sidebar" v-show="showToc">
      <div class="sidebar-header">
        <span>目录</span>
        <el-button :icon="Close" link @click="showToc = false" />
      </div>
      <div class="toc-list">
        <div
          v-for="(item, index) in tocItems"
          :key="index"
          class="toc-item"
          :class="{ active: currentLocation === item.href }"
          :style="{ paddingLeft: (item.level || 0) * 16 + 12 + 'px' }"
          @click="goToTocItem(item)"
        >
          {{ item.label }}
        </div>
      </div>
    </div>

    <div class="epub-main">
      <div class="epub-toolbar">
        <div class="toolbar-left">
          <el-button :icon="List" @click="showToc = !showToc">目录</el-button>
          <el-button :icon="ZoomOut" @click="decreaseFontSize" :disabled="fontSize <= 12">A-</el-button>
          <span class="font-size-info">{{ fontSize }}px</span>
          <el-button :icon="ZoomIn" @click="increaseFontSize" :disabled="fontSize >= 28">A+</el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="setTheme('light')" :class="{ active: theme === 'light' }">浅色</el-button>
          <el-button @click="setTheme('sepia')" :class="{ active: theme === 'sepia' }">护眼</el-button>
          <el-button @click="setTheme('dark')" :class="{ active: theme === 'dark' }">深色</el-button>
          <el-button :icon="isFullscreen ? Close : FullScreen" @click="toggleFullscreen">
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </el-button>
        </div>
      </div>

      <div class="epub-content" ref="contentRef">
        <div v-if="loading" class="epub-loading">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          <p>正在加载电子书...</p>
        </div>
        <div v-else-if="error" class="epub-error">
          <el-icon :size="40" color="#f56c6c"><CircleCloseFilled /></el-icon>
          <p>电子书加载失败</p>
          <el-button type="primary" @click="loadEpub">重新加载</el-button>
        </div>
        <div v-show="!loading && !error" ref="bookRef" class="epub-book"></div>
      </div>

      <div class="epub-nav">
        <el-button :icon="ArrowLeft" @click="prevSection" size="large">上一章</el-button>
        <span class="nav-info">{{ currentChapter }}</span>
        <el-button @click="nextSection" size="large">
          下一章<el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ArrowLeft, ArrowRight, ZoomIn, ZoomOut, FullScreen, Close, Loading, CircleCloseFilled, List } from '@element-plus/icons-vue'
import ePub from 'epubjs'

const props = defineProps({
  resourceId: { type: Number, required: true },
  fileUrl: { type: String, required: true },
  title: { type: String, default: '' }
})

const bookRef = ref(null)
const contentRef = ref(null)
const loading = ref(true)
const error = ref(false)
const showToc = ref(false)
const tocItems = ref([])
const currentLocation = ref('')
const currentChapter = ref('')
const fontSize = ref(16)
const theme = ref('light')
const isFullscreen = ref(false)

let book = null
let rendition = null

const themeClass = computed(() => `theme-${theme.value}`)

const themeStyles = {
  light: { body: { background: '#fff', color: '#333' } },
  sepia: { body: { background: '#f4ecd8', color: '#5b4636' } },
  dark: { body: { background: '#1e1e1e', color: '#ccc' } }
}

function loadEpub() {
  loading.value = true
  error.value = false

  try {
    book = ePub(props.fileUrl)
    rendition = book.renderTo(bookRef.value, {
      width: '100%',
      height: '100%',
      spread: 'none'
    })

    rendition.themes.default({
      'html': { 'font-size': `${fontSize.value}px !important` }
    })

    Object.entries(themeStyles).forEach(([name, styles]) => {
      rendition.themes.register(name, styles)
    })

    rendition.themes.select(theme.value)

    rendition.on('relocated', (location) => {
      currentLocation.value = location.start.href
      if (location.start.label) {
        currentChapter.value = location.start.label
      }
    })

    book.loaded.navigation.then((navigation) => {
      tocItems.value = flattenToc(navigation.toc)
    })

    rendition.display().then(() => {
      loading.value = false
    }).catch(() => {
      loading.value = false
      error.value = true
    })
  } catch {
    loading.value = false
    error.value = true
  }
}

function flattenToc(toc, level = 0) {
  const items = []
  for (const item of toc) {
    items.push({
      label: item.label.trim(),
      href: item.href,
      level
    })
    if (item.subitems && item.subitems.length) {
      items.push(...flattenToc(item.subitems, level + 1))
    }
  }
  return items
}

function goToTocItem(item) {
  if (rendition) {
    rendition.display(item.href)
    showToc.value = false
  }
}

function prevSection() {
  if (rendition) {
    rendition.prev()
  }
}

function nextSection() {
  if (rendition) {
    rendition.next()
  }
}

function increaseFontSize() {
  if (fontSize.value < 28) {
    fontSize.value += 2
    updateFontSize()
  }
}

function decreaseFontSize() {
  if (fontSize.value > 12) {
    fontSize.value -= 2
    updateFontSize()
  }
}

function updateFontSize() {
  if (rendition) {
    rendition.themes.default({
      'html': { 'font-size': `${fontSize.value}px !important` }
    })
  }
}

function setTheme(name) {
  theme.value = name
  if (rendition) {
    rendition.themes.select(name)
  }
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
  nextTick(() => {
    loadEpub()
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (rendition) {
    rendition.destroy()
  }
  if (book) {
    book.destroy()
  }
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
$sepia-bg: #f4ecd8;
$sepia-text: #5b4636;
$sepia-surface: #ebe2cc;
$sepia-border: #d4c9af;
$sepia-border-light: #e8dcc4;
$sepia-text-muted: #8b7355;
$light-surface: #f5f5f5;
$light-border: #e0e0e0;
$light-border-light: #f0f0f0;
$light-text-muted: #666;

.epub-reader {
  display: flex;
  height: 100%;
  background: $bg-white;
  color: $text-primary;

  &.theme-sepia {
    background: $sepia-bg;
    color: $sepia-text;
  }

  &.theme-dark {
    background: $dark-bg;
    color: $dark-text;
  }
}

.epub-sidebar {
  width: 280px;
  border-right: 1px solid $light-border;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  background: inherit;

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $space-3 $space-4;
    font-weight: $font-weight-semibold;
    font-size: $font-size-lg;
    border-bottom: 1px solid $light-border;
  }

  .toc-list {
    flex: 1;
    overflow-y: auto;
  }

  .toc-item {
    padding: $space-2 $space-3;
    cursor: pointer;
    font-size: $font-size-base;
    border-bottom: 1px solid $light-border-light;
    transition: background $transition-fast;

    &:hover {
      background: rgba(64, 158, 255, 0.1);
    }

    &.active {
      color: $primary;
      font-weight: $font-weight-semibold;
    }
  }
}

.epub-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.epub-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-2 $space-4;
  background: $light-surface;
  border-bottom: 1px solid $light-border;
  flex-shrink: 0;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: $space-1;
  }

  .font-size-info {
    font-size: $font-size-sm;
    min-width: 40px;
    text-align: center;
    color: $light-text-muted;
  }
}

.epub-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.epub-book {
  width: 100%;
  height: 100%;
}

.epub-loading,
.epub-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: $space-4;
}

.epub-error {
  p {
    color: $danger;
  }
}

.epub-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-3 $space-6;
  background: $light-surface;
  border-top: 1px solid $light-border;
  flex-shrink: 0;

  .nav-info {
    font-size: $font-size-base;
    color: $light-text-muted;
    text-align: center;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 0 $space-3;
  }
}

.theme-dark {
  .epub-toolbar,
  .epub-nav {
    background: $dark-surface;
    border-color: $dark-border;
  }

  .epub-sidebar {
    border-color: $dark-border;

    .sidebar-header {
      border-color: $dark-border;
    }

    .toc-item {
      border-color: $dark-border;

      &:hover {
        background: rgba(64, 158, 255, 0.15);
      }
    }
  }

  .font-size-info,
  .nav-info {
    color: $dark-text-muted;
  }
}

.theme-sepia {
  .epub-toolbar,
  .epub-nav {
    background: $sepia-surface;
    border-color: $sepia-border;
  }

  .epub-sidebar {
    border-color: $sepia-border;

    .sidebar-header {
      border-color: $sepia-border;
    }

    .toc-item {
      border-color: $sepia-border-light;
    }
  }

  .font-size-info,
  .nav-info {
    color: $sepia-text-muted;
  }
}

@include mobile {
  .epub-sidebar {
    position: absolute;
    z-index: $z-overlay;
    top: 0;
    left: 0;
    bottom: 0;
    box-shadow: $shadow-lg;
  }
  .epub-toolbar {
    flex-wrap: wrap;
    gap: $space-2;
  }
}
</style>
