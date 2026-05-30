it('all API modules should export expected functions', async () => {
  // Auth API
  const auth = await import('@/api/auth')
  expect(auth.login).toBeTypeOf('function')
  expect(auth.logout).toBeTypeOf('function')
  expect(auth.getUserInfo).toBeTypeOf('function')
  expect(auth.register).toBeTypeOf('function')
  expect(auth.getCaptcha).toBeTypeOf('function')

  // Book API
  const book = await import('@/api/book')
  expect(book.getBookList).toBeTypeOf('function')
  expect(book.getBookDetail).toBeTypeOf('function')
  expect(book.createBook).toBeTypeOf('function')
  expect(book.updateBook).toBeTypeOf('function')
  expect(book.deleteBook).toBeTypeOf('function')

  // Borrow API
  const borrow = await import('@/api/borrow')
  expect(borrow.borrowBook).toBeTypeOf('function')
  expect(borrow.returnBook).toBeTypeOf('function')
  expect(borrow.renewBook).toBeTypeOf('function')

  // Seat API
  const seat = await import('@/api/seat')
  expect(seat.getSeatMap).toBeTypeOf('function')
  expect(seat.getSeatDetail).toBeTypeOf('function')
  expect(seat.reserveSeat).toBeTypeOf('function')
  expect(seat.checkIn).toBeTypeOf('function')
  expect(seat.checkOut).toBeTypeOf('function')
  expect(seat.cancelReserve).toBeTypeOf('function')
  expect(seat.getMyReservations).toBeTypeOf('function')

  // Compensation API
  const compensation = await import('@/api/compensation')
  expect(compensation.createCompensation).toBeTypeOf('function')

  // Credit API
  const credit = await import('@/api/credit')
  expect(credit.getCreditInfo).toBeTypeOf('function')

  // Reader API
  const reader = await import('@/api/reader')
  expect(reader.getReaderList).toBeTypeOf('function')

  // Announcement API
  const announcement = await import('@/api/announcement')
  expect(announcement.getAnnouncementList).toBeTypeOf('function')

  // Statistics API
  const statistics = await import('@/api/statistics')
  expect(statistics.getStatisticsOverview).toBeTypeOf('function')

  // Volunteer API
  const volunteer = await import('@/api/volunteer')
  expect(volunteer.getVolunteerList).toBeTypeOf('function')
})
