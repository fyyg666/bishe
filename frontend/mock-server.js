import http from 'node:http'

const PORT = 8080

const wrap = (data, msg = 'success', code = 0) => ({ code, message: msg, data })
const wrapPage = (records, total, current = 1, size = 10) => wrap({ records, total, current, size, pages: Math.ceil(total / size) })

function parseBody(req) {
  return new Promise((resolve) => {
    let body = ''
    req.on('data', (chunk) => { body += chunk })
    req.on('end', () => {
      try { resolve(JSON.parse(body)) } catch { resolve({}) }
    })
  })
}

function getQuery(url) {
  const idx = url.indexOf('?')
  if (idx === -1) return {}
  const params = {}
  new URLSearchParams(url.slice(idx + 1)).forEach((v, k) => { params[k] = isNaN(v) ? v : Number(v) })
  return params
}

// ============ Mock Data ============
let seqBook = 100, seqReader = 100, seqBorrow = 200, seqAnnouncement = 100
let seqReservation = 300, seqVolunteer = 100, seqCompensation = 100
let seqBudget = 100, seqSuggestion = 100, seqVendor = 100, seqPurchase = 200
let seqMarc = 500, seqSerialSub = 300, seqSerialIssue = 400, seqSerialClaim = 100
let seqSerialRouting = 100, seqBranch = 10, seqReport = 100

const categories = [
  { id: 1, name: '计算机科学', code: 'CS', parentId: null, bookCount: 45 },
  { id: 2, name: '文学', code: 'LT', parentId: null, bookCount: 38 },
  { id: 3, name: '历史', code: 'HS', parentId: null, bookCount: 29 },
  { id: 4, name: '哲学', code: 'PL', parentId: null, bookCount: 16 },
  { id: 5, name: '数学', code: 'MA', parentId: null, bookCount: 22 },
  { id: 6, name: '经济学', code: 'EC', parentId: null, bookCount: 31 },
  { id: 7, name: '艺术', code: 'AR', parentId: null, bookCount: 18 },
  { id: 8, name: '自然科学', code: 'NS', parentId: null, bookCount: 27 },
]

const books = [
  { id: 1, title: '深入理解计算机系统', author: 'Randal E. Bryant', isbn: '978-7-111-54493-7', publisher: '机械工业出版社', publishDate: '2016-11-01', categoryId: 1, categoryName: '计算机科学', totalCopies: 8, availableCopies: 5, borrowCount: 127, status: 'AVAILABLE', coverUrl: '', summary: '从程序员视角全面剖析计算机系统的权威教材。' },
  { id: 2, title: '数据结构与算法分析', author: 'Mark Allen Weiss', isbn: '978-7-111-52839-4', publisher: '机械工业出版社', publishDate: '2019-03-15', categoryId: 1, categoryName: '计算机科学', totalCopies: 5, availableCopies: 3, borrowCount: 98, status: 'AVAILABLE', coverUrl: '', summary: '经典数据结构教材，使用Java语言描述。' },
  { id: 3, title: '百年孤独', author: '加西亚·马尔克斯', isbn: '978-7-5442-5399-4', publisher: '南海出版公司', publishDate: '2011-06-01', categoryId: 2, categoryName: '文学', totalCopies: 6, availableCopies: 4, borrowCount: 203, status: 'AVAILABLE', coverUrl: '', summary: '魔幻现实主义文学的代表作。' },
  { id: 4, title: '活着', author: '余华', isbn: '978-7-5302-2153-4', publisher: '北京十月文艺出版社', publishDate: '2017-06-01', categoryId: 2, categoryName: '文学', totalCopies: 10, availableCopies: 7, borrowCount: 312, status: 'AVAILABLE', coverUrl: '', summary: '讲述了一个人一生的故事。' },
  { id: 5, title: 'React设计原理', author: '卡颂', isbn: '978-7-121-45231-2', publisher: '电子工业出版社', publishDate: '2023-01-01', categoryId: 1, categoryName: '计算机科学', totalCopies: 4, availableCopies: 2, borrowCount: 45, status: 'AVAILABLE', coverUrl: '', summary: '深入React内部运行机制。' },
  { id: 6, title: '万历十五年', author: '黄仁宇', isbn: '978-7-101-14466-6', publisher: '中华书局', publishDate: '1982-05-01', categoryId: 3, categoryName: '历史', totalCopies: 5, availableCopies: 3, borrowCount: 156, status: 'AVAILABLE', coverUrl: '', summary: '以1587年为切入点，展现明代社会的深层结构。' },
  { id: 7, title: '苏菲的世界', author: '乔斯坦·贾德', isbn: '978-7-5063-9471-0', publisher: '作家出版社', publishDate: '2017-08-01', categoryId: 4, categoryName: '哲学', totalCopies: 4, availableCopies: 4, borrowCount: 89, status: 'AVAILABLE', coverUrl: '', summary: '以小说的形式讲述西方哲学史。' },
  { id: 8, title: '算法导论', author: 'Thomas H. Cormen', isbn: '978-7-111-40701-0', publisher: '机械工业出版社', publishDate: '2013-01-01', categoryId: 1, categoryName: '计算机科学', totalCopies: 6, availableCopies: 2, borrowCount: 178, status: 'AVAILABLE', coverUrl: '', summary: '算法领域的经典权威教材。' },
  { id: 9, title: '月亮与六便士', author: '毛姆', isbn: '978-7-5327-8234-1', publisher: '上海译文出版社', publishDate: '2018-09-01', categoryId: 2, categoryName: '文学', totalCopies: 5, availableCopies: 3, borrowCount: 134, status: 'AVAILABLE', coverUrl: '', summary: '以高更为原型的艺术追寻故事。' },
  { id: 10, title: '数学之美', author: '吴军', isbn: '978-7-115-37355-7', publisher: '人民邮电出版社', publishDate: '2014-11-01', categoryId: 5, categoryName: '数学', totalCopies: 4, availableCopies: 4, borrowCount: 92, status: 'AVAILABLE', coverUrl: '', summary: '用通俗语言讲述数学在信息领域的应用。' },
  { id: 11, title: '国富论', author: '亚当·斯密', isbn: '978-7-100-18352-2', publisher: '商务印书馆', publishDate: '2015-08-01', categoryId: 6, categoryName: '经济学', totalCopies: 3, availableCopies: 2, borrowCount: 67, status: 'AVAILABLE', coverUrl: '', summary: '现代经济学的奠基之作。' },
  { id: 12, title: '设计中的设计', author: '原研哉', isbn: '978-7-209-04173-2', publisher: '山东人民出版社', publishDate: '2006-11-01', categoryId: 7, categoryName: '艺术', totalCopies: 3, availableCopies: 3, borrowCount: 55, status: 'AVAILABLE', coverUrl: '', summary: '日本设计师原研哉的设计理念。' },
]

