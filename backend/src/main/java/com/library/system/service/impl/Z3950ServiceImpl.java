package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.MarcRecordRequest;
import com.library.system.dto.Z3950SearchResult;
import com.library.system.entity.Z3950Source;
import com.library.system.mapper.Z3950SourceMapper;
import com.library.system.service.MarcRecordService;
import com.library.system.service.Z3950Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Z3950ServiceImpl implements Z3950Service {

    private final Z3950SourceMapper z3950SourceMapper;
    private final MarcRecordService marcRecordService;
    private final RestTemplate restTemplate;

    @Override
    public List<Z3950Source> listSources() {
        return z3950SourceMapper.selectList(new LambdaQueryWrapper<Z3950Source>()
                .eq(Z3950Source::getDeleted, 0)
                .orderByAsc(Z3950Source::getId));
    }

    @Override
    public Z3950Source getSource(Long id) {
        return z3950SourceMapper.selectById(id);
    }

    @Override
    public Z3950Source createSource(Z3950Source source) {
        z3950SourceMapper.insert(source);
        return source;
    }

    @Override
    public Z3950Source updateSource(Long id, Z3950Source source) {
        source.setId(id);
        z3950SourceMapper.updateById(source);
        return source;
    }

    @Override
    public void deleteSource(Long id) {
        z3950SourceMapper.deleteById(id);
    }

    @Override
    public Z3950SearchResult search(Long sourceId, String query, String queryType, int maxResults) {
        Z3950Source source = getSource(sourceId);
        if (source == null || !source.getEnabled()) {
            return Z3950SearchResult.builder().sourceName("Unknown").totalResults(0).records(List.of()).build();
        }

        try {
            String sruQuery = buildSruQuery(query, queryType);
            String url = String.format("http://%s:%d/%s?version=1.1&operation=searchRetrieve&query=%s&maximumRecords=%d&recordSchema=marcxml",
                    source.getHost(), source.getPort(), source.getDatabase(),
                    URLEncoder.encode(sruQuery, StandardCharsets.UTF_8), maxResults);

            String response = restTemplate.getForObject(url, String.class);
            return parseSruResponse(source.getName(), response);
        } catch (Exception e) {
            log.warn("Z39.50查询失败: source={}, query={}, error={}", source.getName(), query, e.getMessage());
            return Z3950SearchResult.builder().sourceName(source.getName()).totalResults(0).records(List.of()).build();
        }
    }

    @Override
    public List<Z3950SearchResult> searchAll(String query, String queryType, int maxResults) {
        List<Z3950Source> sources = listSources().stream()
                .filter(Z3950Source::getEnabled)
                .toList();
        List<Z3950SearchResult> results = new ArrayList<>();
        for (Z3950Source source : sources) {
            results.add(search(source.getId(), query, queryType, maxResults));
        }
        return results;
    }

    @Override
    public void importToMarc(Long sourceId, String query, String queryType) {
        Z3950SearchResult result = search(sourceId, query, queryType, 50);
        for (Z3950SearchResult.MarcRecordResponse record : result.getRecords()) {
            try {
                MarcRecordRequest request = MarcRecordRequest.builder()
                        .recordType("BIB")
                        .controlNumber(record.getIsbn())
                        .fields(List.of(
                                MarcRecordRequest.FieldRequest.builder().tag("010").indicator1(" ").indicator2(" ")
                                        .subfields("[{\"code\":\"a\",\"value\":\"" + escapeJson(record.getIsbn()) + "\"}]")
                                        .displayValue("$a" + record.getIsbn()).sortOrder(1).build(),
                                MarcRecordRequest.FieldRequest.builder().tag("100").indicator1("1").indicator2(" ")
                                        .subfields("[{\"code\":\"a\",\"value\":\"" + escapeJson(record.getAuthor()) + "\"}]")
                                        .displayValue("$a" + record.getAuthor()).sortOrder(2).build(),
                                MarcRecordRequest.FieldRequest.builder().tag("245").indicator1("1").indicator2("0")
                                        .subfields("[{\"code\":\"a\",\"value\":\"" + escapeJson(record.getTitle()) + "\"}]")
                                        .displayValue("$a" + record.getTitle()).sortOrder(3).build(),
                                MarcRecordRequest.FieldRequest.builder().tag("260").indicator1(" ").indicator2(" ")
                                        .subfields("[{\"code\":\"a\",\"value\":\"" + escapeJson(record.getPublisher()) + "\"},{\"code\":\"c\",\"value\":\"" + escapeJson(record.getPublishDate()) + "\"}]")
                                        .displayValue("$a" + record.getPublisher() + " $c" + record.getPublishDate()).sortOrder(4).build()
                        ))
                        .build();
                marcRecordService.createRecord(request);
            } catch (Exception e) {
                log.warn("Z39.50记录导入失败: title={}, error={}", record.getTitle(), e.getMessage());
            }
        }
        log.info("Z39.50批量导入完成: sourceId={}, count={}", sourceId, result.getRecords().size());
    }

    private String buildSruQuery(String query, String queryType) {
        return switch (queryType != null ? queryType : "keyword") {
            case "title" -> "dc.title=\"" + query + "\"";
            case "author" -> "dc.creator=\"" + query + "\"";
            case "isbn" -> "dc.identifier=\"" + query + "\"";
            default -> "dc.title=\"" + query + "\" or dc.creator=\"" + query + "\" or dc.identifier=\"" + query + "\"";
        };
    }

    private Z3950SearchResult parseSruResponse(String sourceName, String xmlResponse) {
        List<Z3950SearchResult.MarcRecordResponse> records = new ArrayList<>();
        int totalResults = 0;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

            NodeList numberRecords = doc.getElementsByTagName("numberOfRecords");
            if (numberRecords.getLength() > 0) {
                totalResults = Integer.parseInt(numberRecords.item(0).getTextContent().trim());
            }

            NodeList recordNodes = doc.getElementsByTagName("record");
            for (int i = 0; i < recordNodes.getLength(); i++) {
                Element record = (Element) recordNodes.item(i);
                Z3950SearchResult.MarcRecordResponse.MarcRecordResponseBuilder recBuilder = Z3950SearchResult.MarcRecordResponse.builder();

                NodeList dataFields = record.getElementsByTagName("datafield");
                for (int j = 0; j < dataFields.getLength(); j++) {
                    Element dataField = (Element) dataFields.item(j);
                    String tag = dataField.getAttribute("tag");

                    NodeList subFields = dataField.getElementsByTagName("subfield");
                    for (int k = 0; k < subFields.getLength(); k++) {
                        Element subField = (Element) subFields.item(k);
                        String code = subField.getAttribute("code");
                        String value = subField.getTextContent().trim();

                        if ("245".equals(tag) && "a".equals(code)) recBuilder.title(value);
                        if ("100".equals(tag) && "a".equals(code)) recBuilder.author(value);
                        if ("020".equals(tag) && "a".equals(code)) recBuilder.isbn(value);
                        if ("260".equals(tag) && "a".equals(code)) recBuilder.publisher(value);
                        if ("260".equals(tag) && "c".equals(code)) recBuilder.publishDate(value);
                    }
                }

                records.add(recBuilder.build());
            }
        } catch (Exception e) {
            log.warn("SRU响应解析失败: {}", e.getMessage());
        }

        return Z3950SearchResult.builder()
                .sourceName(sourceName)
                .totalResults(totalResults)
                .records(records)
                .build();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
