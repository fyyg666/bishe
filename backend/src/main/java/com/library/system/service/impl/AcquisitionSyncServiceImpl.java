package com.library.system.service.impl;

import com.library.system.common.Constants;
import com.library.system.config.BloomFilterConfig;
import com.library.system.dto.IsbnLookupResponse;
import com.library.system.entity.Book;
import com.library.system.entity.PurchaseOrderItem;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.PurchaseOrderItemMapper;
import com.library.system.service.AcquisitionSyncService;
import com.library.system.service.IsbnLookupService;
import com.library.system.service.MarcBookSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcquisitionSyncServiceImpl implements AcquisitionSyncService {

    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final BookMapper bookMapper;
    private final IsbnLookupService isbnLookupService;
    private final MarcBookSyncService marcBookSyncService;
    private final BloomFilterConfig bloomFilterConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveToCatalog(Long orderItemId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "入库数量必须大于0");
        }

        PurchaseOrderItem item = purchaseOrderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "采购明细不存在: " + orderItemId);
        }

        int remaining = item.getReceivedQuantity() - item.getCatalogedQuantity();
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "该采购明细已全部入库");
        }
        if (quantity > remaining) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR,
                    "入库数量超过可入库数量，剩余可入库: " + remaining);
        }

        Book book = null;
        if (item.getIsbn() != null && !item.getIsbn().isBlank()) {
            book = bookMapper.selectByIsbn(item.getIsbn());
        }

        if (book != null) {
            book.setTotalCount(book.getTotalCount() + quantity);
            book.setAvailableCount(book.getAvailableCount() + quantity);
            bookMapper.updateById(book);
            log.info("采购入库-图书库存增加: bookId={}, isbn={}, +{}本", book.getId(), book.getIsbn(), quantity);
        } else {
            Book.BookBuilder builder = Book.builder()
                    .isbn(item.getIsbn())
                    .title(item.getBookTitle())
                    .author(item.getAuthor())
                    .publisher(item.getPublisher())
                    .price(item.getUnitPrice())
                    .totalCount(quantity)
                    .availableCount(quantity)
                    .borrowCount(0)
                    .status(Constants.BookStatus.NORMAL);

            if (item.getIsbn() != null && !item.getIsbn().isBlank()) {
                enrichFromIsbnLookup(builder, item.getIsbn());
            }

            book = builder.build();
            bookMapper.insert(book);
            bloomFilterConfig.addBook(String.valueOf(book.getId()));
            log.info("采购入库-新建图书: bookId={}, isbn={}, title={}", book.getId(), book.getIsbn(), book.getTitle());

            try {
                marcBookSyncService.bookToMarc(book.getId());
                log.info("采购入库-自动创建MARC记录: bookId={}", book.getId());
            } catch (Exception e) {
                log.warn("采购入库-MARC记录创建失败(不影响入库): bookId={}, error={}", book.getId(), e.getMessage());
            }
        }

        item.setCatalogedQuantity(item.getCatalogedQuantity() + quantity);
        if (item.getCatalogedQuantity() >= item.getReceivedQuantity()) {
            item.setStatus("CATALOGED");
        }
        purchaseOrderItemMapper.updateById(item);

        log.info("采购入库完成: orderItemId={}, quantity={}, bookId={}", orderItemId, quantity, book.getId());
    }

    private void enrichFromIsbnLookup(Book.BookBuilder builder, String isbn) {
        try {
            Optional<IsbnLookupResponse> lookup = isbnLookupService.lookup(isbn);
            if (lookup.isPresent()) {
                IsbnLookupResponse info = lookup.get();
                if (info.getAuthor() != null && builder.build().getAuthor() == null) {
                    builder.author(info.getAuthor());
                }
                if (info.getPublisher() != null && builder.build().getPublisher() == null) {
                    builder.publisher(info.getPublisher());
                }
                if (info.getDescription() != null) {
                    builder.description(info.getDescription());
                }
                if (info.getCoverUrl() != null) {
                    builder.coverImage(info.getCoverUrl());
                }
                log.info("采购入库-ISBN查询补充信息: isbn={}, source={}", isbn, info.getSource());
            }
        } catch (Exception e) {
            log.warn("采购入库-ISBN查询失败(不影响入库): isbn={}, error={}", isbn, e.getMessage());
        }
    }
}
