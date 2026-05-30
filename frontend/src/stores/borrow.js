import { defineStore } from 'pinia'
import { ref } from 'vue'
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
    const res = await borrowBook(data)
    return res
  }

  async function returnBookById(id) {
    const res = await returnBook(id)
    return res
  }

  async function renew(id, data) {
    const res = await renewBook(id, data)
    return res
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
