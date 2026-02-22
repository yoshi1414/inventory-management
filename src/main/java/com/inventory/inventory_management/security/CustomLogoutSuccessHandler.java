package com.inventory.inventory_management.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * カスタムログアウト成功ハンドラー
 * <p>
 * ログアウト成功時の処理をカスタマイズします。
 * ログ記録を行い、ログイン画面にリダイレクトします。
 * </p>
 */
@Slf4j
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    /**
     * ログアウト成功時の処理
     * <p>
     * ユーザー情報をログに記録し、ロール情報に基づいて適切なログイン画面にリダイレクトします。
     * 管理者（ROLE_ADMIN）→ /admin/login
     * 一般ユーザー（ROLE_USER）→ /login
     * </p>
     * 
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param authentication 認証情報
     * @throws IOException 入出力例外
     * @throws ServletException サーブレット例外
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        try {
            String redirectUrl = "/login?logout";  // デフォルト: 一般ユーザーログイン画面
            
            if (authentication != null && authentication.getName() != null) {
                log.info("ログアウト成功: ユーザー={}, セッションID={}", 
                        authentication.getName(), 
                        request.getSession(false) != null ? request.getSession(false).getId() : "なし");
                
                // ロール情報を確認して、管理者の場合は /admin/login にリダイレクト
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                
                if (isAdmin) {
                    redirectUrl = "/admin/login?logout";
                    log.debug("管理者ログアウト: {} -> {}", authentication.getName(), redirectUrl);
                } else {
                    log.debug("ユーザーログアウト: {} -> {}", authentication.getName(), redirectUrl);
                }
            } else {
                log.debug("認証情報なしでログアウト処理が実行されました");
            }
            
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("ログアウト成功処理中にエラーが発生: error={}", e.getMessage(), e);
            response.sendRedirect("/login?error");
        }
    }
}
