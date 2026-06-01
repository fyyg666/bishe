import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBookList, getBookDetail, addBook, updateBook, deleteBook } from '@/api/book'

export const useBookStore = defineStore('book', () => {
  // 状态
  const books = ref([])
  const currentBook = ref(null)
  const total = ref(0)
  const loading = ref(false)

  // 方法
  async function fetchBooks(params) {
    loading.value = true
    try {
      const res = await getBookList(params)
      books.value = res.data?.records || res.data || []
      total.value = res.data?.total || res.total || 0
      return books.value
    } finally {
      loading.value = false
    }
  }

  async function fetchBookDetail(id) {
    loading.value = true
    try {
      const res = await getBookDetail(id)
      currentBook.value = res.data || res
      return currentBook.value
    } finally {
      loading.value = false
    }
  }

  async function createBook(data) {
    try {
      const res = await addBook(data)
      return res
    } catch (error) {
      throw error
    }
  }

  async function editBook(id, data) {
    try {
      const res = await updateBook(id, data)
      return res
    } catch (error) {
      throw error
    }
  }

  async function removeBook(id) {
    try {
      const res = await deleteBook(id)
      return res
    } catch (error) {
      throw error
    }
  }

  return {
    books,
    currentBook,
    total,
    loading,
    fetchBooks,
    fetchBookDetail,
    createBook,
    editBook,
    removeBook
  }
})
