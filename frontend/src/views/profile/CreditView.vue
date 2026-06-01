<template>
  <div class="credit-view">
    <!-- 积分概览 -->
    <el-row
      :gutter="20"
      class="credit-overview"
    >
      <el-col :span="8">
        <el-card
          shadow="hover"
          class="credit-card"
        >
          <div class="credit-value">
            {{ creditInfo.score }}
          </div>
          <div class="credit-label">
            当前积分
          </div>
          <div class="credit-level">
            等级: {{ creditInfo.level }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card
          shadow="hover"
          class="credit-card"
        >
          <div class="credit-value warning">
            {{ creditInfo.borrowCount }}
          </div>
          <div class="credit-label">
            累计借阅
          </div>
          <div class="credit-desc">
            本学期已借阅图书数
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card
          shadow="hover"
          class="credit-card"
        >
          <div class="credit-value success">
            {{ creditInfo.returnOnTimeRate }}%
          </div>
          <div class="credit-label">
            按时归还率
          </div>
          <div class="credit-desc">
            近6个月统计数据
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 积分规则 -->
    <el-card class="mb-20">
      <template #header>
        <span>积分规则</span>
      </template>
      <el-table
        :data="rules"
        stripe
      >
        <el-table-column
          prop="action"
          label="行为"
          width="200"
        />
        <el-table-column
          prop="score"
          label="积分变化"
          width="120"
        >
          <template #default="{ row }">
            <span :class="row.score > 0 ? 'text-success' : 'text-danger'">
              {{ row.score > 0 ? '+' : '' }}{{ row.score }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <!-- 积分记录 -->
    <el-card>
      <template #header>
        <span>积分记录</span>
      </template>
      
      <el-table
        :data="records"
        stripe
      >
        <el-table-column
          prop="createTime"
          label="时间"
          width="180"
        />
        <el-table-column
          prop="action"
          label="行为"
          width="150"
        />
        <el-table-column
          prop="change"
          label="积分变化"
          width="120"
        >
          <template #default="{ row }">
            <span :class="row.change > 0 ? 'text-success' : 'text-danger'">
              {{ row.change > 0 ? '+' : '' }}{{ row.change }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="balance"
          label="余额"
          width="100"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCreditInfo, getCreditRecords, getCreditRules } from '@/api/credit'

const creditInfo = ref({
  score: 0,
  level: '普通会员',
  borrowCount: 0,
  returnOnTimeRate: 100
})

const rules = ref([])
const records = ref([])
const total = ref(0)

const pagination = reactive({
  current: 1,
  size: 10
})

onMounted(() => {
  loadCreditInfo()
  loadRules()
  loadRecords()
})

async function loadCreditInfo() {
  try {
    const res = await getCreditInfo()
    // 后端返回纯数字积分值
    const score = typeof res.data === 'number' ? res.data : (res.score || 0)
    creditInfo.value = {
      score: score,
      level: getCreditLevel(score),
      borrowCount: 0,
      returnOnTimeRate: 100
    }
  } catch {
    // 使用默认数据
    creditInfo.value = {
      score: 100,
      level: getCreditLevel(100),
      borrowCount: 0,
      returnOnTimeRate: 100
    }
  }
}

/**
 * 获取信用等级
 * 论文§3.2(4): 铜牌≥60、银牌≥120、金牌≥180、白金≥240、上限300
 */
function getCreditLevel(score) {
  if (score >= 240) return '白金会员'
  if (score >= 180) return '金牌会员'
  if (score >= 120) return '银牌会员'
  if (score >= 60) return '铜牌会员'
  return '普通会员'
}

async function loadRules() {
  try {
    const res = await getCreditRules()
    const rawRules = res.data || []
    rules.value = rawRules.map(r => ({
      action: r.ruleName,
      score: r.type === 'PENALTY' ? -Math.abs(r.score) : r.score,
      description: r.description
    }))
  } catch {
    rules.value = [
      { action: '借阅图书', score: 5, description: '每次借阅成功获得5积分' },
      { action: '按时归还', score: 1, description: '按时归还获得1积分' },
      { action: '逾期归还', score: -5, description: '每逾期一天扣5积分（单日扣分上限）' },
      { action: '损坏图书', score: -50, description: '损坏图书视情况扣分' },
      { action: '丢失图书', score: -100, description: '丢失图书扣100积分' }
    ]
  }
}

async function loadRecords() {
  try {
    const res = await getCreditRecords(pagination)
    const rawRecords = res.data?.records || res.data || []
    // 后端返回格式: { changeValue, balance, typeDesc, createTime, description }
    records.value = rawRecords.map(r => ({
      createTime: r.createTime || r.createdAt,
      action: r.typeDesc || r.type || '其他',
      change: r.changeValue || 0,
      balance: r.balance || r.currentScore || 0,
      description: r.description || r.remark || ''
    }))
    total.value = res.data?.total || res.total || 0
  } catch (error) {
    console.error('加载积分记录失败:', error)
    records.value = []
  }
}
</script>

<style lang="scss" scoped>
.credit-view {
  .credit-overview {
    margin-bottom: 20px;
  }
  
  .credit-card {
    text-align: center;
    padding: 20px;
    
    .credit-value {
      font-size: 36px;
      font-weight: bold;
      color: #409eff;
      
      &.warning { color: #e6a23c; }
      &.success { color: #67c23a; }
    }
    
    .credit-label {
      font-size: 14px;
      color: #909399;
      margin-top: 8px;
    }
    
    .credit-level {
      margin-top: 8px;
      color: #606266;
    }
    
    .credit-desc {
      margin-top: 8px;
      font-size: 12px;
      color: #c0c4cc;
    }
  }
  
  .mb-20 {
    margin-bottom: 20px;
  }
  
  .text-success {
    color: #67c23a;
    font-weight: bold;
  }
  
  .text-danger {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
