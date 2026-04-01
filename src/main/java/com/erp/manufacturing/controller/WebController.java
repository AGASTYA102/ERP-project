package com.erp.manufacturing.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String redirectBasedOnRole(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_GENERAL_MANAGER")) {
            return "redirect:/gm";
        } else if (role.equals("ROLE_DESIGNER")) {
            return "redirect:/designer";
        } else if (role.equals("ROLE_PURCHASE_MANAGER")) {
            return "redirect:/purchase";
        } else if (role.equals("ROLE_PRODUCTION_MANAGER")) {
            return "redirect:/production";
        } else if (role.equals("ROLE_ACCOUNTS")) {
            return "redirect:/accounts";
        }

        return "redirect:/login";
    }
}
