<template>
  <div class="dashboard" v-loading="loading">
    <!-- Stats Row — Apple-style big numbers -->
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-label">图书总数</span>
        <span class="stat-value">{{ stats.bookCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">在借数量</span>
        <span class="stat-value">{{ stats.borrowCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">可用座位</span>
        <span class="stat-value">{{ stats.seatCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">我的积分</span>
        <span class="stat-value">{{ stats.creditScore }}</span>
      </div>
    </div>

    <!-- Content Row -->
    <div class="content-grid">
      <!-- Main column -->
      <div class="main-col">
        <!-- Borrow Trend Chart (admin only) -->
        <div v-if="userStore.isAdmin" class="panel">
          <div class="panel-header">
            <span class="panel-title">借阅趋势</span>
            <el-radio-group
              v-model="trendPeriod"
              size="small"
              @change="loadBorrowTrend"
            >
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChartRef" class="chart-area" />
        </div>

        <!-- Seat Status -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">座位预约情况</span>
          </div>
          <div class="seat-bars">
            <div class="seat-bar-item">
              <div class="seat-bar-info">
                <span class="seat-bar-label">总座位</span>
                <span class="seat-bar-value">{{ seatStats.total }}</span>
              </div>
              <div class="seat-bar-track">
                <div class="seat-bar-fill total" :style="{ width: '100%' }" />
              </div>
            </div>
            <div class="seat-bar-item">
              <div class="seat-bar-info">
                <span class="seat-bar-label">已预约</span>
                <span class="seat-bar-value">{{ seatStats.reserved }}</span>
              </div>
              <div class="seat-bar-track">
                <div
                  class="seat-bar-fill reserved"
                  :style="{ width: seatPercent('reserved') }"
                />
              </div>
            </div>
            <div class="seat-bar-item">
              <div class="seat-bar-info">
                <span class="seat-bar-label">空闲</span>
                <span class="seat-bar-value available">{{ seatStats.available }}</span>
              </div>
              <div class="seat-bar-track">
                <div
                  class="seat-bar-fill free"
                  :style="{ width: seatPercent('available') }"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Side column -->
      <div class="side-col">
        <!-- Quick Actions -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">快捷入口</span>
          </div>
          <div class="quick-actions">
            <div class="quick-card" @click="$router.push('/books/add')">
              <span class="quick-icon">
                <el-icon :size="22"><Plus /></el-icon>
              </span>
              <span class="quick-label">添加图书</span>
            </div>
            <div class="quick-card" @click="$router.push('/borrows/page')">
              <span class="quick-icon">
                <el-icon :size="22"><Tickets /></el-icon>
              </span>
              <span class="quick-label">借阅图书</span>
            </div>
            <div class="quick-card" @click="$router.push('/seats/map')">
              <span class="quick-icon">
                <el-icon :size="22"><Calendar /></el-icon>
              </span>
              <span class="quick-label">预约座位</span>
            </div>
            <div class="quick-card" @click="$router.push('/profile')">
              <span class="quick-icon">
                <el-icon :size="22"><User /></el-icon>
              </span>
              <span class="quick-label">个人信息</span>
            </div>
          </div>
        </div>

        <!-- Recent Notices -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">最近公告</span>
          </div>
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
          <el-empty v-else description="暂无公告" :image-size="48" />
        </div>

        <!-- Hot Books -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">热门图书</span>
          </div>
          <div v-if="hotBooks.length" class="hot-list">
            <div
              v-for="(book, index) in hotBooks"
              :key="book.id || index"
              class="hot-item"
              @click="$router.push('/books')"
            >
              <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="hot-title">{{ book.title || book.bookTitle || book.bookName }}</span>
              <span class="hot-count">{{ book.borrowCount }}次</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="48" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
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

const userStore = useUserStore()
const loading = ref(false)
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

const recentNotices = ref([])
const hotBooks = ref([])

const trendChartRef = ref(null)
let trendChart = null

function seatPercent(type) {
  const total = seatStats.value.total || 1
  return `${(seatStats.value[type] / total) * 100}%`
}

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
  loading.value = true
  try {
    const promises = [loadCreditScore(), loadNotices(), loadHotBooks(), loadSeatStats()]
    if (userStore.isAdmin) {
      promises.push(loadStats())
      promises.push(loadBorrowTrend())
    }
    await Promise.all(promises)
    await nextTick()
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  } finally {
    loading.value = false
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
        backgroundColor: '#fff',
        borderColor: '#E5E5EA',
        borderWidth: 1,
        textStyle: { color: '#1C1C1E', fontSize: 12 },
        axisPointer: {
          type: 'line',
          lineStyle: { color: '#E5E5EA', type: 'dashed' }
        }
      },
      legend: {
        data: ['借阅', '归还'],
        bottom: 0,
        textStyle: { color: '#8E8E93', fontSize: 12 },
        itemWidth: 8,
        itemHeight: 8,
        itemGap: 20
      },
      grid: {
        left: 0,
        right: 0,
        top: 10,
        bottom: 40,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: '#8E8E93',
          fontSize: 11,
          interval: Math.floor(dates.length / 7) || 0
        }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: {
          lineStyle: { color: '#F0F0F3', type: 'dashed' }
        },
        axisLabel: {
          color: '#8E8E93',
          fontSize: 11
        }
      },
      series: [
        {
          name: '借阅',
          type: 'line',
          smooth: true,
          symbol: 'none',
          data: borrows,
          lineStyle: { color: '#0071E3', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0,113,227,0.12)' },
              { offset: 1, color: 'rgba(0,113,227,0.01)' }
            ])
          }
        },
        {
          name: '归还',
          type: 'line',
          smooth: true,
          symbol: 'none',
          data: returns,
          lineStyle: { color: '#34C759', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(52,199,89,0.1)' },
              { offset: 1, color: 'rgba(52,199,89,0.01)' }
            ])
          }
        }
      ]
    }

    trendChart.setOption(option)
    setTimeout(() => trendChart?.resize(), 50)
  } catch (e) {
    console.error('渲染图表失败:', e)
  }
}