const readers = [
  { id: 1, username: 'admin', realName: '系统管理员', email: 'admin@library.cn', phone: '13800000001', role: 'ADMIN', credit: 100, status: 'ACTIVE', createTime: '2024-01-01T00:00:00' },
  { id: 2, username: 'librarian1', realName: '张馆员', email: 'zhang@library.cn', phone: '13800000002', role: 'LIBRARIAN', credit: 100, status: 'ACTIVE', createTime: '2024-01-15T00:00:00' },
  { id: 3, username: 'reader01', realName: '李明', email: 'liming@example.com', phone: '13800000003', role: 'READER', credit: 95, status: 'ACTIVE', createTime: '2024-03-01T00:00:00' },
  { id: 4, username: 'reader02', realName: '王芳', email: 'wangfang@example.com', phone: '13800000004', role: 'READER', credit: 88, status: 'ACTIVE', createTime: '2024-03-10T00:00:00' },
  { id: 5, username: 'reader03', realName: '赵强', email: 'zhaoqiang@example.com', phone: '13800000005', role: 'READER', credit: 75, status: 'ACTIVE', createTime: '2024-04-05T00:00:00' },
  { id: 6, username: 'reader04', realName: '陈静', email: 'chenjing@example.com', phone: '13800000006', role: 'READER', credit: 100, status: 'ACTIVE', createTime: '2024-04-20T00:00:00' },
  { id: 7, username: 'reader05', realName: '刘洋', email: 'liuyang@example.com', phone: '13800000007', role: 'READER', credit: 62, status: 'SUSPENDED', createTime: '2024-05-01T00:00:00' },
]

const borrows = [
  { id: 1, bookId: 1, bookTitle: '深入理解计算机系统', readerId: 3, readerName: '李明', borrowDate: '2024-06-01', dueDate: '2024-07-01', returnDate: null, status: 'BORROWED', renewed: false, fine: 0 },
  { id: 2, bookId: 2, bookTitle: '数据结构与算法分析', readerId: 4, readerName: '王芳', borrowDate: '2024-05-28', dueDate: '2024-06-28', returnDate: null, status: 'BORROWED', renewed: true, fine: 0 },
  { id: 3, bookId: 3, bookTitle: '百年孤独', readerId: 3, readerName: '李明', borrowDate: '2024-05-15', dueDate: '2024-06-15', returnDate: '2024-06-10', status: 'RETURNED', renewed: false, fine: 0 },
  { id: 4, bookId: 5, bookTitle: 'React设计原理', readerId: 5, readerName: '赵强', borrowDate: '2024-06-05', dueDate: '2024-07-05', returnDate: null, status: 'BORROWED', renewed: false, fine: 0 },
  { id: 5, bookId: 8, bookTitle: '算法导论', readerId: 4, readerName: '王芳', borrowDate: '2024-05-20', dueDate: '2024-06-20', returnDate: null, status: 'OVERDUE', renewed: false, fine: 5 },
  { id: 6, bookId: 4, bookTitle: '活着', readerId: 6, readerName: '陈静', borrowDate: '2024-04-10', dueDate: '2024-05-10', returnDate: '2024-05-08', status: 'RETURNED', renewed: false, fine: 0 },
  { id: 7, bookId: 6, bookTitle: '万历十五年', readerId: 5, readerName: '赵强', borrowDate: '2024-06-02', dueDate: '2024-07-02', returnDate: null, status: 'BORROWED', renewed: false, fine: 0 },
]

const borrowRules = [
  { id: 1, role: 'READER', maxBooks: 5, maxDays: 30, maxRenew: 2, finePerDay: 0.5, depositRequired: false },
  { id: 2, role: 'VOLUNTEER', maxBooks: 8, maxDays: 45, maxRenew: 3, finePerDay: 0.3, depositRequired: false },
  { id: 3, role: 'LIBRARIAN', maxBooks: 10, maxDays: 60, maxRenew: 4, finePerDay: 0.2, depositRequired: false },
]

const seats = []
for (let i = 1; i <= 40; i++) {
  const area = i <= 8 ? 'A区-阅览区' : i <= 16 ? 'B区-自习区' : i <= 28 ? 'C区-电子阅览区' : 'D区-研讨区'
  seats.push({
    id: i, seatNumber: `${area.charAt(0)}${String(i).padStart(3, '0')}`,
    area, floor: i <= 16 ? 1 : 2,
    status: i <= 25 ? 'AVAILABLE' : i <= 30 ? 'OCCUPIED' : i <= 35 ? 'RESERVED' : 'MAINTENANCE',
    currentReservation: i <= 30 ? null : { id: 300 + i, userId: 3, userName: '李明', startTime: '09:00', endTime: '12:00' }
  })
}

const reservations = [
  { id: 301, seatId: 31, seatNumber: 'A031', seatArea: 'A区-阅览区', userId: 3, userName: '李明', reservationDate: '2024-06-15', startTime: '09:00', endTime: '12:00', status: 'RESERVED', createTime: '2024-06-14T10:00:00' },
  { id: 302, seatId: 33, seatNumber: 'B033', seatArea: 'B区-自习区', userId: 4, userName: '王芳', reservationDate: '2024-06-15', startTime: '14:00', endTime: '18:00', status: 'CHECKED_IN', createTime: '2024-06-14T15:00:00' },
]

