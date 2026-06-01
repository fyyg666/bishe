import request from '@/utils/request'

export function createBookReservation(bookId) {
  return request({ url: '/book-reservations', method: 'post', params: { bookId } })
}

export function cancelBookReservation(id) {
  return request({ url: `/book-reservations/${id}/cancel`, method: 'post' })
}

export function getMyBookReservations(current = 1, size = 10) {
  return request({ url: '/book-reservations/my', method: 'get', params: { current, size } })
}
