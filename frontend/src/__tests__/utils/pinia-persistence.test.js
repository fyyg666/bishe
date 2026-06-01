describe('pinia-persistence', () => {
  let piniaPersistedstatePlugin, PERSIST_STORES

  beforeEach(async () => {
    vi.clearAllMocks()
    sessionStorage.clear()
    await vi.resetModules()

    const mod = await import('@/utils/pinia-persistence')
    piniaPersistedstatePlugin = mod.piniaPersistedstatePlugin
    PERSIST_STORES = mod.PERSIST_STORES
  })

  function createMockStore(id, initialState = {}) {
    const state = { ...initialState }
    const subscribers = []

    const store = {
      $id: id,
      $patch: vi.fn((patchState) => {
        Object.assign(state, patchState)
      }),
      $subscribe: vi.fn((callback) => {
        subscribers.push(callback)
        return vi.fn()
      }),
      $state: state,
    }

    return { store, subscribers }
  }

  describe('PERSIST_STORES config', () => {
    it('should contain user store id', () => {
      expect(PERSIST_STORES).toContain('user')
    })

    it('should be an array', () => {
      expect(Array.isArray(PERSIST_STORES)).toBe(true)
    })

    it('should not be empty', () => {
      expect(PERSIST_STORES.length).toBeGreaterThan(0)
    })
  })

  describe('store restoration', () => {
    it('should restore state from sessionStorage on init', () => {
      const storedState = { userInfo: { id: 1, username: 'admin', role: 'ADMIN' } }
      sessionStorage.setItem('pinia-user', JSON.stringify(storedState))

      const { store } = createMockStore('user')
      piniaPersistedstatePlugin({ store })

      expect(store.$patch).toHaveBeenCalledWith(storedState)
    })

    it('should exclude token from restored state', () => {
      const storedState = { token: 'should-be-removed', userInfo: { id: 1 } }
      sessionStorage.setItem('pinia-user', JSON.stringify(storedState))

      const { store } = createMockStore('user')
      piniaPersistedstatePlugin({ store })

      const patchArg = store.$patch.mock.calls[0][0]
      expect(patchArg).not.toHaveProperty('token')
      expect(patchArg).toHaveProperty('userInfo')
    })

    it('should not restore when no stored data exists', () => {
      const { store } = createMockStore('user')
      piniaPersistedstatePlugin({ store })

      expect(store.$patch).not.toHaveBeenCalled()
    })

    it('should handle corrupt JSON gracefully', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      sessionStorage.setItem('pinia-user', '{invalid json}')

      const { store } = createMockStore('user')
      expect(() => piniaPersistedstatePlugin({ store })).not.toThrow()

      consoleWarnSpy.mockRestore()
    })
  })

  describe('state persistence subscription', () => {
    it('should subscribe to store changes', () => {
      const { store } = createMockStore('user', { userInfo: null })

      piniaPersistedstatePlugin({ store })

      expect(store.$subscribe).toHaveBeenCalled()
    })

    it('should save state to sessionStorage on change', () => {
      const { store, subscribers } = createMockStore('user', {
        userInfo: { id: 1, username: 'u', role: 'READER' },
      })

      piniaPersistedstatePlugin({ store })
      subscribers[0](null, { userInfo: { id: 1, username: 'u', role: 'READER' } })

      const stored = sessionStorage.getItem('pinia-user')
      expect(stored).toBeTruthy()
      const parsed = JSON.parse(stored)
      expect(parsed).toHaveProperty('userInfo')
      expect(parsed).not.toHaveProperty('token')
    })

    it('should exclude token from persisted state', () => {
      const { store, subscribers } = createMockStore('user', {
        token: 'secret-token',
        userInfo: { id: 1 },
      })

      piniaPersistedstatePlugin({ store })
      subscribers[0](null, { token: 'secret-token', userInfo: { id: 1 } })

      const stored = sessionStorage.getItem('pinia-user')
      const parsed = JSON.parse(stored)
      expect(parsed).not.toHaveProperty('token')
      expect(parsed).toHaveProperty('userInfo')
    })
  })

  describe('store filtering', () => {
    it('should not persist stores not in PERSIST_STORES', () => {
      const { store } = createMockStore('book')
      sessionStorage.setItem('pinia-book', JSON.stringify({ total: 5 }))

      piniaPersistedstatePlugin({ store })

      expect(store.$patch).not.toHaveBeenCalled()
      expect(store.$subscribe).not.toHaveBeenCalled()
    })

    it('should not restore state for unlisted store', () => {
      const storedState = { books: [] }
      sessionStorage.setItem('pinia-seat', JSON.stringify(storedState))

      const { store } = createMockStore('seat')
      piniaPersistedstatePlugin({ store })

      expect(store.$patch).not.toHaveBeenCalled()
    })
  })
})
