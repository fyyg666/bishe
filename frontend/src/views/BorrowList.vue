<template>
  <div class="page-container">
    <div class="page-header">
      <h2>借阅管理</h2>
      <el-button type="primary" @click="$router.push('/borrows/page')">
        <el-icon><Plus /></el-icon>新增借阅
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="读者">
          <el-input v-model="searchForm.readerName" placeholder="读者姓名" clearable />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="searchForm.bookTitle" placeholder="图书名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
            <el-option label="借阅中" value="BORROWED" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 借阅列表 -->
    <el-card>
      <!-- 骨架屏加载状态 -->
      <el-skeleton v-if="loading && borrowList.length === 0" :rows="5" animated />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && borrowList.length === 0" 
        description="暂无借阅记录"
        :show-button="true"
        action-text="刷新"
        @action="loadBorrows"
      />
      
      <!-- 数据表格 -->
      <el-table v-else :data="borrowList" stripe v-loading="loading">
        <el-table-column prop="bookTitle" label="书名" show-overflow-tooltip />
        <el-table-column prop="readerName" label="借阅人" width="100" />
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="dueDate" label="应还日期" width="120" />
        <el-table-column prop="returnDate" label="归还日期" width="120">
          <template #default="{ row }">
            {{ row.returnDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'BORROWED' || row.status === 'OVERDUE'"
              type="success"
              link
              size="small"
              @click="handleReturn(row)"
            >
              归还
            </el-button>
            <el-button type="info" link size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="borrowList.length > 0" class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadBorrows"
          @current-change="loadBorrows"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBorrowList, returnBook } from '@/api/borrow'
import EmptyState from '@/components/EmptyState.vue'
import { useStatusMap } from '@/composables/useStatusMap'

// FIXED: P2-FE-08 - 使用公共composable替代重复的状态映射
const { getStatusType, getStatusText } = useStatusMap('borrow')

const loading = ref(false)
const borrowList = ref([])
const total = ref(0)

const searchForm = reactive({
  readerName: '',
  bookTitle: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

onMounted(() => {
  loadBorrows()
})

async function loadBorrows() {
  loading.value = true
  try {
    const res = await getBorrowList({
      ...searchForm,
      page: pagination.page,
      size: pagination.size
    })
    borrowList.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载借阅列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadBorrows()
}

function resetSearch() {
  searchForm.readerName = ''
  searchForm.bookTitle = ''
  searchForm.status = ''
  handleSearch()
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm('确定要归还此图书吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await returnBook(row.id)
    ElMessage.success('归还成功')
    loadBorrows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('归还失败')
    }
  }
}

function handleDetail(row) {
  // 可扩展为弹出详情对话框
  ElMessage.info(`借阅详情: ${row.bookTitle}`)
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
