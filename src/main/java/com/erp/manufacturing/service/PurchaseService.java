package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.Stock;

import java.util.List;

public interface PurchaseService {
    void checkAndProcessPurchase(Long orderId, String materialName, Double requiredQty, String username);
    List<Stock> getAllStock();
}
