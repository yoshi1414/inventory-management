package com.inventory.inventory_management.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * アクセス拒否時の専用表示コントローラ
 */
@Controller
public class AccessDeniedController {

    @GetMapping("/access-denied")
    public String accessDenied(Model model, HttpServletRequest request) {
        Object msg = request.getAttribute("errorMessage");
        model.addAttribute("message", (msg != null) ? msg : "アクセスが拒否されました。");

        // リクエスト属性 'requestedUri' を参照して戻り先を決定する
        Object reqUriObj = request.getAttribute("requestedUri");
        String returnUrl = "/inventory"; // デフォルト
        if (reqUriObj instanceof String) {
            String reqUri = (String) reqUriObj;
            if (reqUri.startsWith("/admin")) {
                returnUrl = "/admin/login"; // 管理画面からのアクセス拒否の場合は管理者ログインへ戻す
            }
        }
        model.addAttribute("returnUrl", returnUrl);
        return "access-denied";
    }
}
