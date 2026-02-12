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
     * ユーザー情報をログに記録し、ログイン画面にリダイレクトします。
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
            if (authentication != null && authentication.getName() != null) {
                log.info("ログアウト成功: ユーザー={}, セッションID={}", 
                        authentication.getName(), 
                        request.getSession(false) != null ? request.getSession(false).getId() : "なし");
            } else {
                log.debug("認証情報なしでログアウト処理が実行されました");
            }
            
            // ログイン画面にリダイレクト
            response.sendRedirect("/login?logout");
            
        } catch (Exception e) {
            log.error("ログアウト成功処理中にエラーが発生: error={}", e.getMessage(), e);
            response.sendRedirect("/login?error");
        }
    }
}
