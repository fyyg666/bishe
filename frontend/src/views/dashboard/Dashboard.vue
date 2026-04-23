<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon book-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.bookCount }}</div>
            <div class="stat-label">图书总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon borrow-icon">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.borrowCount }}</div>
            <div class="stat-label">在借数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon seat-icon">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.seatCount }}</div>
            <div class="stat-label">可用座位</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon credit-icon">
            <el-icon><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.creditScore }}</div>
            <div class="stat-label">我的积分</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- FIXED: FE-004 - 实现借阅趋势ECharts图表 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>近期借阅趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="loadBorrowTrend">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-actions">
            <div class="quick-item" @click="$router.push('/books/add')">
              <el-icon><Plus /></el-icon>
              <span>添加图书</span>
            </div>
            <div class="quick-item" @click="$router.push('/borrows/page')">
              <el-icon><Tickets /></el-icon>
              <span>借阅图书</span>
            </div>
            <div class="quick-item" @click="$router.push('/seats/reserve')">
              <el-icon><Calendar /></el-icon>
              <span>预约座位</span>
            </div>
            <div class="quick-item" @click="$router.push('/profile')">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </div>
          </div>
        </el-card>
        
        <el-card class="mt-20">
          <template #header>
            <span>最近公告</span>
          </template>
          <div v-if="recentNotices.length" class="notice-list">
            <div
              v-for="notice in recentNotices"
              :key="notice.id"
              class="notice-item"
              @click="$router.push('/announcements')"
            >
              <span class="notice-title">{{ notice.title }}</span>
              <span class="notice-date">{{ formatDate(notice.createdAt) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="60" />
        </el-card>
        
        <el-card class="mt-20">
          <template #header>
            <span>座位预约情况</span>
          </template>
          <div class="seat-status">
            <div class="seat-item">
              <span class="seat-label">总座位</span>
              <span class="seat-value">{{ seatStats.total }}</span>
            </div>
            <div class="seat-item">
              <span class="seat-label">已预约</span>
              <span class="seat-value">{{ seatStats.reserved }}</span>
            </div>
            <div class="seat-item">
              <span class="seat-label">空闲</span>
              <span class="seat-value available">{{ seatStats.available }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
// FIXED: P2-FE-01 - ECharts按需引入，减少包体积
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])
import { ElMessage } from 'element-plus'
import { getStatisticsOverview, getBorrowTrend, getSeatStatistics } from '@/api/statistics'
import { getCreditInfo } from '@/api/credit'
import { getLatestAnnouncements } from '@/api/announcement'
import { useUserStore } from '@/stores/user'

// FIXED: FE-004 - Dashboard集成ECharts图表和真实API调用
const userStore = useUserStore()
const trendPeriod = ref('week')

const stats = ref({
  bookCount: 0,
  borrowCount: 0,
  seatCount: 0,
  creditScore: 0
})

const seatStats = ref({
  total: 0,
  reserved: 0,
  available: 0
})

// FIXED: P1-FE-07 - 公告列表数据
const recentNotices = ref([])

// 图表引用
const trendChartRef = ref(null)
let trendChart = null

onMounted(() => {
  loadDashboardData()
})

onUnmounted(() => {
  trendChart?.dispose()
})

async function loadDashboardData() {
  try {
    // 并行加载统计数据
    await Promise.all([
      loadStats(),
      loadCreditScore(),
      loadSeatStats(),
      loadBorrowTrend(),
      loadNotices()
    ])
    await nextTick()
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

async function loadStats() {
  try {
    const res = await getStatisticsOverview()
    if (res.code === 0 && res.data) {
      stats.value.bookCount = res.data.bookStatistics?.totalBooks || 0
      stats.value.borrowCount = res.data.borrowStatistics?.activeBorrows || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

async function loadCreditScore() {
  try {
    if (userStore.isLoggedIn) {
      const res = await getCreditInfo()
      if (res.data) {
        stats.value.creditScore = res.data.score || 0
      }
    }
  } catch (error) {
    console.error('加载积分失败:', error)
  }
}

async function loadSeatStats() {
  try {
    const res = await getSeatStatistics()
    if (res.data) {
      seatStats.value = {
        total: res.data.totalSeats || 0,
        reserved: res.data.occupiedSeats || 0,
        available: res.data.availableSeats || 0
      }
      stats.value.seatCount = res.data.availableSeats || 0
    }
  } catch (error) {
    console.error('加载座位统计失败:', error)
  }
}

// FIXED: P1-FE-07 - 加载最新公告
async function loadNotices() {
  try {
    const res = await getLatestAnnouncements(5)
    if (res.data) {
      recentNotices.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch (error) {
    console.error('加载公告失败:', error)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

async function loadBorrowTrend() {
  try {
    const days = trendPeriod.value === 'week' ? 7 : 30
    const res = await getBorrowTrend(days)
    if (res.code === 0 && res.data) {
      renderTrendChart(res.data)
    }
  } catch (error) {
    console.error('加载借阅趋势失败:', error)
    renderEmptyChart()
  }
}

function renderTrendChart(data) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const dates = data.map(item => item.date)
  const borrows = data.map(item => item.borrows)
  const returns = data.map(item => item.returns)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['借阅', '归还']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        rotate: 45,
        interval: Math.floor(dates.length / 7)
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '借阅',
        type: 'line',
        smooth: true,
        data: borrows,
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        }
      },
      {
        name: '归还',
        type: 'line',
        smooth: true,
        data: returns,
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.3)' },
            { offset: 1, color: 'rgba(103,194,58,0.05)' }
          ])
        }
      }
    ]
  }

  trendChart.setOption(option)
}

function renderEmptyChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  trendChart.setOption({
    title: {
      text: '暂无借阅趋势数据',
      left: 'center',
      top: 'center',
      textStyle: { color: '#909399', fontSize: 14 }
    }
  })
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  
  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    margin-right: 20px;
    
    &.book-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
    &.borrow-icon { background: linear-gradient(135deg, #f093fb, #f5576c); }
    &.seat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }
    &.credit-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
  }
  
  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
    }
  }
}

.content-row {
  .mt-20 {
    margin-top: 20px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  
  .quick-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #e4e7ed;
      transform: translateY(-2px);
    }
    
    .el-icon {
      font-size: 24px;
      margin-bottom: 8px;
      color: #409eff;
    }
    
    span {
      font-size: 14px;
      color: #606266;
    }
  }
}

.seat-status {
  .seat-item {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #ebeef5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .seat-value {
      font-weight: bold;
      
      &.available {
        color: #67c23a;
      }
    }
  }
}

.notice-list {
  .notice-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #ebeef5;
    cursor: pointer;
    transition: color 0.3s;
    
    &:last-child {
      border-bottom: none;
    }
    
    &:hover {
      .notice-title {
        color: #409eff;
      }
    }
    
    .notice-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: #606266;
      margin-right: 10px;
    }
    
    .notice-date {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
    }
  }
}
</style>
