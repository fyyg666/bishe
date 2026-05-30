import axios from 'axios'

describe('request utils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should make GET request with axios', async () => {
    const mockData = { id: 1, name: 'test' }
    vi.spyOn(axios, 'get').mockResolvedValue({ data: { code: 0, data: mockData } })

    const res = await axios.get('/api/books/1')
    expect(res.data.code).toBe(0)
    expect(res.data.data.name).toBe('test')
  })

  it('should make POST request with body', async () => {
    const body = { username: 'admin', password: 'admin123' }
    vi.spyOn(axios, 'post').mockResolvedValue({ data: { code: 0, data: { token: 'test-token' } } })

    const res = await axios.post('/api/auth/login', body)
    expect(res.data.data.token).toBe('test-token')
  })

  it('should handle error response', async () => {
    vi.spyOn(axios, 'get').mockRejectedValue(new Error('Network Error'))

    await expect(axios.get('/api/books')).rejects.toThrow('Network Error')
  })
})
