<template>
  <div class="unified-search">
    <!-- Hero Search — Apple-style centered -->
    <div class="search-hero">
      <h1 class="search-title">统一检索</h1>
      <p class="search-subtitle">搜索图书馆的纸质图书、电子书、音频与视频资源</p>
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索书名、作者、关键词…"
          size="large"
          clearable
          class="search-input"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button
              :icon="Search"
              class="search-btn"
              @click="handleSearch"
            >
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
      <div class="search-tabs">
        <el-radio-group
          v-model="resourceType"
          size="small"
          @change="handleSearch"
        >
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="PRINT">纸质图书</el-radio-button>
          <el-radio-button value="DIGITAL">数字资源</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Results -->
    <div class="result-area" v-if="keyword || results.length">
      <div v-if="loading" class="result-loading">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton-line skeleton-title" />
          <div class="skeleton-line skeleton-meta" />
        </div>
      </div>

      <template v-else>
        <div v-if="total > 0" class="result-summary">
          找到 <strong>{{ total }}</strong> 条结果
        </div>

        <div class="result-list">
          <div
            v-for="row in results"
            :key="row.id"
            class="result-item"
            @click="row.resourceType === 'PRINT' ? goBookDetail(row) : goDigitalResource(row)"
          >
            <div class="result-icon">
              <el-icon :size="24">
                <Reading v-if="row.resourceType === 'PRINT'" />
                <Monitor v-else-if="row.resourceType === 'EBOOK'" />
                <Headset v-else-if="row.resourceType === 'AUDIO'" />
                <VideoCamera v-else-if="row.resourceType === 'VIDEO'" />
                <Coin v-else />
              </el-icon>
            </div>
            <div class="result-body">
              <div class="result-title">{{ row.title }}</div>
              <div class="result-meta">
                <span v-if="row.author" class="meta-author">{{ row.author }}</span>
                <span v-if="row.publisher" class="meta-publisher">{{ row.publisher }}</span>
                <span class="meta-divider" v-if="row.author || row.publisher">·</span>
                <el-tag size="small" :type="typeTag(row.resourceType)" class="meta-tag">
                  {{ typeLabel(row.resourceType) }}
                </el-tag>
              </div>
              <div v-if="row.resourceType === 'PRINT'" class="result-status">
                <span :class="row.availableCount > 0 ? 'status-available' : 'status-unavailable'">
                  {{ row.availableCount > 0 ? `可借 (${row.availableCount}册)` : '已借出' }}
                </span>
              </div>
            </div>
            <div class="result-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <el-empty
          v-if="total === 0 && keyword"
          description="未找到相关结果，请尝试其他关键词"
          :image-size="80"
        />

        <div v-if="total > 0" class="pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, prev, pager, next, sizes"
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </template>
    </div>

    <!-- Empty state -->
    <div v-else class="search-empty">
      <el-icon class="empty-icon" :size="40"><Search /></el-icon>
      <p class="empty-text">输入关键词开始搜索图书馆资源</p>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'UnifiedSearch' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Reading, Monitor, Headset, VideoCamera, Coin, ArrowRight } from '@element-plus/icons-vue'
import { unifiedSearch } from '@/api/digitalResource'

const router = useRouter()
const loading = ref(false)
const results = ref([])
const total = ref(0)
const keyword = ref('')
const resourceType = ref('ALL')

const pagination = reactive({
  current: 1,
  size: 10
})

onMounted(() => {
  handleSearch()
})

let searchTimer = null

async function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    loading.value = true
    try {
      const params = {
        current: pagination.current,
        size: pagination.size,
        resourceType: resourceType.value
      }
      if (keyword.value) params.keyword = keyword.value
      const res = await unifiedSearch(params)
      results.value = res.data?.records || []
      total.value = res.data?.total || 0
    } catch {
      ElMessage.error('搜索失败')
    } finally {
      loading.value = false
    }
  }, 300)
}

function goBookDetail(row) {
  router.push(`/books/${row.id}`)
}

function goDigitalResource(row) {
  if (row.accessUrl) {
    const url = row.accessUrl
    if (/^https?:\/\//i.test(url)) {
      window.open(url, '_blank', 'noopener,noreferrer')
    } else {
      ElMessage.warning('无效的资源地址')
    }
  } else {
    ElMessage.info('该资源暂无访问地址')
  }
}

function typeLabel(type) {
  const map = { PRINT: '纸质', EBOOK: '电子书', AUDIO: '音频', VIDEO: '视频', DATABASE: '数据库' }
  return map[type] || type
}

