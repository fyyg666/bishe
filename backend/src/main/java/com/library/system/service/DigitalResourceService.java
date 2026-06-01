package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.entity.DigitalResource;

import java.util.List;

public interface DigitalResourceService {

    PageResult<DigitalResource> list(Long current, Long size, String keyword, String resourceType);

    DigitalResource getById(Long id);

    DigitalResource create(DigitalResource resource);

    DigitalResource update(Long id, DigitalResource resource);

    void delete(Long id);

    List<DigitalResource> search(String keyword);
}
