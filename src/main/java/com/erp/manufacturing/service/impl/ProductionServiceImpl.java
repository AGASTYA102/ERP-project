package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Production;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.ProductionRepository;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.ProductionService;
import com.erp.manufacturing.util.WorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProductionServiceImpl implements ProductionService {

    private final ProductionRepository productionRepository;
    private final OrderService orderService;

    public ProductionServiceImpl(ProductionRepository productionRepository, OrderService orderService) {
        this.productionRepository = productionRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public void updateProduction(Production production, Long orderId, String username) {
        if (production == null) {
            throw new IllegalArgumentException("Production details cannot be null");
        }

        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Status guard — only allow if order is READY_FOR_PRODUCTION or IN_PRODUCTION
        WorkflowValidator.validateTransition(order.getStatus(), OrderStatus.READY_FOR_PRODUCTION, OrderStatus.IN_PRODUCTION);

        production.setOrder(order);
        productionRepository.save(production);

        // Determine final status based on completion flags
        boolean isComplete = Boolean.TRUE.equals(production.getPrintingDone())
                && Boolean.TRUE.equals(production.getCorrugationDone())
                && production.getQuantityProduced() != null
                && production.getQuantityProduced() > 0;

        if (isComplete) {
            log.info("Order {} - Production completed. Produced {} units.", orderId, production.getQuantityProduced());
            orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED,
                    "Production completed. Qty produced: " + production.getQuantityProduced()
                            + ", Truck: " + production.getTruckNumber(),
                    username);
        } else {
            log.info("Order {} - Production in progress. Printing Done: {}, Corrugation Done: {}", orderId, production.getPrintingDone(), production.getCorrugationDone());
            if (order.getStatus() == OrderStatus.READY_FOR_PRODUCTION) {
                orderService.updateOrderStatus(orderId, OrderStatus.IN_PRODUCTION,
                        "Production started. Printing: " + production.getPrintingDone()
                                + ", Corrugation: " + production.getCorrugationDone(),
                        username);
            } else {
                orderService.updateOrderStatus(orderId, OrderStatus.IN_PRODUCTION,
                        "Production in progress. Printing: " + production.getPrintingDone()
                                + ", Corrugation: " + production.getCorrugationDone(),
                        username);
            }
        }
    }
}
