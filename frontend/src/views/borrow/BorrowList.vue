<template>
  <div class="borrow-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form
        :model="searchForm"
        inline
      >
        <el-form-item label="图书名称">
          <el-input
            v-model="searchForm.bookName"
            placeholder="请输入图书名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
          >
            <el-option
              label="在借"
              value="BORROWING"
            />
            <el-option
              label="已归还"
              value="RETURNED"
            />
            <el-option
              label="已逾期"
              value="OVERDUE"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="borrows"
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="bookTitle"
          label="图书名称"
          min-width="150"
        />
        <el-table-column
          prop="username"
          label="读者"
          width="100"
        />
        <el-table-column
          prop="borrowDate"
          label="借阅日期"
          width="120"
        />
        <el-table-column
          prop="dueDate"
          label="应还日期"
          width="120"
        />
        <el-table-column
          prop="returnDate"
          label="实际归还日期"
          width="120"
        />
        <el-table-column
          label="逾期罚款"
          width="120"
        >
          <template #default="{ row }">
            <span
              v-if="row.fineAmount > 0"
              style="color: #f56c6c; font-weight: 600;"
            >
              ¥{{ formatFine(row.fineAmount) }}
            </span>
            <span
              v-else
              style="color: #909399;"
            >
              -
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="200"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'BORROWING'"
              type="primary"
              link
              @click="handleRenew(row)"
            >
              续借
            </el-button>
            <el-button
              v-if="row.status === 'BORROWING'"
              type="success"
              link
              @click="handleReturn(row)"
            >
              还书
            </el-button>
            <el-button
              type="info"
              link
              @click="handleDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
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
  current: 1,
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
      current: pagination.current,
      size: pagination.size
    }
    // 使用/my查询当前用户借阅记录
    await borrowStore.fetchMyBorrows(params)
    // FIXED: 从myBorrows读取而非borrows（fetchMyBorrows填充的是myBorrows）
    borrows.value = borrowStore.myBorrows
    total.value = borrowStore.total
  } catch {
    ElMessage.error('加载借阅记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
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
    if (error !== 'cancel' && error !== 'close') {
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
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('还书失败')
    }
  }
}

function handleDetail(row) {
  const statusMap = {
    BORROWING: '在借',
    RETURNED: '已归还',
    OVERDUE: '已逾期'
  }
  const detailLines = [
    `借阅ID：${row.id}`,
    `图书名称：${row.bookTitle}`,
    `状态：${statusMap[row.status] || row.status}`,
    `借阅日期：${row.borrowDate || '-'}`,
    `应还日期：${row.dueDate || '-'}`,
    `实际归还日期：${row.returnDate || '-'}`,
    `罚金：${row.fineAmount != null ? row.fineAmount + ' 元' : '-'}`
  ]
  ElMessageBox.alert(detailLines.join('<br>'), '借阅详情', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '确定'
  })
}

function formatFine(amount) {
  return (amount / 100).toFixed(2)
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
