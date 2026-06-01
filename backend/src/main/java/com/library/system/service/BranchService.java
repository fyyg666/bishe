package com.library.system.service;

import com.library.system.dto.BranchResponse;
import com.library.system.dto.BranchRequest;

import java.util.List;

public interface BranchService {

    List<BranchResponse> listBranches(String status);

    List<BranchResponse> listBranchTree();

    BranchResponse getBranch(Long id);

    BranchResponse createBranch(BranchRequest request);

    BranchResponse updateBranch(Long id, BranchRequest request);

    void deleteBranch(Long id);

    List<BranchResponse> getSubBranches(Long parentId);
}
