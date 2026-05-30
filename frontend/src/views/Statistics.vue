<template>
  <div class="page-container">
    <div class="page-header">
      <h2>统计分析</h2>
    </div>

    <!-- 统计概览卡片 -->
    <el-row
      :gutter="20"
      class="stats-row"
    >
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon book-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ overview.bookStatistics?.totalBooks || 0 }}
            </div>
            <div class="stat-label">
              图书种类
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon copy-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ overview.bookStatistics?.availableCopies || 0 }}
            </div>
            <div class="stat-label">
              可借图书
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon reader-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ overview.readerStatistics?.totalReaders || 0 }}
            </div>
            <div class="stat-label">
              注册读者
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon borrow-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ overview.borrowStatistics?.activeBorrows || 0 }}
            </div>
            <div class="stat-label">
              当前借阅
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row
      :gutter="20"
      class="charts-row"
    >
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>借阅趋势</span>
              <el-select
                v-model="trendDays"
                size="small"
                @change="loadBorrowTrend"
              >
                <el-option
                  label="近7天"
                  :value="7"
                />
                <el-option
                  label="近30天"
                  :value="30"
                />
                <el-option
                  label="近90天"
                  :value="90"
                />
              </el-select>
            </div>
          </template>
          <div
            ref="trendChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>图书分类分布</span>
          </template>
          <div
            ref="categoryChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行图表 -->
    <el-row
      :gutter="20"
      class="charts-row"
    >
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>座位使用率热力图（论文§5.2(4)）</span>
            </div>
          </template>
          <div
            ref="heatmapChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>热门图书 Top 10</span>
            </div>
          </template>
          <!-- 骨架屏 -->
          <el-skeleton
            v-if="loading"
            :rows="3"
            animated
          />
          <!-- 空状态 -->
          <EmptyState 
            v-else-if="hotBooks.length === 0" 
            description="暂无热门图书数据"
          />
          <!-- 数据表格 -->
          <div
            v-else
            class="hot-books-list"
          >
            <el-table
              :data="hotBooks"
              stripe
            >
              <el-table-column
                prop="title"
                label="书名"
                show-overflow-tooltip
              />
              <el-table-column
                prop="author"
                label="作者"
                width="120"
                show-overflow-tooltip
              />
              <el-table-column
                prop="borrowCount"
                label="借阅次数"
                width="100"
                align="center"
              />
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>月度统计</span>
          </template>
          <div
            ref="monthlyChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细数据表格 -->
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>详细统计数据</span>
          <el-button
            type="primary"
            @click="loadAllData"
          >
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane
          label="借阅统计"
          name="borrow"
        >
          <el-descriptions
            :column="3"
            border
          >
            <el-descriptions-item label="总借阅次数">
              {{ overview.borrowStatistics?.totalBorrows || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="当前借阅">
              {{ overview.borrowStatistics?.activeBorrows || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="逾期未还">
              {{ overview.borrowStatistics?.overdueBorrows || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="今日归还">
              {{ overview.borrowStatistics?.returnedToday || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="平均借阅天数">
              {{ overview.borrowStatistics?.averageBorrowDays || 0 }} 天
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane
          label="图书统计"
          name="book"
        >
          <el-descriptions
            :column="3"
            border
          >
            <el-descriptions-item label="图书种类">
              {{ overview.bookStatistics?.totalBooks || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="馆藏总数">
              {{ overview.bookStatistics?.totalCopies || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="可借数量">
              {{ overview.bookStatistics?.availableCopies || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="已借数量">
              {{ overview.bookStatistics?.borrowedCopies || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="图书分类">
              {{ overview.bookStatistics?.categories || 0 }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane
          label="读者统计"
          name="reader"
        >
          <el-descriptions
            :column="3"
            border
          >
            <el-descriptions-item label="注册读者">
              {{ overview.readerStatistics?.totalReaders || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="活跃读者">
              {{ overview.readerStatistics?.activeReaders || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="逾期读者">
              {{ overview.readerStatistics?.overdueReaders || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="平均积分">
              {{ overview.readerStatistics?.averageCreditScore || 0 }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane
          label="座位统计"
          name="seat"
        >
          <el-descriptions
            :column="3"
            border
          >
            <el-descriptions-item label="座位总数">
              {{ overview.seatStatistics?.totalSeats || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="可用座位">
              {{ overview.seatStatistics?.availableSeats || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="已占座位">
              {{ overview.seatStatistics?.occupiedSeats || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="今日预约">
              {{ overview.seatStatistics?.todayReservations || 0 }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
// FIXED: P2-FE-01 - ECharts按需引入，减少包体积
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, HeatmapChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent, DatasetComponent, VisualMapComponent,
  CalendarComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, LineChart, PieChart, HeatmapChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, DatasetComponent, VisualMapComponent, CalendarComponent, CanvasRenderer])
import { ElMessage } from 'element-plus'
import { getStatisticsOverview, getBorrowTrend, getHotBooks, getCategoryDistribution, getMonthlyStats, getSeatHeatmap } from '@/api/statistics'
import EmptyState from '@/components/EmptyState.vue'

// 响应式数据
const activeTab = ref('borrow')
const trendDays = ref(30)
const loading = ref(false)
const overview = reactive({
  borrowStatistics: {},
  bookStatistics: {},
  readerStatistics: {},
  seatStatistics: {}
})
const hotBooks = ref([])
const borrowTrend = ref([])
const categoryDistribution = ref([])
const monthlyStats = ref([])
const seatHeatmap = ref([])

// 图表引用
const trendChartRef = ref(null)
const categoryChartRef = ref(null)
const monthlyChartRef = ref(null)
const heatmapChartRef = ref(null)

// 图表实例
let trendChart = null
let categoryChart = null
let monthlyChart = null
let heatmapChart = null

// 加载综合概览
async function loadOverview() {
  try {
    const res = await getStatisticsOverview()
    if (res.code === 0 && res.data) {
      Object.assign(overview, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载借阅趋势
async function loadBorrowTrend() {
  try {
    const res = await getBorrowTrend(trendDays.value)
    if (res.code === 0 && res.data) {
      borrowTrend.value = res.data
      renderTrendChart()
    }
  } catch (error) {
    console.error('加载借阅趋势失败:', error)
  }
}

// 加载热门图书
async function loadHotBooks() {
  try {
    const res = await getHotBooks(10)
    if (res.code === 0 && res.data) {
      hotBooks.value = res.data
    }
  } catch (error) {
    console.error('加载热门图书失败:', error)
  }
}

// 加载分类分布
async function loadCategoryDistribution() {
  try {
    const res = await getCategoryDistribution()
    if (res.code === 0 && res.data) {
      categoryDistribution.value = res.data
      renderCategoryChart()
    }
  } catch (error) {
    console.error('加载分类分布失败:', error)
  }
}

// 加载月度统计
async function loadMonthlyStats() {
  try {
    const res = await getMonthlyStats(12)
    if (res.code === 0 && res.data) {
      monthlyStats.value = res.data
      renderMonthlyChart()
    }
  } catch (error) {
    console.error('加载月度统计失败:', error)
  }
}

// 加载座位热力图
async function loadSeatHeatmap() {
  try {
    const res = await getSeatHeatmap()
    if (res.code === 0 && res.data) {
      seatHeatmap.value = res.data
      renderHeatmapChart()
    }
  } catch (error) {
    console.error('加载座位热力图失败:', error)
  }
}

// 加载所有数据
async function loadAllData() {
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadBorrowTrend(),
      loadHotBooks(),
      loadCategoryDistribution(),
      loadMonthlyStats(),
      loadSeatHeatmap()
    ])
    ElMessage.success('数据已刷新')
  } finally {
    loading.value = false
  }
}

// 渲染借阅趋势图
function renderTrendChart() {
  if (!trendChartRef.value) return
  
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  const dates = borrowTrend.value.map(item => item.date)
  const borrows = borrowTrend.value.map(item => item.borrows)
  const returns = borrowTrend.value.map(item => item.returns)
  
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
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '归还',
        type: 'line',
        smooth: true,
        data: returns,
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 渲染分类分布图
function renderCategoryChart() {
  if (!categoryChartRef.value) return
  
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  
  const data = categoryDistribution.value.map(item => ({
    name: item.categoryName,
    value: item.count
  }))
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c}'
        },
        data: data,
        color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9090FF', '#90FF90', '#FF90FF']
      }
    ]
  }
  
  categoryChart.setOption(option)
}

// 渲染座位热力图
function renderHeatmapChart() {
  if (!heatmapChartRef.value) return

  if (!heatmapChart) {
    heatmapChart = echarts.init(heatmapChartRef.value)
  }

  if (!seatHeatmap.value || seatHeatmap.value.length === 0) return

  // 解析数据
  const areas = [...new Set(seatHeatmap.value.map(item => item.areaLabel))]
  const hourSlots = [...new Set(seatHeatmap.value.map(item => item.hourSlot))]

  const data = seatHeatmap.value.map(item => [
    item.hourSlot,
    item.areaLabel,
    item.usageRate || 0
  ])

  const option = {
    tooltip: {
      position: 'top',
      formatter: function (params) {
        return params.value[0] + '<br/>' + params.value[1] + ': ' + params.value[2] + '%'
      }
    },
    grid: {
      left: '10%',
      right: '5%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hourSlots,
      splitArea: { show: true },
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'category',
      data: areas,
      splitArea: { show: true }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#f0f9ff', '#bae0ff', '#69b1ff', '#1677ff', '#0050b3']
      },
      text: ['使用率(%)', '']
    },
    series: [{
      type: 'heatmap',
      data: data,
      label: {
        show: true,
        formatter: function (params) {
          return params.value[2] > 0 ? params.value[2] + '%' : ''
        },
        fontSize: 11
      },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  }

  heatmapChart.setOption(option)
}

// 渲染月度统计图
function renderMonthlyChart() {
  if (!monthlyChartRef.value) return
  
  if (!monthlyChart) {
    monthlyChart = echarts.init(monthlyChartRef.value)
  }
  
  const months = monthlyStats.value.map(item => item.month)
  const borrows = monthlyStats.value.map(item => item.borrows)
  const returns = monthlyStats.value.map(item => item.returns)
  const newReaders = monthlyStats.value.map(item => item.newReaders)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['借阅', '归还', '新增读者']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '借阅',
        type: 'bar',
        data: borrows,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '归还',
        type: 'bar',
        data: returns,
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '新增读者',
        type: 'bar',
        data: newReaders,
        itemStyle: { color: '#E6A23C' }
      }
    ]
  }
  
  monthlyChart.setOption(option)
}

// 窗口大小变化时重绘图表
function handleResize() {
  trendChart?.resize()
  categoryChart?.resize()
  monthlyChart?.resize()
  heatmapChart?.resize()
}

// 生命周期钩子
onMounted(async () => {
  await loadAllData()
  await nextTick()
  renderTrendChart()
  renderCategoryChart()
  renderMonthlyChart()
  renderHeatmapChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
  monthlyChart?.dispose()
  heatmapChart?.dispose()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 15px;

  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 15px;

    .el-icon {
      font-size: 30px;
      color: #fff;
    }

    &.book-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.copy-icon {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    }

    &.reader-icon {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }

    &.borrow-icon {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }
  }

  .stat-info {
    flex: 1;

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 5px;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
    }
  }
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .chart-container {
    height: 300px;
  }
}

.hot-books-list {
  max-height: 300px;
  overflow-y: auto;
}

.detail-card {
  margin-bottom: 20px;
}
</style>
