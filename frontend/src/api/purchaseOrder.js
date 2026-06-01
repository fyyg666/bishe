import request from '@/utils/request'

export function listPurchaseOrders(params) {
  return request({
    url: '/purchase-orders',
    method: 'get',
    params
  })
}

export function getPurchaseOrder(id) {
  return request({
    url: `/purchase-orders/${id}`,
    method: 'get'
  })
}

export function createPurchaseOrder(data) {
  return request({
    url: '/purchase-orders',
    method: 'post',
    data
  })
}

export function updatePurchaseOrder(id, data) {
  return request({
    url: `/purchase-orders/${id}`,
    method: 'put',
    data
  })
}

export function deletePurchaseOrder(id) {
  return request({
    url: `/purchase-orders/${id}`,
    method: 'delete'
  })
}

export function submitForApproval(id) {
  return request({
    url: `/purchase-orders/${id}/submit`,
    method: 'post'
  })
}

export function approvePurchaseOrder(id) {
  return request({
    url: `/purchase-orders/${id}/approve`,
    method: 'post'
  })
}

export function receiveItems(orderId, itemId, receivedQty) {
  return request({
    url: `/purchase-orders/${orderId}/items/${itemId}/receive`,
    method: 'post',
    params: { receivedQty }
  })
}

export function cancelPurchaseOrder(id) {
  return request({
    url: `/purchase-orders/${id}/cancel`,
    method: 'post'
  })
}
