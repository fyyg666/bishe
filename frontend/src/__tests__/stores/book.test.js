import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/book', () => ({
  getBookList: vi.fn(),
  getBookDetail: vi.fn(),
  addBook: vi.fn(),
  updateBook: vi.fn(),
  deleteBook: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() },
}))

import { getBookList, getBookDetail, addBook, updateBook, deleteBook } from '@/api/book'
import { ElMessage } from 'element-plus'

describe('useBookStore', () => {
  let store

  beforeEach(async () => {
    vi.clearAllMocks()
    await vi.resetModules()
    setActivePinia(createPinia())
    const { useBookStore } = await import('@/stores/book')
    store = useBookStore()
  })

  describe('initial state', () => {
    it('books should be empty array', () => {
      expect(store.books).toEqual([])
    })

    it('currentBook should be null', () => {
      expect(store.currentBook).toBeNull()
    })

    it('total should be 0', () => {
      expect(store.total).toBe(0)
    })

    it('loading should be false', () => {
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchBooks', () => {
    it('should fetch books and update state', async () => {
      const bookList = [
        { id: 1, title: 'Vue入门', author: '张三' },
        { id: 2, title: 'React实战', author: '李四' },
      ]
      getBookList.mockResolvedValue({ data: { records: bookList, total: 2 } })

      const result = await store.fetchBooks({ current: 1, size: 10 })

      expect(getBookList).toHaveBeenCalledWith({ current: 1, size: 10 })
      expect(store.books).toEqual(bookList)
      expect(store.total).toBe(2)
      expect(result).toEqual(bookList)
    })

    it('should set loading to true during fetch', async () => {
      getBookList.mockImplementation(() => new Promise((r) => setTimeout(() => r({ data: { records: [], total: 0 } }), 10)))

      const promise = store.fetchBooks()
      expect(store.loading).toBe(true)

      await promise
      expect(store.loading).toBe(false)
    })

    it('should handle empty result', async () => {
      getBookList.mockResolvedValue({ data: { records: [], total: 0 } })

      await store.fetchBooks()

      expect(store.books).toEqual([])
      expect(store.total).toBe(0)
    })

    it('should handle flat data without records wrapper', async () => {
      const bookList = [{ id: 1, title: 'Flat' }]
      getBookList.mockResolvedValue({ data: bookList })

      await store.fetchBooks()

      expect(store.books).toEqual(bookList)
    })

    it('should set loading to false even on error', async () => {
      getBookList.mockRejectedValue(new Error('Network error'))

      try { await store.fetchBooks() } catch {}
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchBookDetail', () => {
    it('should fetch book detail and set currentBook', async () => {
      const bookDetail = { id: 1, title: 'Vue入门', author: '张三', isbn: '123456' }
      getBookDetail.mockResolvedValue({ data: bookDetail })

      const result = await store.fetchBookDetail(1)

      expect(getBookDetail).toHaveBeenCalledWith(1)
      expect(store.currentBook).toEqual(bookDetail)
      expect(result).toEqual(bookDetail)
    })

    it('should handle response without data wrapper', async () => {
      const bookDetail = { id: 2, title: 'Direct', author: '李四' }
      getBookDetail.mockResolvedValue(bookDetail)

      await store.fetchBookDetail(2)

      expect(store.currentBook).toEqual(bookDetail)
    })

    it('should set loading to false after fetch', async () => {
      getBookDetail.mockResolvedValue({ data: { id: 1, title: 'T' } })

      await store.fetchBookDetail(1)

      expect(store.loading).toBe(false)
    })
  })

  describe('createBook', () => {
    it('should call addBook API', async () => {
      const bookData = { title: '新书', author: '作者', isbn: '111' }
      addBook.mockResolvedValue({ data: { id: 3 } })

      const res = await store.createBook(bookData)

      expect(addBook).toHaveBeenCalledWith(bookData)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      addBook.mockRejectedValue(new Error('ISBN已存在'))

      await expect(store.createBook({})).rejects.toThrow('ISBN已存在')
      expect(ElMessage.error).toHaveBeenCalledWith('ISBN已存在')
    })
  })

  describe('editBook', () => {
    it('should call updateBook API', async () => {
      const bookData = { title: '修改后书名' }
      updateBook.mockResolvedValue({ data: { success: true } })

      const res = await store.editBook(1, bookData)

      expect(updateBook).toHaveBeenCalledWith(1, bookData)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      updateBook.mockRejectedValue(new Error('更新失败'))

      await expect(store.editBook(1, {})).rejects.toThrow('更新失败')
      expect(ElMessage.error).toHaveBeenCalledWith('更新失败')
    })
  })

  describe('removeBook', () => {
    it('should call deleteBook API', async () => {
      deleteBook.mockResolvedValue({ data: { success: true } })

      const res = await store.removeBook(1)

      expect(deleteBook).toHaveBeenCalledWith(1)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      deleteBook.mockRejectedValue(new Error('图书不存在'))

      await expect(store.removeBook(999)).rejects.toThrow('图书不存在')
      expect(ElMessage.error).toHaveBeenCalledWith('图书不存在')
    })
  })
})
