import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/borrow', () => ({
  getMyBorrows: vi.fn(),
  borrowBook: vi.fn(),
  returnBook: vi.fn(),
  renewBook: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() },
}))

import { getMyBorrows, borrowBook, returnBook, renewBook } from '@/api/borrow'
import { ElMessage } from 'element-plus'

describe('useBorrowStore', () => {
  let store

  beforeEach(async () => {
    vi.clearAllMocks()
    await vi.resetModules()
    setActivePinia(createPinia())
    const { useBorrowStore } = await import('@/stores/borrow')
    store = useBorrowStore()
  })

  describe('initial state', () => {
    it('borrows should be empty array', () => {
      expect(store.borrows).toEqual([])
    })

    it('myBorrows should be empty array', () => {
      expect(store.myBorrows).toEqual([])
    })

    it('total should be 0', () => {
      expect(store.total).toBe(0)
    })

    it('loading should be false', () => {
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchMyBorrows', () => {
    it('should fetch my borrows and update state', async () => {
      const borrowData = [
        { id: 1, bookTitle: '测试书籍', status: 'BORROWED' },
        { id: 2, bookTitle: '第二本书', status: 'OVERDUE' },
      ]
      getMyBorrows.mockResolvedValue({ data: { records: borrowData, total: 2 } })

      const result = await store.fetchMyBorrows({ current: 1, size: 10 })

      expect(getMyBorrows).toHaveBeenCalledWith({ current: 1, size: 10 })
      expect(store.myBorrows).toEqual(borrowData)
      expect(store.total).toBe(2)
      expect(result).toEqual(borrowData)
    })

    it('should set loading to true during fetch', async () => {
      getMyBorrows.mockImplementation(() => new Promise((r) => setTimeout(() => r({ data: { records: [], total: 0 } }), 10)))

      const promise = store.fetchMyBorrows()
      expect(store.loading).toBe(true)

      await promise
      expect(store.loading).toBe(false)
    })

    it('should handle empty result', async () => {
      getMyBorrows.mockResolvedValue({ data: { records: [], total: 0 } })

      await store.fetchMyBorrows()

      expect(store.myBorrows).toEqual([])
      expect(store.total).toBe(0)
    })

    it('should handle flat data response', async () => {
      const borrowData = [{ id: 1, bookTitle: 'Flat' }]
      getMyBorrows.mockResolvedValue({ data: borrowData, total: 1 })

      await store.fetchMyBorrows()

      expect(store.myBorrows).toEqual(borrowData)
    })
  })

  describe('borrow', () => {
    it('should call borrowBook API', async () => {
      const borrowData = { bookId: 1, readerId: 2 }
      borrowBook.mockResolvedValue({ data: { id: 100 } })

      const res = await store.borrow(borrowData)

      expect(borrowBook).toHaveBeenCalledWith(borrowData)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      borrowBook.mockRejectedValue(new Error('借阅失败'))

      await expect(store.borrow({})).rejects.toThrow('借阅失败')
      expect(ElMessage.error).toHaveBeenCalledWith('借阅失败')
    })
  })

  describe('returnBookById', () => {
    it('should call returnBook API', async () => {
      returnBook.mockResolvedValue({ data: { success: true } })

      const res = await store.returnBookById(100)

      expect(returnBook).toHaveBeenCalledWith(100)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      returnBook.mockRejectedValue(new Error('还书失败'))

      await expect(store.returnBookById(100)).rejects.toThrow('还书失败')
      expect(ElMessage.error).toHaveBeenCalledWith('还书失败')
    })
  })

  describe('renew', () => {
    it('should call renewBook API with data', async () => {
      renewBook.mockResolvedValue({ data: { success: true } })

      const res = await store.renew(100, { extendDays: 7 })

      expect(renewBook).toHaveBeenCalledWith(100, { extendDays: 7 })
      expect(res).toBeDefined()
    })

    it('should show error on failure', async () => {
      renewBook.mockRejectedValue(new Error('续借次数已达上限'))

      await expect(store.renew(100, {})).rejects.toThrow('续借次数已达上限')
      expect(ElMessage.error).toHaveBeenCalledWith('续借次数已达上限')
    })
  })
})
