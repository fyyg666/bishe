<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row
      :gutter="20"
      class="stats-row"
    >
      <el-col :span="6">
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <div class="stat-icon book-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ stats.bookCount }}
            </div>
            <div class="stat-label">
              图书总数
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <div class="stat-icon borrow-icon">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ stats.borrowCount }}
            </div>
            <div class="stat-label">
              在借数量
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <div class="stat-icon seat-icon">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ stats.seatCount }}
            </div>
            <div class="stat-label">
              可用座位
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <div class="stat-icon credit-icon">
            <el-icon><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ stats.creditScore }}
            </div>
            <div class="stat-label">
              我的积分
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- FIXED: FE-004 - 实现借阅趋势ECharts图表（仅管理员可见） -->
    <el-row
      :gutter="20"
      class="content-row"
    >
      <el-col :span="userStore.isAdmin ? 16 : 24">
        <el-card v-if="userStore.isAdmin">
          <template #header>
            <div class="card-header">
              <span>近期借阅趋势</span>
              <el-radio-group
                v-model="trendPeriod"
                size="small"
                @change="loadBorrowTrend"
              >
                <el-radio-button value="week">
                  本周
                </el-radio-button>
                <el-radio-button value="month">
                  本月
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div
            ref="trendChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-actions">
            <div
              class="quick-item"
              @click="$router.push('/books/add')"
            >
              <el-icon><Plus /></el-icon>
              <span>添加图书</span>
            </div>
            <div
              class="quick-item"
              @click="$router.push('/borrows/page')"
            >
              <el-icon><Tickets /></el-icon>
              <span>借阅图书</span>
            </div>
            <div
              class="quick-item"
              @click="$router.push('/seats/map')"
            >
              <el-icon><Calendar /></el-icon>
              <span>预约座位</span>
            </div>
            <div
              class="quick-item"
              @click="$router.push('/profile')"
            >
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </div>
          </div>
        </el-card>
        
        <el-card class="mt-20">
          <template #header>
            <span>最近公告</span>
          </template>
          <div
            v-if="recentNotices.length"
            class="notice-list"
          >
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
          <el-empty
            v-else
            description="暂无公告"
            :image-size="60"
          />
        </el-card>
        
        <el-card class="mt-20">
          <template #header>
            <span>热门图书 TOP5</span>
          </template>
          <div
            v-if="hotBooks.length"
            class="hot-books-list"
          >
            <div
              v-for="(book, index) in hotBooks"
              :key="book.id || index"
              class="hot-book-item"
              @click="$router.push('/books')"
            >
              <span class="hot-book-rank">{{ index + 1 }}</span>
              <span class="hot-book-title">{{ book.title || book.bookTitle || book.bookName }}</span>
              <span class="hot-book-count">{{ book.borrowCount }}次</span>
            </div>
          </div>
          <el-empty
            v-else
            description="暂无数据"
            :image-size="60"
          />
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
import { getStatisticsOverview, getBorrowTrend, getSeatStatistics, getHotBooks } from '@/api/statistics'
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

// P2: 热门图书
const hotBooks = ref([])

// 图表引用
const trendChartRef = ref(null)
let trendChart = null

function handleResize() {
  trendChart?.resize()
}

