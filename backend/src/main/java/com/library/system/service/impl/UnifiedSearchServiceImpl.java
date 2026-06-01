package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.PageResult;
import com.library.system.dto.UnifiedSearchResponse;
import com.library.system.entity.Book;
import com.library.system.entity.DigitalResource;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.DigitalResourceMapper;
import com.library.system.service.UnifiedSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedSearchServiceImpl implements UnifiedSearchService {

    private final BookMapper bookMapper;
    private final DigitalResourceMapper digitalResourceMapper;

    @Override
    public PageResult<UnifiedSearchResponse> search(String keyword, String resourceType, Long current, Long size) {
        List<UnifiedSearchResponse> allResults = new ArrayList<>();

        boolean includePrint = !"DIGITAL".equals(resourceType);
        boolean includeDigital = !"PRINT".equals(resourceType);

        if (includePrint) {
            allResults.addAll(searchBooks(keyword));
        }
        if (includeDigital) {
            allResults.addAll(searchDigitalResources(keyword));
        }

        allResults.sort(Comparator.comparingInt(r -> 0));

        long total = allResults.size();
        long fromIndex = (current - 1) * size;
        long toIndex = Math.min(fromIndex + size, total);

        List<UnifiedSearchResponse> pagedRecords;
        if (fromIndex >= total) {
            pagedRecords = List.of();
        } else {
            pagedRecords = allResults.subList((int) fromIndex, (int) toIndex);
        }

        return PageResult.of(current, size, total, pagedRecords);
    }

    private List<UnifiedSearchResponse> searchBooks(String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getDeleted, 0);
        wrapper.eq(Book::getStatus, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        wrapper.orderByDesc(Book::getBorrowCount);
        List<Book> books = bookMapper.selectList(wrapper);
        return books.stream().map(book -> UnifiedSearchResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .resourceType("PRINT")
                .format("PAPER")
                .coverUrl(book.getCoverImage())
                .availableCount(book.getAvailableCount())
                .build()
        ).collect(Collectors.toList());
    }

    private List<UnifiedSearchResponse> searchDigitalResources(String keyword) {
        LambdaQueryWrapper<DigitalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DigitalResource::getDeleted, 0);
        wrapper.eq(DigitalResource::getStatus, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DigitalResource::getTitle, keyword)
                    .or().like(DigitalResource::getAuthor, keyword)
                    .or().like(DigitalResource::getIsbn, keyword));
        }
        wrapper.orderByDesc(DigitalResource::getBorrowCount);
        List<DigitalResource> resources = digitalResourceMapper.selectList(wrapper);
        return resources.stream().map(dr -> UnifiedSearchResponse.builder()
                .id(dr.getId())
                .title(dr.getTitle())
                .author(dr.getAuthor())
                .isbn(dr.getIsbn())
                .resourceType(dr.getResourceType())
                .format(dr.getFormat())
                .coverUrl(dr.getCoverUrl())
                .accessUrl(dr.getAccessUrl())
                .availableCount(null)
                .build()
        ).collect(Collectors.toList());
    }
}
