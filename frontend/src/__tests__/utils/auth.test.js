beforeEach(async () => {
  sessionStorage.clear()
  await vi.resetModules()
})

it('getToken - should return null when no token set', async () => {
  const auth = await import('@/utils/auth')
  expect(auth.getToken()).toBeNull()
})

it('setToken/getToken - should store and retrieve access token', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('test-access-token')
  expect(auth.getToken()).toBe('test-access-token')
  expect(sessionStorage.getItem('access_token')).toBe('test-access-token')
})

it('setToken - should persist access token to sessionStorage', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('persisted-token', 'refresh-value')
  expect(sessionStorage.getItem('access_token')).toBe('persisted-token')
  expect(auth.getRefreshToken()).toBe('refresh-value')
})

it('setToken - should only update refresh token when provided', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('access1', 'refresh1')
  auth.setToken('access2')
  expect(auth.getToken()).toBe('access2')
  expect(auth.getRefreshToken()).toBe('refresh1')
})

it('clearToken - should clear all tokens', async () => {
  const auth = await import('@/utils/auth')
  auth.setToken('t', 'r')
  auth.clearToken()
  expect(auth.getToken()).toBeNull()
  expect(auth.getRefreshToken()).toBeNull()
  expect(sessionStorage.getItem('access_token')).toBeNull()
})

it('should restore token from sessionStorage on module load', async () => {
  sessionStorage.setItem('access_token', 'restored-token')
  const auth = await import('@/utils/auth')
  expect(auth.getToken()).toBe('restored-token')
})
