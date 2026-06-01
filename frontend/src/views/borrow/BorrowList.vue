<template>
  <div class="borrow-list">
    <el-card class="search-card">
      <div class="search-header" @click="isMobile && (searchCollapsed = !searchCollapsed)">
        <span v-if="isMobile" class="search-toggle">
          <el-icon><Search /></el-icon>
          <span>搜索筛选</span>
          <el-icon class="toggle-arrow" :class="{ expanded: !searchCollapsed }"><ArrowDown /></el-icon>
        </span>
      </div>
      <el-form
        v-show="!isMobile || !searchCollapsed"
        :model="searchForm"
        inline
        :class="{ 'mobile-form': isMobile }"
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
          <el-button
            type="success"
            @click="handleExport"
          >
            <span v-if="!isMobile">导出Excel</span>
            <el-icon v-else><Download /></el-icon>
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="table-wrapper">
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
            v-if="!isMobile"
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
            v-if="!isMobile"
            prop="dueDate"
            label="应还日期"
            width="120"
          />
          <el-table-column
            v-if="!isMobile"
            prop="returnDate"
            label="实际归还日期"
            width="120"
          />
          <el-table-column
            v-if="!isMobile"
            label="逾期罚款"
            width="120"
          >
            <template #default="{ row }">
              <span v-if="row.fineAmount > 0" class="fine-amount">
                ¥{{ formatFine(row.fineAmount) }}
              </span>
              <span v-else class="fine-none">-</span>
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
            :width="isMobile ? '120' : '160'"
            fixed="right"
          >
            <template #default="{ row }">
              <div class="table-actions">
                <el-tooltip content="续借" placement="top">
                  <el-button
                    v-if="row.status === 'BORROWING'"
                    type="primary"
                    link
                    :icon="RefreshRight"
                    @click="handleRenew(row)"
                  />
                </el-tooltip>
                <el-tooltip content="还书" placement="top">
                  <el-button
                    v-if="row.status === 'BORROWING'"
                    type="success"
                    link
                    :icon="CircleCheck"
                    @click="handleReturn(row)"
                  />
                </el-tooltip>
                <el-tooltip content="详情" placement="top">
                  <el-button
                    type="primary"
                    link
                    :icon="View"
                    @click="handleDetail(row)"
                  />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
          :size="isMobile ? 'small' : 'default'"
          @size-change="loadBorrows"
          @current-change="loadBorrows"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'Borrows' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, CircleCheck, View } from '@element-plus/icons-vue'
import { useBorrowStore } from '@/stores/borrow'
import { exportBorrows } from '@/api/borrow'
import { useStatusMap } from '@/composables/useStatusMap'
import { useResponsive } from '@/composables/useResponsive'

const { getStatusType, getStatusText } = useStatusMap('borrow')
const { isMobile } = useResponsive()

const borrowStore = useBorrowStore()
const router = useRouter()

const loading = ref(false)
const borrows = ref([])
const total = ref(0)
const searchCollapsed = ref(true)

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
    await borrowStore.fetchMyBorrows(params)
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
  router.push(`/borrows/${row.id}`)
}

function formatFine(amount) {
  return (amount / 100).toFixed(2)
}

function handleExport() {
  const params = {}
  if (searchForm.status) params.status = searchForm.status
  exportBorrows(params).then(res => {
    const blob = res instanceof Blob ? res : new Blob([res.data || res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `借阅记录_${new Date().toISOString().slice(0,10)}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  }).catch(() => ElMessage.error('导出失败'))
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.borrow-list {
  .search-card {
    margin-bottom: $space-5;
  }

  .search-header {
    display: none;
  }

  .search-toggle {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 14px;
    color: $text-regular;

    .toggle-arrow {
      transition: transform 0.2s;
      &.expanded {
        transform: rotate(180deg);
      }
    }
  }

  .mobile-form {
    :deep(.el-form-item) {
      margin-right: 0;
      width: 100%;
    }
  }

  .table-wrapper {
    overflow-x: auto;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $space-5;
  }
}

/* ── Fine Amount ─────────────────────── */
.fine-amount {
  color: $danger;
  font-weight: $font-weight-semibold;
  font-variant-numeric: tabular-nums;
}

.fine-none {
  color: $text-secondary;
}

/* ── Table Actions ───────────────────── */
.table-actions {
  display: flex;
  gap: $space-1;
  align-items: center;
}

@include mobile {
  .borrow-list {
    .search-header {
      display: block;
    }

    .pagination {
      justify-content: center;
    }
  }
}
</style>
