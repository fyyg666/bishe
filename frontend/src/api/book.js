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

export function uploadFile(formData) {
  return request({
    url: '/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function advancedSearchBooks(params) {
  return request({
    url: '/books/advanced-search',
    method: 'get',
    params
  })
}

export function getCategoryFacet() {
  return request({
    url: '/books/facets/categories',
    method: 'get'
  })
}

export function getAuthorFacet() {
  return request({
    url: '/books/facets/authors',
    method: 'get'
  })
}

export function getHotBooks() {
  return request({ url: '/books/hot', method: 'get' })
}

export function getNewBooks() {
  return request({ url: '/books/new', method: 'get' })
}

export function checkIsbn(isbn) {
  return request({ url: '/books/check-isbn', method: 'get', params: { isbn } })
}

export function lookupIsbn(isbn) {
  return request({ url: '/books/isbn-lookup', method: 'get', params: { isbn } })
}