function renderEmptyChart() {
  if (!trendChartRef.value) return
  try {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      title: {
        text: '暂无借阅趋势数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#8E8E93', fontSize: 14, fontWeight: 400 }
      }
    })
    setTimeout(() => trendChart?.resize(), 50)
  } catch (e) {
    console.error('渲染空图表失败:', e)
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.dashboard {
  padding: 0;
}

/* ── Stats Grid ─────────────────────────── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $space-4;
  margin-bottom: $space-8;

  @include tablet {
    grid-template-columns: repeat(2, 1fr);
  }

  @include mobile {
    grid-template-columns: repeat(2, 1fr);
    gap: $space-3;
  }
}

.stat-card {
  @include card;
  display: flex;
  flex-direction: column;
  gap: $space-2;
  padding: $space-6;

  @include mobile {
    padding: $space-4;
  }

  .stat-label {
    font-size: 13px;
    font-weight: $font-weight-medium;
    color: $text-secondary;
    letter-spacing: 0;
  }

  .stat-value {
    font-size: 40px;
    font-weight: $font-weight-bold;
    letter-spacing: -0.022em;
    line-height: 1.1;
    color: $text-primary;
    font-family: $font-mono;
    font-variant-numeric: tabular-nums;

    @include mobile {
      font-size: 24px;
    }
  }
}

/* ── Content Grid ────────────────────────── */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: $space-6;
  align-items: start;

  @include tablet {
    grid-template-columns: 1fr;
  }

  @include mobile {
    grid-template-columns: 1fr;
    gap: $space-4;
  }
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: $space-6;
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: $space-5;
}

/* ── Panel ───────────────────────────────── */
.panel {
  @include card;
  padding: 0;
  overflow: hidden;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $space-5 $space-6;
    border-bottom: 1px solid $border-light;

    .panel-title {
      font-size: $font-size-lg;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }
  }
}

/* ── Chart ───────────────────────────────── */
.chart-area {
  height: 280px;
  width: 100%;
  padding: $space-5 $space-6;

  @include mobile {
    height: 220px;
    padding: $space-4;
  }
}

/* ── Quick Actions ───────────────────────── */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-3;
  padding: $space-5;

  .quick-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-2;
    padding: $space-5 $space-3;
    background: $gray-50;
    border-radius: $radius-lg;
    cursor: pointer;
    border: 1px solid transparent;
    transition: all $transition-base;

    &:hover {
      background: $bg-card;
      border-color: $gray-200;
    }

    &:active {
      transform: scale(0.97);
      transition: transform 60ms $ease-spring-out;
    }

    &:not(:active) {
      transition: transform 200ms $ease-bounce, background $transition-base, border-color $transition-base;
    }

    .quick-icon {
      color: $primary;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .quick-label {
      font-size: $font-size-xs;
      font-weight: $font-weight-medium;
      color: $text-primary;
    }
  }
}

/* ── Seat Bars ───────────────────────────── */
.seat-bars {
  padding: $space-5 $space-6;

  .seat-bar-item {
    margin-bottom: $space-5;

    &:last-child {
      margin-bottom: 0;
    }

    .seat-bar-info {
      display: flex;
      justify-content: space-between;
      margin-bottom: $space-2;

      .seat-bar-label {
        font-size: $font-size-sm;
        color: $text-regular;
      }

      .seat-bar-value {
        font-size: $font-size-sm;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        font-variant-numeric: tabular-nums;

        &.available {
          color: $success;
        }
      }
    }

    .seat-bar-track {
      height: 4px;
      background: $gray-100;
      border-radius: 2px;
      overflow: hidden;

      .seat-bar-fill {
        height: 100%;
        border-radius: 2px;
        transition: width $transition-slow;

        &.total { background: $primary; }
        &.reserved { background: $warning; }
        &.free { background: $success; }
      }
    }
  }
}

/* ── Notice List ─────────────────────────── */
.notice-list {
  padding: $space-3 $space-6 $space-5;

  .notice-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $space-3 $space-2;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: background $transition-fast;
    margin: 0 (-$space-2);

    &:hover {
      background: $gray-50;

      .notice-title {
        color: $primary;
      }
    }

    .notice-title {
      flex: 1;
      @include truncate;
      font-size: $font-size-sm;
      color: $text-primary;
      margin-right: $space-3;
      transition: color $transition-fast;
    }

    .notice-date {
      font-size: 11px;
      color: $text-secondary;
      white-space: nowrap;
    }
  }
}

/* ── Hot Books List ──────────────────────── */
.hot-list {
  padding: $space-3 $space-6 $space-5;

  .hot-item {
    display: flex;
    align-items: center;
    padding: $space-3 $space-2;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: background $transition-fast;
    margin: 0 (-$space-2);

    &:hover {
      background: $gray-50;
    }

    .hot-rank {
      width: 22px;
      height: 22px;
      border-radius: $radius-full;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: $font-weight-semibold;
      color: $text-secondary;
      background: $gray-100;
      margin-right: $space-3;
      flex-shrink: 0;

      &.top {
        background: $primary-lighter;
        color: $primary;
      }
    }

    .hot-title {
      flex: 1;
      @include truncate;
      font-size: $font-size-sm;
      color: $text-primary;
      margin-right: $space-2;
    }

    .hot-count {
      font-size: 11px;
      font-weight: $font-weight-medium;
      color: $text-secondary;
      white-space: nowrap;
    }
  }
}
</style>
