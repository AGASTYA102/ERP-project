package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.Production;

public interface ProductionService {
    void updateProduction(Production production, Long orderId, String username);
}
