package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.entity.SerialSubscription;

public interface SerialSubscriptionService {

    PageResult<SerialSubscription> listSubscriptions(Long current, Long size, String status);

    SerialSubscription getSubscription(Long id);

    SerialSubscription createSubscription(SerialSubscription subscription);

    SerialSubscription updateSubscription(Long id, SerialSubscription subscription);

    void deleteSubscription(Long id);

    void generateExpectedIssues(Long subscriptionId);
}
