package com.library.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.MarcRecordResponse;
import com.library.system.service.MarcExportService;
import com.library.system.service.MarcRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarcExportServiceImpl implements MarcExportService {

    private final MarcRecordService marcRecordService;
    private final ObjectMapper objectMapper;

    private static final char FIELD_TERMINATOR = 0x1E;
    private static final char RECORD_TERMINATOR = 0x1D;
    private static final char SUBFIELD_DELIMITER = 0x1F;

    @Override
    public void exportRecords(List<Long> recordIds, OutputStream outputStream) throws Exception {
        for (Long id : recordIds) {
            MarcRecordResponse record = marcRecordService.getRecord(id);
            byte[] iso2709 = convertToIso2709(record);
            outputStream.write(iso2709);
        }
        outputStream.flush();
        log.info("MARC导出完成: count={}", recordIds.size());
    }

    private byte[] convertToIso2709(MarcRecordResponse record) {
        StringBuilder variableFields = new StringBuilder();
        StringBuilder directory = new StringBuilder();

        int offset = 0;
        for (MarcRecordResponse.MarcFieldDto field : record.getFields()) {
            StringBuilder fieldData = new StringBuilder();

            if (field.getTag().compareTo("010") < 0) {
                fieldData.append(field.getDisplayValue() != null ? field.getDisplayValue() : "");
            } else {
                fieldData.append(field.getIndicator1() != null ? field.getIndicator1() : " ");
                fieldData.append(field.getIndicator2() != null ? field.getIndicator2() : " ");

                if (field.getSubfields() != null && !field.getSubfields().isEmpty()) {
                    try {
                        List<Map<String, String>> subfields = objectMapper.readValue(
                                field.getSubfields(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                        for (Map<String, String> entry : subfields) {
                            fieldData.append((char) SUBFIELD_DELIMITER);
                            fieldData.append(entry.getOrDefault("code", "a"));
                            fieldData.append(entry.getOrDefault("value", ""));
                        }
                    } catch (Exception e) {
                        log.warn("子字段解析失败: tag={}", field.getTag());
                    }
                }
            }

            fieldData.append((char) FIELD_TERMINATOR);
            String fieldStr = fieldData.toString();
            int fieldLength = fieldStr.getBytes(StandardCharsets.UTF_8).length;

            directory.append(String.format("%-3s%04d%05d", field.getTag(), fieldLength, offset));
            variableFields.append(fieldStr);
            offset += fieldLength;
        }

        variableFields.append((char) RECORD_TERMINATOR);
        String variableStr = variableFields.toString();

        String leader = record.getLeader();
        if (leader == null || leader.length() < 24) {
            leader = "00000nam  2200000   4500";
        }

        int baseAddress = 24 + directory.length();
        int recordLength = 24 + directory.length() + variableStr.getBytes(StandardCharsets.UTF_8).length;

        leader = String.format("%05d", recordLength) + leader.substring(5, 12)
                + String.format("%05d", baseAddress) + leader.substring(17);

        String fullRecord = leader + directory.toString() + (char) FIELD_TERMINATOR + variableStr;
        return fullRecord.getBytes(StandardCharsets.UTF_8);
    }
}
