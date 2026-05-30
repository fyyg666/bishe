import request from '@/utils/request'

/**
 * 获取图书分类列表
 * 依赖后端 GET /categories API，后端未就绪时返回空（由调用方提供 fallback 兜底）
 */
export function getCategories() {
  return request({
    url: '/categories',
    method: 'get'
  })
}
