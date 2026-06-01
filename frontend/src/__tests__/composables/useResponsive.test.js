import { ref, computed, onMounted, onUnmounted } from 'vue'

vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual,
    onMounted: vi.fn(),
    onUnmounted: vi.fn(),
  }
})

describe('useResponsive', () => {
  let useResponsive, onMountedCallbacks, onUnmountedCallbacks
  let originalInnerWidth

  beforeAll(() => {
    originalInnerWidth = window.innerWidth
  })

  afterAll(() => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value: originalInnerWidth,
    })
  })

  beforeEach(async () => {
    onMountedCallbacks = []
    onUnmountedCallbacks = []
    vi.clearAllMocks()

    onMounted.mockImplementation((cb) => {
      onMountedCallbacks.push(cb)
    })
    onUnmounted.mockImplementation((cb) => {
      onUnmountedCallbacks.push(cb)
    })

    const mod = await import('@/composables/useResponsive')
    useResponsive = mod.useResponsive
  })

  function setWindowWidth(value) {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value,
    })
  }

  it('should report desktop when width >= 992', () => {
    setWindowWidth(1200)
    const result = useResponsive()
    onMountedCallbacks[0]()

    expect(result.isDesktop.value).toBe(true)
    expect(result.isTablet.value).toBe(false)
    expect(result.isMobile.value).toBe(false)
  })

  it('should report tablet when width between 768 and 992', () => {
    setWindowWidth(800)
    const result = useResponsive()
    onMountedCallbacks[0]()

    expect(result.isTablet.value).toBe(true)
    expect(result.isDesktop.value).toBe(false)
    expect(result.isMobile.value).toBe(false)
  })

  it('should report mobile when width < 768', () => {
    setWindowWidth(375)
    const result = useResponsive()
    onMountedCallbacks[0]()

    expect(result.isMobile.value).toBe(true)
    expect(result.isTablet.value).toBe(false)
    expect(result.isDesktop.value).toBe(false)
  })

  it('should expose current width', () => {
    setWindowWidth(1024)
    const result = useResponsive()
    onMountedCallbacks[0]()

    expect(result.width.value).toBe(1024)
  })

  it('should register resize listener on mounted', () => {
    const addEventListenerSpy = vi.spyOn(window, 'addEventListener')

    useResponsive()
    onMountedCallbacks[0]()

    expect(addEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function))
  })

  it('should remove resize listener on unmounted', () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')

    const result = useResponsive()
    onMountedCallbacks[0]()

    const resizeHandler = window.addEventListener.mock.calls[0][1]
    onUnmountedCallbacks[0]()

    expect(removeEventListenerSpy).toHaveBeenCalledWith('resize', resizeHandler)
  })

  it('should handle resize event and update width', () => {
    setWindowWidth(1200)
    const result = useResponsive()
    onMountedCallbacks[0]()

    setWindowWidth(500)
    const resizeHandler = window.addEventListener.mock.calls[0][1]
    resizeHandler()

    expect(result.width.value).toBe(500)
    expect(result.isMobile.value).toBe(true)
  })
})
