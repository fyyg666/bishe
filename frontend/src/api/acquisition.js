import request from '@/utils/request'

export function getPurchaseOrderList(params) {
  return request({
    url: '/purchase-orders',
    method: 'get',
    params
  })
}

export function getPurchaseOrderDetail(id) {
  return request({
    url: `/purchase-orders/${id}`,
    method: 'get'
  })
}

export function receiveToCatalog(itemId, quantity = 1) {
  return request({
    url: `/acquisition/items/${itemId}/catalog`,
    method: 'post',
    params: { quantity }
  })
}
