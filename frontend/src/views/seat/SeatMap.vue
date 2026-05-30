<template>
  <div class="seat-map">
    <!-- 区域选择 -->
    <el-card class="area-card">
      <el-radio-group
        v-model="currentArea"
        @change="handleAreaChange"
      >
        <el-radio-button value="A">
          A区
        </el-radio-button>
        <el-radio-button value="B">
          B区
        </el-radio-button>
        <el-radio-button value="C">
          C区
        </el-radio-button>
      </el-radio-group>
      
      <div class="seat-legend">
        <span class="legend-item"><span class="seat available" />空闲</span>
        <span class="legend-item"><span class="seat reserved" />已预约</span>
        <span class="legend-item"><span class="seat occupied" />使用中</span>
        <span class="legend-item"><span class="seat selected" />已选</span>
      </div>
    </el-card>

    <!-- 座位地图 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ currentArea }}区座位图</span>
          <el-button
            type="primary"
            size="small"
            :disabled="!selectedSeat"
            @click="goToReserve"
          >
            预约选中的座位
          </el-button>
        </div>
      </template>
      
      <div class="seat-grid">
        <div
          v-for="row in 8"
          :key="row"
          class="seat-row"
        >
          <span class="row-label">{{ row }}排</span>
          <div
            v-for="col in 10"
            :key="col"
            class="seat"
            :class="getSeatClass(row, col, getSeatStatus(row, col))"
            @click="handleSeatClick(row, col)"
          >
            {{ (row - 1) * 10 + col }}
          </div>
        </div>
      </div>
      
      <!-- 选中座位信息 -->
      <div
        v-if="selectedSeat"
        class="selected-info"
      >
        <el-alert
          type="info"
          :closable="false"
        >
          已选中: {{ currentArea }}区 {{ selectedSeat.row }}排 {{ selectedSeat.col }}号
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSeatMap } from '@/api/seat'

const router = useRouter()
const currentArea = ref('A')
const selectedSeat = ref(null)

// FIXED: P1-FE-06 - 座位状态数据（由API填充）
const seatStatus = ref({})
const loading = ref(false)

// FIXED: P1-FE-06 - 组件挂载时加载座位图
onMounted(() => {
  loadSeatMap()
})

function handleAreaChange() {
  selectedSeat.value = null
  loadSeatMap()
}

// FIXED: P1-FE-06 - 调用API获取座位状态
async function loadSeatMap() {
  loading.value = true
  try {
    const res = await getSeatMap(currentArea.value)
    if (res.data) {
      // API返回座位列表，转换为行列映射
      const newStatus = {}
      const seats = Array.isArray(res.data) ? res.data : (res.data.seats || [])
      seats.forEach(seat => {
        const row = seat.row || Math.ceil(seat.seatNumber / 10)
        const col = seat.col || ((seat.seatNumber - 1) % 10 + 1)
        const status = seat.status || 'available'
        newStatus[`${row}-${col}`] = status
      })
      seatStatus.value = newStatus
    }
  } catch (error) {
    console.error('加载座位地图失败:', error)
    ElMessage.warning('加载座位数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function getSeatStatus(row, col) {
  const key = `${row}-${col}`
  return seatStatus.value[key] || 'available'
}

function getSeatClass(row, col, status) {
  return {
    available: status === 'available',
    reserved: status === 'reserved',
    occupied: status === 'occupied',
    selected: selectedSeat.value?.row === row && selectedSeat.value?.col === col
  }
}

function handleSeatClick(row, col) {
  const status = getSeatStatus(row, col)
  if (status === 'reserved' || status === 'occupied') {
    ElMessage.warning('该座位不可预约')
    return
  }
  selectedSeat.value = { row, col }
}

function goToReserve() {
  if (!selectedSeat.value) {
    ElMessage.warning('请先选择一个座位')
    return
  }
  const { row, col } = selectedSeat.value
  const seatNumber = (row - 1) * 10 + col
  // FIXED: 使用 name 跳转 + 传递 row/col 参数，SeatReserve 可以按需使用
  router.push({
    name: 'SeatReserve',
    query: {
      area: currentArea.value,
      seatNumber: String(seatNumber),
      row: String(row),
      col: String(col)
    }
  })
}
</script>

<style lang="scss" scoped>
.seat-map {
  .area-card {
    margin-bottom: 20px;
    
    .seat-legend {
      display: flex;
      gap: 20px;
      margin-top: 15px;
      
      .legend-item {
        display: flex;
        align-items: center;
        gap: 5px;
        font-size: 14px;
        color: #606266;
        
        .seat {
          width: 20px;
          height: 20px;
          border-radius: 4px;
          
          &.available { background: #67c23a; }
          &.reserved { background: #e6a23c; }
          &.occupied { background: #909399; }
          &.selected { background: #409eff; }
        }
      }
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .seat-grid {
    .seat-row {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      
      .row-label {
        width: 50px;
        color: #909399;
      }
      
      .seat {
        width: 40px;
        height: 40px;
        margin: 0 5px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;
        font-size: 12px;
        color: #fff;
        
        &.available {
          background: #67c23a;
          &:hover { transform: scale(1.1); }
        }
        &.reserved { background: #e6a23c; cursor: not-allowed; }
        &.occupied { background: #909399; cursor: not-allowed; }
        &.selected { background: #409eff; transform: scale(1.1); }
      }
    }
  }
  
  .selected-info {
    margin-top: 20px;
  }
}
</style>
