<template>
  <div class="page-container">
    <div class="page-header">
      <h2>图书管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>新增图书
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable>
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
            <el-option label="可借" value="AVAILABLE" />
            <el-option label="已借出" value="BORROWED" />
            <el-option label="不可借" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图书列表 -->
    <el-card>
      <!-- 骨架屏加载状态 -->
      <el-skeleton v-if="loading && bookList.length === 0" :rows="5" animated />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && bookList.length === 0" 
        description="暂无图书数据"
        :show-button="true"
        action-text="刷新"
        @action="loadBooks"
      />
      
      <!-- 数据表格 -->
      <el-table v-else :data="bookList" stripe v-loading="loading">
        <el-table-column prop="title" label="书名" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="publisher" label="出版社" width="120" show-overflow-tooltip />
        <el-table-column prop="availableCopies" label="可借/总数" width="100" align="center">
          <template #default="{ row }">
            {{ row.availableCopies }}/{{ row.totalCopies }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="bookList.length > 0" class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadBooks"
          @current-change="loadBooks"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookList, deleteBook } from '@/api/book'
import EmptyState from '@/components/EmptyState.vue'
import { useStatusMap } from '@/composables/useStatusMap'

// FIXED: P2-FE-08 - 使用公共composable替代重复的状态映射
const { getStatusType, getStatusText } = useStatusMap('book')
const router = useRouter()

const loading = ref(false)
const bookList = ref([])
const total = ref(0)
const categories = ref(['文学', '计算机', '历史', '经济', '教育', '科学', '艺术'])

const searchForm = reactive({
  keyword: '',
  category: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

onMounted(() => {
  loadBooks()
})

async function loadBooks() {
  loading.value = true
  try {
    const res = await getBookList({
      ...searchForm,
      page: pagination.page,
      size: pagination.size
    })
    bookList.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadBooks()
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  handleSearch()
}

function showAddDialog() {
  // FIXED: P2-FE-05 - 使用router.push替代window.location.hash
  router.push('/books/add')
}

function handleEdit(row) {
  router.push(`/books/add?id=${row.id}`)
}

function handleDetail(row) {
  router.push(`/books/${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除《${row.title}》吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBook(row.id)
    ElMessage.success('删除成功')
    loadBooks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// FIXED: P2-FE-08 - 状态映射已提取到composable/useStatusMap.js
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #303133;
  }
}

.search-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
