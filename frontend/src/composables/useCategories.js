import { ref } from 'vue'
import { getCategories } from '@/api/category'

const HARDCODED_CATEGORIES = ['文学', '科技', '历史', '艺术', '哲学', '经济']

export function useCategories() {
  const categoryOptions = ref(
    HARDCODED_CATEGORIES.map(c => ({ label: c, value: c }))
  )

  async function loadCategories() {
    try {
      const res = await getCategories()
      const list = res.data || []
      if (Array.isArray(list) && list.length > 0) {
        categoryOptions.value = list.map(c => ({
          label: c.name || c,
          value: c.id != null ? c.id : (c.name || c)
        }))
      }
    } catch {
      categoryOptions.value = HARDCODED_CATEGORIES.map(c => ({ label: c, value: c }))
    }
  }

  return { categoryOptions, loadCategories }
}
