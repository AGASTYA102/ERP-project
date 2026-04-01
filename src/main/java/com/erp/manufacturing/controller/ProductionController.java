package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Production;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.ProductionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/production")
@PreAuthorize("hasRole('PRODUCTION_MANAGER')")
public class ProductionController {

    private final OrderService orderService;
    private final ProductionService productionService;

    public ProductionController(OrderService orderService, ProductionService productionService) {
        this.orderService = orderService;
        this.productionService = productionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("readyOrders", orderService.getOrdersByStatus(OrderStatus.READY_FOR_PRODUCTION));
        model.addAttribute("inProductionOrders", orderService.getOrdersByStatus(OrderStatus.IN_PRODUCTION));
        return "production/dashboard";
    }

    @PostMapping("/start/{orderId}")
    public String startProduction(@PathVariable Long orderId, Authentication auth) {
        orderService.updateOrderStatus(orderId, OrderStatus.IN_PRODUCTION, "Production started", auth.getName());
        return "redirect:/production";
    }

    @GetMapping("/complete/{orderId}")
    public String showCompleteForm(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("production", new Production());
        return "production/complete-form";
    }

    @PostMapping("/complete")
    public String completeProduction(
            @ModelAttribute Production production,
            @RequestParam("orderId") Long orderId,
            Authentication auth
    ) {
        productionService.updateProduction(production, orderId, auth.getName());
        return "redirect:/production";
    }
}
