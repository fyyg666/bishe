package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseOrderRequest;
import com.library.system.dto.PurchaseOrderResponse;
import com.library.system.entity.PurchaseOrder;
import com.library.system.entity.PurchaseOrderItem;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.PurchaseOrderItemMapper;
import com.library.system.mapper.PurchaseOrderMapper;
import com.library.system.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse createOrder(PurchaseOrderRequest request) {
        String orderNo = "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(orderNo);
        order.setVendorId(request.getVendorId());
        order.setStatus("DRAFT");
        order.setTotalAmount(totalAmount);
        order.setRemark(request.getRemark());
        purchaseOrderMapper.insert(order);

        for (PurchaseOrderRequest.ItemRequest itemReq : request.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(order.getId());
            item.setBookTitle(itemReq.getBookTitle());
            item.setIsbn(itemReq.getIsbn());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setReceivedQuantity(0);
            purchaseOrderItemMapper.insert(item);
        }

        log.info("采购订单创建成功: orderNo={}", orderNo);
        return getOrder(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse updateOrder(Long id, PurchaseOrderRequest request) {
        PurchaseOrder order = getExistingOrder(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "只有草稿状态的订单可以编辑");
        }

        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setVendorId(request.getVendorId());
        order.setTotalAmount(totalAmount);
        order.setRemark(request.getRemark());
        purchaseOrderMapper.updateById(order);

        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));

        for (PurchaseOrderRequest.ItemRequest itemReq : request.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(id);
            item.setBookTitle(itemReq.getBookTitle());
            item.setIsbn(itemReq.getIsbn());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setReceivedQuantity(0);
            purchaseOrderItemMapper.insert(item);
        }

        log.info("采购订单更新成功: id={}", id);
        return getOrder(id);
    }

    @Override
    public PurchaseOrderResponse getOrder(Long id) {
        PurchaseOrder order = getExistingOrder(id);
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        PurchaseOrder order = getExistingOrder(id);
        if (!"DRAFT".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "只有草稿或已取消的订单可以删除");
        }
        purchaseOrderMapper.deleteById(id);
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));
        log.info("采购订单删除成功: id={}", id);
    }

    @Override
    public PageResult<PurchaseOrderResponse> listOrders(Long current, Long size, String status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PurchaseOrder::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseOrder::getCreateTime);

        Page<PurchaseOrder> page = new Page<>(current, size);
        Page<PurchaseOrder> result = purchaseOrderMapper.selectPage(page, wrapper);

        List<PurchaseOrderResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse submitForApproval(Long id) {
        PurchaseOrder order = getExistingOrder(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "只有草稿状态的订单可以提交审批");
        }
        order.setStatus("PENDING_APPROVAL");
        purchaseOrderMapper.updateById(order);
        log.info("采购订单提交审批: id={}", id);
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse approveOrder(Long id) {
        PurchaseOrder order = getExistingOrder(id);
        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "只有待审批的订单可以审批");
        }
        order.setStatus("APPROVED");
        order.setApproveTime(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        log.info("采购订单审批通过: id={}", id);
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse receiveItems(Long orderId, Long itemId, int receivedQty) {
        PurchaseOrder order = getExistingOrder(orderId);
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIAL_RECEIVED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "只有已审批或部分收货的订单可以收货");
        }

        PurchaseOrderItem item = purchaseOrderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new ResourceNotFoundException(ErrorCode.PURCHASE_ORDER_ITEM_NOT_FOUND, "采购明细不存在");
        }

        item.setReceivedQuantity(item.getReceivedQuantity() + receivedQty);
        purchaseOrderItemMapper.updateById(item);

        List<PurchaseOrderItem> allItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));

        boolean allReceived = allItems.stream()
                .allMatch(i -> i.getReceivedQuantity() >= i.getQuantity());
        boolean anyReceived = allItems.stream()
                .anyMatch(i -> i.getReceivedQuantity() > 0);

        if (allReceived) {
            order.setStatus("COMPLETED");
        } else if (anyReceived) {
            order.setStatus("PARTIAL_RECEIVED");
        }
        purchaseOrderMapper.updateById(order);

        log.info("采购订单收货: orderId={}, itemId={}, receivedQty={}", orderId, itemId, receivedQty);
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponse cancelOrder(Long id) {
        PurchaseOrder order = getExistingOrder(id);
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_STATUS_ERROR, "已完成或已取消的订单不能取消");
        }
        order.setStatus("CANCELLED");
        purchaseOrderMapper.updateById(order);
        log.info("采购订单取消: id={}", id);
        return convertToResponse(order);
    }

    private PurchaseOrder getExistingOrder(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.PURCHASE_ORDER_NOT_FOUND, "采购订单不存在");
        }
        return order;
    }

    private PurchaseOrderResponse convertToResponse(PurchaseOrder order) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, order.getId()));

        List<PurchaseOrderResponse.OrderItemDto> itemDtos = items.stream()
                .map(item -> PurchaseOrderResponse.OrderItemDto.builder()
                        .id(item.getId())
                        .bookTitle(item.getBookTitle())
                        .isbn(item.getIsbn())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .receivedQuantity(item.getReceivedQuantity())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .vendorId(order.getVendorId())
                .vendorName("")
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .remark(order.getRemark())
                .createTime(order.getCreateTime())
                .approveTime(order.getApproveTime())
                .updateTime(order.getUpdateTime())
                .items(itemDtos)
                .build();
    }
}
