package com.inventory.inventory_management.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理者用メニュー画面のコントローラー
 */
@Controller
public class AdminMenuController {

    /**
     * 管理者用メニュー画面を表示
     * @param model モデル
     * @return admin/menu.html
     */
    @GetMapping("/admin/menu")
    public String showAdminMenu(Model model) {
        // ログインユーザー名を取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        model.addAttribute("username", username);
        return "admin/menu";
    }
}
