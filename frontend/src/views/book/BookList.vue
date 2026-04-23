<template>
  <div class="book-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="书名">
          <el-input v-model="searchForm.name" placeholder="请输入书名" clearable />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="searchForm.author" placeholder="请输入作者" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable>
            <el-option label="文学" value="文学" />
            <el-option label="科技" value="科技" />
            <el-option label="历史" value="历史" />
            <el-option label="艺术" value="艺术" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <div class="table-toolbar">
      <el-button type="primary" :icon="Plus" @click="$router.push('/books/add')">添加图书</el-button>
      <el-button :icon="Download" @click="handleExport">导出</el-button>
    </div>

    <!-- 表格 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="books"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="书名" min-width="150" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="publisher" label="出版社" width="150" />
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.stock > 0 ? 'success' : 'danger'">
              {{ row.stock > 0 ? '在架' : '借出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="PAGE_SIZE_OPTIONS"
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
import { Plus, Download } from '@element-plus/icons-vue'
import { useBookStore } from '@/stores/book'

// P3-003: 消除魔法值 - 提取分页常量
const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 10
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

const router = useRouter()
const bookStore = useBookStore()

const loading = ref(false)
const books = ref([])
const total = ref(0)
const selectedBooks = ref([])

const searchForm = reactive({
  name: '',
  author: '',
  category: ''
})

const pagination = reactive({
  page: DEFAULT_PAGE,
  size: DEFAULT_PAGE_SIZE
})

onMounted(() => {
  loadBooks()
})

async function loadBooks() {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      size: pagination.size
    }
    await bookStore.fetchBooks(params)
    books.value = bookStore.books
    total.value = bookStore.total
  } catch (error) {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = DEFAULT_PAGE
  loadBooks()
}

function handleReset() {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  handleSearch()
}

function handleSelectionChange(selection) {
  selectedBooks.value = selection
}

function handleDetail(row) {
  router.push(`/books/${row.id}`)
}

function handleEdit(row) {
  router.push(`/books/add?id=${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该图书吗？', '提示', {
      type: 'warning'
    })
    await bookStore.removeBook(row.id)
    ElMessage.success('删除成功')
    loadBooks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}
</script>

<style lang="scss" scoped>
.book-list {
  .search-card {
    margin-bottom: 20px;
  }

  .table-toolbar {
    margin-bottom: 20px;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
