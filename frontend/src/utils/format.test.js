import { describe, it, expect } from 'vitest'
import { formatDate, formatDateTime, calculateDaysBetween } from './format'

describe('format.js - 日期格式化工具函数', () => {
  it('formatDate should format date correctly', () => {
    const date = new Date('2024-01-15')
    expect(formatDate(date)).toBe('2024-01-15')
  })

  it('formatDate should handle null input', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
  })

  it('formatDateTime should format datetime correctly', () => {
    const date = new Date('2024-01-15T14:30:00')
    const result = formatDateTime(date)
    expect(result).toContain('2024-01-15')
    expect(result).toContain('14:30')
  })

  it('formatDateTime should handle null input', () => {
    expect(formatDateTime(null)).toBe('')
  })

  it('calculateDaysBetween should calculate days correctly', () => {
    const start = new Date('2024-01-01')
    const end = new Date('2024-01-15')
    expect(calculateDaysBetween(start, end)).toBe(14)
  })

  it('calculateDaysBetween should return 0 for same date', () => {
    const date = new Date('2024-01-15')
    expect(calculateDaysBetween(date, date)).toBe(0)
  })

  it('calculateDaysBetween should handle invalid input', () => {
    expect(calculateDaysBetween(null, new Date())).toBe(0)
    expect(calculateDaysBetween(new Date(), null)).toBe(0)
  })
})
