package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.Design;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.DesignRepository;
import com.erp.manufacturing.service.DesignService;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.util.WorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DesignServiceImpl implements DesignService {

    private final DesignRepository designRepository;
    private final OrderService orderService;

    public DesignServiceImpl(DesignRepository designRepository, OrderService orderService) {
        this.designRepository = designRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public void submitDesign(Design design, Long orderId, String username) {
        if (design == null || design.getDesignFilePath() == null || design.getDesignFilePath().isEmpty()) {
            throw new IllegalArgumentException("Design details and file path cannot be null or empty");
        }

        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Validate status — design can only be submitted when DESIGN_PENDING
        WorkflowValidator.validateTransition(order.getStatus(), OrderStatus.DESIGN_PENDING);

        // Link design to order and save
        design.setOrder(order);
        designRepository.save(design);

        // Transition: DESIGN_PENDING → DESIGN_COMPLETED → PURCHASE_PENDING
        log.info("Design submitted for order {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.DESIGN_COMPLETED,
                "Design submitted successfully", username);
        orderService.updateOrderStatus(orderId, OrderStatus.PURCHASE_PENDING,
                "Sent to Purchase Department for material check", username);
    }
}
