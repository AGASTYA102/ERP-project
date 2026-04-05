package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.Client;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Product;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.ClientRepository;
import com.erp.manufacturing.repository.OrderRepository;
import com.erp.manufacturing.repository.ProductRepository;
import com.erp.manufacturing.service.OrderLogService;
import com.erp.manufacturing.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final OrderLogService orderLogService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ClientRepository clientRepository,
                            ProductRepository productRepository,
                            OrderLogService orderLogService) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.orderLogService = orderLogService;
    }

    @Override
    @Transactional
    public OrderEntity createOrder(OrderEntity order, String username) {
        // Validation check for empty objects
        if (order == null || order.getClient() == null || order.getClient().getId() == null ||
            order.getProduct() == null || order.getProduct().getId() == null) {
            throw new IllegalArgumentException("Invalid order: Client and Product are required");
        }

        // Validate client exists
        Client client = clientRepository.findById(order.getClient().getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + order.getClient().getId()));
        order.setClient(client);

        // Validate product exists
        Product product = productRepository.findById(order.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + order.getProduct().getId()));
        order.setProduct(product);

        // Set defaults
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderStatus.CREATED);

        OrderEntity savedOrder = orderRepository.save(order);

        // Log creation
        orderLogService.logAction(savedOrder, "Order created with status CREATED", username);
        log.info("Order {} created with status {}", savedOrder.getId(), OrderStatus.CREATED);

        // Transition to DESIGN_PENDING
        savedOrder.setStatus(OrderStatus.DESIGN_PENDING);
        orderRepository.save(savedOrder);
        orderLogService.logAction(savedOrder, "Status changed to DESIGN_PENDING — sent to Design team", username);
        log.info("Order {} moved from {} to {}", savedOrder.getId(), OrderStatus.CREATED, OrderStatus.DESIGN_PENDING);

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String action, String username) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        orderLogService.logAction(order, action + " — Status updated to " + newStatus.name(), username);
        log.info("Order {} moved from {} to {}", orderId, oldStatus, newStatus);
    }
}
