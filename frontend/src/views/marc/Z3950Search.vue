<template>
  <div class="z3950-search">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="数据源">
          <el-select v-model="searchForm.sourceId" placeholder="全部数据源" clearable style="width: 200px">
            <el-option v-for="s in sources" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检索类型">
          <el-select v-model="searchForm.queryType" style="width: 120px">
            <el-option label="关键词" value="keyword" />
            <el-option label="题名" value="title" />
            <el-option label="作者" value="author" />
            <el-option label="ISBN" value="isbn" />
          </el-select>
        </el-form-item>
        <el-form-item label="检索词">
          <el-input v-model="searchForm.query" placeholder="输入检索词" clearable style="width: 260px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="searching" @click="handleSearch">检索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="table-toolbar">
      <el-button type="primary" :icon="Download" :disabled="!allRecords.length" @click="handleImportAll">批量导入全部</el-button>
    </div>

    <el-card v-for="result in searchResults" :key="result.sourceName" class="result-card">
      <template #header>
        <div class="result-header">
          <span>{{ result.sourceName }}</span>
          <el-tag type="info" size="small">共 {{ result.totalResults }} 条</el-tag>
        </div>
      </template>
      <el-table :data="result.records" stripe>
        <el-table-column prop="title" label="题名" min-width="200" />
        <el-table-column prop="author" label="作者" width="150" />
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="publisher" label="出版社" width="150" />
        <el-table-column prop="publishDate" label="出版日期" width="120" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleImportOne(row)">导入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="searched && !searchResults.length" description="未检索到结果" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getZ3950Sources, searchZ3950, searchAllZ3950, importZ3950ToMarc } from '@/api/z3950'

const sources = ref([])
const searchResults = ref([])
const searching = ref(false)
const searched = ref(false)

const searchForm = reactive({
  sourceId: null,
  query: '',
  queryType: 'keyword'
})

const allRecords = computed(() => searchResults.value.flatMap(r => r.records))

onMounted(() => {
  loadSources()
})

async function loadSources() {
  try {
    const res = await getZ3950Sources()
    sources.value = res.data || []
  } catch {
    ElMessage.error('加载数据源失败')
  }
}

async function handleSearch() {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索词')
    return
  }
  searching.value = true
  searched.value = false
  try {
    if (searchForm.sourceId) {
      const res = await searchZ3950(searchForm.sourceId, searchForm.query, searchForm.queryType)
      searchResults.value = res.data ? [res.data] : []
    } else {
      const res = await searchAllZ3950(searchForm.query, searchForm.queryType)
      searchResults.value = res.data || []
    }
  } catch {
    ElMessage.error('检索失败')
    searchResults.value = []
  } finally {
    searching.value = false
    searched.value = true
  }
}

function handleReset() {
  searchForm.sourceId = null
  searchForm.query = ''
  searchForm.queryType = 'keyword'
  searchResults.value = []
  searched.value = false
}

async function handleImportOne(record) {
  try {
    await ElMessageBox.confirm(`确定要导入「${record.title}」到本地编目吗？`, '提示', { type: 'info' })
    await importZ3950ToMarc(searchForm.sourceId, record.isbn || record.title, 'isbn')
    ElMessage.success('导入成功')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('导入失败')
    }
  }
}

async function handleImportAll() {
  try {
    await ElMessageBox.confirm(`确定要批量导入全部 ${allRecords.value.length} 条记录吗？`, '提示', { type: 'warning' })
    const sourceId = searchForm.sourceId
    if (sourceId) {
      await importZ3950ToMarc(sourceId, searchForm.query, searchForm.queryType)
      ElMessage.success('批量导入成功')
    } else {
      for (const result of searchResults.value) {
        const source = sources.value.find(s => s.name === result.sourceName)
        if (source && result.records.length) {
          try {
            await importZ3950ToMarc(source.id, searchForm.query, searchForm.queryType)
          } catch {
            // continue importing other sources
          }
        }
      }
      ElMessage.success('批量导入完成')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('批量导入失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.z3950-search {
  .search-card {
    margin-bottom: 16px;
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .result-card {
    margin-bottom: 16px;
    border: none;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  .result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
</style>
