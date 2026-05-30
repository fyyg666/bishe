<template>
  <div class="book-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>图书详情</span>
          <el-button @click="$router.back()">
            返回
          </el-button>
        </div>
      </template>
      
      <el-descriptions
        v-if="book"
        :column="2"
        border
      >
        <el-descriptions-item label="书名">
          {{ book.title }}
        </el-descriptions-item>
        <el-descriptions-item label="作者">
          {{ book.author }}
        </el-descriptions-item>
        <el-descriptions-item label="ISBN">
          {{ book.isbn }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          {{ book.categoryName }}
        </el-descriptions-item>
        <el-descriptions-item label="出版社">
          {{ book.publisher }}
        </el-descriptions-item>
        <el-descriptions-item label="出版日期">
          {{ book.publishDate }}
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          {{ book.availableCount }}
        </el-descriptions-item>
        <el-descriptions-item label="价格">
          ¥{{ book.price }}
        </el-descriptions-item>
        <el-descriptions-item
          label="简介"
          :span="2"
        >
          {{ book.description }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="book.availableCount > 0 ? 'success' : 'danger'">
            {{ book.availableCount > 0 ? '在架' : '借出' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="入库时间">
          {{ book.createTime }}
        </el-descriptions-item>
      </el-descriptions>
      
      <div class="action-bar">
        <el-button
          type="primary"
          @click="handleBorrow"
        >
          借阅此书
        </el-button>
        <el-button @click="$router.push('/books')">
          返回列表
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookStore } from '@/stores/book'

const route = useRoute()
const router = useRouter()
const bookStore = useBookStore()

const loading = ref(false)
const book = ref(null)

onMounted(() => {
  loadBookDetail()
})

async function loadBookDetail() {
  loading.value = true
  try {
    const id = route.params.id
    await bookStore.fetchBookDetail(id)
    book.value = bookStore.currentBook
  } catch {
    ElMessage.error('加载图书详情失败')
  } finally {
    loading.value = false
  }
}

function handleBorrow() {
  if (book.value?.availableCount <= 0) {
    ElMessage.warning('该书目前无库存')
    return
  }
  router.push({ path: '/borrows/page', query: { bookId: book.value.id } })
}
</script>

<style lang="scss" scoped>
.book-detail {
  :deep(.el-card) {
    border: none;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  :deep(.el-card__body) {
    padding: 32px 40px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .action-bar {
    margin-top: 24px;
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}
</style>
