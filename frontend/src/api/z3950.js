import request from '@/utils/request'

export function getZ3950Sources() {
  return request({ url: '/z3950/sources', method: 'get' })
}

export function createZ3950Source(data) {
  return request({ url: '/z3950/sources', method: 'post', data })
}

export function updateZ3950Source(id, data) {
  return request({ url: `/z3950/sources/${id}`, method: 'put', data })
}

export function deleteZ3950Source(id) {
  return request({ url: `/z3950/sources/${id}`, method: 'delete' })
}

export function searchZ3950(sourceId, query, queryType, maxResults = 20) {
  return request({ url: '/z3950/search', method: 'get', params: { sourceId, query, queryType, maxResults } })
}

export function searchAllZ3950(query, queryType, maxResults = 10) {
  return request({ url: '/z3950/search-all', method: 'get', params: { query, queryType, maxResults } })
}

export function importZ3950ToMarc(sourceId, query, queryType) {
  return request({ url: '/z3950/import', method: 'post', params: { sourceId, query, queryType } })
}
