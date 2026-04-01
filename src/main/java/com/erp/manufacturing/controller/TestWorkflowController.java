package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.*;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.ProductionRepository;
import com.erp.manufacturing.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private final ProductionRepository productionRepository;

    public TestWorkflowController(OrderService orderService, DesignService designService,
                                   PurchaseService purchaseService, ProductionService productionService,
                                   AccountsService accountsService, ProductionRepository productionRepository) {
        this.orderService = orderService;
        this.designService = designService;
        this.purchaseService = purchaseService;
        this.productionService = productionService;
        this.accountsService = accountsService;
        this.productionRepository = productionRepository;
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
        Optional<OrderEntity> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
        }
        
        OrderEntity order = orderOpt.get();
        if (order.getStatus() != OrderStatus.DESIGN_PENDING) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot submit design. Order is in " + order.getStatus() + " state.");
        }

        Design design = new Design();
        design.setDesignFilePath("/test/path/design.pdf");
        designService.submitDesign(design, orderId, "test-user");
        
        OrderEntity updatedOrder = orderService.getOrderById(orderId).get();
        return buildResponse(orderId, updatedOrder.getStatus().name(), "Design submitted successfully");
    }

    @PostMapping("/process-purchase")
    public ResponseEntity<Map<String, Object>> processPurchase(@RequestParam Long orderId, @RequestParam String materialName, @RequestParam Double requiredQty) {
        Optional<OrderEntity> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
        }

        OrderEntity order = orderOpt.get();
        if (order.getStatus() != OrderStatus.PURCHASE_PENDING) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot process purchase. Order is in " + order.getStatus() + " state.");
        }

        purchaseService.checkAndProcessPurchase(orderId, materialName, requiredQty, "test-user");
        OrderEntity updatedOrder = orderService.getOrderById(orderId).get();
        return buildResponse(orderId, updatedOrder.getStatus().name(), "Purchase processed successfully");
    }

    @PostMapping("/update-production")
    public ResponseEntity<Map<String, Object>> updateProduction(@RequestParam Long orderId, @RequestParam Boolean isComplete, @RequestParam Integer quantityProduced) {
        Optional<OrderEntity> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
        }

        OrderEntity order = orderOpt.get();
        if (order.getStatus() != OrderStatus.READY_FOR_PRODUCTION && order.getStatus() != OrderStatus.IN_PRODUCTION) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot update production. Order is in " + order.getStatus() + " state.");
        }

        Production production = productionRepository.findByOrderId(orderId).orElse(new Production());
        production.setOrder(order);
        production.setPrintingDone(true);
        production.setCorrugationDone(true);
        if (isComplete) {
            production.setQuantityProduced(quantityProduced);
            production.setTruckNumber("TRK-1234");
        }
        productionService.updateProduction(production, orderId, "test-user");
        
        OrderEntity updatedOrder = orderService.getOrderById(orderId).get();
        return buildResponse(orderId, updatedOrder.getStatus().name(), "Production updated successfully");
    }

    @PostMapping("/finalize-accounts")
    public ResponseEntity<Map<String, Object>> finalizeAccounts(@RequestParam Long orderId, @RequestParam BigDecimal rate) {
        Optional<OrderEntity> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
        }

        OrderEntity order = orderOpt.get();
        if (order.getStatus() != OrderStatus.COMPLETED) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot finalize accounts. Order is in " + order.getStatus() + " state.");
        }

        Accounts accounts = new Accounts();
        accounts.setRate(rate);
        accounts.setBillNumber("BILL-1001");
        accounts.setPaymentStatus("PENDING");
        
        accountsService.finalizeOrder(accounts, orderId, "test-user");
        
        OrderEntity updatedOrder = orderService.getOrderById(orderId).get();
        return buildResponse(orderId, updatedOrder.getStatus().name(), "Accounts finalized successfully");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(Long orderId, String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", status);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
