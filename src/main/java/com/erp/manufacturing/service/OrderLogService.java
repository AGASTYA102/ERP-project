package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.OrderLog;

import java.util.List;

public interface OrderLogService {
    void logAction(OrderEntity order, String action, String username);
    List<OrderLog> getLogsForOrder(Long orderId);
}
