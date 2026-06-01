package com.library.system.service;

import com.library.system.dto.BookReservationResponse;
import com.library.system.dto.PageResult;

public interface BookReservationService {

    BookReservationResponse createReservation(Long userId, Long bookId);

    void cancelReservation(Long userId, Long reservationId);

    PageResult<BookReservationResponse> getMyReservations(Long userId, Long current, Long size);

    void notifyNextInQueue(Long bookId);
}
