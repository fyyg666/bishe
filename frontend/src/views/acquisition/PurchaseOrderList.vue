<template>
  <div class="purchase-order-page">
    <el-card class="page-header">
      <div class="header-content">
        <h2>采购订单管理</h2>
        <div class="header-actions">
          <el-select
            v-model="filterStatus"
            placeholder="状态筛选"
            clearable
            style="width: 140px; margin-right: 12px"
            @change="fetchList"
          >
            <el-option
              label="草稿"
              value="DRAFT"
            />
            <el-option
              label="待审批"
              value="PENDING_APPROVAL"
            />
            <el-option
              label="已审批"
              value="APPROVED"
            />
            <el-option
              label="部分收货"
              value="PARTIAL_RECEIVED"
            />
            <el-option
              label="已完成"
              value="COMPLETED"
            />
            <el-option
              label="已取消"
              value="CANCELLED"
            />
          </el-select>
          <el-button
            type="primary"
            @click="openCreateDialog"
          >
            新建采购单
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="page-body">
      <el-table
        v-loading="loading"
        :data="orderList"
        stripe
      >
        <el-table-column
          prop="orderNo"
          label="采购单号"
          min-width="160"
        />
        <el-table-column
          prop="vendorName"
          label="供应商"
          min-width="120"
        >
          <template #default="{ row }">
            {{ row.vendorName || `供应商#${row.vendorId}` }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="totalAmount"
          label="总金额(元)"
          width="120"
          align="right"
        >
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="170"
        />
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              @click="$router.push(`/purchase-orders/${row.id}`)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              size="small"
              type="primary"
              @click="handleSubmit(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_APPROVAL'"
              size="small"
              type="success"
              @click="handleApprove(row)"
            >
              审批
            </el-button>
            <el-button
              v-if="canCancel(row.status)"
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

    <el-dialog
      v-model="showCreateDialog"
      title="新建采购单"
      width="720px"
      destroy-on-close
    >
      <el-form
        :model="createForm"
        label-width="90px"
      >
        <el-form-item label="供应商">
          <el-select
            v-model="createForm.vendorId"
            placeholder="选择供应商"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="v in vendorOptions"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item label="采购明细">
          <el-table
            :data="createForm.items"
            border
            style="width: 100%"
          >
            <el-table-column
              label="书名"
              min-width="160"
            >
              <template #default="{ row }">
                <el-input
                  v-model="row.bookTitle"
                  placeholder="书名"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="ISBN"
              width="150"
            >
              <template #default="{ row }">
                <el-input
                  v-model="row.isbn"
                  placeholder="ISBN"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="数量"
              width="90"
            >
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  size="small"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="单价"
              width="110"
            >
              <template #default="{ row }">
                <el-input-number
                  v-model="row.unitPrice"
                  :min="0"
                  :precision="2"
                  size="small"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="70"
              align="center"
            >
              <template #default="{ $index }">
                <el-button
                  type="danger"
                  link
                  @click="createForm.items.splice($index, 1)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            style="margin-top: 8px; width: 100%"
            @click="addItem"
          >
            + 添加图书
          </el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listPurchaseOrders,
  createPurchaseOrder,
  submitForApproval,
  approvePurchaseOrder,
  cancelPurchaseOrder
} from '@/api/purchaseOrder'
import { getVendorList } from '@/api/vendor'

const loading = ref(false)
const creating = ref(false)
const orderList = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const filterStatus = ref('')
const showCreateDialog = ref(false)
const vendorOptions = ref([])

const createForm = ref({
  vendorId: null,
  remark: '',
  items: [{ bookTitle: '', isbn: '', quantity: 1, unitPrice: 0 }]
})

function addItem() {
  createForm.value.items.push({ bookTitle: '', isbn: '', quantity: 1, unitPrice: 0 })
}

async function fetchList() {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await listPurchaseOrders(params)
    orderList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadVendors() {
  try {
    const res = await getVendorList({ current: 1, size: 200 })
    vendorOptions.value = res.data?.records || []
  } catch {
    ElMessage.error('加载供应商列表失败')
  }

function openCreateDialog() {
  createForm.value = {
    vendorId: null,
    remark: '',
    items: [{ bookTitle: '', isbn: '', quantity: 1, unitPrice: 0 }]
  }
  showCreateDialog.value = true
}

async function handleCreate() {
  const form = createForm.value
  if (!form.vendorId) {
    ElMessage.warning('请选择供应商')
    return
  }
  if (form.items.length === 0 || form.items.some(i => !i.bookTitle)) {
    ElMessage.warning('请填写采购明细中的书名')
    return
  }
  creating.value = true
  try {
    await createPurchaseOrder(form)
    ElMessage.success('采购单创建成功')
    showCreateDialog.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.message || '创建失败') }
  finally { creating.value = false }
}

async function handleSubmit(row) {
  try {
    await ElMessageBox.confirm('确定提交审批？', '提交确认')
    await submitForApproval(row.id)
    ElMessage.success('已提交审批')
    fetchList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('提交审批失败')
    }
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm('确定审批通过？', '审批确认')
    await approvePurchaseOrder(row.id)
    ElMessage.success('审批通过')
    fetchList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('审批失败')
    }
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定取消该采购订单？', '取消确认')
    await cancelPurchaseOrder(row.id)
    ElMessage.success('已取消')
    fetchList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('取消失败')
    }
  }
}

function canCancel(status) {
  return ['DRAFT', 'PENDING_APPROVAL', 'APPROVED'].includes(status)
}

function statusLabel(status) {
  const map = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已审批',
    PARTIAL_RECEIVED: '部分收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function statusTagType(status) {
  const map = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVED: '',
    PARTIAL_RECEIVED: '',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchList()
  loadVendors()
})
</script>

<style lang="scss" scoped>
.purchase-order-page {
  .page-header { margin-bottom: 16px; }
  .header-content {
    display: flex; justify-content: space-between; align-items: center;
    h2 { margin: 0; }
  }
  .header-actions { display: flex; align-items: center; }
  .pagination { margin-top: 16px; text-align: right; }
}
</style>
