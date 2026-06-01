package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.SerialClaimRequest;
import com.library.system.dto.SerialClaimResponse;

public interface SerialClaimService {

    PageResult<SerialClaimResponse> listClaims(Long current, Long size, String claimStatus, String claimType);

    SerialClaimResponse getClaim(Long id);

    SerialClaimResponse createClaim(SerialClaimRequest request, Long operatorId);

    SerialClaimResponse updateClaim(Long id, SerialClaimRequest request);

    void resolveClaim(Long id, String resolution);

    void closeClaim(Long id);

    int checkAndAutoClaim();
}
