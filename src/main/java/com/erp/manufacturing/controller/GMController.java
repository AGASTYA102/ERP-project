package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Client;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.Product;
import com.erp.manufacturing.service.ClientService;
import com.erp.manufacturing.service.OrderService;
import com.erp.manufacturing.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/gm")
@PreAuthorize("hasRole('GENERAL_MANAGER')")
public class GMController {

    private final OrderService orderService;
    private final ClientService clientService;
    private final ProductService productService;
    private final com.erp.manufacturing.service.OrderLogService orderLogService;

    public GMController(OrderService orderService, ClientService clientService, ProductService productService, com.erp.manufacturing.service.OrderLogService orderLogService) {
        this.orderService = orderService;
        this.clientService = clientService;
        this.productService = productService;
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
    public String showClientForm(Model model, @RequestParam(required = false) String source) {
        model.addAttribute("client", new Client());
        model.addAttribute("source", source);
        return "gm/client-form";
    }

    @PostMapping("/clients/save")
    public String saveClient(@Valid @ModelAttribute Client client, BindingResult result, @RequestParam(required = false) String source) {
        if (result.hasErrors()) {
            return "gm/client-form";
        }
        clientService.createClient(client);
        if ("order-form".equals(source)) {
            return "redirect:/gm/orders/new";
        }
        return "redirect:/gm/clients";
    }

    // Product Management
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "gm/products";
    }

    @GetMapping("/products/new")
    public String showProductForm(Model model, @RequestParam(required = false) String source) {
        model.addAttribute("product", new Product());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("source", source);
        return "gm/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute Product product, BindingResult result, 
                             @RequestParam(required = false) String source, 
                             @RequestParam(value = "designFile", required = false) org.springframework.web.multipart.MultipartFile designFile,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.getAllClients());
            return "gm/product-form";
        }
        
        if (designFile != null && !designFile.isEmpty()) {
            // Assume we have a fileService for uploading
            // String fileUrl = fileService.upload(designFile);
            // product.setDesignApprovedFileUrl(fileUrl);
        }
        
        productService.createProduct(product);
        if ("order-form".equals(source)) {
            return "redirect:/gm/orders/new";
        }
        return "redirect:/gm/products";
    }

    @GetMapping("/api/products/filter")
    @ResponseBody
    public java.util.List<Product> filterProducts(@RequestParam Long clientId) {
        // We use the repository directly or via service to get client-specific products
        // For simplicity and speed in this demo, accessing service
        return productService.getProductsByClient(clientId);
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
    public String saveOrder(@Valid @ModelAttribute("order") OrderEntity order, BindingResult result, Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.getAllClients());
            model.addAttribute("products", productService.getAllProducts());
            return "gm/order-form";
        }
        
        // Use the atomic service method instead of segmented controller logic
        orderService.punchOrder(order, auth.getName());
        
        return "redirect:/gm";
    }
}
