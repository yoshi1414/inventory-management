package com.inventory.inventory_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * CSRF設定クラス
 * CSRFトークンをセッションベースで管理します。
 */
@Configuration
public class CsrfConfig {
    
    /**
     * CSRFトークンリポジトリの設定
     * HttpSessionベースのCSRFトークン管理（XSS攻撃からの保護を強化）
     * @return CSRFトークンリポジトリ
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }
}
