package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminProductController {

    @GetMapping("/admin/products")
    public String showAdminProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        model.addAttribute("products", mockProducts());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("price", price);
        model.addAttribute("sort", sort);

        return "admin/products";
    }

    @GetMapping("/admin/products/create")
    public String showCreateForm(Model model) {
        // 商品登録フォーム表示
        return "admin/product-create";
    }

    @PostMapping("/admin/products/create")
    public String createProduct(
            @RequestParam String productName,
            @RequestParam String category,
            @RequestParam int price,
            @RequestParam int stock,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        // 商品登録処理（モック）
        redirectAttributes.addFlashAttribute("message", "商品を登録しました。");
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/{productId}/edit")
    public String showEditForm(@PathVariable String productId, Model model) {
        // 商品編集フォーム表示
        model.addAttribute("productId", productId);
        return "admin/product-edit";
    }

    @PostMapping("/admin/products/{productId}/edit")
    public String updateProduct(
            @PathVariable String productId,
            @RequestParam String productName,
            @RequestParam String category,
            @RequestParam int price,
            @RequestParam int stock,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        // 商品更新処理（モック）
        redirectAttributes.addFlashAttribute("message", "商品を更新しました。");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{productId}/delete")
    public String deleteProduct(
            @PathVariable String productId,
            RedirectAttributes redirectAttributes) {

        // 商品削除処理（モック）
        redirectAttributes.addFlashAttribute("message", "商品を削除しました。");
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/import")
    public String showImportForm(Model model) {
        // CSVインポートフォーム表示
        return "admin/product-import";
    }

    @PostMapping("/admin/products/import")
    public String importProducts(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes redirectAttributes) {

        // CSVインポート処理（モック）
        redirectAttributes.addFlashAttribute("message", "商品をインポートしました。");
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/export")
    public String exportProducts() {
        // CSVエクスポート処理（モック）
        // 実際にはResponseEntityやHttpServletResponseを使用してCSVファイルを返す
        return "redirect:/admin/products";
    }

    private Object mockProducts() {
        // モックデータ（後でサービス層から取得）
        return null;
    }
}
