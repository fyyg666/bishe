beforeEach(() => {
  // Clear jsdom cookies between tests
  document.cookie.split(';').forEach(c => {
    document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/')
  })
})

it('getToken - should return undefined when no token set', async () => {
  const auth = await import('@/utils/auth')
  expect(auth.getToken()).toBeUndefined()
})

it('setToken/getToken - should store and retrieve access token', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('test-access-token')
  expect(auth.getToken()).toBe('test-access-token')
})

it('removeToken - should clear access token', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('test-access-token')
  auth.removeToken()
  expect(auth.getToken()).toBeUndefined()
})

it('getRefreshToken/setRefreshToken - should manage refresh token', async () => {
  const auth = await import('@/utils/auth')
  expect(auth.getRefreshToken()).toBeUndefined()
  auth.setRefreshToken('test-refresh-token')
  expect(auth.getRefreshToken()).toBe('test-refresh-token')
})

it('clearAuthCookies - should clear all auth cookies', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('t')
  auth.setRefreshToken('r')
  auth.setCsrfToken('c')
  auth.clearAuthCookies()
  expect(auth.getToken()).toBeUndefined()
  expect(auth.getRefreshToken()).toBeUndefined()
  expect(auth.getCsrfToken()).toBeUndefined()
})

it('CSRF token functions should work', async () => {
  const auth = await import('@/utils/auth')
  auth.setCsrfToken('csrf-value')
  expect(auth.getCsrfToken()).toBe('csrf-value')
  auth.removeCsrfToken()
  expect(auth.getCsrfToken()).toBeUndefined()
})
