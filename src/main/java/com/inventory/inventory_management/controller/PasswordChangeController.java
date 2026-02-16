package com.inventory.inventory_management.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.inventory_management.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * パスワード変更コントローラー
 */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeController {
    
    private final UserService userService;
    
    /**
     * パスワード変更画面を表示
     * @param model モデル
     * @return パスワード変更画面のビュー名
     */
    @GetMapping("/password")
    public String showPasswordChangePage(Model model) {
        log.debug("パスワード変更画面を表示");
        return "password-change";
    }
    
    /**
     * パスワード変更処理
     * @param currentPassword 現在のパスワード
     * @param newPassword 新しいパスワード
     * @param confirmPassword 新しいパスワード（確認）
     * @param redirectAttributes リダイレクト用の属性
     * @return リダイレクト先
     */
    @PostMapping("/password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 認証情報からユーザー名を取得
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            log.info("パスワード変更リクエスト: username={}", username);
            
            // 入力値のバリデーション
            if (currentPassword == null || currentPassword.isEmpty()) {
                log.warn("現在のパスワードが入力されていません: username={}", username);
                redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードを入力してください");
                return "redirect:/users/password";
            }
            
            if (newPassword == null || newPassword.isEmpty()) {
                log.warn("新しいパスワードが入力されていません: username={}", username);
                redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードを入力してください");
                return "redirect:/users/password";
            }
            
            if (confirmPassword == null || confirmPassword.isEmpty()) {
                log.warn("確認用パスワードが入力されていません: username={}", username);
                redirectAttributes.addFlashAttribute("errorMessage", "確認用パスワードを入力してください");
                return "redirect:/users/password";
            }
            
            // 新しいパスワードと確認用パスワードが一致しているか確認
            if (!newPassword.equals(confirmPassword)) {
                log.warn("新しいパスワードと確認用パスワードが一致しません: username={}", username);
                redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードと確認用パスワードが一致しません");
                return "redirect:/users/password";
            }
            
            // パスワード変更処理
            userService.changePassword(username, currentPassword, newPassword);
            
            log.info("パスワード変更成功: username={}", username);
            redirectAttributes.addFlashAttribute("successMessage", "パスワードを変更しました");
            return "redirect:/users/password";
            
        } catch (IllegalArgumentException e) {
            // ビジネスロジックのエラー（現在のパスワードが間違っている、バリデーションエラーなど）
            log.warn("パスワード変更に失敗: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/password";
            
        } catch (Exception e) {
            // 予期しないエラー
            log.error("パスワード変更処理中にエラーが発生: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "パスワード変更に失敗しました。システム管理者にお問い合わせください");
            return "redirect:/users/password";
        }
    }
}