function typeTag(type) {
  const map = { PRINT: '', BOOK: '', EBOOK: 'success', AUDIO: 'warning', VIDEO: 'danger', DATABASE: 'info' }
  return map[type] || ''
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.unified-search {
  max-width: $content-max-width;
  margin: 0 auto;
}

/* ── Hero Search ──────────────────────── */
.search-hero {
  text-align: center;
  padding: $space-12 0 $space-8;

  @include mobile {
    padding: $space-6 0 $space-5;
  }
}

.search-title {
  font-size: $font-size-3xl;
  font-weight: $font-weight-bold;
  letter-spacing: -0.022em;
  color: $text-primary;
  margin-bottom: $space-2;

  @include mobile {
    font-size: 24px;
  }
}

.search-subtitle {
  font-size: $font-size-lg;
  color: $text-secondary;
  margin-bottom: $space-8;

  @include mobile {
    font-size: $font-size-sm;
    margin-bottom: $space-5;
  }
}

.search-box {
  max-width: 640px;
  margin: 0 auto $space-5;

  .search-input {
    :deep(.el-input__wrapper) {
      background: $bg-input;
      border-radius: $radius-lg;
      border: 1px solid transparent;
      box-shadow: none !important;
      height: 48px;
      padding-left: 16px;
      transition: all $transition-base;

      &:hover {
        background: $gray-200;
      }

      &.is-focus {
        background: $bg-card;
        border-color: $primary;
        box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.12) !important;
      }
    }

    :deep(.el-input__inner) {
      font-size: 16px;

      &::placeholder {
        color: $text-placeholder;
      }
    }

    :deep(.el-input-group__append) {
      background: $primary;
      border: none;
      border-radius: 0 $radius-lg $radius-lg 0;
      box-shadow: none !important;
      overflow: hidden;
    }

    .search-btn {
      height: 48px;
      border-radius: 0;
      margin: -1px -1px -1px 0;
      background: $primary;
      border: none;
      color: #fff;
      font-weight: $font-weight-medium;
      padding: 0 24px;

      &:hover {
        background: $primary-dark;
      }
    }
  }
}

.search-tabs {
  display: flex;
  justify-content: center;
}

/* ── Results ──────────────────────────── */
.result-area {
  min-height: 200px;
}

.result-summary {
  font-size: $font-size-sm;
  color: $text-secondary;
  margin-bottom: $space-4;
  padding: 0 $space-1;

  strong {
    color: $text-primary;
    font-weight: $font-weight-semibold;
  }
}

.result-list {
  @include card($padding: 0, $radius: $radius-lg);
  overflow: hidden;

  .result-item {
    display: flex;
    align-items: center;
    gap: $space-4;
    padding: $space-4 $space-5;
    cursor: pointer;
    transition: background $transition-fast;

    &:not(:last-child) {
      border-bottom: 1px solid $border-light;
    }

    &:hover {
      background: $gray-50;
    }
  }

  .result-icon {
    width: 44px;
    height: 44px;
    border-radius: $radius-md;
    background: $primary-lighter;
    color: $primary;
    @include flex-center;
    flex-shrink: 0;
  }

  .result-body {
    flex: 1;
    min-width: 0;
  }

  .result-title {
    font-size: $font-size-base;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    margin-bottom: 4px;
    @include truncate;
  }

  .result-meta {
    display: flex;
    align-items: center;
    gap: $space-2;
    flex-wrap: wrap;

    .meta-author,
    .meta-publisher {
      font-size: $font-size-xs;
      color: $text-secondary;
    }

    .meta-divider {
      color: $text-placeholder;
    }

    .meta-tag {
      font-size: 11px;
    }
  }

  .result-status {
    margin-top: 4px;

    .status-available {
      font-size: $font-size-xs;
      color: $success;
      font-weight: $font-weight-medium;
    }

    .status-unavailable {
      font-size: $font-size-xs;
      color: $text-secondary;
    }
  }

  .result-arrow {
    color: $text-placeholder;
    flex-shrink: 0;
    transition: transform $transition-fast;

    .result-item:hover & {
      transform: translateX(3px);
      color: $primary;
    }
  }
}

/* ── Skeleton ──────────────────────────── */
.result-loading {
  padding: $space-4 0;
}

.skeleton-row {
  padding: $space-4 $space-5;
  border-radius: $radius-md;
  margin-bottom: $space-2;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: $gray-100;
  margin-bottom: 8px;
  animation: pulse 1.5s ease-in-out infinite;

  &.skeleton-title {
    width: 60%;
    height: 16px;
  }

  &.skeleton-meta {
    width: 40%;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.8; }
}

/* ── Empty State ──────────────────────── */
.search-empty {
  text-align: center;
  padding: $space-16 0;
  color: $text-secondary;

  .empty-icon {
    margin-bottom: $space-4;
    opacity: 0.3;
  }

  .empty-text {
    font-size: $font-size-lg;
  }
}

/* ── Pagination ──────────────────────── */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $space-5;

  @include mobile {
    justify-content: center;
  }
}
</style>
