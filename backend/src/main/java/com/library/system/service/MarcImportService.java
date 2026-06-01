package com.library.system.service;

import com.library.system.dto.MarcRecordResponse;

import java.io.InputStream;
import java.util.List;

public interface MarcImportService {

    List<MarcRecordResponse> previewImport(InputStream inputStream, int previewCount) throws Exception;

    List<MarcRecordResponse> importRecords(InputStream inputStream) throws Exception;
}
