<template>
  <div class="compensation-page">
    <el-card class="page-header">
      <div class="header-content">
        <h2>赔偿管理</h2>
        <el-button
          type="primary"
          @click="showCreateDialog = true"
        >
          新建赔偿单
        </el-button>
      </div>
    </el-card>

    <el-card class="page-body">
      <el-table
        v-loading="loading"
        :data="compensationList"
        stripe
      >
        <el-table-column
          prop="orderNo"
          label="赔偿单号"
          min-width="180"
        />
        <el-table-column
          prop="username"
          label="用户"
          min-width="120"
        />
        <el-table-column
          prop="bookTitle"
          label="书名"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="compTypeDesc"
          label="类型"
          width="80"
        />
        <el-table-column
          prop="amount"
          label="金额(元)"
          width="100"
        />
        <el-table-column
          prop="paymentMethodDesc"
          label="支付方式"
          width="120"
        />
        <el-table-column
          prop="statusDesc"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'PAID' ? 'success' : row.status === 'CANCELLED' ? 'info' : 'warning'">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              @click="viewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="primary"
              @click="openPayment(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 创建赔偿单对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建赔偿单"
      width="500px"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="createForm.userId" />
        </el-form-item>
        <el-form-item label="借阅记录ID" prop="borrowId">
          <el-input v-model="createForm.borrowId" />
        </el-form-item>
        <el-form-item label="图书ID" prop="bookId">
          <el-input v-model="createForm.bookId" />
        </el-form-item>
        <el-form-item label="书名" prop="bookTitle">
          <el-input v-model="createForm.bookTitle" />
        </el-form-item>
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="createForm.isbn" />
        </el-form-item>
        <el-form-item label="赔偿类型" prop="compType">
          <el-select v-model="createForm.compType">
            <el-option
              label="丢失"
              value="LOST"
            />
            <el-option
              label="损坏"
              value="DAMAGE"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="赔偿金额" prop="amount">
          <el-input
            v-model="createForm.amount"
            type="number"
          />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="createForm.paymentMethod">
            <el-option
              label="现金"
              value="CASH"
            />
            <el-option
              label="积分抵扣"
              value="CREDIT"
            />
            <el-option
              label="志愿服务"
              value="VOLUNTEER"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="createForm.remark"
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="creating"
          @click="handleCreate"
        >
          确认创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 处理支付对话框 -->
    <el-dialog
      v-model="showPaymentDialog"
      title="处理赔偿"
      width="450px"
    >
      <el-form
        :model="paymentForm"
        label-width="120px"
      >
        <el-form-item label="赔偿单号">
          {{ currentCompensation?.orderNo }}
        </el-form-item>
        <el-form-item label="用户名">
          {{ currentCompensation?.username }}
        </el-form-item>
        <el-form-item label="书名">
          {{ currentCompensation?.bookTitle }}
        </el-form-item>
        <el-form-item label="赔偿金额">
          {{ currentCompensation?.amount }} 元
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="paymentForm.method">
            <el-option
              label="现金"
              value="CASH"
            />
            <el-option
              label="积分抵扣"
              value="CREDIT"
            />
            <el-option
              label="志愿服务抵扣"
              value="VOLUNTEER"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="paymentForm.method === 'CREDIT'"
          label="积分数量"
        >
          <el-input
            v-model="paymentForm.creditAmount"
            type="number"
          />
        </el-form-item>
        <el-form-item
          v-if="paymentForm.method === 'VOLUNTEER'"
          label="志愿时长(小时)"
        >
          <el-input
            v-model="paymentForm.volunteerHours"
            type="number"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="paymentForm.remark"
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPaymentDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="processing"
          @click="handleProcess"
        >
          确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCompensations, createCompensation,
  processCashPayment, processCreditPayment, processVolunteerPayment,
  cancelCompensation, getCompensation
} from '@/api/compensation'

const loading = ref(false)
const creating = ref(false)
const processing = ref(false)
const compensationList = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const showCreateDialog = ref(false)
const showPaymentDialog = ref(false)
const currentCompensation = ref(null)
const createFormRef = ref(null)

const createRules = {
  compType: [{ required: true, message: '请选择赔偿类型', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入赔偿金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '赔偿金额必须为正数', trigger: 'blur' }
  ]
}

const createForm = ref({
  userId: '', borrowId: '', bookId: '', bookTitle: '',
  isbn: '', compType: 'LOST', amount: 0, paymentMethod: 'CASH', remark: ''
})

const paymentForm = ref({
  method: 'CASH', creditAmount: 0, volunteerHours: 0, remark: ''
})

async function fetchList() {
  loading.value = true
  try {
    const res = await listCompensations({ current: current.value, size: size.value })
    compensationList.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      await createCompensation(createForm.value)
      ElMessage.success('赔偿单创建成功')
      showCreateDialog.value = false
      fetchList()
    } catch (e) { ElMessage.error(e.message) }
    finally { creating.value = false }
  })
}

async function viewDetail(row) {
  try {
    const res = await getCompensation(row.id)
    currentCompensation.value = res.data || res
    showPaymentDialog.value = true
  } catch (e) { ElMessage.error(e.message) }
}

function openPayment(row) {
  currentCompensation.value = row
  paymentForm.value = { method: 'CASH', creditAmount: 0, volunteerHours: 0, remark: '' }
  showPaymentDialog.value = true
}

async function handleProcess() {
  if (!currentCompensation.value) return
  processing.value = true
  try {
    const id = currentCompensation.value.id
    if (paymentForm.value.method === 'CASH') {
      await processCashPayment(id, paymentForm.value.remark)
    } else if (paymentForm.value.method === 'CREDIT') {
      await processCreditPayment(id, paymentForm.value.creditAmount, paymentForm.value.remark)
    } else {
      await processVolunteerPayment(id, paymentForm.value.volunteerHours, paymentForm.value.remark)
    }
    ElMessage.success('赔偿处理成功')
    showPaymentDialog.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
  finally { processing.value = false }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定取消该赔偿订单？')
    await cancelCompensation(row.id, '管理员取消')
    ElMessage.success('已取消')
    fetchList()
  } catch { /* ignore */ }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.compensation-page {
  .page-header { margin-bottom: 16px; }
  .header-content {
    display: flex; justify-content: space-between; align-items: center;
    h2 { margin: 0; }
  }
  .pagination { margin-top: 16px; text-align: right; }
}
</style>
