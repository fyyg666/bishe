import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSeatMap, reserveSeat, cancelReserve, getMyReservations, checkIn, checkOut } from '@/api/seat'

export const useSeatStore = defineStore('seat', () => {
  // 状态
  const seats = ref([])
  const myReservations = ref([])
  const loading = ref(false)

  // 方法
  async function fetchSeatMap(area) {
    loading.value = true
    try {
      const res = await getSeatMap(area)
      seats.value = res.data || []
      return seats.value
    } finally {
      loading.value = false
    }
  }

  async function reserve(data) {
    try {
      const res = await reserveSeat(data)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function cancel(id) {
    try {
      const res = await cancelReserve(id)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function doCheckIn(id) {
    try {
      const res = await checkIn(id)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function doCheckOut(id) {
    try {
      const res = await checkOut(id)
      return res
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
      throw error
    }
  }

  async function fetchMyReservations(params) {
    loading.value = true
    try {
      const res = await getMyReservations(params)
      myReservations.value = res.data?.records || res.data || []
      return myReservations.value
    } finally {
      loading.value = false
    }
  }

  return {
    seats,
    myReservations,
    loading,
    fetchSeatMap,
    reserve,
    cancel,
    doCheckIn,
    doCheckOut,
    fetchMyReservations
  }
})
