package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理者用ログイン画面のコントローラー
 */
@Controller
public class AdminLoginController {

    /**
     * 管理者用ログイン画面を表示
     * @return admin/login.html
     */
    @GetMapping("/admin/login")
    public String showAdminLoginPage() {
        return "admin/login";
    }
}
