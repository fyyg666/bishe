package com.library.system.service;

import com.library.system.dto.MarcFrameworkResponse;

import java.util.List;

public interface MarcFrameworkService {

    List<MarcFrameworkResponse> listFrameworks();

    MarcFrameworkResponse getFramework(Long id);

    MarcFrameworkResponse getFrameworkByCode(String code);

    MarcFrameworkResponse createFramework(MarcFrameworkResponse request);

    MarcFrameworkResponse updateFramework(Long id, MarcFrameworkResponse request);

    void deleteFramework(Long id);
}
