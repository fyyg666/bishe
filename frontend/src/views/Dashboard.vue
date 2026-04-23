<template>
  <div class="dashboard">
    <!-- Statistics Cards -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon book-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalBooks }}</div>
            <div class="stat-label">图书总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon reader-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalReaders }}</div>
            <div class="stat-label">注册读者</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon borrow-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.activeBorrows }}</div>
            <div class="stat-label">当前借阅</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon overdue-icon">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.overdueCount }}</div>
            <div class="stat-label">逾期未还</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- FIXED: FE-004 - 实现借阅趋势图表 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>借阅趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="loadBorrowTrend">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <!-- 骨架屏 -->
          <el-skeleton v-if="loading" :rows="3" animated />
          <!-- 空状态 -->
          <EmptyState 
            v-else-if="borrowTrend.length === 0" 
            description="暂无借阅趋势数据"
          />
          <!-- 图表 -->
          <div v-else ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="notice-card">
          <template #header>
            <div class="card-header">
              <span>最新公告</span>
              <el-button type="primary" link @click="$router.push('/announcements')">
                查看更多
              </el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="notice in recentNotices"
              :key="notice.id"
              :type="notice.type"
              :timestamp="notice.time"
            >
              {{ notice.title }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分类分布 + 热门图书 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>图书分类分布</span>
          </template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>热门图书 Top 5</span>
              <el-button type="primary" link @click="$router.push('/statistics')">
                查看更多
              </el-button>
            </div>
          </template>
          <el-table :data="hotBooks" stripe>
            <el-table-column prop="title" label="书名" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
            <el-table-column prop="borrowCount" label="借阅次数" width="100" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <el-card class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="action-buttons">
        <el-button type="primary" @click="$router.push('/books')">
          <el-icon><Plus /></el-icon>新增图书
        </el-button>
        <el-button type="success" @click="$router.push('/borrows')">
          <el-icon><Reading /></el-icon>借阅登记
        </el-button>
        <el-button type="warning" @click="$router.push('/readers')">
          <el-icon><User /></el-icon>新增读者
        </el-button>
        <el-button type="info" @click="$router.push('/announcements')">
          <el-icon><Bell /></el-icon>发布公告
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getStatisticsOverview, getBorrowTrend, getHotBooks, getCategoryDistribution } from '@/api/statistics'
import EmptyState from '@/components/EmptyState.vue'

// FIXED: FE-004 - Dashboard集成ECharts图表和真实API调用
const trendPeriod = ref('week')
const loading = ref(false)

const statistics = reactive({
  totalBooks: 0,
  totalReaders: 0,
  activeBorrows: 0,
  overdueCount: 0
})

const recentNotices = ref([])
const hotBooks = ref([])
const borrowTrend = ref([])

// 图表引用
const trendChartRef = ref(null)
const categoryChartRef = ref(null)
let trendChart = null
let categoryChart = null

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await getStatisticsOverview()
    if (res.code === 0 && res.data) {
      const data = res.data
      statistics.totalBooks = data.bookStatistics?.totalBooks || 0
      statistics.totalReaders = data.readerStatistics?.totalReaders || 0
      statistics.activeBorrows = data.borrowStatistics?.activeBorrows || 0
      statistics.overdueCount = data.borrowStatistics?.overdueBorrows || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载借阅趋势
async function loadBorrowTrend() {
  try {
    const daysMap = { week: 7, month: 30, year: 365 }
    const days = daysMap[trendPeriod.value] || 30
    const res = await getBorrowTrend(days)
    if (res.code === 0 && res.data) {
      borrowTrend.value = res.data
      renderTrendChart(res.data)
    }
  } catch (error) {
    console.error('加载借阅趋势失败:', error)
    borrowTrend.value = []
    renderEmptyTrendChart()
  }
}

// 加载热门图书
async function loadHotBooks() {
  try {
    const res = await getHotBooks(5)
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
      renderCategoryChart(res.data)
    }
  } catch (error) {
    console.error('加载分类分布失败:', error)
    renderEmptyCategoryChart()
  }
}

// 渲染借阅趋势图
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

// 渲染空趋势图
function renderEmptyTrendChart() {
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

// 渲染分类分布图
function renderCategoryChart(data) {
  if (!categoryChartRef.value) return
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }

  const chartData = data.map(item => ({
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
        data: chartData,
        color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB', '#36CFC9', '#FF85C0']
      }
    ]
  }

  categoryChart.setOption(option)
}

// 渲染空分类图
function renderEmptyCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  categoryChart.setOption({
    title: {
      text: '暂无分类数据',
      left: 'center',
      top: 'center',
      textStyle: { color: '#909399', fontSize: 14 }
    }
  })
}

// 窗口大小变化时重绘图表
function handleResize() {
  trendChart?.resize()
  categoryChart?.resize()
}

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadStatistics(),
    loadBorrowTrend(),
    loadHotBooks(),
    loadCategoryDistribution()
  ])
  await nextTick()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .statistics-row {
    margin-bottom: 20px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 10px;

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

      &.reader-icon {
        background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      }

      &.borrow-icon {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }

      &.overdue-icon {
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

  .content-row {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .chart-card {
    .chart-container {
      height: 300px;
    }
  }

  .notice-card {
    height: 100%;
  }

  .quick-actions {
    .action-buttons {
      display: flex;
      gap: 15px;

      .el-button {
        display: flex;
        align-items: center;
        gap: 5px;
      }
    }
  }
}
</style>
