package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminProductController {

    @GetMapping("/admin/products")
    public String showAdminProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "stock", required = false) String stock,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        model.addAttribute("products", mockProducts());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("stock", stock);
        model.addAttribute("sort", sort);

        return "admin/products";
    }

    private Object mockProducts() {
        return null;
    }
}
