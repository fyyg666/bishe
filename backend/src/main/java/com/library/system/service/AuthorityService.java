package com.library.system.service;

import com.library.system.dto.AuthorityRecordRequest;
import com.library.system.dto.AuthorityRecordResponse;
import com.library.system.dto.PageResult;

import java.util.List;

public interface AuthorityService {

    PageResult<AuthorityRecordResponse> listAuthorities(Long current, Long size, String authorityType, String keyword);

    AuthorityRecordResponse getAuthority(Long id);

    AuthorityRecordResponse createAuthority(AuthorityRecordRequest request);

    AuthorityRecordResponse updateAuthority(Long id, AuthorityRecordRequest request);

    void deleteAuthority(Long id);

    List<AuthorityRecordResponse> searchByHeading(String authorityType, String keyword);
}
