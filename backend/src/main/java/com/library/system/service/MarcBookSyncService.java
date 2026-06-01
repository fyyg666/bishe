package com.library.system.service;

import com.library.system.dto.MarcRecordResponse;

public interface MarcBookSyncService {

    MarcRecordResponse bookToMarc(Long bookId);

    void marcToBook(Long marcRecordId);

    void syncFromBook(Long bookId);

    void syncFromMarc(Long marcRecordId);
}
