import { computed } from 'vue'

export function usePasswordStrength(password) {
  const strength = computed(() => {
    if (!password.value) return 0
    let score = 0
    if (password.value.length >= 8) score++
    if (/[a-z]/.test(password.value)) score++
    if (/[A-Z]/.test(password.value)) score++
    if (/[0-9]/.test(password.value)) score++
    if (/[^a-zA-Z0-9]/.test(password.value)) score++
    return score
  })

  const strengthText = computed(() => {
    const texts = ['', '弱', '较弱', '中等', '较强', '强']
    return texts[strength.value] || ''
  })

  const strengthColor = computed(() => {
    const colors = ['', '#F56C6C', '#E6A23C', '#E6A23C', '#67C23A', '#67C23A']
    return colors[strength.value] || ''
  })

  return { strength, strengthText, strengthColor }
}
