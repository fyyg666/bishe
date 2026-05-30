import { formatDate, getPaginationParams } from '@/utils/format'

describe('formatDate', () => {
  it('should format date with YYYY-MM-DD', () => {
    const date = new Date('2026-05-10 14:30:00')
    expect(formatDate(date, 'YYYY-MM-DD')).toBe('2026-05-10')
  })

  it('should format date with HH:mm:ss', () => {
    const date = new Date('2026-05-10 14:30:00')
    expect(formatDate(date, 'HH:mm:ss')).toBe('14:30:00')
  })

  it('should handle string date input', () => {
    expect(formatDate('2026-05-10')).toBe('2026-05-10')
  })

  it('should use default format YYYY-MM-DD', () => {
    expect(formatDate('2026-05-10')).toBe('2026-05-10')
  })

  it('should return empty for invalid date', () => {
    expect(formatDate('invalid-date')).toBe('')
  })
})

describe('getPaginationParams', () => {
  it('should return defaults when no args', () => {
    expect(getPaginationParams()).toEqual({ current: 1, size: 10 })
  })

  it('should return custom pagination', () => {
    expect(getPaginationParams(3, 20)).toEqual({ current: 3, size: 20 })
  })
})
