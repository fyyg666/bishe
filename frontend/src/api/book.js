import request from '@/utils/request'

export function getBookList(params) {
  return request({
    url: '/books',
    method: 'get',
    params
  })
}

export function getBookDetail(id) {
  return request({
    url: `/books/${id}`,
    method: 'get'
  })
}

export function createBook(data) {
  return request({
    url: '/books',
    method: 'post',
    data
  })
}

// 兼容性别名导出
export const addBook = createBook

export function updateBook(id, data) {
  return request({
    url: `/books/${id}`,
    method: 'put',
    data
  })
}

export function deleteBook(id) {
  return request({
    url: `/books/${id}`,
    method: 'delete'
  })
}

export function getBookCategories() {
  return request({
    url: '/books/categories',
    method: 'get'
  })
}

export function exportBooks(params) {
  return request({
    url: '/books/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function importBooks(data) {
  return request({
    url: '/books/import',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
