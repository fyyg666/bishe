package com.library.system.service.impl;

import java.util.List;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.ReportTemplateRequest;
import com.library.system.dto.ReportTemplateResponse;
import com.library.system.entity.ReportTemplate;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.ReportTemplateMapper;
import com.library.system.service.ReportTemplateService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplateServiceImpl implements ReportTemplateService {

    private final ReportTemplateMapper reportTemplateMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE"
    );

    private static final Pattern PARAM_PATTERN = Pattern.compile(":(\\w+)");

    @Override
    public List<ReportTemplateResponse> listTemplates(String category) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ReportTemplate::getCategory, category);
        }
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        List<ReportTemplate> templates = reportTemplateMapper.selectList(wrapper);
        return templates.stream().map(this::convertToResponse).toList();
    }

    @Override
    public ReportTemplateResponse getTemplate(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null || template.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND, "报表模板不存在");
        }
        return convertToResponse(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateResponse createTemplate(ReportTemplateRequest request, Long userId) {
        validateSql(request.getSqlTemplate());

        ReportTemplate template = new ReportTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSqlTemplate(request.getSqlTemplate());
        template.setParameters(request.getParameters());
        template.setCategory(request.getCategory());
        template.setCreatedBy(userId);

        reportTemplateMapper.insert(template);
        log.info("报表模板创建成功: {}", template.getName());
        return convertToResponse(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateResponse updateTemplate(Long id, ReportTemplateRequest request) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null || template.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND, "报表模板不存在");
        }

        validateSql(request.getSqlTemplate());

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSqlTemplate(request.getSqlTemplate());
        template.setParameters(request.getParameters());
        template.setCategory(request.getCategory());

        reportTemplateMapper.updateById(template);
        log.info("报表模板更新成功: id={}", id);
        return convertToResponse(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null || template.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND, "报表模板不存在");
        }
        reportTemplateMapper.deleteById(id);
        log.info("报表模板删除成功: id={}", id);
    }

    @Override
    public List<Map<String, Object>> executeTemplate(Long id, Map<String, Object> params) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null || template.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND, "报表模板不存在");
        }

        validateSql(template.getSqlTemplate());

        String sql = resolveParams(template.getSqlTemplate(), params);
        log.debug("执行报表SQL: {}", sql);

        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("报表执行失败: id={}, sql={}", id, sql, e);
            throw new BusinessException(ErrorCode.REPORT_EXECUTE_FAILED, "报表执行失败: " + e.getMessage());
        }
    }

    @Override
    public void exportToExcel(Long id, Map<String, Object> params, OutputStream outputStream) {
        List<Map<String, Object>> data = executeTemplate(id, params);

        if (data.isEmpty()) {
            EasyExcel.write(outputStream).sheet("报表数据").doWrite(Collections.emptyList());
            return;
        }

        List<String> headers = new ArrayList<>(data.get(0).keySet());
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : data) {
            List<Object> rowData = new ArrayList<>();
            for (String header : headers) {
                rowData.add(row.get(header));
            }
            rows.add(rowData);
        }

        List<List<String>> head = headers.stream().map(Collections::singletonList).toList();

        EasyExcel.write(outputStream)
                .head(head)
                .sheet("报表数据")
                .doWrite(rows);
    }

    @Override
    public void exportToCsv(Long id, Map<String, Object> params, OutputStream outputStream) {
        List<Map<String, Object>> data = executeTemplate(id, params);

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');

            if (!data.isEmpty()) {
                List<String> headers = new ArrayList<>(data.get(0).keySet());
                writer.write(String.join(",", headers));
                writer.write('\n');

                for (Map<String, Object> row : data) {
                    List<String> values = new ArrayList<>();
                    for (String header : headers) {
                        Object value = row.get(header);
                        values.add(escapeCsvValue(value != null ? String.valueOf(value) : ""));
                    }
                    writer.write(String.join(",", values));
                    writer.write('\n');
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_EXECUTE_FAILED, "CSV导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportToPdf(Long id, Map<String, Object> params, OutputStream outputStream) {
        ReportTemplateResponse template = getTemplate(id);
        List<Map<String, Object>> data = executeTemplate(id, params);

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font headerFont = new Font(bf, 10, Font.BOLD);
            Font bodyFont = new Font(bf, 9, Font.NORMAL);

            Paragraph title = new Paragraph(template.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph timestamp = new Paragraph(
                    "生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    bodyFont);
            timestamp.setAlignment(Element.ALIGN_RIGHT);
            document.add(timestamp);
            document.add(Chunk.NEWLINE);

            if (!data.isEmpty()) {
                List<String> headers = new ArrayList<>(data.get(0).keySet());
                PdfPTable table = new PdfPTable(headers.size());
                table.setWidthPercentage(100);

                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(new Color(220, 220, 220));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                for (Map<String, Object> row : data) {
                    for (String header : headers) {
                        Object value = row.get(header);
                        String cellValue = value != null ? String.valueOf(value) : "";
                        table.addCell(new Phrase(cellValue, bodyFont));
                    }
                }

                document.add(table);
            }

            document.close();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_EXECUTE_FAILED, "PDF导出失败: " + e.getMessage());
        }
    }

    private String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void validateSql(String sql) {
        String upperSql = sql.toUpperCase().trim();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                throw new BusinessException(ErrorCode.REPORT_SQL_INVALID,
                        "报表SQL包含禁止的关键字: " + keyword);
            }
        }
    }

    private String resolveParams(String sqlTemplate, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return sqlTemplate;
        }

        Matcher matcher = PARAM_PATTERN.matcher(sqlTemplate);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            if (value != null) {
                String safeValue = sanitizeValue(value);
                matcher.appendReplacement(result, safeValue);
            } else {
                matcher.appendReplacement(result, "NULL");
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String sanitizeValue(Object value) {
        if (value instanceof Number) {
            return value.toString();
        }
        String strValue = String.valueOf(value);
        return "'" + strValue.replace("'", "''") + "'";
    }

    private ReportTemplateResponse convertToResponse(ReportTemplate template) {
        return ReportTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .sqlTemplate(template.getSqlTemplate())
                .parameters(template.getParameters())
                .category(template.getCategory())
                .createdBy(template.getCreatedBy())
                .createTime(template.getCreateTime())
                .build();
    }
}
