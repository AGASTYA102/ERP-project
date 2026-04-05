package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.Accounts;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.AccountsRepository;
import com.erp.manufacturing.service.AccountsService;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.util.WorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountsServiceImpl implements AccountsService {

    private final AccountsRepository accountsRepository;
    private final OrderService orderService;

    public AccountsServiceImpl(AccountsRepository accountsRepository, OrderService orderService) {
        this.accountsRepository = accountsRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public void finalizeOrder(Accounts accounts, Long orderId, String username) {
        if (accounts == null || accounts.getRate() == null) {
            throw new IllegalArgumentException("Account details and rate are required");
        }

        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        WorkflowValidator.validateTransition(order.getStatus(), OrderStatus.COMPLETED);

        // Link accounts to order
        accounts.setOrder(order);

        // Calculate total amount ALWAYS on server-side
        if (order.getQuantity() != null) {
            BigDecimal total = accounts.getRate().multiply(BigDecimal.valueOf(order.getQuantity()));
            accounts.setTotalAmount(total);
        } else {
            throw new IllegalArgumentException("Order quantity is missing, cannot calculate total amount");
        }

        accountsRepository.save(accounts);
        log.info("Order {} - Invoice generated. Total Amount: {}", orderId, accounts.getTotalAmount());

        // Close the order
        orderService.updateOrderStatus(orderId, OrderStatus.CLOSED,
                "Invoice generated. Bill: " + accounts.getBillNumber()
                        + ", Amount: " + accounts.getTotalAmount()
                        + ", Payment: " + accounts.getPaymentStatus(),
                username);
    }
}

