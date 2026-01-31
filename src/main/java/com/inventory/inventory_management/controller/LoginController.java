package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ログイン画面のコントローラー
 */
@Controller
public class LoginController {

    /**
     * 一般ユーザー用ログイン画面を表示
     * @return login.html
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    /**
     * 管理者用ログイン画面を表示
     * @return admin/login.html
     */
    @GetMapping("/admin/login")
    public String showAdminLoginPage() {
        return "admin/login";
    }
}
