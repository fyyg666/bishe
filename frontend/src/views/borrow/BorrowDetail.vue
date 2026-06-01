<template>
  <div class="borrow-detail">
    <el-page-header @back="$router.back()" content="借阅详情" />

    <div v-loading="loading" class="detail-content">
      <template v-if="detail">
        <el-card class="info-card">
          <template #header>
            <span>图书信息</span>
          </template>
          <div class="book-info">
            <el-image
              v-if="detail.bookCover"
              :src="detail.bookCover"
              fit="cover"
              class="book-cover"
            />
            <div v-else class="book-cover book-cover-placeholder">
              <el-icon :size="40"><Picture /></el-icon>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="书名">{{ detail.bookTitle }}</el-descriptions-item>
              <el-descriptions-item label="作者">{{ detail.bookAuthor }}</el-descriptions-item>
              <el-descriptions-item label="ISBN">{{ detail.bookIsbn }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(detail.status)">
                  {{ getStatusText(detail.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <el-card class="timeline-card">
          <template #header>
            <span>借阅时间线</span>
          </template>
          <el-timeline>
            <el-timeline-item
              timestamp="借阅日期"
              placement="top"
              type="primary"
            >
              <el-card shadow="never">
                <p>{{ detail.borrowDate || '-' }}</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item
              timestamp="应还日期"
              placement="top"
              :type="isOverdue ? 'danger' : 'warning'"
            >
              <el-card shadow="never">
                <p>{{ detail.dueDate || '-' }}</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item
              v-if="detail.returnDate"
              timestamp="实际归还日期"
              placement="top"
              type="success"
            >
              <el-card shadow="never">
                <p>{{ detail.returnDate }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card v-if="isOverdue" class="fine-card">
          <template #header>
            <span style="color: #f56c6c;">逾期信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="逾期天数">
              <span style="color: #f56c6c; font-weight: 600;">{{ overdueDays }} 天</span>
            </el-descriptions-item>
            <el-descriptions-item label="罚款金额">
              <span style="color: #f56c6c; font-weight: 600;">¥{{ formatFine(detail.fineAmount) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card v-if="detail.renewHistory && detail.renewHistory.length" class="renew-card">
          <template #header>
            <span>续借记录</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in detail.renewHistory"
              :key="index"
              :timestamp="item.renewDate"
              placement="top"
            >
              <el-card shadow="never">
                <p>续借 {{ item.extendDays }} 天，新到期日：{{ item.newDueDate }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card v-if="detail.creditChange != null" class="credit-card">
          <template #header>
            <span>信用积分变动</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="积分变动">
              <span :style="{ color: detail.creditChange < 0 ? '#f56c6c' : '#67c23a', fontWeight: 600 }">
                {{ detail.creditChange > 0 ? '+' : '' }}{{ detail.creditChange }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </template>

      <el-empty v-else-if="!loading" description="未找到借阅记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getBorrowDetail } from '@/api/borrow'
import { useStatusMap } from '@/composables/useStatusMap'

const { getStatusType, getStatusText } = useStatusMap('borrow')

const route = useRoute()
const loading = ref(false)
const detail = ref(null)

const isOverdue = computed(() => detail.value?.status === 'OVERDUE')

const overdueDays = computed(() => {
  if (!detail.value || !isOverdue.value) return 0
  const due = new Date(detail.value.dueDate)
  const now = detail.value.returnDate ? new Date(detail.value.returnDate) : new Date()
  const diff = Math.ceil((now - due) / (1000 * 60 * 60 * 24))
  return Math.max(diff, 0)
})

function formatFine(amount) {
  if (amount == null) return '0.00'
  return (amount / 100).toFixed(2)
}

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getBorrowDetail(id)
    detail.value = res.data || res
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.borrow-detail {
  .el-page-header {
    margin-bottom: 20px;
  }

  .detail-content {
    min-height: 200px;
  }

  .info-card,
  .timeline-card,
  .fine-card,
  .renew-card,
  .credit-card {
    margin-bottom: 20px;
  }

  .book-info {
    display: flex;
    gap: 24px;
    align-items: flex-start;

    .book-cover {
      width: 120px;
      height: 160px;
      flex-shrink: 0;
      border-radius: 4px;
    }

    .book-cover-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      color: #c0c4cc;
    }

    .el-descriptions {
      flex: 1;
    }
  }
}
</style>
