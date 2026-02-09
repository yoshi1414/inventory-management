package com.inventory.inventory_management.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * 認証成功イベントを監視するリスナー
 * ログイン成功時にLoginAttemptServiceを呼び出します。
 */
@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationSuccessListener.class);
    
    private final LoginAttemptService loginAttemptService;
    
    /**
     * コンストラクタ
     * @param loginAttemptService ログイン試行管理サービス
     */
    public AuthenticationSuccessListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }
    
    @Override
    @Async // 非同期処理を追加
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = "unknown";
        String sessionId = "unknown";
        
        // 認証の詳細情報（IPアドレス、セッションID）を取得
        Object details = event.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails webDetails = (WebAuthenticationDetails) details;
            ipAddress = webDetails.getRemoteAddress();
            sessionId = webDetails.getSessionId();
        }
        
        try {
            logger.info("認証成功イベント検出: username={}, ipAddress={}, sessionId={}", 
                       username, ipAddress, sessionId);
            loginAttemptService.loginSucceeded(username);
        } catch (Exception e) {
            logger.error("認証成功処理中にエラーが発生しました: username={}, ipAddress={}, error={}", 
                        username, ipAddress, e.getMessage());
        }
    }
}
