<template>
  <div class="purchase-order-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>采购订单详情</span>
          <el-button @click="$router.back()">
            返回
          </el-button>
        </div>
      </template>

      <template v-if="order">
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="采购单号">
            {{ order.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商">
            {{ order.vendorName || `供应商#${order.vendorId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            ¥{{ order.totalAmount }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(order.status)">
              {{ statusLabel(order.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ order.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="审批时间">
            {{ order.approveTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ order.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          状态流转
        </el-divider>
        <el-steps
          :active="activeStep"
          align-center
          finish-status="success"
        >
          <el-step title="草稿" />
          <el-step title="待审批" />
          <el-step title="已审批" />
          <el-step title="收货中" />
          <el-step title="已完成" />
        </el-steps>

        <el-divider content-position="left">
          采购明细
        </el-divider>

        <el-table
          :data="order.items"
          border
          style="width: 100%"
        >
          <el-table-column
            prop="bookTitle"
            label="书名"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            prop="isbn"
            label="ISBN"
            width="160"
          />
          <el-table-column
            prop="quantity"
            label="采购数量"
            width="100"
            align="center"
          />
          <el-table-column
            prop="unitPrice"
            label="单价"
            width="100"
            align="right"
          >
            <template #default="{ row }">
              ¥{{ row.unitPrice }}
            </template>
          </el-table-column>
          <el-table-column
            prop="receivedQuantity"
            label="已收货"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              {{ row.receivedQuantity }} / {{ row.quantity }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="120"
            align="center"
          >
            <template #default="{ row }">
              <el-button
                v-if="canReceive(row)"
                type="primary"
                size="small"
                @click="openReceiveDialog(row)"
              >
                收货
              </el-button>
              <span
                v-else
                class="text-muted"
              >-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <el-dialog
      v-model="receiveDialogVisible"
      title="收货登记"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="书名">
          {{ receiveItem?.bookTitle }}
        </el-form-item>
        <el-form-item label="采购数量">
          {{ receiveItem?.quantity }}
        </el-form-item>
        <el-form-item label="已收货">
          {{ receiveItem?.receivedQuantity }}
        </el-form-item>
        <el-form-item label="本次收货">
          <el-input-number
            v-model="receiveQty"
            :min="1"
            :max="receiveItem ? receiveItem.quantity - receiveItem.receivedQuantity : 1"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="receiving"
          @click="handleReceive"
        >
          确认收货
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPurchaseOrder, receiveItems } from '@/api/purchaseOrder'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const receiveDialogVisible = ref(false)
const receiveItem = ref(null)
const receiveQty = ref(1)
const receiving = ref(false)

const activeStep = computed(() => {
  if (!order.value) return 0
  const map = {
    DRAFT: 0,
    PENDING_APPROVAL: 1,
    APPROVED: 2,
    PARTIAL_RECEIVED: 3,
    COMPLETED: 4,
    CANCELLED: -1
  }
  return map[order.value.status] ?? 0
})

onMounted(() => {
  loadOrder()
})

async function loadOrder() {
  const id = Number(route.params.id)
  if (!id || isNaN(id)) {
    router.push('/purchase-orders')
    return
  }
  loading.value = true
  try {
    const res = await getPurchaseOrder(id)
    order.value = res.data || res
  } catch {
    ElMessage.error('加载采购订单详情失败')
  } finally {
    loading.value = false
  }
}

function canReceive(row) {
  if (!order.value) return false
  const status = order.value.status
  if (status !== 'APPROVED' && status !== 'PARTIAL_RECEIVED') return false
  return row.receivedQuantity < row.quantity
}

function openReceiveDialog(row) {
  receiveItem.value = row
  receiveQty.value = row.quantity - row.receivedQuantity
  receiveDialogVisible.value = true
}

async function handleReceive() {
  if (!receiveItem.value || !order.value) return
  receiving.value = true
  try {
    await receiveItems(order.value.id, receiveItem.value.id, receiveQty.value)
    ElMessage.success('收货登记成功')
    receiveDialogVisible.value = false
    loadOrder()
  } catch (e) {
    ElMessage.error(e.message || '收货登记失败')
  } finally {
    receiving.value = false
  }
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
</script>

<style lang="scss" scoped>
.purchase-order-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .text-muted {
    color: #999;
  }
}
</style>
