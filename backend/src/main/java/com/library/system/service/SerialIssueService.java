package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.entity.SerialIssue;

public interface SerialIssueService {

    PageResult<SerialIssue> listIssues(Long subscriptionId, Long current, Long size);

    SerialIssue receiveIssue(Long issueId);

    SerialIssue markMissing(Long issueId);

    int checkOverdueIssues();
}