onMounted(() => {
  nextTick(() => loadDashboardData())
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

async function loadDashboardData() {
  try {
    const promises = [loadCreditScore(), loadNotices(), loadHotBooks()]
    // 仅管理员加载统计概览、座位统计和借阅趋势
    if (userStore.isAdmin) {
      promises.push(loadSeatStats())
      promises.push(loadStats())
      promises.push(loadBorrowTrend())
    }
    await Promise.all(promises)
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
        stats.value.creditScore = res.data || 0
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

// P2: 加载热门图书
async function loadHotBooks() {
  try {
    const res = await getHotBooks(5)
    if (res.data) {
      hotBooks.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch (error) {
    console.error('加载热门图书失败:', error)
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
  
  // 确保 DOM 已挂载后再初始化 ECharts
  try {
    if (trendChart) {
      trendChart.dispose()
    }
    trendChart = echarts.init(trendChartRef.value)
    
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
    // 等 DOM 布局完成后再调整图表尺寸
    setTimeout(() => trendChart?.resize(), 50)
  } catch (e) {
    console.error('渲染图表失败:', e)
  }
}

function renderEmptyChart() {
  if (!trendChartRef.value) return
  try {
    if (trendChart) {
      trendChart.dispose()
    }
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      title: {
        text: '暂无借阅趋势数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#909399', fontSize: 14 }
      }
    })
    setTimeout(() => trendChart?.resize(), 50)
  } catch (e) {
    console.error('渲染空图表失败:', e)
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  border-radius: 12px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1) !important;
  }

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    padding: 24px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    color: #fff;
    margin-right: 20px;
    flex-shrink: 0;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    transition: transform 0.3s ease;

    &.book-icon {
      background: linear-gradient(135deg, #667eea, #764ba2);
    }
    &.borrow-icon {
      background: linear-gradient(135deg, #f093fb, #f5576c);
    }
    &.seat-icon {
      background: linear-gradient(135deg, #4facfe, #00f2fe);
      box-shadow: 0 6px 16px rgba(79, 172, 254, 0.3);
    }
    &.credit-icon {
      background: linear-gradient(135deg, #43e97b, #38f9d7);
      box-shadow: 0 6px 16px rgba(67, 233, 123, 0.3);
    }
  }

  &:hover .stat-icon {
    transform: scale(1.05) rotate(3deg);
  }

  .stat-info {
    .stat-value {
      font-size: 30px;
      font-weight: 700;
      color: #1a1a2e;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 2px;
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
  font-weight: 600;
  font-size: 15px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  .quick-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px 12px;
    background: #f5f7fa;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 1px solid transparent;

    &:hover {
      background: #fff;
      border-color: #ebeef5;
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
    }

    .el-icon {
      font-size: 26px;
      margin-bottom: 10px;
      transition: transform 0.3s ease;
    }

    &:hover .el-icon {
      transform: scale(1.15);
    }

    &:nth-child(1) .el-icon { color: #667eea; }
    &:nth-child(2) .el-icon { color: #f5576c; }
    &:nth-child(3) .el-icon { color: #4facfe; }
    &:nth-child(4) .el-icon { color: #43e97b; }

    span {
      font-size: 13px;
      color: #606266;
      font-weight: 500;
    }
  }
}

.seat-status {
  .seat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f2f5;

    &:last-child {
      border-bottom: none;
    }

    .seat-label {
      font-size: 14px;
      color: #606266;
    }

    .seat-value {
      font-weight: 600;
      font-size: 16px;
      color: #303133;

      &.available {
        color: #67c23a;
      }

      &.reserved {
        color: #e6a23c;
      }

      &.total {
        color: #409eff;
      }
    }
  }
}

.notice-list {
  .notice-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    transition: all 0.25s ease;
    border-radius: 6px;
    padding: 10px 8px;
    margin: 0 -8px;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f5f7fa;

      .notice-title {
        color: #667eea;
      }
    }

    .notice-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: #606266;
      margin-right: 12px;
      transition: color 0.25s;
    }

    .notice-date {
      font-size: 12px;
      color: #c0c4cc;
      white-space: nowrap;
    }
  }
}

.hot-books-list {
  .hot-book-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    transition: all 0.25s ease;
    border-radius: 6px;
    padding: 10px 8px;
    margin: 0 -8px;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f5f7fa;

      .hot-book-title {
        color: #667eea;
      }
    }

    .hot-book-rank {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #f0f2f5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      color: #909399;
      margin-right: 10px;
      flex-shrink: 0;
    }

    &:nth-child(1) .hot-book-rank {
      background: linear-gradient(135deg, #f5a623, #f7c948);
      color: #fff;
    }

    &:nth-child(2) .hot-book-rank {
      background: linear-gradient(135deg, #a0a4a8, #c0c4cc);
      color: #fff;
    }

    &:nth-child(3) .hot-book-rank {
      background: linear-gradient(135deg, #cd7f32, #daa06a);
      color: #fff;
    }

    .hot-book-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: #606266;
      margin-right: 8px;
      transition: color 0.25s;
    }

    .hot-book-count {
      font-size: 12px;
      font-weight: 600;
      color: #f5576c;
      white-space: nowrap;
    }
  }
}

// 右侧卡片统一风格
:deep(.el-card) {
  border: none;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  .el-card__header {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f2f5;
    font-weight: 600;
    font-size: 15px;
    color: #1a1a2e;
  }

  .el-card__body {
    padding: 16px 20px;
  }
}
</style>
