package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.common.Constants;
import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.MarcRecordResponse;
import com.library.system.entity.Book;
import com.library.system.entity.MarcRecord;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.MarcRecordMapper;
import com.library.system.service.MarcBookSyncService;
import com.library.system.service.MarcRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarcBookSyncServiceImpl implements MarcBookSyncService {

    private final BookMapper bookMapper;
    private final MarcRecordMapper marcRecordMapper;
    private final MarcRecordService marcRecordService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarcRecordResponse bookToMarc(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("图书不存在: " + bookId);
        }

        List<MarcRecordRequest.FieldRequest> fields = new ArrayList<>();

        fields.add(MarcRecordRequest.FieldRequest.builder()
                .tag("001")
                .displayValue(String.valueOf(book.getId()))
                .sortOrder(0)
                .build());

        List<Map<String, String>> sub010 = new ArrayList<>();
        if (book.getIsbn() != null) {
            sub010.add(Map.of("code", "a", "value", book.getIsbn()));
        }
        if (book.getPrice() != null) {
            sub010.add(Map.of("code", "d", "value", "CNY" + book.getPrice().toPlainString()));
        }
        if (!sub010.isEmpty()) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("010")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(sub010))
                    .sortOrder(1)
                    .build());
        }

        if (book.getAuthor() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("100")
                    .indicator1("1")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getAuthor()))))
                    .sortOrder(2)
                    .build());
        }

        if (book.getTitle() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("245")
                    .indicator1("1")
                    .indicator2("0")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getTitle()))))
                    .sortOrder(3)
                    .build());
        }

        List<Map<String, String>> sub260 = new ArrayList<>();
        if (book.getPublisher() != null) {
            sub260.add(Map.of("code", "a", "value", book.getPublisher()));
        }
        if (book.getPublishDate() != null) {
            sub260.add(Map.of("code", "c", "value", book.getPublishDate().toString()));
        }
        if (!sub260.isEmpty()) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("260")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(sub260))
                    .sortOrder(4)
                    .build());
        }

        if (book.getDescription() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("300")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getDescription()))))
                    .sortOrder(5)
                    .build());
        }

        if (book.getLocation() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("905")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getLocation()))))
                    .sortOrder(6)
                    .build());
        }

        MarcRecordRequest request = MarcRecordRequest.builder()
                .recordType("BIBLIOGRAPHIC")
                .leader("00000nam a2200000 a 4500")
                .controlNumber(String.valueOf(book.getId()))
                .bookId(bookId)
                .fields(fields)
                .build();

        return marcRecordService.createRecord(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void marcToBook(Long marcRecordId) {
        MarcRecordResponse marc = marcRecordService.getRecord(marcRecordId);

        Map<String, MarcRecordResponse.MarcFieldDto> fieldMap = marc.getFields().stream()
                .collect(Collectors.toMap(
                        MarcRecordResponse.MarcFieldDto::getTag,
                        f -> f,
                        (a, b) -> a));

        String isbn = extractSubfield(fieldMap.get("010"), "a");
        String author = extractSubfield(fieldMap.get("100"), "a");
        String title = extractSubfield(fieldMap.get("245"), "a");
        String publisher = extractSubfield(fieldMap.get("260"), "a");
        String publishDateStr = extractSubfield(fieldMap.get("260"), "c");
        String description = extractSubfield(fieldMap.get("300"), "a");
        String location = extractSubfield(fieldMap.get("905"), "a");

        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .publisher(publisher)
                .publishDate(parseDate(publishDateStr))
                .description(description)
                .location(location)
                .totalCount(1)
                .availableCount(1)
                .borrowCount(0)
                .status(Constants.BookStatus.NORMAL)
                .build();

        bookMapper.insert(book);

        marcRecordService.updateRecord(marcRecordId, MarcRecordRequest.builder()
                .recordType(marc.getRecordType())
                .leader(marc.getLeader())
                .controlNumber(marc.getControlNumber())
                .bookId(book.getId())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("图书不存在: " + bookId);
        }

        MarcRecord existing = findMarcByBookId(bookId);
        if (existing != null) {
            MarcRecordRequest updateReq = buildMarcRequestFromBook(book);
            marcRecordService.updateRecord(existing.getId(), updateReq);
        } else {
            bookToMarc(bookId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromMarc(Long marcRecordId) {
        MarcRecordResponse marc = marcRecordService.getRecord(marcRecordId);

        Map<String, MarcRecordResponse.MarcFieldDto> fieldMap = marc.getFields().stream()
                .collect(Collectors.toMap(
                        MarcRecordResponse.MarcFieldDto::getTag,
                        f -> f,
                        (a, b) -> a));

        String isbn = extractSubfield(fieldMap.get("010"), "a");
        String author = extractSubfield(fieldMap.get("100"), "a");
        String title = extractSubfield(fieldMap.get("245"), "a");
        String publisher = extractSubfield(fieldMap.get("260"), "a");
        String publishDateStr = extractSubfield(fieldMap.get("260"), "c");
        String description = extractSubfield(fieldMap.get("300"), "a");
        String location = extractSubfield(fieldMap.get("905"), "a");

        if (marc.getBookId() != null) {
            Book book = bookMapper.selectById(marc.getBookId());
            if (book != null) {
                if (isbn != null) book.setIsbn(isbn);
                if (author != null) book.setAuthor(author);
                if (title != null) book.setTitle(title);
                if (publisher != null) book.setPublisher(publisher);
                if (publishDateStr != null) book.setPublishDate(parseDate(publishDateStr));
                if (description != null) book.setDescription(description);
                if (location != null) book.setLocation(location);
                bookMapper.updateById(book);
                return;
            }
        }

        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .publisher(publisher)
                .publishDate(parseDate(publishDateStr))
                .description(description)
                .location(location)
                .totalCount(1)
                .availableCount(1)
                .borrowCount(0)
                .status(Constants.BookStatus.NORMAL)
                .build();

        bookMapper.insert(book);

        marcRecordService.updateRecord(marcRecordId, MarcRecordRequest.builder()
                .recordType(marc.getRecordType())
                .leader(marc.getLeader())
                .controlNumber(marc.getControlNumber())
                .bookId(book.getId())
                .build());
    }

    private MarcRecord findMarcByBookId(Long bookId) {
        LambdaQueryWrapper<MarcRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarcRecord::getBookId, bookId)
                .last("LIMIT 1");
        return marcRecordMapper.selectOne(wrapper);
    }

    private MarcRecordRequest buildMarcRequestFromBook(Book book) {
        List<MarcRecordRequest.FieldRequest> fields = new ArrayList<>();

        fields.add(MarcRecordRequest.FieldRequest.builder()
                .tag("001")
                .displayValue(String.valueOf(book.getId()))
                .sortOrder(0)
                .build());

        List<Map<String, String>> sub010 = new ArrayList<>();
        if (book.getIsbn() != null) {
            sub010.add(Map.of("code", "a", "value", book.getIsbn()));
        }
        if (book.getPrice() != null) {
            sub010.add(Map.of("code", "d", "value", "CNY" + book.getPrice().toPlainString()));
        }
        if (!sub010.isEmpty()) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("010")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(sub010))
                    .sortOrder(1)
                    .build());
        }

        if (book.getAuthor() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("100")
                    .indicator1("1")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getAuthor()))))
                    .sortOrder(2)
                    .build());
        }

        if (book.getTitle() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("245")
                    .indicator1("1")
                    .indicator2("0")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getTitle()))))
                    .sortOrder(3)
                    .build());
        }

        List<Map<String, String>> sub260 = new ArrayList<>();
        if (book.getPublisher() != null) {
            sub260.add(Map.of("code", "a", "value", book.getPublisher()));
        }
        if (book.getPublishDate() != null) {
            sub260.add(Map.of("code", "c", "value", book.getPublishDate().toString()));
        }
        if (!sub260.isEmpty()) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("260")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(sub260))
                    .sortOrder(4)
                    .build());
        }

        if (book.getDescription() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("300")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getDescription()))))
                    .sortOrder(5)
                    .build());
        }

        if (book.getLocation() != null) {
            fields.add(MarcRecordRequest.FieldRequest.builder()
                    .tag("905")
                    .indicator1(" ")
                    .indicator2(" ")
                    .subfields(toJson(List.of(Map.of("code", "a", "value", book.getLocation()))))
                    .sortOrder(6)
                    .build());
        }

        return MarcRecordRequest.builder()
                .recordType("BIBLIOGRAPHIC")
                .leader("00000nam a2200000 a 4500")
                .controlNumber(String.valueOf(book.getId()))
                .bookId(book.getId())
                .fields(fields)
                .build();
    }

    private String extractSubfield(MarcRecordResponse.MarcFieldDto field, String code) {
        if (field == null || field.getSubfields() == null) {
            return null;
        }
        try {
            List<Map<String, String>> subs = objectMapper.readValue(
                    field.getSubfields(), new TypeReference<List<Map<String, String>>>() {});
            return subs.stream()
                    .filter(s -> code.equals(s.get("code")))
                    .map(s -> s.get("value"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("解析子字段失败: tag={}, code={}, error={}", field.getTag(), code, e.getMessage());
            return null;
        }
    }

    private String toJson(List<Map<String, String>> subfields) {
        try {
            return objectMapper.writeValueAsString(subfields);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化子字段失败", e);
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.length() >= 10) {
                return LocalDate.parse(dateStr.substring(0, 10));
            } else if (dateStr.length() >= 7) {
                return LocalDate.parse(dateStr.substring(0, 7) + "-01");
            } else if (dateStr.length() >= 4) {
                return LocalDate.of(Integer.parseInt(dateStr.substring(0, 4)), 1, 1);
            }
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
        }
        return null;
    }
}
