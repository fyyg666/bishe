import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  clearToken: vi.fn(),
}))

import { login as apiLogin, logout as apiLogout, getUserInfo } from '@/api/auth'
import { getToken, setToken as setAuthToken, clearToken } from '@/utils/auth'

describe('useUserStore', () => {
  let store

  beforeEach(async () => {
    vi.clearAllMocks()
    await vi.resetModules()
    setActivePinia(createPinia())
    const { useUserStore } = await import('@/stores/user')
    store = useUserStore()
  })

  describe('initial state', () => {
    it('token should be empty string', () => {
      expect(store.token).toBe('')
    })

    it('userInfo should be null', () => {
      expect(store.userInfo).toBeNull()
    })

    it('isLoggedIn should be false', () => {
      expect(store.isLoggedIn).toBe(false)
    })

    it('username should be empty string', () => {
      expect(store.username).toBe('')
    })

    it('fetchingUserInfo should be false', () => {
      expect(store.fetchingUserInfo).toBe(false)
    })
  })

  describe('computed properties', () => {
    it('isLoggedIn should be true when token is set', () => {
      store.token = 'test-token'
      expect(store.isLoggedIn).toBe(true)
    })

    it('isAdmin should return true for ADMIN role', () => {
      store.userInfo = { role: 'ADMIN', username: 'admin' }
      expect(store.isAdmin).toBe(true)
    })

    it('isAdmin should return true for LIBRARIAN role', () => {
      store.userInfo = { role: 'LIBRARIAN', username: 'librarian' }
      expect(store.isAdmin).toBe(true)
    })

    it('isAdmin should return false for READER role', () => {
      store.userInfo = { role: 'READER', username: 'reader' }
      expect(store.isAdmin).toBe(false)
    })

    it('isAdmin should return false when userInfo is null', () => {
      expect(store.isAdmin).toBe(false)
    })

    it('username should return username from userInfo', () => {
      store.userInfo = { username: 'testuser' }
      expect(store.username).toBe('testuser')
    })

    it('userRole should return role from userInfo', () => {
      store.userInfo = { role: 'ADMIN' }
      expect(store.userRole).toBe('ADMIN')
    })
  })

  describe('login action', () => {
    it('should call apiLogin with login form', async () => {
      const loginForm = { username: 'admin', password: 'admin123' }
      apiLogin.mockResolvedValue({
        accessToken: 'token-abc',
        refreshToken: 'refresh-abc',
        userInfo: { id: 1, username: 'admin', role: 'ADMIN' },
      })

      const res = await store.login(loginForm)

      expect(apiLogin).toHaveBeenCalledWith(loginForm)
      expect(store.token).toBe('token-abc')
      expect(setAuthToken).toHaveBeenCalledWith('token-abc', 'refresh-abc')
      expect(store.userInfo).toBeDefined()
    })

    it('should handle response with data wrapper', async () => {
      const loginForm = { username: 'reader', password: 'pass' }
      apiLogin.mockResolvedValue({
        data: {
          token: 'wrapped-token',
          refreshToken: 'wrapped-refresh',
          userInfo: { id: 2, username: 'reader', role: 'READER' },
        },
      })

      await store.login(loginForm)

      expect(store.token).toBe('wrapped-token')
    })

    it('should use userInfo from res when no nested userInfo', async () => {
      apiLogin.mockResolvedValue({
        token: 't',
        userInfo: null,
        data: { id: 3, username: 'u3', role: 'READER' },
      })

      await store.login({ username: 'u3', password: 'p' })

      expect(store.userInfo).toEqual({ id: 3, username: 'u3', role: 'READER' })
    })
  })

  describe('logout action', () => {
    it('should call apiLogout and clear state', async () => {
      store.token = 'some-token'
      store.userInfo = { username: 'test' }

      await store.logout()

      expect(apiLogout).toHaveBeenCalled()
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(clearToken).toHaveBeenCalled()
    })

    it('should clear state even if apiLogout fails', async () => {
      store.token = 'some-token'
      store.userInfo = { username: 'test' }
      apiLogout.mockRejectedValue(new Error('Network error'))

      await store.logout()

      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(clearToken).toHaveBeenCalled()
    })
  })

  describe('fetchUserInfo action', () => {
    it('should fetch and set userInfo', async () => {
      const userData = { id: 1, username: 'fetched', role: 'ADMIN' }
      getUserInfo.mockResolvedValue({ data: userData })

      store.token = 'valid-token'
      await store.fetchUserInfo()

      expect(getUserInfo).toHaveBeenCalled()
      expect(store.userInfo).toEqual(userData)
    })

    it('should handle flat response without data wrapper', async () => {
      const userData = { id: 2, username: 'flat', role: 'READER' }
      getUserInfo.mockResolvedValue(userData)

      store.token = 'valid-token'
      await store.fetchUserInfo()

      expect(store.userInfo).toEqual(userData)
    })

    it('should skip fetch when fetchingUserInfo is true', async () => {
      store.fetchingUserInfo = true
      await store.fetchUserInfo()

      expect(getUserInfo).not.toHaveBeenCalled()
    })

    it('should call logout when fetch fails and no existing userInfo', async () => {
      getUserInfo.mockRejectedValue(new Error('Auth error'))
      store.token = 'valid-token'

      await store.fetchUserInfo()

      expect(clearToken).toHaveBeenCalled()
    })

    it('should not call logout when fetch fails but userInfo exists', async () => {
      store.userInfo = { id: 1, username: 'existing' }
      store.token = 'valid-token'
      getUserInfo.mockRejectedValue(new Error('Network error'))

      const calledBefore = clearToken.mock.calls.length

      await store.fetchUserInfo()

      expect(getUserInfo).toHaveBeenCalled()
      expect(clearToken).toHaveBeenCalledTimes(calledBefore)
    })
  })

  describe('locking mechanism', () => {
    it('fetchingUserInfo should prevent concurrent fetches', async () => {
      getUserInfo.mockImplementation(() => new Promise((r) => setTimeout(r, 10)))

      store.token = 'valid-token'
      const p1 = store.fetchUserInfo()
      const p2 = store.fetchUserInfo()

      await Promise.all([p1, p2])

      expect(getUserInfo).toHaveBeenCalledTimes(1)
    })

    it('fetchingUserInfo should reset to false after fetch', async () => {
      getUserInfo.mockResolvedValue({ data: { id: 1, username: 'u', role: 'READER' } })
      store.token = 'valid-token'

      await store.fetchUserInfo()

      expect(store.fetchingUserInfo).toBe(false)
    })
  })

  describe('setToken / setNewToken', () => {
    it('should update token and call setAuthToken', () => {
      store.setToken('new-token-value')
      expect(store.token).toBe('new-token-value')
      expect(setAuthToken).toHaveBeenCalledWith('new-token-value')
    })
  })

  describe('initUser', () => {
    it('should fetch user info when token exists', async () => {
      store.token = 'existing-token'
      getUserInfo.mockResolvedValue({ data: { id: 1, username: 'u' } })

      store.initUser()

      await vi.waitFor(() => {
        expect(getUserInfo).toHaveBeenCalled()
      })
    })

    it('should not fetch user info when token is empty', () => {
      store.initUser()

      expect(getUserInfo).not.toHaveBeenCalled()
    })
  })
})
