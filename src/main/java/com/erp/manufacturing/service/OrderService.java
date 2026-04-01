package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderEntity createOrder(OrderEntity order, String username);
    void punchOrder(OrderEntity order, String username);
    List<OrderEntity> getAllOrders();
    Optional<OrderEntity> getOrderById(Long id);
    List<OrderEntity> getOrdersByStatus(OrderStatus status);
    void updateOrderStatus(Long orderId, OrderStatus newStatus, String action, String username);
    void saveOrderManually(OrderEntity order);
}
