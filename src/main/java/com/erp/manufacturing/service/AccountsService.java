package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.Accounts;

public interface AccountsService {
    void finalizeOrder(Accounts accounts, Long orderId, String username);
}
