package com.inventory.inventory_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 一般ユーザー用ログイン画面のコントローラー
 */
@Controller
public class UserLoginController {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginController.class);

    /**
     * 一般ユーザー用ログイン画面を表示
     * @return login.html
     */
    @GetMapping("/login")
    public String showLoginPage() {
        logger.debug("一般ユーザー用ログイン画面を表示");
        return "login";
    }
}
