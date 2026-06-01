package com.library.system.service;

import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.MarcRecordResponse;

import java.util.List;

public interface MarcRecordService {

    MarcRecordResponse createRecord(MarcRecordRequest request);

    MarcRecordResponse getRecord(Long id);

    MarcRecordResponse updateRecord(Long id, MarcRecordRequest request);

    void deleteRecord(Long id);

    List<MarcRecordResponse> listRecords(Long current, Long size, String recordType, String keyword);

    MarcRecordResponse getRecordByBookId(Long bookId);

    void linkToBook(Long recordId, Long bookId);
}
