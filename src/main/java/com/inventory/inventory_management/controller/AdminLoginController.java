package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用ログイン画面のコントローラー
 */
@Slf4j
@Controller
public class AdminLoginController {

    /**
     * 管理者用ログイン画面を表示
     * @return admin/login.html
     */
    @GetMapping("/admin/login")
    public String showAdminLoginPage() {
        log.debug("管理者用ログイン画面を表示");
        return "admin/login";
    }
}