const announcements = [
  { id: 1, title: '图书馆暑假开放时间调整通知', content: '暑假期间（7月1日-8月31日），图书馆开放时间调整为每日9:00-17:00，周末正常开放。', type: 'NOTICE', status: 'PUBLISHED', publisher: '张馆员', createTime: '2024-06-01T09:00:00', publishTime: '2024-06-01T09:00:00' },
  { id: 2, title: '新书上架通知', content: '本月新到馆图书500余册，涵盖计算机、文学、历史等多个类别，欢迎读者前来借阅。', type: 'NEWS', status: 'PUBLISHED', publisher: '张馆员', createTime: '2024-05-28T14:00:00', publishTime: '2024-05-28T14:00:00' },
  { id: 3, title: '关于延长还书期限的通知', content: '因期末复习需要，所有在借图书的还书日期自动延长至7月15日。', type: 'NOTICE', status: 'PUBLISHED', publisher: '张馆员', createTime: '2024-06-10T08:00:00', publishTime: '2024-06-10T08:00:00' },
]

const volunteers = [
  { id: 1, userId: 3, userName: '李明', activityName: '图书整理', description: '协助整理三楼阅览区图书', date: '2024-06-20', startTime: '09:00', endTime: '12:00', hours: 3, status: 'APPROVED', createTime: '2024-06-05T10:00:00' },
  { id: 2, userId: 4, userName: '王芳', activityName: '读者引导', description: '为新生提供图书馆使用指导', date: '2024-06-22', startTime: '14:00', endTime: '17:00', hours: 3, status: 'PENDING', createTime: '2024-06-08T09:00:00' },
  { id: 3, userId: 6, userName: '陈静', activityName: '新书验收', description: '协助验收新到馆图书', date: '2024-06-18', startTime: '09:00', endTime: '16:00', hours: 6, status: 'APPROVED', createTime: '2024-06-03T15:00:00' },
]

const compensations = [
  { id: 1, bookId: 2, bookTitle: '数据结构与算法分析(副本)', readerId: 5, readerName: '赵强', type: 'LOST', amount: 79.0, reason: '遗失图书', status: 'PENDING', createTime: '2024-06-10T10:00:00' },
  { id: 2, bookId: 10, bookTitle: '数学之美', readerId: 5, readerName: '赵强', type: 'DAMAGE', amount: 30.0, reason: '书页破损', status: 'APPROVED', createTime: '2024-06-08T16:00:00', approveTime: '2024-06-09T10:00:00' },
]

const budgets = [
  { id: 1, name: '2024年图书采购预算', year: 2024, totalAmount: 500000, usedAmount: 320000, remainingAmount: 180000, category: 'BOOK', status: 'ACTIVE', description: '本年度纸质图书采购预算', createTime: '2024-01-01' },
  { id: 2, name: '2024年数字资源预算', year: 2024, totalAmount: 300000, usedAmount: 150000, remainingAmount: 150000, category: 'DIGITAL', status: 'ACTIVE', description: '电子期刊和数据库订阅', createTime: '2024-01-01' },
  { id: 3, name: '2024年设备维护预算', year: 2024, totalAmount: 100000, usedAmount: 45000, remainingAmount: 55000, category: 'EQUIPMENT', status: 'ACTIVE', description: '阅览室设备和家具维护', createTime: '2024-01-01' },
]

const suggestions = [
  { id: 1, userId: 3, userName: '李明', bookTitle: 'Clean Architecture', author: 'Robert C. Martin', isbn: '978-0-13-449416-6', publisher: 'Prentice Hall', reason: '软件架构领域的经典著作，希望能入藏', status: 'PENDING', createTime: '2024-06-12T10:00:00' },
  { id: 2, userId: 4, userName: '王芳', bookTitle: '三体全集', author: '刘慈欣', isbn: '978-7-5366-9293-0', publisher: '重庆出版社', reason: '最受欢迎的国产科幻', status: 'APPROVED', createTime: '2024-05-20T09:00:00', approveTime: '2024-05-25T15:00:00' },
]

const vendors = [
  { id: 1, name: '新华书店集团', contactPerson: '王经理', phone: '010-88880001', email: 'vendor1@xinhuabook.com', address: '北京市西城区', status: 'ACTIVE', createTime: '2024-01-01' },
  { id: 2, name: '当当图书供应商', contactPerson: '李经理', phone: '010-88880002', email: 'vendor2@dangdang.com', address: '北京市朝阳区', status: 'ACTIVE', createTime: '2024-01-15' },
  { id: 3, name: '中国图书进出口总公司', contactPerson: '赵经理', phone: '010-88880003', email: 'vendor3@cnpiec.com', address: '北京市东城区', status: 'ACTIVE', createTime: '2024-02-01' },
]

const purchaseOrders = [
  { id: 1, title: '2024年Q2计算机类图书采购', vendorId: 1, vendorName: '新华书店集团', totalAmount: 85000, itemCount: 120, status: 'APPROVED', createTime: '2024-04-01', items: [] },
  { id: 2, title: '2024年Q2文学类图书采购', vendorId: 2, vendorName: '当当图书供应商', totalAmount: 45000, itemCount: 80, status: 'PENDING', createTime: '2024-05-15', items: [] },
]

const marcRecords = [
  { id: 1, title: '深入理解计算机系统 / Computer Systems: A Programmer\'s Perspective', isbn: '978-7-111-54493-7', author: '(美) Randal E. Bryant, David R. O\'Hallaron著', publisher: '机械工业出版社', publishDate: '2016', format: 'BK', language: 'chi', fields: [{ tag: '100', ind1: '1', ind2: ' ', subfields: [{ code: 'a', value: 'Bryant, Randal E.' }] }] },
]

