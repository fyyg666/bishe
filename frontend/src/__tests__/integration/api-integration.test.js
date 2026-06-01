// @vitest-environment node

import { describe, it, expect, beforeAll } from 'vitest'

const BASE = 'http://localhost:8080/api/v1'

async function request(path, options = {}) {
  const url = path.startsWith('http') ? path : `${BASE}${path}`
  const { method = 'GET', body, headers = {}, ...rest } = options
  const fetchOptions = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    ...rest,
  }
  if (body) {
    fetchOptions.body = JSON.stringify(body)
  }
  const response = await fetch(url, fetchOptions)
  const text = await response.text()
  let data
  try {
    data = JSON.parse(text)
    if (typeof data === 'string') {
      data = JSON.parse(data)
    }
  } catch {
    data = text
  }
  return { status: response.status, headers: response.headers, data }
}

function getAuth(accessToken) {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
}

let adminToken
let adminInfo
let readerToken
let readerInfo

describe('API Integration Tests', () => {
  // ================================================================
  // Module 1: Authentication (认证模块)
  // ================================================================
  describe('Authentication (认证模块)', () => {
    it('should login as admin and return accessToken/userInfo', async () => {
      const { status, data } = await request('/auth/login', {
        method: 'POST',
        body: { username: 'admin', password: 'any' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('登录成功')
      expect(data.accessToken).toBeDefined()
      expect(data.accessToken).toContain('mock-jwt-token-')
      expect(data.refreshToken).toBe('mock-refresh-token')
      expect(data.userInfo).toBeDefined()
      expect(data.userInfo.username).toBe('admin')
      expect(data.userInfo.role).toBe('ADMIN')

      adminToken = data.accessToken
      adminInfo = data.userInfo
    })

    it('should login as reader and return accessToken/userInfo', async () => {
      const { status, data } = await request('/auth/login', {
        method: 'POST',
        body: { username: 'reader01', password: 'any' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.accessToken).toBeDefined()
      expect(data.userInfo.role).toBe('READER')
      expect(data.userInfo.username).toBe('reader01')

      readerToken = data.accessToken
      readerInfo = data.userInfo
    })

    it('should return 401 for invalid login credentials', async () => {
      const { status, data } = await request('/auth/login', {
        method: 'POST',
        body: { username: 'nonexistent', password: 'wrong' },
      })

      expect(status).toBe(401)
      expect(data.code).toBe(401)
      expect(data.message).toBe('用户名或密码错误')
    })

    it('should register a new user', async () => {
      const { status, data } = await request('/auth/register', {
        method: 'POST',
        body: {
          username: 'newreader',
          realName: '新读者',
          email: 'new@example.com',
          phone: '13800000099',
          password: 'pass123',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('注册成功')
      expect(data.data.username).toBe('newreader')
      expect(data.data.role).toBe('READER')
      expect(data.data.credit).toBe(100)
      expect(data.data.status).toBe('ACTIVE')
    })

    it('should get user info with valid token', async () => {
      const { status, data } = await request('/auth/info', {
        headers: getAuth(readerToken),
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBeDefined()
      expect(data.data.username).toBe('admin')
    })

    it('should return 401 when accessing /auth/info without token', async () => {
      const { status, data } = await request('/auth/info')

      expect(status).toBe(401)
      expect(data.code).toBe(401)
      expect(data.message).toBe('未登录')
    })

    it('should logout successfully', async () => {
      const { status, data } = await request('/auth/logout', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('退出成功')
    })
  })

  // ================================================================
  // Module 2: Books (图书模块)
  // ================================================================
  describe('Books (图书模块)', () => {
    it('should get book list with pagination', async () => {
      const { status, data } = await request('/books?current=1&size=5')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.records.length).toBeLessThanOrEqual(5)
      expect(data.data.total).toBeGreaterThan(0)
      expect(data.data.current).toBe(1)
      expect(data.data.size).toBe(5)
      expect(data.data.pages).toBeGreaterThanOrEqual(1)
    })

    it('should search books by keyword', async () => {
      const { status, data } = await request('/books?keyword=计算机')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every(
        (b) =>
          b.title.includes('计算机') ||
          b.author.includes('计算机') ||
          b.isbn.includes('计算机')
      )).toBe(true)
    })

    it('should filter books by categoryId', async () => {
      const { status, data } = await request('/books?categoryId=1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every((b) => b.categoryId === 1)).toBe(true)
    })

    it('should get book detail by id', async () => {
      const { status, data } = await request('/books/2')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.id).toBe(2)
      expect(data.data.title).toBe('数据结构与算法分析')
      expect(data.data.author).toBe('Mark Allen Weiss')
      expect(data.data.isbn).toBe('978-7-111-52839-4')
    })

    it('should return null for non-existent book id', async () => {
      const { status, data } = await request('/books/99999')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBeNull()
    })

    it('should create a new book', async () => {
      const { status, data } = await request('/books', {
        method: 'POST',
        body: {
          title: '新书标题',
          author: '新作者',
          isbn: '978-7-999-00001-0',
          publisher: '测试出版社',
          publishDate: '2024-01-01',
          categoryId: 1,
          categoryName: '计算机科学',
          totalCopies: 3,
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.title).toBe('新书标题')
      expect(data.data.borrowCount).toBe(0)
      expect(data.data.status).toBe('AVAILABLE')
    })

    it('should update an existing book', async () => {
      const { status, data } = await request('/books/1', {
        method: 'PUT',
        body: { title: '更新后的书名', totalCopies: 10 },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('更新成功')
      expect(data.data.title).toBe('更新后的书名')
      expect(data.data.totalCopies).toBe(10)
    })

    it('should return 404 when updating non-existent book', async () => {
      const { status, data } = await request('/books/99999', {
        method: 'PUT',
        body: { title: 'test' },
      })

      expect(status).toBe(404)
      expect(data.code).toBe(404)
      expect(data.message).toBe('图书不存在')
    })

    it('should delete a book', async () => {
      const { status, data } = await request('/books/1', {
        method: 'DELETE',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('删除成功')
    })

    it('should get hot books', async () => {
      const { status, data } = await request('/books/hot')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      if (data.data.length > 0) {
        expect(data.data[0].borrowCount).toBeGreaterThan(50)
      }
    })

    it('should get new books sorted by publishDate', async () => {
      const { status, data } = await request('/books/new')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      if (data.data.length > 1) {
        const dates = data.data.map((b) => new Date(b.publishDate).getTime())
        for (let i = 1; i < dates.length; i++) {
          expect(dates[i - 1]).toBeGreaterThanOrEqual(dates[i])
        }
      }
    })

    it('should check ISBN existence', async () => {
      const { status, data } = await request('/books/check-isbn?isbn=978-7-111-54493-7')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBe(true)
    })

    it('should check non-existent ISBN', async () => {
      const { status, data } = await request('/books/check-isbn?isbn=999-9-999-99999-9')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBe(false)
    })

    it('should perform advanced search', async () => {
      const { status, data } = await request('/books/advanced-search?title=数据结构&current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.length).toBeGreaterThan(0)
      expect(data.data.records.every((b) => b.title.includes('数据结构'))).toBe(true)
    })

    it('should perform advanced search by author', async () => {
      const { status, data } = await request('/books/advanced-search?author=余华')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every((b) => b.author.includes('余华'))).toBe(true)
    })

    it('should get category facets', async () => {
      const { status, data } = await request('/books/facets/categories')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data.length).toBe(8)
      expect(data.data[0]).toHaveProperty('id')
      expect(data.data[0]).toHaveProperty('name')
      expect(data.data[0]).toHaveProperty('count')
    })

    it('should get author facets', async () => {
      const { status, data } = await request('/books/facets/authors')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
    })

    it('should do ISBN lookup', async () => {
      const { status, data } = await request('/books/isbn-lookup?isbn=978-7-111-54493-7')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.isbn).toBe('978-7-111-54493-7')
      expect(data.data.title).toBe('Mock图书')
      expect(data.data.author).toBe('Mock作者')
    })

    it('should export books as xlsx', async () => {
      const response = await fetch(`${BASE}/books/export`)

      expect(response.status).toBe(200)
      expect(response.headers.get('content-type')).toContain('spreadsheet')
    })

    it('should import books', async () => {
      const { status, data } = await request('/books/import', {
        method: 'POST',
        body: { books: [] },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.success).toBe(10)
      expect(data.data.total).toBe(10)
    })
  })

  // ================================================================
  // Module 3: Categories (分类模块)
  // ================================================================
  describe('Categories (分类模块)', () => {
    it('should get all categories', async () => {
      const { status, data } = await request('/categories')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data.length).toBe(8)
      expect(data.data[0]).toHaveProperty('name')
      expect(data.data[0]).toHaveProperty('code')
      expect(data.data[0]).toHaveProperty('bookCount')
    })
  })

  // ================================================================
  // Module 4: Readers (读者模块)
  // ================================================================
  describe('Readers (读者模块)', () => {
    let createdReaderId

    it('should get reader list with pagination', async () => {
      const { status, data } = await request('/readers?current=1&size=5')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
      expect(data.data.records.every((r) => r.role === 'READER')).toBe(true)
    })

    it('should search readers by keyword', async () => {
      const { status, data } = await request('/readers?keyword=李明')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.length).toBeGreaterThan(0)
      expect(data.data.records[0].realName).toContain('李明')
    })

    it('should get reader detail', async () => {
      const { status, data } = await request('/readers/3')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.id).toBe(3)
      expect(data.data.username).toBe('reader01')
      expect(data.data.realName).toContain('李明')
    })

    it('should return null for non-existent reader', async () => {
      const { status, data } = await request('/readers/99999')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBeNull()
    })

    it('should create a new reader', async () => {
      const { status, data } = await request('/readers', {
        method: 'POST',
        body: {
          username: 'reader_new',
          realName: '测试读者',
          email: 'test@example.com',
          phone: '13800000100',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.username).toBe('reader_new')
      expect(data.data.role).toBe('READER')
      expect(data.data.credit).toBe(100)
      expect(data.data.status).toBe('ACTIVE')

      createdReaderId = data.data.id
    })

    it('should update an existing reader', async () => {
      const { status, data } = await request('/readers/3', {
        method: 'PUT',
        body: { realName: '李明（已更新）' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('更新成功')
      expect(data.data.realName).toBe('李明（已更新）')
    })

    it('should return 404 when updating non-existent reader', async () => {
      const { status, data } = await request('/readers/99999', {
        method: 'PUT',
        body: { realName: 'test' },
      })

      expect(status).toBe(404)
      expect(data.code).toBe(404)
      expect(data.message).toBe('读者不存在')
    })

    it('should delete a reader', async () => {
      const { status, data } = await request('/readers/5', {
        method: 'DELETE',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('删除成功')
    })
  })

  // ================================================================
  // Module 5: Borrows (借阅模块)
  // ================================================================
  describe('Borrows (借阅模块)', () => {
    it('should get borrow list with pagination', async () => {
      const { status, data } = await request('/borrows?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
      expect(data.data.records[0]).toHaveProperty('bookTitle')
      expect(data.data.records[0]).toHaveProperty('readerName')
      expect(data.data.records[0]).toHaveProperty('status')
    })

    it('should filter borrows by status', async () => {
      const { status, data } = await request('/borrows?status=BORROWED')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every((b) => b.status === 'BORROWED')).toBe(true)
    })

    it('should filter borrows by keyword', async () => {
      const { status, data } = await request('/borrows?keyword=计算机')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      if (data.data.records.length > 0) {
        expect(
          data.data.records.every(
            (b) =>
              b.bookTitle.includes('计算机') || b.readerName.includes('计算机')
          )
        ).toBe(true)
      }
    })

    it('should get a borrow record detail', async () => {
      const { status, data } = await request('/borrows/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.id).toBe(1)
      expect(data.data.bookTitle).toBe('深入理解计算机系统')
      expect(data.data.readerName).toBe('李明')
    })

    it('should return null for non-existent borrow record', async () => {
      const { status, data } = await request('/borrows/99999')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBeNull()
    })

    it('should create a new borrow record', async () => {
      const { status, data } = await request('/borrows', {
        method: 'POST',
        body: {
          bookId: 9,
          bookTitle: '月亮与六便士',
          readerId: 6,
          readerName: '陈静',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('借阅成功')
      expect(data.data.status).toBe('BORROWED')
      expect(data.data.borrowDate).toBeDefined()
      expect(data.data.dueDate).toBeDefined()
    })

    it('should return a book', async () => {
      const { status, data } = await request('/borrows/1/return', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('还书成功')
    })

    it('should renew a book', async () => {
      const { status, data } = await request('/borrows/2/renew', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('续借成功')
    })
  })

  // ================================================================
  // Module 6: Borrow Rules (借阅规则模块)
  // ================================================================
  describe('Borrow Rules (借阅规则模块)', () => {
    it('should get all borrow rules', async () => {
      const { status, data } = await request('/borrow-rules')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data.length).toBeGreaterThanOrEqual(3)
      expect(data.data[0]).toHaveProperty('maxBooks')
      expect(data.data[0]).toHaveProperty('maxDays')
      expect(data.data[0]).toHaveProperty('maxRenew')
      expect(data.data[0]).toHaveProperty('finePerDay')
    })

    it('should get a specific borrow rule', async () => {
      const { status, data } = await request('/borrow-rules/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.role).toBe('READER')
    })

    it('should create a borrow rule', async () => {
      const { status, data } = await request('/borrow-rules', {
        method: 'POST',
        body: { role: 'GUEST', maxBooks: 2, maxDays: 14, maxRenew: 1, finePerDay: 1.0 },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
    })

    it('should update a borrow rule', async () => {
      const { status, data } = await request('/borrow-rules/1', {
        method: 'PUT',
        body: { maxBooks: 6 },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('更新成功')
    })

    it('should delete a borrow rule', async () => {
      const { status, data } = await request('/borrow-rules/1', {
        method: 'DELETE',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('删除成功')
    })
  })

  // ================================================================
  // Module 7: Seats (座位模块)
  // ================================================================
  describe('Seats (座位模块)', () => {
    let reservationId

    it('should get seat map with pagination', async () => {
      const { status, data } = await request('/seats?current=1&size=20')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBe(40)
    })

    it('should filter seats by area', async () => {
      const { status, data } = await request('/seats?area=A区-阅览区')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every((s) => s.area === 'A区-阅览区')).toBe(true)
    })

    it('should get seat detail', async () => {
      const { status, data } = await request('/seats/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.id).toBe(1)
      expect(data.data.area).toBe('A区-阅览区')
      expect(data.data.status).toBe('AVAILABLE')
    })

    it('should return null for non-existent seat', async () => {
      const { status, data } = await request('/seats/99999')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBeNull()
    })

    it('should reserve a seat', async () => {
      const { status, data } = await request('/seats/reserve', {
        method: 'POST',
        body: {
          seatId: 10,
          userId: 3,
          reservationDate: '2024-06-20',
          startTime: '09:00',
          endTime: '12:00',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('预约成功')
      expect(data.data.status).toBe('RESERVED')
      expect(data.data.id).toBeGreaterThan(0)

      reservationId = data.data.id
    })

    it('should check in to a seat reservation', async () => {
      const { status, data } = await request('/seats/checkin/31', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('签到成功')
    })

    it('should check out from a seat', async () => {
      const { status, data } = await request('/seats/checkout/31', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('签退成功')
    })

    it('should check seat availability', async () => {
      const { status, data } = await request('/seats/check-availability?seatId=1&date=2024-06-20&startTime=09:00&endTime=12:00')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.available).toBe(true)
    })

    it('should get my reservations', async () => {
      const { status, data } = await request('/seats/my?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThanOrEqual(0)
    })

    it('should get reading rooms list', async () => {
      const { status, data } = await request('/seats/reading-rooms')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data).toContain('A区-阅览区')
      expect(data.data).toContain('B区-自习区')
      expect(data.data).toContain('C区-电子阅览区')
      expect(data.data).toContain('D区-研讨区')
    })

    it('should cancel a seat reservation', async () => {
      const { status, data } = await request('/seats/cancel/31', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('取消成功')
    })
  })

  // ================================================================
  // Module 8: Announcements (公告模块)
  // ================================================================
  describe('Announcements (公告模块)', () => {
    it('should get announcement list with pagination', async () => {
      const { status, data } = await request('/announcements?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
      expect(data.data.records[0]).toHaveProperty('title')
      expect(data.data.records[0]).toHaveProperty('type')
      expect(data.data.records[0]).toHaveProperty('status')
    })

    it('should get latest announcements', async () => {
      const { status, data } = await request('/announcements/latest?limit=2')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data.length).toBeLessThanOrEqual(2)
    })

    it('should get announcement detail', async () => {
      const { status, data } = await request('/announcements/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.id).toBe(1)
      expect(data.data.title).toBe('图书馆暑假开放时间调整通知')
    })

    it('should create an announcement', async () => {
      const { status, data } = await request('/announcements', {
        method: 'POST',
        body: {
          title: '测试公告',
          content: '这是一条测试公告',
          type: 'NOTICE',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.title).toBe('测试公告')
      expect(data.data.status).toBe('DRAFT')
      expect(data.data.publisher).toBe('张馆员')
    })

    it('should update an announcement', async () => {
      const { status, data } = await request('/announcements/1', {
        method: 'PUT',
        body: { title: '更新的公告标题' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('更新成功')
    })

    it('should publish an announcement', async () => {
      const { status, data } = await request('/announcements/1/publish', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('发布成功')
    })

    it('should delete an announcement', async () => {
      const { status, data } = await request('/announcements/1', {
        method: 'DELETE',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('删除成功')
    })
  })

  // ================================================================
  // Module 9: Notifications (通知模块)
  // ================================================================
  describe('Notifications (通知模块)', () => {
    it('should get notification list with pagination', async () => {
      const { status, data } = await request('/notifications?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
      expect(data.data.records[0]).toHaveProperty('title')
      expect(data.data.records[0]).toHaveProperty('type')
      expect(data.data.records[0]).toHaveProperty('status')
    })

    it('should get unread notification count', async () => {
      const { status, data } = await request('/notifications/unread-count')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(typeof data.data).toBe('number')
      expect(data.data).toBeGreaterThanOrEqual(0)
    })

    it('should mark a specific notification as read', async () => {
      const { status, data } = await request('/notifications/1/read', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('已读')
    })

    it('should mark all notifications as read', async () => {
      const { status, data } = await request('/notifications/read-all', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('全部已读')
    })

    it('should have zero unread after read-all', async () => {
      const { status, data } = await request('/notifications/unread-count')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toBe(0)
    })
  })

  // ================================================================
  // Module 10: Statistics (统计模块)
  // ================================================================
  describe('Statistics (统计模块)', () => {
    it('should get statistics overview', async () => {
      const { status, data } = await request('/statistics/overview')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('totalBooks')
      expect(data.data).toHaveProperty('totalReaders')
      expect(data.data).toHaveProperty('totalBorrows')
      expect(data.data).toHaveProperty('borrowsToday')
      expect(data.data).toHaveProperty('borrowsThisMonth')
      expect(data.data).toHaveProperty('overdueCount')
      expect(data.data).toHaveProperty('popularBooks')
      expect(data.data).toHaveProperty('categoryDistribution')
      expect(data.data).toHaveProperty('monthlyBorrows')
    })

    it('should get basic statistics', async () => {
      const { status, data } = await request('/statistics')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.totalBooks).toBeGreaterThan(0)
    })

    it('should get monthly borrow statistics', async () => {
      const { status, data } = await request('/statistics/borrows/monthly')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data.length).toBe(6)
      expect(data.data[0]).toHaveProperty('month')
      expect(data.data[0]).toHaveProperty('count')
    })

    it('should get category distribution statistics', async () => {
      const { status, data } = await request('/statistics/categories')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data[0]).toHaveProperty('name')
      expect(data.data[0]).toHaveProperty('value')
    })

    it('should get popular books statistics', async () => {
      const { status, data } = await request('/statistics/popular-books')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      if (data.data.length > 0) {
        expect(data.data[0]).toHaveProperty('title')
        expect(data.data[0]).toHaveProperty('borrowCount')
      }
    })
  })

  // ================================================================
  // Module 11: Credits (信用模块)
  // ================================================================
  describe('Credits (信用模块)', () => {
    it('should get credit list', async () => {
      const { status, data } = await request('/credits')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(Array.isArray(data.data)).toBe(true)
      expect(data.data[0]).toHaveProperty('score')
      expect(data.data[0]).toHaveProperty('level')
    })

    it('should get credit logs', async () => {
      const { status, data } = await request('/credits/logs?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.records[0]).toHaveProperty('change')
      expect(data.data.records[0]).toHaveProperty('reason')
      expect(data.data.records[0]).toHaveProperty('balance')
    })
  })

  // ================================================================
  // Module 12: Volunteers (志愿者模块)
  // ================================================================
  describe('Volunteers (志愿者模块)', () => {
    it('should get volunteer list', async () => {
      const { status, data } = await request('/volunteers?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should get my volunteer records', async () => {
      const { status, data } = await request('/volunteers/my')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should get volunteer stats', async () => {
      const { status, data } = await request('/volunteers/stats')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('totalHours')
      expect(data.data).toHaveProperty('totalServices')
      expect(data.data).toHaveProperty('currentMonthHours')
    })

    it('should apply for volunteer service', async () => {
      const { status, data } = await request('/volunteers', {
        method: 'POST',
        body: {
          userId: 3,
          userName: '李明',
          activityName: '测试志愿活动',
          description: '帮助整理图书馆',
          date: '2024-07-01',
          startTime: '10:00',
          endTime: '12:00',
          hours: 2,
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('申请成功')
      expect(data.data.status).toBe('PENDING')
    })

    it('should review a volunteer application', async () => {
      const { status, data } = await request('/volunteers/1/review', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('审核完成')
    })

    it('should get pending volunteers', async () => {
      const { status, data } = await request('/volunteers/pending')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
    })

    it('should cancel a volunteer activity', async () => {
      const { status, data } = await request('/volunteers/1/cancel', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('取消成功')
    })
  })

  // ================================================================
  // Module 13: Compensations (赔偿模块)
  // ================================================================
  describe('Compensations (赔偿模块)', () => {
    it('should get compensation list', async () => {
      const { status, data } = await request('/compensations?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should filter compensations by status', async () => {
      const { status, data } = await request('/compensations?status=PENDING')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.every((c) => c.status === 'PENDING')).toBe(true)
    })

    it('should create a compensation record', async () => {
      const { status, data } = await request('/compensations', {
        method: 'POST',
        body: {
          bookId: 3,
          bookTitle: '百年孤独',
          readerId: 5,
          readerName: '赵强',
          type: 'LOST',
          amount: 59.0,
          reason: '遗失',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.status).toBe('PENDING')
    })

    it('should approve a compensation', async () => {
      const { status, data } = await request('/compensations/1/approve', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('审批成功')
    })
  })

  // ================================================================
  // Module 14: Suggestions (荐购模块)
  // ================================================================
  describe('Suggestions (荐购模块)', () => {
    it('should get suggestion list', async () => {
      const { status, data } = await request('/suggestions?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should create a purchase suggestion', async () => {
      const { status, data } = await request('/suggestions', {
        method: 'POST',
        body: {
          bookTitle: '推荐图书',
          author: '推荐作者',
          reason: '这是一本好书',
          userId: 3,
          userName: '李明',
        },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.status).toBe('PENDING')
    })

    it('should approve a suggestion', async () => {
      const { status, data } = await request('/suggestions/1/approve', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('审批成功')
    })
  })

  // ================================================================
  // Module 15: Unified Search (统一搜索)
  // ================================================================
  describe('Unified Search (统一搜索)', () => {
    it('should search across all resources', async () => {
      const { status, data } = await request('/search?keyword=计算机&current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      if (data.data.records.length > 0) {
        expect(data.data.records[0]).toHaveProperty('type')
        expect(data.data.records[0]).toHaveProperty('score')
      }
    })

    it('should return empty results for no-match keyword', async () => {
      const { status, data } = await request('/search?keyword=xyznonexistent&current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records.length).toBe(0)
    })
  })

  // ================================================================
  // Module 16: Digital Resources (数字资源模块)
  // ================================================================
  describe('Digital Resources (数字资源模块)', () => {
    it('should get digital resource list', async () => {
      const { status, data } = await request('/digital-resources')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should get digital resource detail', async () => {
      const { status, data } = await request('/digital-resources/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('title')
      expect(data.data).toHaveProperty('type')
    })
  })

  // ================================================================
  // Module 17: Profile (个人中心)
  // ================================================================
  describe('Profile (个人中心)', () => {
    it('should get user profile', async () => {
      const { status, data } = await request('/profile')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('username')
      expect(data.data).toHaveProperty('realName')
      expect(data.data).toHaveProperty('email')
    })
  })

  // ================================================================
  // Module 18: Captcha (验证码模块)
  // ================================================================
  describe('Captcha (验证码模块)', () => {
    it('should get captcha', async () => {
      const { status, data } = await request('/captcha')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('captchaId')
      expect(data.data).toHaveProperty('captchaImage')
    })
  })

  // ================================================================
  // Module 19: Budget Funds (预算模块)
  // ================================================================
  describe('Budget Funds (预算模块)', () => {
    it('should get budget list', async () => {
      const { status, data } = await request('/budget-funds?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.records[0]).toHaveProperty('totalAmount')
      expect(data.data.records[0]).toHaveProperty('usedAmount')
      expect(data.data.records[0]).toHaveProperty('remainingAmount')
    })

    it('should create a budget', async () => {
      const { status, data } = await request('/budget-funds', {
        method: 'POST',
        body: { name: '测试预算', year: 2024, totalAmount: 100000, category: 'BOOK' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
    })
  })

  // ================================================================
  // Module 20: Vendors (供应商模块)
  // ================================================================
  describe('Vendors (供应商模块)', () => {
    it('should get vendor list', async () => {
      const { status, data } = await request('/vendors?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should create a vendor', async () => {
      const { status, data } = await request('/vendors', {
        method: 'POST',
        body: { name: '新供应商', contactPerson: '联系人', phone: '010-99999999' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
      expect(data.data.status).toBe('ACTIVE')
    })
  })

  // ================================================================
  // Module 21: Purchase Orders (采购订单模块)
  // ================================================================
  describe('Purchase Orders (采购订单模块)', () => {
    it('should get purchase order list', async () => {
      const { status, data } = await request('/purchase-orders?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should create a purchase order', async () => {
      const { status, data } = await request('/purchase-orders', {
        method: 'POST',
        body: { title: '测试采购单', vendorId: 1, totalAmount: 50000 },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
    })

    it('should approve a purchase order', async () => {
      const { status, data } = await request('/purchase-orders/1/approve', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('审批成功')
    })
  })

  // ================================================================
  // Module 22: MARC Records (MARC记录模块)
  // ================================================================
  describe('MARC Records (MARC记录模块)', () => {
    it('should get MARC record list', async () => {
      const { status, data } = await request('/marc/records?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should get MARC record detail', async () => {
      const { status, data } = await request('/marc/records/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data).toHaveProperty('isbn')
      expect(data.data).toHaveProperty('format')
    })
  })

  // ================================================================
  // Module 23: Serial Subscriptions (期刊订阅模块)
  // ================================================================
  describe('Serial Subscriptions (期刊订阅模块)', () => {
    it('should get serial subscriptions', async () => {
      const { status, data } = await request('/serial/subscriptions?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should get serial issues', async () => {
      const { status, data } = await request('/serial/issues?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.records[0]).toHaveProperty('volume')
      expect(data.data.records[0]).toHaveProperty('issue')
    })

    it('should mark an issue as received', async () => {
      const { status, data } = await request('/serial/issues/1/receive', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('到刊登记成功')
    })

    it('should mark an issue as missing', async () => {
      const { status, data } = await request('/serial/issues/3/missing', {
        method: 'POST',
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('缺刊标记成功')
    })
  })

  // ================================================================
  // Module 24: Branches (分馆模块)
  // ================================================================
  describe('Branches (分馆模块)', () => {
    it('should get branch list', async () => {
      const { status, data } = await request('/branches?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.total).toBeGreaterThan(0)
    })

    it('should get branch detail', async () => {
      const { status, data } = await request('/branches/1')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.name).toBe('总馆')
    })
  })

  // ================================================================
  // Module 25: Reports (报表模块)
  // ================================================================
  describe('Reports (报表模块)', () => {
    it('should get report list', async () => {
      const { status, data } = await request('/reports?current=1&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should create a report', async () => {
      const { status, data } = await request('/reports', {
        method: 'POST',
        body: { name: '测试报表', type: 'BORROW', description: '测试描述' },
      })

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.message).toBe('创建成功')
    })
  })

  // ================================================================
  // Module 26: Edge Cases & Boundary Tests (边界条件测试)
  // ================================================================
  describe('Edge Cases & Boundary Tests (边界条件测试)', () => {
    it('should return 404 for non-existent API endpoint', async () => {
      const { status, data } = await request('/nonexistent-endpoint')

      expect(status).toBe(404)
      expect(data.code).toBe(404)
      expect(data.message).toContain('Not Found')
    })

    it('should return 404 for non-existent resource sub-path', async () => {
      const { status, data } = await request('/books/nonexistent/path')

      expect(status).toBe(404)
      expect(data.code).toBe(404)
    })

    it('should handle large page number gracefully', async () => {
      const { status, data } = await request('/books?current=99999&size=10')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
      expect(data.data.records.length).toBe(0)
    })

    it('should handle zero size pagination', async () => {
      const { status, data } = await request('/books?current=1&size=0')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should handle large size pagination', async () => {
      const { status, data } = await request('/books?current=1&size=100')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should handle missing query parameters gracefully', async () => {
      const { status, data } = await request('/books')

      expect(status).toBe(200)
      expect(data.code).toBe(0)
      expect(data.data.records).toBeDefined()
    })

    it('should handle empty body on login', async () => {
      const { status, data } = await request('/auth/login', {
        method: 'POST',
        body: {},
      })

      expect(status).toBe(401)
      expect(data.code).toBe(401)
    })

    it('should return 200 for OPTIONS preflight (CORS)', async () => {
      const response = await fetch(`${BASE}/books`, { method: 'OPTIONS' })

      expect(response.status).toBe(204)
    })

    it('should return CORS headers in response', async () => {
      const response = await fetch(`${BASE}/books`)

      expect(response.headers.get('access-control-allow-origin')).toBe('*')
      expect(response.headers.get('content-type')).toContain('application/json')
    })
  })
})
