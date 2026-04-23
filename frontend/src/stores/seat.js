import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSeatMap, reserveSeat, cancelReserve, getMyReservations } from '@/api/seat'

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
    const res = await reserveSeat(data)
    return res
  }

  async function cancel(id) {
    const res = await cancelReserve(id)
    return res
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
    fetchMyReservations
  }
})
