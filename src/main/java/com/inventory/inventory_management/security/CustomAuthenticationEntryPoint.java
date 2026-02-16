package com.inventory.inventory_management.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

/**
 * カスタム認証エントリーポイント
 * 
 * 認証されていないリクエストに対して、リクエストタイプに応じた適切な処理を行います：
 * - 存在しないエンドポイント → 404エラーページへリダイレクト
 * - その他の認証が必要なエンドポイント → ログイン画面へリダイレクト
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 未認証アクセス時のレスポンスを制御します。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param authException 認証例外
     * @throws IOException 入出力例外
     * @throws ServletException サーブレット例外
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        
        log.debug("認証エントリーポイント: URI={}, method={}, exception={}", 
                  requestUri, requestMethod, authException.getMessage());

        // 静的リソースへのアクセスの場合は403を返す
        if (requestUri.matches("^/(css|js|images|static|storage)/.*")) {
            log.warn("静的リソースへの無許可アクセス: URI={}", requestUri);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        // GETリクエストで実在しないエンドポイント（APIではない）の場合は404として処理
        if ("GET".equals(requestMethod) && !requestUri.startsWith("/api/")) {
            if (!hasMappedHandler(request)) {
                log.warn("存在しないエンドポイントへのアクセス（404）: URI={}", requestUri);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "お探しのページは存在しません。");
                return;
            }
        }

        // デフォルト: ログイン画面へリダイレクト
        log.debug("認証が必要: URI={}、ログイン画面へリダイレクト", requestUri);
        response.sendRedirect(request.getContextPath() + "/login");
    }

    /**
     * リクエストに対応するハンドラが存在するかを判定します。
     *
     * @param request HTTPリクエスト
     * @return ハンドラが存在する場合true
     */
    private boolean hasMappedHandler(HttpServletRequest request) {
        try {
            return requestMappingHandlerMapping.getHandler(request) != null;
        } catch (Exception ex) {
            log.error("ハンドラ判定中に例外が発生しました: URI={}", request.getRequestURI(), ex);
            return true;
        }
    }
}
