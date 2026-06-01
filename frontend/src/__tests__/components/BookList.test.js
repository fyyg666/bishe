import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  useRoute: () => ({
    params: {},
    query: {},
  }),
}))

vi.mock('@/stores/book', () => ({
  useBookStore: () => ({
    books: [],
    total: 0,
    loading: false,
    fetchBooks: vi.fn().mockResolvedValue([]),
    removeBook: vi.fn().mockResolvedValue(),
  }),
}))

vi.mock('@/composables/useCategories', () => ({
  useCategories: () => ({
    categoryOptions: [
      { label: '文学', value: '文学' },
      { label: '科技', value: '科技' },
    ],
    loadCategories: vi.fn(),
  }),
}))

vi.mock('@/composables/useResponsive', () => ({
  useResponsive: () => ({
    isMobile: false,
    isTablet: false,
    isDesktop: true,
    width: 1200,
  }),
}))

vi.mock('@/utils/auth', () => ({
  getToken: () => 'mock-token',
}))

vi.mock('@/api/book', () => ({
  advancedSearchBooks: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  getCategoryFacet: vi.fn().mockResolvedValue({ data: [] }),
  getAuthorFacet: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/api/suggestion', () => ({
  createSuggestion: vi.fn().mockResolvedValue({}),
}))

vi.mock('@element-plus/icons-vue', () => {
  const iconStub = { template: '<span />' }
  return {
    Plus: iconStub,
    Download: iconStub,
    Upload: iconStub,
    UploadFilled: iconStub,
    Search: iconStub,
    ArrowDown: iconStub,
    View: iconStub,
    Edit: iconStub,
  }
})

const mockRow = { availableCount: 1, id: 1, title: 'Test', author: 'Author', isbn: '', category: '', publisher: '' }

const elStubs = {
  'el-table': { template: '<div><slot /></div>', props: ['data'] },
  'el-table-column': {
    props: ['prop', 'label', 'width', 'type', 'fixed', 'minWidth'],
    setup(_props, { slots }) {
      return () => {
        if (slots.default) {
          return slots.default({ row: mockRow, $index: 0 })
        }
        return null
      }
    },
  },
  'el-pagination': { template: '<div />', props: ['total', 'pageSize', 'currentPage'] },
  'el-button': { template: '<button><slot /></button>', props: ['type', 'disabled', 'link', 'icon'] },
  'el-input': { template: '<input />', props: ['modelValue', 'disabled'] },
  'el-form': { template: '<form><slot /></form>', props: ['model', 'inline', 'labelWidth'] },
  'el-form-item': { template: '<div><slot /></div>', props: ['prop', 'label'] },
  'el-card': { template: '<div><slot /></div>' },
  'el-tag': { template: '<span><slot /></span>', props: ['type', 'size'] },
  'el-empty': { template: '<div />' },
  'el-skeleton': { template: '<div />' },
  'el-breadcrumb': { template: '<div />' },
  'el-breadcrumb-item': { template: '<span />' },
  'el-menu': { template: '<div />' },
  'el-menu-item': { template: '<div />' },
  'el-sub-menu': { template: '<div />' },
  'el-icon': { template: '<i />' },
  'el-tooltip': { template: '<div />' },
  'el-date-picker': { template: '<input />' },
  'el-select': { template: '<select><slot /></select>', props: ['modelValue', 'clearable'] },
  'el-option': { template: '<option><slot /></option>', props: ['label', 'value'] },
  'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
  'el-upload': { template: '<div />' },
  'el-descriptions': { template: '<div><slot /></div>' },
  'el-descriptions-item': { template: '<div><slot /></div>' },
  'el-scrollbar': { template: '<div><slot /></div>' },
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  'router-link': { template: '<a><slot /></a>' },
}

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') },
}))

describe('BookList Component', () => {
  let wrapper

  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  it('should mount without errors', async () => {
    const { default: BookList } = await import('@/views/book/BookList.vue')

    expect(() => {
      wrapper = mount(BookList, {
        global: {
          stubs: elStubs,
        },
      })
    }).not.toThrow()

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.book-list').exists()).toBe(true)
  })

  it('should render search form elements', async () => {
    const { default: BookList } = await import('@/views/book/BookList.vue')

    wrapper = mount(BookList, {
      global: {
        stubs: elStubs,
      },
    })

    const formElements = wrapper.findAll('form')
    expect(formElements.length).toBeGreaterThan(0)

    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThan(0)
  })
})
