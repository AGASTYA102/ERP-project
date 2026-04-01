package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Accounts;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.service.AccountsService;
import com.erp.manufacturing.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/accounts")
@PreAuthorize("hasRole('ACCOUNTS')")
public class AccountsController {

    private final OrderService orderService;
    private final AccountsService accountsService;

    public AccountsController(OrderService orderService, AccountsService accountsService) {
        this.orderService = orderService;
        this.accountsService = accountsService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("orders", orderService.getOrdersByStatus(OrderStatus.COMPLETED));
        return "accounts/dashboard";
    }

    @GetMapping("/invoice/{orderId}")
    public String showInvoiceForm(@PathVariable Long orderId, Model model) {
        com.erp.manufacturing.entity.OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        model.addAttribute("orderId", orderId);
        model.addAttribute("order", order);
        model.addAttribute("accounts", new com.erp.manufacturing.entity.Accounts());
        return "accounts/invoice-form";
    }

    @PostMapping("/invoice")
    public String createInvoice(
            @ModelAttribute Accounts accounts,
            @RequestParam("orderId") Long orderId,
            Authentication auth
    ) {
        accountsService.finalizeOrder(accounts, orderId, auth.getName());
        return "redirect:/accounts";
    }
}
