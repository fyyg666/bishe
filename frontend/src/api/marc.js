import request from '@/utils/request'

export function getMarcRecords(params) {
  return request({ url: '/marc/records', method: 'get', params })
}

export function getMarcRecord(id) {
  return request({ url: `/marc/records/${id}`, method: 'get' })
}

export function createMarcRecord(data) {
  return request({ url: '/marc/records', method: 'post', data })
}

export function updateMarcRecord(id, data) {
  return request({ url: `/marc/records/${id}`, method: 'put', data })
}

export function deleteMarcRecord(id) {
  return request({ url: `/marc/records/${id}`, method: 'delete' })
}

export function getMarcRecordByBookId(bookId) {
  return request({ url: `/marc/records/book/${bookId}`, method: 'get' })
}

export function linkMarcToBook(recordId, bookId) {
  return request({ url: `/marc/records/${recordId}/link/${bookId}`, method: 'post' })
}

export function importMarcFile(formData) {
  return request({
    url: '/marc/records/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportMarcRecords(ids) {
  return request({
    url: '/marc/records/export',
    method: 'post',
    data: { ids },
    responseType: 'blob'
  })
}
