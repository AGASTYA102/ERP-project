package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.*;
import com.erp.manufacturing.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// TODO: remove before production
@RestController
@RequestMapping("/test/workflow")
@Slf4j
public class TestWorkflowController {

    private final OrderService orderService;
    private final DesignService designService;
    private final PurchaseService purchaseService;
    private final ProductionService productionService;
    private final AccountsService accountsService;

    public TestWorkflowController(OrderService orderService, DesignService designService,
                                  PurchaseService purchaseService, ProductionService productionService,
                                  AccountsService accountsService) {
        this.orderService = orderService;
        this.designService = designService;
        this.purchaseService = purchaseService;
        this.productionService = productionService;
        this.accountsService = accountsService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam Long clientId, @RequestParam Long productId, @RequestParam Integer quantity) {
        OrderEntity order = new OrderEntity();
        
        Client client = new Client();
        client.setId(clientId);
        order.setClient(client);
        
        Product product = new Product();
        product.setId(productId);
        order.setProduct(product);
        
        order.setQuantity(quantity);
        
        OrderEntity savedOrder = orderService.createOrder(order, "test-user");
        return buildResponse(savedOrder.getId(), savedOrder.getStatus().name(), "Order created successfully");
    }

    @PostMapping("/submit-design")
    public ResponseEntity<Map<String, Object>> submitDesign(@RequestParam Long orderId) {
        Design design = new Design();
        design.setDesignFilePath("/test/path/design.pdf");
        designService.submitDesign(design, orderId, "test-user");
        
        OrderEntity order = orderService.getOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        return buildResponse(orderId, order.getStatus().name(), "Design submitted successfully");
    }

    @PostMapping("/process-purchase")
    public ResponseEntity<Map<String, Object>> processPurchase(@RequestParam Long orderId, @RequestParam String materialName, @RequestParam Double requiredQty) {
        purchaseService.checkAndProcessPurchase(orderId, materialName, requiredQty, "test-user");
        OrderEntity order = orderService.getOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        return buildResponse(orderId, order.getStatus().name(), "Purchase processed successfully");
    }

    @PostMapping("/update-production")
    public ResponseEntity<Map<String, Object>> updateProduction(@RequestParam Long orderId, @RequestParam Boolean isComplete, @RequestParam Integer quantityProduced) {
        Production production = new Production();
        production.setPrintingDone(true);
        production.setCorrugationDone(true);
        if (isComplete) {
            production.setQuantityProduced(quantityProduced);
            production.setTruckNumber("TRK-1234");
        }
        productionService.updateProduction(production, orderId, "test-user");
        
        OrderEntity order = orderService.getOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        return buildResponse(orderId, order.getStatus().name(), "Production updated successfully");
    }

    @PostMapping("/finalize-accounts")
    public ResponseEntity<Map<String, Object>> finalizeAccounts(@RequestParam Long orderId, @RequestParam BigDecimal rate) {
        Accounts accounts = new Accounts();
        accounts.setRate(rate);
        accounts.setBillNumber("BILL-1001");
        accounts.setPaymentStatus("PENDING");
        
        accountsService.finalizeOrder(accounts, orderId, "test-user");
        
        OrderEntity order = orderService.getOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        return buildResponse(orderId, order.getStatus().name(), "Accounts finalized successfully");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(Long orderId, String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", status);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}