const serialSubscriptions = [
  { id: 1, title: '计算机学报', issn: '0254-4164', publisher: '科学出版社', frequency: 'MONTHLY', startYear: 2024, status: 'ACTIVE', createTime: '2024-01-01' },
  { id: 2, title: '软件学报', issn: '1000-9825', publisher: '科学出版社', frequency: 'MONTHLY', startYear: 2024, status: 'ACTIVE', createTime: '2024-01-01' },
  { id: 3, title: '中国社会科学', issn: '1002-4921', publisher: '中国社会科学院', frequency: 'MONTHLY', startYear: 2024, status: 'ACTIVE', createTime: '2024-01-01' },
]

const serialIssues = [
  { id: 1, subscriptionId: 1, subscriptionTitle: '计算机学报', volume: '47', issue: '1', year: 2024, expectedDate: '2024-01-15', receivedDate: '2024-01-15', status: 'RECEIVED' },
  { id: 2, subscriptionId: 1, subscriptionTitle: '计算机学报', volume: '47', issue: '2', year: 2024, expectedDate: '2024-02-15', receivedDate: '2024-02-16', status: 'RECEIVED' },
  { id: 3, subscriptionId: 1, subscriptionTitle: '计算机学报', volume: '47', issue: '3', year: 2024, expectedDate: '2024-03-15', receivedDate: null, status: 'MISSING' },
  { id: 4, subscriptionId: 1, subscriptionTitle: '计算机学报', volume: '47', issue: '4', year: 2024, expectedDate: '2024-04-15', receivedDate: '2024-04-14', status: 'RECEIVED' },
  { id: 5, subscriptionId: 1, subscriptionTitle: '计算机学报', volume: '47', issue: '5', year: 2024, expectedDate: '2024-05-15', receivedDate: null, status: 'EXPECTED' },
  { id: 6, subscriptionId: 2, subscriptionTitle: '软件学报', volume: '35', issue: '1', year: 2024, expectedDate: '2024-01-15', receivedDate: '2024-01-16', status: 'RECEIVED' },
]

const branches = [
  { id: 1, name: '总馆', address: '北京市海淀区中关村南大街', phone: '010-88880001', openingHours: '08:00-22:00', status: 'ACTIVE' },
  { id: 2, name: '东区分馆', address: '北京市朝阳区建国路', phone: '010-88880002', openingHours: '09:00-21:00', status: 'ACTIVE' },
  { id: 3, name: '西区分馆', address: '北京市海淀区学院路', phone: '010-88880003', openingHours: '09:00-20:00', status: 'ACTIVE' },
]

const reports = [
  { id: 1, name: '月度借阅统计报表', type: 'BORROW', description: '按类别统计月度借阅情况', createTime: '2024-06-01', status: 'COMPLETED' },
  { id: 2, name: '读者活跃度分析', type: 'READER', description: '分析读者借阅频率和偏好', createTime: '2024-06-10', status: 'PENDING' },
]

let creditLogs = [
  { id: 1, userId: 3, change: 5, reason: '按时还书', balance: 100, createTime: '2024-05-10T10:00:00' },
  { id: 2, userId: 3, change: -3, reason: '逾期还书', balance: 97, createTime: '2024-05-15T09:00:00' },
  { id: 3, userId: 3, change: -2, reason: '图书轻微污损', balance: 95, createTime: '2024-06-01T14:00:00' },
  { id: 4, userId: 4, change: 5, reason: '按时还书', balance: 93, createTime: '2024-04-20T10:00:00' },
  { id: 5, userId: 4, change: -5, reason: '预约未签到', balance: 88, createTime: '2024-05-05T09:00:00' },
]

const credits = [
  { id: 1, userId: 3, userName: '李明', role: 'READER', score: 95, level: '良好' },
  { id: 2, userId: 4, userName: '王芳', role: 'READER', score: 88, level: '良好' },
]

const notifications = [
  { id: 1, title: '还书提醒', content: '您借阅的《算法导论》将于3天后到期，请及时归还或续借。', type: 'BORROW', status: 'UNREAD', createTime: '2024-06-17T08:00:00' },
  { id: 2, title: '预约成功', content: '您的座位预约已确认：A区阅览区 A031号，6月15日 09:00-12:00。', type: 'SEAT', status: 'UNREAD', createTime: '2024-06-14T10:00:00' },
  { id: 3, title: '荐购审批通过', content: '您推荐的《三体全集》已通过审批，预计2周内到馆。', type: 'SUGGESTION', status: 'READ', createTime: '2024-05-25T15:00:00' },
]

const statistics = {
  totalBooks: books.length,
  totalReaders: readers.filter(r => r.role === 'READER').length,
  totalBorrows: 1247,
  borrowsToday: 12,
  borrowsThisMonth: 156,
  overdueCount: 3,
  popularBooks: books.slice(0, 5).map(b => ({ title: b.title, borrowCount: b.borrowCount })),
  categoryDistribution: categories.map(c => ({ name: c.name, value: c.bookCount })),
  monthlyBorrows: [
    { month: '2024-01', count: 98 }, { month: '2024-02', count: 86 }, { month: '2024-03', count: 112 },
    { month: '2024-04', count: 135 }, { month: '2024-05', count: 148 }, { month: '2024-06', count: 156 },
  ],
}

