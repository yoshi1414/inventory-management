package com.inventory.inventory_management.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理者ページへ一般ユーザーがアクセスした場合に表示するカスタムハンドラ。
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String username = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "anonymous";
        logger.info("アクセス拒否ハンドリング: username={}, uri={}", username, request.getRequestURI());

        // 元の要求URIを保持しておき、ビューで戻り先を切り替えられるようにする
        request.setAttribute("requestedUri", request.getRequestURI());
        // ビュー側で表示するメッセージをリクエスト属性にセットしてフォワード
        request.setAttribute("errorMessage", "このページは管理者専用です。管理者アカウントでログインしてください。");
        request.getRequestDispatcher("/access-denied").forward(request, response);
    }
}
