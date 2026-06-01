package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        boolean includePrint = !"DIGITAL".equals(resourceType);
        boolean includeDigital = !"PRINT".equals(resourceType);

        long pCount = includePrint ? countBooks(keyword) : 0;
        long dCount = includeDigital ? countDigitalResources(keyword) : 0;
        long total = pCount + dCount;

        if (total == 0) {
            return PageResult.of(current, size, 0, List.of());
        }

        long fromIndex = (current - 1) * size;
        if (fromIndex >= total) {
            return PageResult.of(current, size, total, List.of());
        }
        long toIndex = Math.min(fromIndex + size, total);

        if (!includePrint || !includeDigital || pCount == 0 || dCount == 0) {
            if (!includeDigital || dCount == 0) {
                return PageResult.of(current, size, pCount, searchBooksPaged(keyword, current, size));
            } else {
                return PageResult.of(current, size, dCount, searchDigitalResourcesPaged(keyword, current, size));
            }
        }

        long minCount = Math.min(pCount, dCount);
        long interleavedCount = 2 * minCount;
        boolean printIsLonger = pCount >= dCount;

        long pStart = -1, pEnd = -1, dStart = -1, dEnd = -1;
        for (long i = fromIndex; i < toIndex; i++) {
            if (i < interleavedCount) {
                if (i % 2 == 0) {
                    long idx = i / 2;
                    if (pStart == -1) pStart = idx;
                    pEnd = idx;
                } else {
                    long idx = i / 2;
                    if (dStart == -1) dStart = idx;
                    dEnd = idx;
                }
            } else {
                long tailIdx = i - interleavedCount;
                if (printIsLonger) {
                    long idx = minCount + tailIdx;
                    if (pStart == -1) pStart = idx;
                    pEnd = idx;
                } else {
                    long idx = minCount + tailIdx;
                    if (dStart == -1) dStart = idx;
                    dEnd = idx;
                }
            }
        }

        List<UnifiedSearchResponse> printResults = pStart >= 0
                ? searchBooksRange(keyword, pStart, (int) (pEnd - pStart + 1))
                : List.of();
        List<UnifiedSearchResponse> digitalResults = dStart >= 0
                ? searchDigitalResourcesRange(keyword, dStart, (int) (dEnd - dStart + 1))
                : List.of();

        List<UnifiedSearchResponse> pageRecords = new ArrayList<>();
        int pIdx = 0, dIdx = 0;
        for (long i = fromIndex; i < toIndex; i++) {
            if (i < interleavedCount) {
                if (i % 2 == 0) {
                    pageRecords.add(printResults.get(pIdx++));
                } else {
                    pageRecords.add(digitalResults.get(dIdx++));
                }
            } else {
                if (printIsLonger) {
                    pageRecords.add(printResults.get(pIdx++));
                } else {
                    pageRecords.add(digitalResults.get(dIdx++));
                }
            }
        }

        return PageResult.of(current, size, total, pageRecords);
    }

    private long countBooks(String keyword) {
        return bookMapper.selectCount(buildBookWrapper(keyword));
    }

    private long countDigitalResources(String keyword) {
        return digitalResourceMapper.selectCount(buildDigitalResourceWrapper(keyword));
    }

    private List<UnifiedSearchResponse> searchBooksPaged(String keyword, long current, long size) {
        LambdaQueryWrapper<Book> wrapper = buildBookWrapper(keyword);
        wrapper.orderByDesc(Book::getBorrowCount);
        Page<Book> page = bookMapper.selectPage(new Page<>(current, size, false), wrapper);
        return page.getRecords().stream().map(this::toPrintResponse).collect(Collectors.toList());
    }

    private List<UnifiedSearchResponse> searchDigitalResourcesPaged(String keyword, long current, long size) {
        LambdaQueryWrapper<DigitalResource> wrapper = buildDigitalResourceWrapper(keyword);
        wrapper.orderByDesc(DigitalResource::getBorrowCount);
        Page<DigitalResource> page = digitalResourceMapper.selectPage(new Page<>(current, size, false), wrapper);
        return page.getRecords().stream().map(this::toDigitalResponse).collect(Collectors.toList());
    }

    private List<UnifiedSearchResponse> searchBooksRange(String keyword, long offset, int limit) {
        LambdaQueryWrapper<Book> wrapper = buildBookWrapper(keyword);
        wrapper.orderByDesc(Book::getBorrowCount);
        wrapper.last("LIMIT " + offset + ", " + limit);
        return bookMapper.selectList(wrapper).stream().map(this::toPrintResponse).collect(Collectors.toList());
    }

    private List<UnifiedSearchResponse> searchDigitalResourcesRange(String keyword, long offset, int limit) {
        LambdaQueryWrapper<DigitalResource> wrapper = buildDigitalResourceWrapper(keyword);
        wrapper.orderByDesc(DigitalResource::getBorrowCount);
        wrapper.last("LIMIT " + offset + ", " + limit);
        return digitalResourceMapper.selectList(wrapper).stream().map(this::toDigitalResponse).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<Book> buildBookWrapper(String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getDeleted, 0);
        wrapper.eq(Book::getStatus, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<DigitalResource> buildDigitalResourceWrapper(String keyword) {
        LambdaQueryWrapper<DigitalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DigitalResource::getDeleted, 0);
        wrapper.eq(DigitalResource::getStatus, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DigitalResource::getTitle, keyword)
                    .or().like(DigitalResource::getAuthor, keyword)
                    .or().like(DigitalResource::getIsbn, keyword));
        }
        return wrapper;
    }

    private UnifiedSearchResponse toPrintResponse(Book book) {
        return UnifiedSearchResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .resourceType("PRINT")
                .format("PAPER")
                .coverUrl(book.getCoverImage())
                .availableCount(book.getAvailableCount())
                .build();
    }

    private UnifiedSearchResponse toDigitalResponse(DigitalResource dr) {
        return UnifiedSearchResponse.builder()
                .id(dr.getId())
                .title(dr.getTitle())
                .author(dr.getAuthor())
                .isbn(dr.getIsbn())
                .resourceType(dr.getResourceType())
                .format(dr.getFormat())
                .coverUrl(dr.getCoverUrl())
                .accessUrl(dr.getAccessUrl())
                .availableCount(null)
                .build();
    }
}
