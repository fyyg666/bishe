package com.library.system.service;

import java.io.OutputStream;
import java.util.List;

public interface MarcExportService {

    void exportRecords(List<Long> recordIds, OutputStream outputStream) throws Exception;
}
