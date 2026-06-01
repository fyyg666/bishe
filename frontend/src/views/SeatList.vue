<template>
  <div class="page-container">
    <div class="page-header">
      <h2>座位预约</h2>
      <el-button
        type="success"
        @click="$router.push('/seats/map')"
      >
        <el-icon><Grid /></el-icon>可视化选座
      </el-button>
      <el-button
        type="primary"
        @click="$router.push('/seats/reserve')"
      >
        <el-icon><Calendar /></el-icon>预约座位
      </el-button>
    </div>

    <!-- 座位状态概览 -->
    <el-row
      :gutter="20"
      class="stats-row"
    >
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">
            {{ seatStats.total }}
          </div>
          <div class="stat-label">
            座位总数
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value available">
            {{ seatStats.available }}
          </div>
          <div class="stat-label">
            可用座位
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value occupied">
            {{ seatStats.occupied }}
          </div>
          <div class="stat-label">
            已占用
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value maintenance">
            {{ seatStats.maintenance }}
          </div>
          <div class="stat-label">
            维护中
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 我的预约 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的预约</span>
          <el-button
            type="primary"
            link
            @click="loadReservations"
          >
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- 骨架屏加载状态 -->
      <el-skeleton
        v-if="loading && reservations.length === 0"
        :rows="5"
        animated
      />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && reservations.length === 0" 
        description="暂无座位预约"
        :show-button="true"
        action-text="预约座位"
        @action="$router.push('/seats/reserve')"
      />
      
      <!-- 数据表格 -->
      <el-table
        v-else
        v-loading="loading"
        :data="reservations"
        stripe
      >
        <el-table-column
          prop="seatNumber"
          label="座位号"
          width="100"
        />
        <el-table-column
          prop="area"
          label="区域"
          width="120"
        />
        <el-table-column
          prop="reservationDate"
          label="预约日期"
          width="120"
        />
        <el-table-column
          prop="startTime"
          label="开始时间"
          width="100"
        />
        <el-table-column
          prop="endTime"
          label="结束时间"
          width="100"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              link
              size="small"
              @click="handleCancel(row)"
            >
              取消预约
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleCheckIn(row)"
            >
              签到
            </el-button>
            <el-button
              v-if="row.status === 'CHECKED_IN'"
              type="warning"
              link
              size="small"
              @click="handleCheckOut(row)"
            >
              签退
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="reservations.length > 0"
        class="pagination"
      >
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          layout="total, prev, pager, next"
          @size-change="loadReservations"
          @current-change="loadReservations"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'SeatList' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSeatMap, getMyReservations, cancelReserve, checkIn, checkOut } from '@/api/seat'
import EmptyState from '@/components/EmptyState.vue'
import { useStatusMap } from '@/composables/useStatusMap'

// FIXED: P2-FE-08 - 使用公共composable替代重复的状态映射
const { getStatusType, getStatusText } = useStatusMap('seat')

const loading = ref(false)
const reservations = ref([])
const total = ref(0)

const seatStats = reactive({
  total: 0,
  available: 0,
  occupied: 0,
  maintenance: 0
})

const pagination = reactive({
  current: 1,
  size: 10
})

onMounted(() => {
  loadSeatStats()
  loadReservations()
})

async function loadSeatStats() {
  try {
    const res = await getSeatMap()
    if (res.data) {
      const seats = res.data.records || res.data || []
      seatStats.total = seats.length
      seatStats.available = seats.filter(s => s.status === 'AVAILABLE').length
      seatStats.occupied = seats.filter(s => s.status === 'OCCUPIED').length
      seatStats.maintenance = seats.filter(s => s.status === 'MAINTENANCE').length
    }
  } catch (error) {
    console.error('加载座位统计失败:', error)
  }
}

async function loadReservations() {
  loading.value = true
  try {
    const res = await getMyReservations({
      current: pagination.current,
      size: pagination.size
    })
    reservations.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载预约列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要取消此预约吗？取消将扣除积分。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelReserve(row.id)
    ElMessage.success('取消成功')
    loadReservations()
    loadSeatStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

async function handleCheckIn(row) {
  try {
    await ElMessageBox.confirm('确认签到吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await checkIn(row.id)
    ElMessage.success('签到成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('签到失败')
    }
  }
}

async function handleCheckOut(row) {
  try {
    await ElMessageBox.confirm('确认签退吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await checkOut(row.id)
    ElMessage.success('签退成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('签退失败')
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;

  .stat-value {
    font-size: 32px;
    font-weight: bold;
    color: #303133;

    &.available { color: #67c23a; }
    &.occupied { color: #e6a23c; }
    &.maintenance { color: #909399; }
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 8px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
