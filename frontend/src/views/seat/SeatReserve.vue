<template>
  <div class="seat-reserve">
    <el-card>
      <template #header>
        <span>座位预约</span>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="区域">
          <el-radio-group v-model="form.area">
            <el-radio label="A">A区</el-radio>
            <el-radio label="B">B区</el-radio>
            <el-radio label="C">C区</el-radio>
            <el-radio label="D">D区</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="座位号" prop="seatId">
          <el-input-number v-model="form.seatId" :min="1" :max="100" :controls="false" />
        </el-form-item>
        
        <el-form-item label="预约日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-time-select
            v-model="form.startTime"
            start="08:00"
            step="01:00"
            end="20:00"
            placeholder="选择开始时间"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTime">
          <el-time-select
            v-model="form.endTime"
            start="09:00"
            step="01:00"
            end="21:00"
            placeholder="选择结束时间"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ loading ? '预约中...' : '确认预约' }}
          </el-button>
          <el-button @click="$router.push('/seats')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 我的预约 -->
    <el-card class="mt-20">
      <template #header>
        <span>我的预约</span>
      </template>
      
      <el-table :data="reservations" stripe>
        <el-table-column prop="area" label="区域" width="80" />
        <el-table-column prop="seatId" label="座位号" width="100" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="100" />
        <el-table-column prop="endTime" label="结束时间" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleCancel(row)">取消预约</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSeatStore } from '@/stores/seat'
import { useStatusMap } from '@/composables/useStatusMap'

// FIXED: P2-FE-08 - 使用公共composable替代重复的状态映射
const { getStatusType } = useStatusMap('seat')

const seatStore = useSeatStore()

const formRef = ref(null)
const loading = ref(false)
const reservations = ref([])

const form = reactive({
  area: 'A',
  seatId: null,
  date: '',
  startTime: '09:00',
  endTime: '17:00'
})

const rules = {
  seatId: [{ required: true, message: '请输入座位号', trigger: 'blur' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

onMounted(() => {
  loadReservations()
})

async function loadReservations() {
  try {
    await seatStore.fetchMyReservations()
    reservations.value = seatStore.myReservations
  } catch (error) {
    ElMessage.error('加载预约记录失败')
  }
}

function disabledDate(date) {
  return date < new Date()
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await seatStore.reserve(form)
      ElMessage.success('预约成功')
      loadReservations()
    } catch (error) {
      ElMessage.error(error.message || '预约失败')
    } finally {
      loading.value = false
    }
  })
}

// FIXED: P2-FE-08 - 状态映射已提取到composable/useStatusMap.js
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要取消预约吗？', '提示', { type: 'warning' })
    await seatStore.cancel(row.id)
    ElMessage.success('取消成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.seat-reserve {
  max-width: 800px;
  
  .mt-20 {
    margin-top: 20px;
  }
  
  .el-input-number {
    width: 200px;
  }
}
</style>