// ============ Route Handler ============
function handleRoute(req, res, body, query) {
  const method = req.method
  const url = new URL(req.url, 'http://localhost')
  const path = url.pathname

  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type')

  if (method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  try {
    let result = null
    const pathParts = path.replace('/api/v1', '')

    // ── Auth ──
    if (pathParts === '/auth/login' && method === 'POST') {
      const { username } = body
      const reader = readers.find(r => r.username === username)
      if (!reader) { result = { code: 401, message: '用户名或密码错误', data: null }; res.statusCode = 401 }
      else result = { code: 0, message: '登录成功', accessToken: 'mock-jwt-token-' + Date.now(), refreshToken: 'mock-refresh-token', userInfo: reader }
    }
    else if (pathParts === '/auth/register' && method === 'POST') {
      const newReader = { id: ++seqReader, username: body.username, realName: body.realName || body.username, email: body.email || '', phone: body.phone || '', role: 'READER', credit: 100, status: 'ACTIVE', createTime: new Date().toISOString() }
      readers.push(newReader)
      result = wrap(newReader, '注册成功')
    }
    else if (pathParts === '/auth/logout' && method === 'POST') {
      result = wrap(null, '退出成功')
    }
    else if (pathParts === '/auth/info' && method === 'GET') {
      const auth = req.headers.authorization
      if (!auth) { res.statusCode = 401; result = { code: 401, message: '未登录', data: null } }
      else result = { code: 0, data: readers[0] } // mock admin
    }
    else if (pathParts === '/captcha' && method === 'GET') {
      result = wrap({ captchaId: 'mock-captcha-id', captchaImage: '' })
    }

    // ── Books ──
    else if (pathParts === '/books' && method === 'GET') {
      const { current = 1, size = 10, keyword, categoryId } = query
      let filtered = [...books]
      if (keyword) filtered = filtered.filter(b => b.title.includes(keyword) || b.author.includes(keyword) || b.isbn.includes(keyword))
      if (categoryId) filtered = filtered.filter(b => b.categoryId === Number(categoryId))
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts === '/books/hot' && method === 'GET') {
      result = wrap(books.filter(b => b.borrowCount > 50).sort((a, b) => b.borrowCount - a.borrowCount).slice(0, 10))
    }
    else if (pathParts === '/books/new' && method === 'GET') {
      result = wrap([...books].sort((a, b) => new Date(b.publishDate) - new Date(a.publishDate)).slice(0, 10))
    }
    else if (pathParts === '/books/check-isbn' && method === 'GET') {
      result = wrap(books.some(b => b.isbn === query.isbn))
    }
    else if (pathParts === '/books/isbn-lookup' && method === 'GET') {
      result = wrap({ isbn: query.isbn, title: 'Mock图书', author: 'Mock作者', publisher: 'Mock出版社' })
    }
    else if (pathParts === '/books/advanced-search' && method === 'GET') {
      const { current = 1, size = 10 } = query
      let filtered = [...books]
      if (query.title) filtered = filtered.filter(b => b.title.includes(query.title))
      if (query.author) filtered = filtered.filter(b => b.author.includes(query.author))
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts === '/books/facets/categories' && method === 'GET') {
      result = wrap(categories.map(c => ({ id: c.id, name: c.name, count: c.bookCount })))
    }
    else if (pathParts === '/books/facets/authors' && method === 'GET') {
      result = wrap([{ name: '余华', count: 1 }, { name: 'Randal E. Bryant', count: 1 }])
    }
    else if (pathParts.match(/^\/books\/(\d+)$/) && method === 'GET') {
      const id = Number(pathParts.match(/\/books\/(\d+)$/)[1])
      result = wrap(books.find(b => b.id === id) || null)
    }
    else if (pathParts.match(/^\/books\/(\d+)$/) && method === 'PUT') {
      const id = Number(pathParts.match(/\/books\/(\d+)$/)[1])
      const idx = books.findIndex(b => b.id === id)
      if (idx >= 0) { books[idx] = { ...books[idx], ...body }; result = wrap(books[idx], '更新成功') }
      else { res.statusCode = 404; result = { code: 404, message: '图书不存在' } }
    }
    else if (pathParts.match(/^\/books\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts === '/books' && method === 'POST') {
      const newBook = { id: ++seqBook, ...body, borrowCount: 0, status: 'AVAILABLE', totalCopies: body.totalCopies || 1, availableCopies: body.totalCopies || 1 }
      books.push(newBook)
      result = wrap(newBook, '创建成功')
    }
    else if (pathParts === '/books/export' && method === 'GET') {
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
      res.writeHead(200)
      res.end(Buffer.from('mock-xlsx'))
      return
    }
    else if (pathParts === '/books/import' && method === 'POST') {
      result = wrap({ success: 10, failed: 0, total: 10 }, '导入成功')
    }

    // ── Categories ──
    else if (pathParts === '/categories' && method === 'GET') {
      result = wrap(categories)
    }

    // ── Readers ──
    else if (pathParts === '/readers' && method === 'GET') {
      const { current = 1, size = 10, keyword } = query
      let filtered = readers.filter(r => r.role === 'READER')
      if (keyword) filtered = filtered.filter(r => r.realName.includes(keyword) || r.username.includes(keyword))
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts.match(/^\/readers\/(\d+)$/) && method === 'GET') {
      const id = Number(pathParts.match(/\/readers\/(\d+)$/)[1])
      result = wrap(readers.find(r => r.id === id) || null)
    }
    else if (pathParts === '/readers' && method === 'POST') {
      const newReader = { id: ++seqReader, role: 'READER', credit: 100, status: 'ACTIVE', createTime: new Date().toISOString(), ...body }
      readers.push(newReader)
      result = wrap(newReader, '创建成功')
    }
    else if (pathParts.match(/^\/readers\/(\d+)$/) && method === 'PUT') {
      const id = Number(pathParts.match(/\/readers\/(\d+)$/)[1])
      const idx = readers.findIndex(r => r.id === id)
      if (idx >= 0) { readers[idx] = { ...readers[idx], ...body }; result = wrap(readers[idx], '更新成功') }
      else { res.statusCode = 404; result = { code: 404, message: '读者不存在' } }
    }
    else if (pathParts.match(/^\/readers\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Borrows ──
    else if (pathParts === '/borrows' && method === 'GET') {
      const { current = 1, size = 10, status, keyword } = query
      let filtered = [...borrows]
      if (status) filtered = filtered.filter(b => b.status === status)
      if (keyword) filtered = filtered.filter(b => b.bookTitle.includes(keyword) || b.readerName.includes(keyword))
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts.match(/^\/borrows\/(\d+)$/) && method === 'GET') {
      const id = Number(pathParts.match(/\/borrows\/(\d+)$/)[1])
      result = wrap(borrows.find(b => b.id === id) || null)
    }
    else if (pathParts === '/borrows' && method === 'POST') {
      const newBorrow = { id: ++seqBorrow, ...body, borrowDate: new Date().toISOString().slice(0, 10), dueDate: new Date(Date.now() + 30*86400000).toISOString().slice(0, 10), returnDate: null, status: 'BORROWED', renewed: false, fine: 0 }
      borrows.push(newBorrow)
      result = wrap(newBorrow, '借阅成功')
    }
    else if (pathParts.match(/^\/borrows\/(\d+)\/return$/) && method === 'POST') {
      result = wrap(null, '还书成功')
    }
    else if (pathParts.match(/^\/borrows\/(\d+)\/renew$/) && method === 'POST') {
      result = wrap(null, '续借成功')
    }

    // ── Borrow Rules ──
    else if (pathParts === '/borrow-rules' && method === 'GET') {
      result = wrap(borrowRules)
    }
    else if (pathParts.match(/^\/borrow-rules\/(\d+)$/) && method === 'GET') {
      result = wrap(borrowRules[0])
    }
    else if (pathParts === '/borrow-rules' && method === 'POST') {
      const rule = { id: borrowRules.length + 1, ...body }
      borrowRules.push(rule)
      result = wrap(rule, '创建成功')
    }
    else if (pathParts.match(/^\/borrow-rules\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/borrow-rules\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Seats ──
    else if (pathParts === '/seats' && method === 'GET') {
      const { area } = query
      const filtered = area ? seats.filter(s => s.area === area) : seats
      result = wrapPage(filtered, filtered.length, 1, 100)
    }
    else if (pathParts.match(/^\/seats\/(\d+)$/) && method === 'GET') {
      const id = Number(pathParts.match(/\/seats\/(\d+)$/)[1])
      result = wrap(seats.find(s => s.id === id) || null)
    }
    else if (pathParts === '/seats/reserve' && method === 'POST') {
      result = wrap({ id: ++seqReservation, ...body, status: 'RESERVED', createTime: new Date().toISOString() }, '预约成功')
    }
    else if (pathParts.match(/^\/seats\/cancel\/(\d+)$/) && method === 'POST') {
      result = wrap(null, '取消成功')
    }
    else if (pathParts === '/seats/my' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(reservations, reservations.length, current, size)
    }
    else if (pathParts.match(/^\/seats\/checkin\/(\d+)$/) && method === 'POST') {
      result = wrap(null, '签到成功')
    }
    else if (pathParts.match(/^\/seats\/checkout\/(\d+)$/) && method === 'POST') {
      result = wrap(null, '签退成功')
    }
    else if (pathParts === '/seats/check-availability' && method === 'GET') {
      result = wrap({ available: true })
    }
    else if (pathParts === '/seats/reading-rooms' && method === 'GET') {
      result = wrap(['A区-阅览区', 'B区-自习区', 'C区-电子阅览区', 'D区-研讨区'])
    }

    // ── Announcements ──
    else if (pathParts === '/announcements' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(announcements.slice((current-1)*size, current*size), announcements.length, current, size)
    }
    else if (pathParts === '/announcements/latest' && method === 'GET') {
      result = wrap(announcements.slice(-(query.limit || 5)))
    }
    else if (pathParts.match(/^\/announcements\/(\d+)$/) && method === 'GET') {
      const id = Number(pathParts.match(/\/announcements\/(\d+)$/)[1])
      result = wrap(announcements.find(a => a.id === id) || null)
    }
    else if (pathParts === '/announcements' && method === 'POST') {
      const a = { id: ++seqAnnouncement, status: 'DRAFT', createTime: new Date().toISOString(), publishTime: null, publisher: '张馆员', ...body }
      announcements.push(a)
      result = wrap(a, '创建成功')
    }
    else if (pathParts.match(/^\/announcements\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/announcements\/(\d+)\/publish$/) && method === 'POST') {
      result = wrap(null, '发布成功')
    }
    else if (pathParts.match(/^\/announcements\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Volunteers ──
    else if (pathParts === '/volunteers' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(volunteers.slice((current-1)*size, current*size), volunteers.length, current, size)
    }
    else if (pathParts === '/volunteers/my' && method === 'GET') {
      result = wrapPage(volunteers, volunteers.length, 1, 10)
    }
    else if (pathParts === '/volunteers/stats' && method === 'GET') {
      result = wrap({ totalHours: 36, totalServices: 12, currentMonthHours: 6 })
    }
    else if (pathParts.match(/^\/volunteers\/(\d+)$/) && method === 'GET') {
      result = wrap(volunteers[0])
    }
    else if (pathParts === '/volunteers' && method === 'POST') {
      const v = { id: ++seqVolunteer, status: 'PENDING', createTime: new Date().toISOString(), ...body }
      volunteers.push(v)
      result = wrap(v, '申请成功')
    }
    else if (pathParts.match(/^\/volunteers\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/volunteers\/(\d+)\/cancel$/) && method === 'POST') {
      result = wrap(null, '取消成功')
    }
    else if (pathParts.match(/^\/volunteers\/(\d+)\/review$/) && method === 'POST') {
      result = wrap(null, '审核完成')
    }
    else if (pathParts === '/volunteers/pending' && method === 'GET') {
      result = wrapPage(volunteers.filter(v => v.status === 'PENDING'), 1, 1, 10)
    }
    else if (pathParts.match(/^\/volunteers\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Profile / Credits ──
    else if (pathParts === '/profile' && method === 'GET') {
      result = wrap(readers[0])
    }
    else if (pathParts === '/credits' && method === 'GET') {
      result = wrap(credits)
    }
    else if (pathParts === '/credits/logs' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(creditLogs.slice((current-1)*size, current*size), creditLogs.length, current, size)
    }

    // ── Compensations ──
    else if (pathParts === '/compensations' && method === 'GET') {
      const { current = 1, size = 10, status } = query
      let filtered = [...compensations]
      if (status) filtered = filtered.filter(c => c.status === status)
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts.match(/^\/compensations\/(\d+)$/) && method === 'GET') {
      result = wrap(compensations[0])
    }
    else if (pathParts === '/compensations' && method === 'POST') {
      const c = { id: ++seqCompensation, status: 'PENDING', createTime: new Date().toISOString(), ...body }
      compensations.push(c)
      result = wrap(c, '创建成功')
    }
    else if (pathParts.match(/^\/compensations\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/compensations\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts.match(/^\/compensations\/(\d+)\/approve$/) && method === 'POST') {
      result = wrap(null, '审批成功')
    }

    // ── Budget Funds ──
    else if (pathParts === '/budget-funds' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(budgets.slice((current-1)*size, current*size), budgets.length, current, size)
    }
    else if (pathParts.match(/^\/budget-funds\/(\d+)$/) && method === 'GET') {
      result = wrap(budgets[0])
    }
    else if (pathParts === '/budget-funds' && method === 'POST') {
      const b = { id: ++seqBudget, createTime: new Date().toISOString().slice(0, 10), ...body }
      budgets.push(b)
      result = wrap(b, '创建成功')
    }
    else if (pathParts.match(/^\/budget-funds\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/budget-funds\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Suggestions ──
    else if (pathParts === '/suggestions' && method === 'GET') {
      const { current = 1, size = 10, status } = query
      let filtered = [...suggestions]
      if (status) filtered = filtered.filter(s => s.status === status)
      result = wrapPage(filtered.slice((current-1)*size, current*size), filtered.length, current, size)
    }
    else if (pathParts.match(/^\/suggestions\/(\d+)$/) && method === 'GET') {
      result = wrap(suggestions[0])
    }
    else if (pathParts === '/suggestions' && method === 'POST') {
      const s = { id: ++seqSuggestion, status: 'PENDING', createTime: new Date().toISOString(), ...body }
      suggestions.push(s)
      result = wrap(s, '创建成功')
    }
    else if (pathParts.match(/^\/suggestions\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/suggestions\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts.match(/^\/suggestions\/(\d+)\/approve$/) && method === 'POST') {
      result = wrap(null, '审批成功')
    }

    // ── MARC ──
    else if (pathParts === '/marc/records' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(marcRecords.slice((current-1)*size, current*size), marcRecords.length, current, size)
    }
    else if (pathParts.match(/^\/marc\/records\/(\d+)$/) && method === 'GET') {
      result = wrap(marcRecords[0])
    }
    else if (pathParts === '/marc/records' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/marc\/records\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/marc\/records\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts === '/marc/frameworks' && method === 'GET') {
      result = wrap([])
    }
    else if (pathParts === '/z3950/search' && method === 'GET') {
      result = wrap({ results: [], total: 0 })
    }

    // ── Purchase Orders ──
    else if (pathParts === '/purchase-orders' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(purchaseOrders.slice((current-1)*size, current*size), purchaseOrders.length, current, size)
    }
    else if (pathParts.match(/^\/purchase-orders\/(\d+)$/) && method === 'GET') {
      result = wrap(purchaseOrders[0])
    }
    else if (pathParts === '/purchase-orders' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/purchase-orders\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/purchase-orders\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts.match(/^\/purchase-orders\/(\d+)\/approve$/) && method === 'POST') {
      result = wrap(null, '审批成功')
    }
    else if (pathParts === '/acquisition/items') {
      result = wrap([])
    }
    else if (pathParts.match(/^\/acquisition\/items\/\d+\/catalog$/) && method === 'POST') {
      result = wrap(null, '编目成功')
    }

    // ── Vendors ──
    else if (pathParts === '/vendors' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(vendors.slice((current-1)*size, current*size), vendors.length, current, size)
    }
    else if (pathParts.match(/^\/vendors\/(\d+)$/) && method === 'GET') {
      result = wrap(vendors[0])
    }
    else if (pathParts === '/vendors' && method === 'POST') {
      const v = { id: ++seqVendor, status: 'ACTIVE', createTime: new Date().toISOString().slice(0, 10), ...body }
      vendors.push(v)
      result = wrap(v, '创建成功')
    }
    else if (pathParts.match(/^\/vendors\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/vendors\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Serial ──
    else if (pathParts === '/serial/subscriptions' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(serialSubscriptions.slice((current-1)*size, current*size), serialSubscriptions.length, current, size)
    }
    else if (pathParts.match(/^\/serial\/subscriptions\/(\d+)$/) && method === 'GET') {
      result = wrap(serialSubscriptions[0])
    }
    else if (pathParts === '/serial/subscriptions' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/serial\/subscriptions\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/serial\/subscriptions\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts.match(/^\/serial\/subscriptions\/\d+\/generate-issues$/) && method === 'POST') {
      result = wrap(null, '预期到刊生成成功')
    }
    else if (pathParts === '/serial/issues' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(serialIssues.slice((current-1)*size, current*size), serialIssues.length, current, size)
    }
    else if (pathParts.match(/^\/serial\/issues\/\d+\/receive$/) && method === 'POST') {
      result = wrap(null, '到刊登记成功')
    }
    else if (pathParts.match(/^\/serial\/issues\/\d+\/missing$/) && method === 'POST') {
      result = wrap(null, '缺刊标记成功')
    }
    else if (pathParts === '/serial/issues/check-overdue' && method === 'POST') {
      result = wrap(1, '检查完成')
    }
    else if (pathParts === '/serial/claims' && method === 'GET') {
      result = wrapPage([], 0, 1, 10)
    }
    else if (pathParts.match(/^\/serial\/claims\/\d+$/) && method === 'GET') {
      result = wrap(null)
    }
    else if (pathParts === '/serial/claims' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/serial\/claims\/\d+$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/serial\/claims\/\d+\/resolve$/) && method === 'PUT') {
      result = wrap(null, ' 处理成功')
    }
    else if (pathParts.match(/^\/serial\/claims\/\d+\/close$/) && method === 'PUT') {
      result = wrap(null, '关闭成功')
    }
    else if (pathParts === '/serial-routings' && method === 'GET') {
      result = wrapPage([], 0, 1, 10)
    }
    else if (pathParts.match(/^\/serial-routings\/\d+$/) && method === 'GET') {
      result = wrap(null)
    }
    else if (pathParts === '/serial-routings' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts === '/serial-routings/batch' && method === 'POST') {
      result = wrap(null, '批量创建成功')
    }
    else if (pathParts.match(/^\/serial-routings\/\d+\/send$/) && method === 'PUT') {
      result = wrap(null, '发出成功')
    }
    else if (pathParts.match(/^\/serial-routings\/\d+\/deliver$/) && method === 'PUT') {
      result = wrap(null, '签收成功')
    }
    else if (pathParts.match(/^\/serial-routings\/\d+$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }
    else if (pathParts === '/serial-routings/templates' && method === 'GET') {
      result = wrap([])
    }
    else if (pathParts === '/serial-routings/templates' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/serial-routings\/templates\/\d+$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Branches ──
    else if (pathParts === '/branches' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(branches.slice((current-1)*size, current*size), branches.length, current, size)
    }
    else if (pathParts.match(/^\/branches\/(\d+)$/) && method === 'GET') {
      result = wrap(branches[0])
    }
    else if (pathParts === '/branches' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/branches\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/branches\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Reports ──
    else if (pathParts === '/reports' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(reports.slice((current-1)*size, current*size), reports.length, current, size)
    }
    else if (pathParts.match(/^\/reports\/(\d+)$/) && method === 'GET') {
      result = wrap(reports[0])
    }
    else if (pathParts === '/reports' && method === 'POST') {
      result = wrap(body, '创建成功')
    }
    else if (pathParts.match(/^\/reports\/(\d+)$/) && method === 'PUT') {
      result = wrap(body, '更新成功')
    }
    else if (pathParts.match(/^\/reports\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Statistics ──
    else if (pathParts === '/statistics/overview' && method === 'GET') {
      result = wrap(statistics)
    }
    else if (pathParts === '/statistics' && method === 'GET') {
      result = wrap(statistics)
    }
    else if (pathParts === '/statistics/borrows/monthly' && method === 'GET') {
      result = wrap(statistics.monthlyBorrows)
    }
    else if (pathParts === '/statistics/categories' && method === 'GET') {
      result = wrap(statistics.categoryDistribution)
    }
    else if (pathParts === '/statistics/popular-books' && method === 'GET') {
      result = wrap(statistics.popularBooks)
    }

    // ── Notifications ──
    else if (pathParts === '/notifications/unread-count' && method === 'GET') {
      result = wrap(notifications.filter(n => n.status === 'UNREAD').length)
    }
    else if (pathParts === '/notifications' && method === 'GET') {
      const { current = 1, size = 10 } = query
      result = wrapPage(notifications.slice((current-1)*size, current*size), notifications.length, current, size)
    }
    else if (pathParts === '/notifications/read-all' && method === 'POST') {
      notifications.forEach(n => n.status = 'READ')
      result = wrap(null, '全部已读')
    }
    else if (pathParts.match(/^\/notifications\/(\d+)\/read$/) && method === 'POST') {
      const id = Number(pathParts.match(/\/notifications\/(\d+)\/read$/)[1])
      const n = notifications.find(n => n.id === id)
      if (n) n.status = 'READ'
      result = wrap(null, '已读')
    }

    // ── Digital Resources ──
    else if (pathParts === '/digital-resources' && method === 'GET') {
      const resources = [
        { id: 1, title: '图书馆数字化指南.pdf', type: 'PDF', size: '2.5MB', url: '/files/mock.pdf', createTime: '2024-05-01' },
        { id: 2, title: '计算机科学经典论文合集.epub', type: 'EPUB', size: '5.1MB', url: '/files/mock.epub', createTime: '2024-05-10' },
        { id: 3, title: '读者手册.pdf', type: 'PDF', size: '1.2MB', url: '/files/mock.pdf', createTime: '2024-06-01' },
      ]
      result = wrapPage(resources, resources.length, 1, 10)
    }
    else if (pathParts.match(/^\/digital-resources\/(\d+)$/) && method === 'GET') {
      result = wrap({ id: 1, title: '图书馆数字化指南.pdf', type: 'PDF', size: '2.5MB', url: '/files/mock.pdf' })
    }
    else if (pathParts === '/digital-resources' && method === 'POST') {
      result = wrap(body, '上传成功')
    }
    else if (pathParts.match(/^\/digital-resources\/(\d+)$/) && method === 'DELETE') {
      result = wrap(null, '删除成功')
    }

    // ── Unified Search ──
    else if (pathParts === '/search' && method === 'GET') {
      const { current = 1, size = 10, keyword } = query
      let results = []
      if (keyword) {
        results = books.filter(b => b.title.includes(keyword) || b.author.includes(keyword) || b.isbn.includes(keyword))
          .map(b => ({ ...b, type: 'BOOK', score: 0.9 }))
      }
      result = wrapPage(results, results.length, current, size)
    }

    // ── 404 fallback ──
    else {
      res.statusCode = 404
      result = { code: 404, message: 'Not Found: ' + pathParts, data: null }
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    res.writeHead(res.statusCode || 200)
    res.end(JSON.stringify(result))
  } catch (e) {
    res.writeHead(500)
    res.end(JSON.stringify({ code: 500, message: e.message, data: null }))
  }
}

// ============ Start Server ============
const server = http.createServer(async (req, res) => {
  const body = ['POST', 'PUT'].includes(req.method) ? await parseBody(req) : {}
  const query = getQuery(req.url)
  handleRoute(req, res, body, query)
})

server.listen(PORT, () => {
  console.log(`\n  🔧 Mock API Server running at http://localhost:${PORT}\n`)
  console.log(`  📚 模拟数据已就绪: ${books.length}本图书, ${readers.length}位读者, ${borrows.length}条借阅记录\n`)
  console.log(`  🔑 登录账号: admin / 任意密码 (管理员)`)
  console.log(`  🔑 登录账号: reader01 / 任意密码 (读者)\n`)
})
