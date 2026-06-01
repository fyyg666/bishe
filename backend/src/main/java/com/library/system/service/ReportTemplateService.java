package com.library.system.service;

import com.library.system.dto.ReportTemplateRequest;
import com.library.system.dto.ReportTemplateResponse;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ReportTemplateService {

    List<ReportTemplateResponse> listTemplates(String category);

    ReportTemplateResponse getTemplate(Long id);

    ReportTemplateResponse createTemplate(ReportTemplateRequest request, Long userId);

    ReportTemplateResponse updateTemplate(Long id, ReportTemplateRequest request);

    void deleteTemplate(Long id);

    List<Map<String, Object>> executeTemplate(Long id, Map<String, Object> params);

    void exportToExcel(Long id, Map<String, Object> params, OutputStream outputStream);

    void exportToCsv(Long id, Map<String, Object> params, OutputStream outputStream);

    void exportToPdf(Long id, Map<String, Object> params, OutputStream outputStream);
}
