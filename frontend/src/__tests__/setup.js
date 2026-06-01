// Vitest 测试环境初始化
import { vi } from 'vitest'

function createStorageMock() {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] ?? null),
    setItem: vi.fn((key, value) => { store[key] = String(value) }),
    removeItem: vi.fn((key) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
    get length() { return Object.keys(store).length },
    key: vi.fn((index) => Object.keys(store)[index] ?? null),
  }
}

if (typeof window !== 'undefined') {
  const localStorageMock = createStorageMock()
  Object.defineProperty(window, 'localStorage', { value: localStorageMock })

  const sessionStorageMock = createStorageMock()
  Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock })

  // 模拟 matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })

  // 模拟 IntersectionObserver
  class MockIntersectionObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  Object.defineProperty(window, 'IntersectionObserver', {
    writable: true,
    value: MockIntersectionObserver,
  })

  // 模拟 ResizeObserver
  class MockResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    value: MockResizeObserver,
  })
}
