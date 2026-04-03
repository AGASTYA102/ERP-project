package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Design;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.repository.DesignRepository;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.PurchaseService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/purchase")
@PreAuthorize("hasRole('PURCHASE_MANAGER')")
public class PurchaseController {

    private final OrderService orderService;
    private final PurchaseService purchaseService;
    private final DesignRepository designRepository;

    public PurchaseController(OrderService orderService, PurchaseService purchaseService, DesignRepository designRepository) {
        this.orderService = orderService;
        this.purchaseService = purchaseService;
        this.designRepository = designRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("orders", orderService.getOrdersByStatus(OrderStatus.PURCHASE_PENDING));
        model.addAttribute("stocks", purchaseService.getAllStock());
        return "purchase/dashboard";
    }

    @GetMapping("/process/{orderId}")
    public String showProcessForm(@PathVariable Long orderId, Model model) {
        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + orderId));
        Design design = designRepository.findByOrderId(orderId).orElse(null);

        model.addAttribute("orderId", orderId);
        model.addAttribute("order", order);
        model.addAttribute("design", design);
        return "purchase/process-form";
    }

    @PostMapping("/process")
    public String processPurchase(
            @RequestParam("orderId") Long orderId,
            @RequestParam("materialName") String materialName,
            @RequestParam("requiredQty") Double requiredQty,
            Authentication auth
    ) {
        purchaseService.checkAndProcessPurchase(orderId, materialName, requiredQty, auth.getName());
        return "redirect:/purchase";
    }
}
