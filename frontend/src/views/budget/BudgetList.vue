<template>
  <div class="budget-page">
    <el-card class="page-header">
      <div class="header-content">
        <h2>预算管理</h2>
        <div>
          <el-select
            v-model="fiscalYear"
            placeholder="财政年度"
            clearable
            style="width: 120px; margin-right: 12px"
            @change="fetchList"
          >
            <el-option
              v-for="y in yearOptions"
              :key="y"
              :label="y + '年'"
              :value="y"
            />
          </el-select>
          <el-button
            type="primary"
            @click="showCreateDialog = true"
          >
            新建预算
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="page-body">
      <el-table
        v-loading="loading"
        :data="fundList"
        stripe
      >
        <el-table-column
          prop="name"
          label="预算名称"
          min-width="150"
        />
        <el-table-column
          prop="totalAmount"
          label="总金额(元)"
          width="130"
        />
        <el-table-column
          prop="usedAmount"
          label="已用金额(元)"
          width="130"
        />
        <el-table-column
          prop="remaining"
          label="剩余金额(元)"
          width="130"
        >
          <template #default="{ row }">
            <span :style="{ color: row.remaining < 0 ? '#f56c6c' : '' }">{{ row.remaining }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="fiscalYear"
          label="财政年度"
          width="100"
        />
        <el-table-column
          label="操作"
          width="250"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              @click="openAllocate(row)"
            >
              分配
            </el-button>
            <el-button
              size="small"
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingFund ? '编辑预算' : '新建预算'"
      width="480px"
    >
      <el-form
        :model="fundForm"
        label-width="100px"
      >
        <el-form-item label="预算名称">
          <el-input v-model="fundForm.name" />
        </el-form-item>
        <el-form-item label="总金额(元)">
          <el-input
            v-model="fundForm.totalAmount"
            type="number"
          />
        </el-form-item>
        <el-form-item label="财政年度">
          <el-input
            v-model="fundForm.fiscalYear"
            type="number"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSave"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框 -->
    <el-dialog
      v-model="showAllocateDialog"
      title="预算分配"
      width="450px"
    >
      <el-form
        :model="allocateForm"
        label-width="100px"
      >
        <el-form-item label="预算名称">
          {{ currentFund?.name }}
        </el-form-item>
        <el-form-item label="剩余金额">
          {{ currentFund?.remaining }} 元
        </el-form-item>
        <el-form-item label="订单ID">
          <el-input
            v-model="allocateForm.orderId"
            type="number"
          />
        </el-form-item>
        <el-form-item label="分配金额(元)">
          <el-input
            v-model="allocateForm.amount"
            type="number"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAllocateDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="allocating"
          @click="handleAllocate"
        >
          确认分配
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFunds, createFund, updateFund, deleteFund, allocateToOrder } from '@/api/budget'

const loading = ref(false)
const submitting = ref(false)
const allocating = ref(false)
const fundList = ref([])
const fiscalYear = ref(null)
const showCreateDialog = ref(false)
const showAllocateDialog = ref(false)
const editingFund = ref(null)
const currentFund = ref(null)

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const arr = []
  for (let y = currentYear + 1; y >= currentYear - 5; y--) arr.push(y)
  return arr
})

const fundForm = ref({ name: '', totalAmount: 0, fiscalYear: currentYear })
const allocateForm = ref({ orderId: '', amount: 0 })

async function fetchList() {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (fiscalYear.value) params.fiscalYear = fiscalYear.value
    const res = await listFunds(params)
    fundList.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载预算列表失败')
  }
  finally { loading.value = false }
}

function openEdit(row) {
  editingFund.value = row
  fundForm.value = { name: row.name, totalAmount: row.totalAmount, fiscalYear: row.fiscalYear }
  showCreateDialog.value = true
}

function openAllocate(row) {
  currentFund.value = row
  allocateForm.value = { orderId: '', amount: 0 }
  showAllocateDialog.value = true
}

async function handleSave() {
  submitting.value = true
  try {
    if (editingFund.value) {
      await updateFund(editingFund.value.id, fundForm.value)
      ElMessage.success('预算更新成功')
    } else {
      await createFund(fundForm.value)
      ElMessage.success('预算创建成功')
    }
    showCreateDialog.value = false
    editingFund.value = null
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
  finally { submitting.value = false }
}

async function handleAllocate() {
  if (!currentFund.value) return
  allocating.value = true
  try {
    await allocateToOrder(currentFund.value.id, allocateForm.value.orderId, allocateForm.value.amount)
    ElMessage.success('预算分配成功')
    showAllocateDialog.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
  finally { allocating.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该预算？')
    await deleteFund(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除预算失败')
    }
  }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.budget-page {
  .page-header { margin-bottom: $space-4; }
  .header-content {
    display: flex; justify-content: space-between; align-items: center;
    h2 { margin: 0; }
  }
  .pagination { margin-top: $space-4; text-align: right; }
}
</style>
