package com.library.system.service.impl;

import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.MarcRecordResponse;
import com.library.system.service.MarcImportService;
import com.library.system.service.MarcRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarcImportServiceImpl implements MarcImportService {

    private final MarcRecordService marcRecordService;

    private static final char FIELD_TERMINATOR = 0x1E;
    private static final char RECORD_TERMINATOR = 0x1D;
    private static final char SUBFIELD_DELIMITER = 0x1F;

    @Override
    public List<MarcRecordResponse> previewImport(InputStream inputStream, int previewCount) throws Exception {
        List<byte[]> rawRecords = readRawRecords(inputStream);
        List<MarcRecordResponse> result = new ArrayList<>();
        int count = Math.min(rawRecords.size(), previewCount);
        for (int i = 0; i < count; i++) {
            try {
                MarcRecordRequest request = parseIso2709(rawRecords.get(i));
                MarcRecordResponse response = convertToPreviewResponse(request);
                result.add(response);
            } catch (Exception e) {
                log.warn("预览第{}条记录解析失败: {}", i + 1, e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MarcRecordResponse> importRecords(InputStream inputStream) throws Exception {
        List<byte[]> rawRecords = readRawRecords(inputStream);
        List<MarcRecordResponse> result = new ArrayList<>();
        for (int i = 0; i < rawRecords.size(); i++) {
            try {
                MarcRecordRequest request = parseIso2709(rawRecords.get(i));
                MarcRecordResponse response = marcRecordService.createRecord(request);
                result.add(response);
            } catch (Exception e) {
                log.warn("导入第{}条记录失败: {}", i + 1, e.getMessage());
            }
        }
        log.info("MARC导入完成: 总数={}, 成功={}", rawRecords.size(), result.size());
        return result;
    }

    private List<byte[]> readRawRecords(InputStream inputStream) throws IOException {
        List<byte[]> records = new ArrayList<>();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] readBuffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(readBuffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                buffer.write(readBuffer[i]);
                if (readBuffer[i] == RECORD_TERMINATOR) {
                    records.add(buffer.toByteArray());
                    buffer.reset();
                }
            }
        }
        return records;
    }

    private MarcRecordRequest parseIso2709(byte[] rawRecord) {
        String recordStr = new String(rawRecord, StandardCharsets.UTF_8);

        if (recordStr.length() < 24) {
            throw new IllegalArgumentException("MARC记录头标区不足24字节");
        }

        String leader = recordStr.substring(0, 24);
        int baseAddress;
        try {
            baseAddress = Integer.parseInt(leader.substring(12, 17).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的基地址: " + leader.substring(12, 17));
        }

        String directoryStr = recordStr.substring(24, baseAddress);
        String variableFieldsStr = recordStr.substring(baseAddress);

        List<MarcRecordRequest.FieldRequest> fields = new ArrayList<>();
        String controlNumber = null;

        for (int i = 0; i + 12 <= directoryStr.length(); i += 12) {
            String entry = directoryStr.substring(i, i + 12);
            String tag = entry.substring(0, 3);
            int fieldLength = Integer.parseInt(entry.substring(3, 7).trim());
            int fieldOffset = Integer.parseInt(entry.substring(7, 12).trim());

            String fieldData = variableFieldsStr.substring(fieldOffset,
                    Math.min(fieldOffset + fieldLength, variableFieldsStr.length()));
            fieldData = fieldData.replace(String.valueOf((char) FIELD_TERMINATOR), "").trim();

            if (tag.compareTo("010") < 0) {
                MarcRecordRequest.FieldRequest field = MarcRecordRequest.FieldRequest.builder()
                        .tag(tag)
                        .indicator1(" ")
                        .indicator2(" ")
                        .displayValue(fieldData)
                        .sortOrder(fields.size())
                        .build();
                fields.add(field);
                if ("001".equals(tag)) {
                    controlNumber = fieldData;
                }
            } else {
                String ind1 = fieldData.length() > 0 ? String.valueOf(fieldData.charAt(0)) : " ";
                String ind2 = fieldData.length() > 1 ? String.valueOf(fieldData.charAt(1)) : " ";

                String subfieldData = fieldData.length() > 2 ? fieldData.substring(2) : "";
                List<Subfield> subfields = parseSubfields(subfieldData);

                String displayValue = subfields.stream()
                        .filter(sf -> sf.value != null && !sf.value.isEmpty())
                        .map(sf -> "$" + sf.code + sf.value)
                        .reduce((a, b) -> a + " " + b)
                        .orElse("");

                String subfieldsJson = subfields.stream()
                        .map(sf -> "{\"code\":\"" + sf.code + "\",\"value\":\"" + escapeJson(sf.value) + "\"}")
                        .reduce((a, b) -> a + "," + b)
                        .map(s -> "[" + s + "]")
                        .orElse("[]");

                MarcRecordRequest.FieldRequest field = MarcRecordRequest.FieldRequest.builder()
                        .tag(tag)
                        .indicator1(ind1)
                        .indicator2(ind2)
                        .subfields(subfieldsJson)
                        .displayValue(displayValue)
                        .sortOrder(fields.size())
                        .build();
                fields.add(field);
            }
        }

        return MarcRecordRequest.builder()
                .recordType("BIB")
                .leader(leader)
                .controlNumber(controlNumber)
                .fields(fields)
                .build();
    }

    private List<Subfield> parseSubfields(String data) {
        List<Subfield> result = new ArrayList<>();
        if (data == null || data.isEmpty()) return result;

        String[] parts = data.split(String.valueOf((char) SUBFIELD_DELIMITER));
        for (String part : parts) {
            if (part.isEmpty()) continue;
            char code = part.charAt(0);
            String value = part.length() > 1 ? part.substring(1) : "";
            result.add(new Subfield(code, value));
        }
        return result;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private MarcRecordResponse convertToPreviewResponse(MarcRecordRequest request) {
        List<MarcRecordResponse.MarcFieldDto> fieldDtos = request.getFields().stream()
                .map(f -> MarcRecordResponse.MarcFieldDto.builder()
                        .tag(f.getTag())
                        .indicator1(f.getIndicator1())
                        .indicator2(f.getIndicator2())
                        .subfields(f.getSubfields())
                        .displayValue(f.getDisplayValue())
                        .sortOrder(f.getSortOrder())
                        .build())
                .toList();

        return MarcRecordResponse.builder()
                .recordType(request.getRecordType())
                .leader(request.getLeader())
                .controlNumber(request.getControlNumber())
                .fields(fieldDtos)
                .build();
    }

    private record Subfield(char code, String value) {}
}
