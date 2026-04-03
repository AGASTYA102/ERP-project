package com.erp.manufacturing.controller;

import com.erp.manufacturing.entity.Design;
import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.enums.OrderStatus;
import com.erp.manufacturing.service.DesignService;
import com.erp.manufacturing.service.FileService;
import com.erp.manufacturing.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/designer")
@PreAuthorize("hasRole('DESIGNER')")
public class DesignerController {

    private final OrderService orderService;
    private final DesignService designService;
    private final FileService fileService;

    public DesignerController(OrderService orderService, DesignService designService, FileService fileService) {
        this.orderService = orderService;
        this.designService = designService;
        this.fileService = fileService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("orders", orderService.getOrdersByStatus(OrderStatus.DESIGN_PENDING));
        return "designer/dashboard";
    }

    @GetMapping("/upload/{orderId}")
    public String showUploadForm(@PathVariable Long orderId, Model model) {
        OrderEntity order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + orderId));
        model.addAttribute("order", order);
        model.addAttribute("design", new Design());
        model.addAttribute("existingDies", designService.getDistinctDieIds());
        model.addAttribute("existingPalletes", designService.getDistinctPalleteIds());
        return "designer/upload-form";
    }

    @PostMapping("/upload")
    public String processUpload(
            @ModelAttribute Design design,
            @RequestParam("orderId") Long orderId,
            @RequestParam("designFile") MultipartFile designFile,
            @RequestParam("confirmationFile") MultipartFile confirmationFile,
            Authentication authentication
    ) {
        String designPath = fileService.storeFile(designFile);
        String confirmationPath = fileService.storeFile(confirmationFile);

        if (designPath == null || confirmationPath == null) {
            throw new RuntimeException("Design file and Confirmation file are both required.");
        }

        design.setDesignFilePath(designPath);
        design.setConfirmationFilePath(confirmationPath);

        designService.submitDesign(design, orderId, authentication.getName());
        return "redirect:/designer";
    }
}
