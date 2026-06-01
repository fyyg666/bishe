package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.entity.Vendor;

public interface VendorService {

    PageResult<Vendor> listVendors(Long current, Long size, String keyword, String status);

    Vendor getVendorById(Long id);

    Vendor createVendor(Vendor vendor);

    Vendor updateVendor(Long id, Vendor vendor);

    void deleteVendor(Long id);
}
