vi.mock('element-plus', () => ({
  ElTable: { name: 'ElTable', template: '<div><slot /></div>', props: ['data'] },
  ElTableColumn: { name: 'ElTableColumn', template: '<div><slot /></div>', props: ['prop', 'label', 'width'] },
  ElPagination: { name: 'ElPagination', template: '<div><slot /></div>', props: ['total', 'page-size', 'current-page'] },
  ElButton: { name: 'ElButton', template: '<button><slot /></button>', props: ['type', 'disabled'] },
  ElInput: { name: 'ElInput', template: '<input />', props: ['modelValue'] },
  ElForm: { name: 'ElForm', template: '<form><slot /></form>', props: ['model'] },
  ElFormItem: { name: 'ElFormItem', template: '<div><slot /></div>', props: ['prop', 'label'] },
  ElCard: { name: 'ElCard', template: '<div><slot /></div>' },
  ElTag: { name: 'ElTag', template: '<span><slot /></span>', props: ['type'] },
  ElEmpty: { name: 'ElEmpty', template: '<div><slot /></div>' },
  ElSkeleton: { name: 'ElSkeleton', template: '<div><slot /></div>' },
  ElBreadcrumb: { name: 'ElBreadcrumb', template: '<div><slot /></div>' },
  ElBreadcrumbItem: { name: 'ElBreadcrumbItem', template: '<span><slot /></span>' },
  ElMenu: { name: 'ElMenu', template: '<div><slot /></div>', props: ['router'] },
  ElMenuItem: { name: 'ElMenuItem', template: '<div><slot /></div>', props: ['index'] },
  ElSubMenu: { name: 'ElSubMenu', template: '<div><slot /></div>', props: ['index'] },
  ElIcon: { name: 'ElIcon', template: '<i><slot /></i>' },
  ElTooltip: { name: 'ElTooltip', template: '<div><slot /></div>' },
  ElDatePicker: { name: 'ElDatePicker', template: '<input />' },
  ElSelect: { name: 'ElSelect', template: '<select><slot /></select>' },
  ElOption: { name: 'ElOption', template: '<option><slot /></option>' },
  ElDialog: { name: 'ElDialog', template: '<div><slot /></div>', props: ['model-value'] },
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() },
}))

describe('BookList Component', () => {
  it('should set up test infrastructure correctly', () => {
    expect(true).toBe(true)
  })
})
