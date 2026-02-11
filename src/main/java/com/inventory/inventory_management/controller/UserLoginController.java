package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * 一般ユーザー用ログイン画面のコントローラー
 */
@Slf4j
@Controller
public class UserLoginController {

    /**
     * 一般ユーザー用ログイン画面を表示
     * @return login.html
     */
    @GetMapping("/login")
    public String showLoginPage() {
        log.debug("一般ユーザー用ログイン画面を表示");
        return "login";
    }
}
