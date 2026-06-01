import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/seat', () => ({
  getSeatMap: vi.fn(),
  reserveSeat: vi.fn(),
  cancelReserve: vi.fn(),
  getMyReservations: vi.fn(),
  checkIn: vi.fn(),
  checkOut: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() },
}))

import { getSeatMap, reserveSeat, cancelReserve, getMyReservations, checkIn, checkOut } from '@/api/seat'
import { ElMessage } from 'element-plus'

describe('useSeatStore', () => {
  let store

  beforeEach(async () => {
    vi.clearAllMocks()
    await vi.resetModules()
    setActivePinia(createPinia())
    const { useSeatStore } = await import('@/stores/seat')
    store = useSeatStore()
  })

  describe('initial state', () => {
    it('seats should be empty array', () => {
      expect(store.seats).toEqual([])
    })

    it('myReservations should be empty array', () => {
      expect(store.myReservations).toEqual([])
    })

    it('loading should be false', () => {
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchSeatMap', () => {
    it('should fetch seats and update state', async () => {
      const seatData = [
        { id: 1, seatNumber: 'A01', area: 'A区', status: 'AVAILABLE' },
        { id: 2, seatNumber: 'A02', area: 'A区', status: 'OCCUPIED' },
      ]
      getSeatMap.mockResolvedValue({ data: seatData })

      const result = await store.fetchSeatMap('A区')

      expect(getSeatMap).toHaveBeenCalledWith('A区')
      expect(store.seats).toEqual(seatData)
      expect(result).toEqual(seatData)
      expect(store.loading).toBe(false)
    })

    it('should set loading to true during fetch', async () => {
      getSeatMap.mockImplementation(() => new Promise((r) => setTimeout(() => r({ data: [] }), 10)))

      const promise = store.fetchSeatMap()
      expect(store.loading).toBe(true)

      await promise
      expect(store.loading).toBe(false)
    })

    it('should handle empty data response', async () => {
      getSeatMap.mockResolvedValue({ data: [] })

      await store.fetchSeatMap()

      expect(store.seats).toEqual([])
    })

    it('should set loading to false even on error', async () => {
      getSeatMap.mockRejectedValue(new Error('Network error'))

      try { await store.fetchSeatMap() } catch {}
      expect(store.loading).toBe(false)
    })

    it('should handle response without data wrapper', async () => {
      const seatData = [{ id: 1, seatNumber: 'B01', area: 'B区' }]
      getSeatMap.mockResolvedValue({ data: seatData })

      await store.fetchSeatMap()

      expect(store.seats).toEqual(seatData)
    })
  })

  describe('reserve', () => {
    it('should call reserveSeat API', async () => {
      const reserveData = { seatNumber: 'A01', area: 'A区', startTime: '09:00', endTime: '12:00' }
      reserveSeat.mockResolvedValue({ data: { id: 100 } })

      const res = await store.reserve(reserveData)

      expect(reserveSeat).toHaveBeenCalledWith(reserveData)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      const error = new Error('座位已被占用')
      reserveSeat.mockRejectedValue(error)

      await expect(store.reserve({})).rejects.toThrow('座位已被占用')
      expect(ElMessage.error).toHaveBeenCalledWith('座位已被占用')
    })
  })

  describe('cancel', () => {
    it('should call cancelReserve API', async () => {
      cancelReserve.mockResolvedValue({ data: { success: true } })

      const res = await store.cancel(100)

      expect(cancelReserve).toHaveBeenCalledWith(100)
      expect(res).toBeDefined()
    })

    it('should show error message on failure', async () => {
      cancelReserve.mockRejectedValue(new Error('取消失败'))

      await expect(store.cancel(100)).rejects.toThrow('取消失败')
      expect(ElMessage.error).toHaveBeenCalledWith('取消失败')
    })
  })

  describe('doCheckIn', () => {
    it('should call checkIn API', async () => {
      checkIn.mockResolvedValue({ data: { success: true } })

      const res = await store.doCheckIn(100)

      expect(checkIn).toHaveBeenCalledWith(100)
      expect(res).toBeDefined()
    })
  })

  describe('doCheckOut', () => {
    it('should call checkOut API', async () => {
      checkOut.mockResolvedValue({ data: { success: true } })

      const res = await store.doCheckOut(100)

      expect(checkOut).toHaveBeenCalledWith(100)
      expect(res).toBeDefined()
    })
  })

  describe('fetchMyReservations', () => {
    it('should fetch my reservations', async () => {
      const reservationData = [{ id: 1, seatNumber: 'A01', status: 'RESERVED' }]
      getMyReservations.mockResolvedValue({ data: { records: reservationData, total: 1 } })

      const result = await store.fetchMyReservations({ current: 1, size: 10 })

      expect(getMyReservations).toHaveBeenCalledWith({ current: 1, size: 10 })
      expect(store.myReservations).toEqual(reservationData)
      expect(result).toEqual(reservationData)
    })

    it('should handle flat data response', async () => {
      const reservationData = [{ id: 1, seatNumber: 'A01' }]
      getMyReservations.mockResolvedValue({ data: reservationData })

      await store.fetchMyReservations()

      expect(store.myReservations).toEqual(reservationData)
    })
  })
})
