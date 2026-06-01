vi.mock('@/api/category', () => ({
  getCategories: vi.fn(),
}))

import { getCategories } from '@/api/category'

describe('useCategories', () => {
  let useCategories

  beforeEach(async () => {
    vi.clearAllMocks()
    const mod = await import('@/composables/useCategories')
    useCategories = mod.useCategories
  })

  it('should have default hardcoded categories on init', () => {
    const { categoryOptions } = useCategories()

    expect(categoryOptions.value.length).toBe(6)
    expect(categoryOptions.value[0]).toEqual({ label: '文学', value: '文学' })
    expect(categoryOptions.value[1]).toEqual({ label: '科技', value: '科技' })
    expect(categoryOptions.value[5]).toEqual({ label: '经济', value: '经济' })
  })

  it('should load categories from API and update options', async () => {
    const apiCategories = [
      { id: 1, name: '计算机' },
      { id: 2, name: '数学' },
    ]
    getCategories.mockResolvedValue({ data: apiCategories })

    const { categoryOptions, loadCategories } = useCategories()
    await loadCategories()

    expect(getCategories).toHaveBeenCalled()
    expect(categoryOptions.value).toEqual([
      { label: '计算机', value: 1 },
      { label: '数学', value: 2 },
    ])
  })

  it('should fallback to hardcoded categories if API items have no name or id', async () => {
    getCategories.mockResolvedValue({ data: [{ value: 'cat1' }, { value: 'cat2' }] })

    const { categoryOptions, loadCategories } = useCategories()
    await loadCategories()

    expect(categoryOptions.value.length).toBe(2)
    expect(categoryOptions.value[0].label.value).toBe('cat1')
    expect(categoryOptions.value[0].value.value).toBe('cat1')
  })

  it('should fallback to hardcoded categories on API error', async () => {
    getCategories.mockRejectedValue(new Error('Network error'))

    const { categoryOptions, loadCategories } = useCategories()
    await loadCategories()

    expect(categoryOptions.value.length).toBe(6)
    expect(categoryOptions.value[0]).toEqual({ label: '文学', value: '文学' })
  })

  it('should keep default categories when API returns empty array', async () => {
    getCategories.mockResolvedValue({ data: [] })

    const { categoryOptions, loadCategories } = useCategories()
    await loadCategories()

    expect(categoryOptions.value.length).toBe(6)
  })

  it('should keep default categories when API returns non-array', async () => {
    getCategories.mockResolvedValue({ data: null })

    const { categoryOptions, loadCategories } = useCategories()
    await loadCategories()

    expect(categoryOptions.value.length).toBe(6)
  })

  it('should expose loadCategories as a function', () => {
    const { loadCategories } = useCategories()

    expect(loadCategories).toBeTypeOf('function')
  })
})
