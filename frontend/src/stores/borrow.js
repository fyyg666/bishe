import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyBorrows, borrowBook, returnBook, renewBook } from '@/api/borrow'

export const useBorrowStore = defineStore('borrow', () => {
  // 状态
  const borrows = ref([])
  const myBorrows = ref([])
  const total = ref(0)
  const loading = ref(false)

  // 方法
  async function fetchMyBorrows(params) {
    loading.value = true
    try {
      const res = await getMyBorrows(params)
      myBorrows.value = res.data?.records || res.data || []
      total.value = res.data?.total || res.total || 0
      return myBorrows.value
    } finally {
      loading.value = false
    }
  }

  async function borrow(data) {
    try {
      const res = await borrowBook(data)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function returnBookById(id) {
    try {
      const res = await returnBook(id)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function renew(id, data) {
    try {
      const res = await renewBook(id, data)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  return {
    borrows,
    myBorrows,
    total,
    loading,
    fetchMyBorrows,
    borrow,
    returnBookById,
    renew
  }
})
