package com.library.system.service;

public interface AcquisitionSyncService {
    void receiveToCatalog(Long orderItemId, int quantity);
}
