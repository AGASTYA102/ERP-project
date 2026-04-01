package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Purchase;
import com.erp.manufacturing.entity.Stock;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.PurchaseRepository;
import com.erp.manufacturing.repository.StockRepository;
import com.erp.manufacturing.service.OrderLogService;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.PurchaseService;
import com.erp.manufacturing.util.WorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final StockRepository stockRepository;
    private final OrderService orderService;
    private final OrderLogService orderLogService;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               StockRepository stockRepository,
                               OrderService orderService,
                               OrderLogService orderLogService) {
        this.purchaseRepository = purchaseRepository;
        this.stockRepository = stockRepository;
        this.orderService = orderService;
        this.orderLogService = orderLogService;
    }

    @Override
    @Transactional
    public void checkAndProcessPurchase(Long orderId, String materialName, Double requiredQty, String username) {
        if (materialName == null || materialName.trim().isEmpty() || requiredQty == null || requiredQty <= 0) {
            throw new IllegalArgumentException("Valid materialName and positive requiredQty are mandatory");
        }

        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Status guard — only allow processing if order is in PURCHASE_PENDING
        WorkflowValidator.validateTransition(order.getStatus(), OrderStatus.PURCHASE_PENDING);

        Optional<Stock> stockOpt = stockRepository.findByMaterialName(materialName);
        double available = stockOpt.map(Stock::getQuantity).orElse(0.0);

        if (available < requiredQty) {
            // Insufficient stock -> create purchase entry
            log.warn("[Order {}] Insufficient stock for {}. Required: {}, Available: {}. Creating Purchase Entry.", orderId, materialName, requiredQty, available);
            Purchase purchase = Purchase.builder()
                    .order(order)
                    .materialName(materialName)
                    .requiredQty(requiredQty)
                    .availableQty(available)
                    .vendorName(null)
                    .status("PENDING")
                    .build();
            purchaseRepository.save(purchase);

            orderLogService.logAction(order,
                    "Purchase raised for " + materialName
                            + " (required: " + requiredQty + ", available: " + available + ")",
                    username);
        } else {
            // Sufficient stock -> deduct and move to production
            log.info("[Order {}] Sufficient stock for {}. Deducting {} unit(s).", orderId, materialName, requiredQty);
            Stock stock = stockOpt.get();
            stock.setQuantity(stock.getQuantity() - requiredQty);
            stockRepository.save(stock);

            orderService.updateOrderStatus(orderId, OrderStatus.READY_FOR_PRODUCTION,
                    "Stock verified and deducted for " + materialName, username);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stock> getAllStock() {
        return stockRepository.findAll();
    }
}
