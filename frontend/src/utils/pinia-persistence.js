/**
 * Pinia 持久化插件
 * 使用 sessionStorage 持久化 store 状态（避免 localStorage 的 XSS 风险）
 * 
 * 配置方式：在 src/main.js 中配置 PERSIST_STORES 数组
 */

// 需要持久化的 store ID 列表（在 main.js 中配置）
export const PERSIST_STORES = ['user']

/**
 * 持久化插件主体
 */
export function piniaPersistedstatePlugin({ store }) {
  // 检查是否启用持久化
  if (PERSIST_STORES.includes(store.$id)) {
    // 从 sessionStorage 恢复状态
    const stored = sessionStorage.getItem(`pinia-${store.$id}`)
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        const { token, ...stateToRestore } = parsed
        store.$patch(stateToRestore)
      } catch (e) {
        console.warn(`恢复 store ${store.$id} 状态失败:`, e)
      }
    }

    // 订阅状态变化并保存
    store.$subscribe((mutation, state) => {
      try {
        const { token, ...stateToPersist } = state
        sessionStorage.setItem(`pinia-${store.$id}`, JSON.stringify(stateToPersist))
      } catch (e) {
        console.warn(`保存 store ${store.$id} 状态失败:`, e)
      }
    })
  }
}
