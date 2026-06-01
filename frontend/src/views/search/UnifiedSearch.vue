<template>
  <div class="unified-search">
    <el-card class="search-card">
      <div class="search-header">
        <el-input
          v-model="keyword"
          placeholder="搜索图书、电子书、音频、视频..."
          size="large"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button
              :icon="Search"
              @click="handleSearch"
            />
          </template>
        </el-input>
      </div>
      <div class="search-tabs">
        <el-radio-group
          v-model="resourceType"
          @change="handleSearch"
        >
          <el-radio-button value="ALL">
            全部
          </el-radio-button>
          <el-radio-button value="PRINT">
            纸质图书
          </el-radio-button>
          <el-radio-button value="DIGITAL">
            数字资源
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card class="result-card">
      <el-table
        v-loading="loading"
        :data="results"
        stripe
      >
        <el-table-column
          prop="title"
          label="题名"
          min-width="180"
        />
        <el-table-column
          prop="author"
          label="作者"
          width="120"
        />
        <el-table-column
          prop="resourceType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="typeTag(row.resourceType)">
              {{ typeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="format"
          label="格式"
          width="80"
        />
        <el-table-column
          prop="publisher"
          label="出版社"
          width="150"
        />
        <el-table-column
          prop="availableCount"
          label="可借/访问"
          width="100"
        >
          <template #default="{ row }">
            <span v-if="row.resourceType === 'PRINT'">
              {{ row.availableCount ?? '-' }}
            </span>
            <span v-else>在线访问</span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.resourceType === 'PRINT'"
              type="primary"
              link
              @click="goBookDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-else
              type="primary"
              link
              @click="goDigitalResource(row)"
            >
              访问资源
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'UnifiedSearch' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
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

async function handleSearch() {
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
}

function goBookDetail(row) {
  router.push(`/books/${row.id}`)
}

function goDigitalResource(row) {
  if (row.accessUrl) {
    window.open(row.accessUrl, '_blank')
  } else {
    ElMessage.info('该资源暂无访问地址')
  }
}

function typeLabel(type) {
  const map = { PRINT: '纸质', EBOOK: '电子书', AUDIO: '音频', VIDEO: '视频', DATABASE: '数据库' }
  return map[type] || type
}

function typeTag(type) {
  const map = { PRINT: '', EBOOK: 'success', AUDIO: 'warning', VIDEO: 'danger', DATABASE: 'info' }
  return map[type] || ''
}
</script>

<style lang="scss" scoped>
.unified-search {
  .search-card {
    margin-bottom: 16px;
  }

  .search-header {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;

    .search-input {
      max-width: 600px;
    }
  }

  .search-tabs {
    display: flex;
    justify-content: center;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
