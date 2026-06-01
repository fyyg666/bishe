package com.library.system.service;

import com.library.system.dto.Z3950SearchResult;
import com.library.system.entity.Z3950Source;
import java.util.List;

public interface Z3950Service {

    List<Z3950Source> listSources();

    Z3950Source getSource(Long id);

    Z3950Source createSource(Z3950Source source);

    Z3950Source updateSource(Long id, Z3950Source source);

    void deleteSource(Long id);

    Z3950SearchResult search(Long sourceId, String query, String queryType, int maxResults);

    List<Z3950SearchResult> searchAll(String query, String queryType, int maxResults);

    void importToMarc(Long sourceId, String query, String queryType);
}
