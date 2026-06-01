package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseOrderRequest;
import com.library.system.dto.PurchaseOrderResponse;

public interface PurchaseOrderService {

    PurchaseOrderResponse createOrder(PurchaseOrderRequest request);

    PurchaseOrderResponse updateOrder(Long id, PurchaseOrderRequest request);

    PurchaseOrderResponse getOrder(Long id);

    void deleteOrder(Long id);

    PageResult<PurchaseOrderResponse> listOrders(Long current, Long size, String status);

    PurchaseOrderResponse submitForApproval(Long id);

    PurchaseOrderResponse approveOrder(Long id);

    PurchaseOrderResponse receiveItems(Long orderId, Long itemId, int receivedQty);

    PurchaseOrderResponse cancelOrder(Long id);
}
