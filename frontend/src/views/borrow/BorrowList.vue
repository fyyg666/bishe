<template>
  <div class="borrow-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="图书名称">
          <el-input v-model="searchForm.bookName" placeholder="请输入图书名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="在借" value="BORROWING" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table v-loading="loading" :data="borrows" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="bookName" label="图书名称" min-width="150" />
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="dueDate" label="应还日期" width="120" />
        <el-table-column prop="returnDate" label="实际归还日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link v-if="row.status === 'BORROWING'" @click="handleRenew(row)">续借</el-button>
            <el-button type="success" link v-if="row.status === 'BORROWING'" @click="handleReturn(row)">还书</el-button>
            <el-button type="info" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
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
import { useBorrowStore } from '@/stores/borrow'
import { useStatusMap } from '@/composables/useStatusMap'

// FIXED: P2-FE-08 - 使用公共composable替代重复的状态映射
const { getStatusType, getStatusText } = useStatusMap('borrow')

const borrowStore = useBorrowStore()

const loading = ref(false)
const borrows = ref([])
const total = ref(0)

const searchForm = reactive({
  bookName: '',
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
    const params = {
      ...searchForm,
      page: pagination.page,
      size: pagination.size
    }
    await borrowStore.fetchBorrows(params)
    borrows.value = borrowStore.borrows
    total.value = borrowStore.total
  } catch (error) {
    ElMessage.error('加载借阅记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadBorrows()
}

function handleReset() {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  handleSearch()
}

async function handleRenew(row) {
  try {
    await ElMessageBox.confirm('确定要续借该书吗？', '提示', { type: 'info' })
    await borrowStore.renew(row.id, { extendDays: 30 })
    ElMessage.success('续借成功')
    loadBorrows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('续借失败')
    }
  }
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm('确定要归还该书吗？', '提示', { type: 'info' })
    await borrowStore.returnBookById(row.id)
    ElMessage.success('还书成功')
    loadBorrows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('还书失败')
    }
  }
}

function handleDetail(row) {
  ElMessage.info('详情功能开发中')
}
</script>

<style lang="scss" scoped>
.borrow-list {
  .search-card {
    margin-bottom: 20px;
  }
  
  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
