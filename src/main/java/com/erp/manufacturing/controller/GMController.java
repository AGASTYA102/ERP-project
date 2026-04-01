package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Client;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Product;
import com.erp.manufacturing.service.ClientService;
import com.erp.manufacturing.service.JobCardService;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.erp.manufacturing.entity.JobCard;

@Controller
@RequestMapping("/gm")
@PreAuthorize("hasRole('GENERAL_MANAGER')")
public class GMController {

    private final OrderService orderService;
    private final ClientService clientService;
    private final ProductService productService;
    private final JobCardService jobCardService;
    private final com.erp.manufacturing.service.OrderLogService orderLogService;

    public GMController(OrderService orderService, ClientService clientService, ProductService productService, JobCardService jobCardService, com.erp.manufacturing.service.OrderLogService orderLogService) {
        this.orderService = orderService;
        this.clientService = clientService;
        this.productService = productService;
        this.jobCardService = jobCardService;
        this.orderLogService = orderLogService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "gm/dashboard";
    }

    @GetMapping("/orders/{id}")
    public String orderDetails(@PathVariable Long id, Model model) {
        OrderEntity order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + id));
        model.addAttribute("order", order);
        model.addAttribute("logs", orderLogService.getLogsForOrder(id));
        return "gm/order-details";
    }

    // Client Management
    @GetMapping("/clients")
    public String listClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "gm/clients";
    }

    @GetMapping("/clients/new")
    public String showClientForm(Model model) {
        model.addAttribute("client", new Client());
        return "gm/client-form";
    }

    @PostMapping("/clients/save")
    public String saveClient(@Valid @ModelAttribute Client client, BindingResult result) {
        if (result.hasErrors()) {
            return "gm/client-form";
        }
        clientService.createClient(client);
        return "redirect:/gm/clients";
    }

    // Product Management
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "gm/products";
    }

    @GetMapping("/products/new")
    public String showProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "gm/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute Product product, BindingResult result) {
        if (result.hasErrors()) {
            return "gm/product-form";
        }
        productService.createProduct(product);
        return "redirect:/gm/products";
    }

    // Order Punching
    @GetMapping("/orders/new")
    public String showOrderForm(Model model) {
        model.addAttribute("order", new OrderEntity());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("products", productService.getAllProducts());
        return "gm/order-form";
    }

    @PostMapping("/orders/save")
    public String saveOrder(@Valid @ModelAttribute OrderEntity order, BindingResult result, Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.getAllClients());
            model.addAttribute("products", productService.getAllProducts());
            return "gm/order-form";
        }
        OrderEntity savedOrder = orderService.createOrder(order, auth.getName());
        
        // Auto-create JobCard
        Product product = savedOrder.getProduct();
        JobCard jobCard = JobCard.builder()
                .order(savedOrder)
                .dieNo(product.getDieNo())
                .plateId(product.getPlateId())
                .sheetSize(product.getSheetSize())
                .build();
        jobCardService.createJobCard(jobCard);
        
        return "redirect:/gm";
    }
}
