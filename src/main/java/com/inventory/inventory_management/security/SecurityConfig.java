package com.inventory.inventory_management.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Spring Securityの設定クラス
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${security.remember-me.key}")
    private String rememberMeKey;
    
    @Value("${security.remember-me.use-secure-cookie}")
    private boolean rememberMeUseSecureCookie;
    
    @Value("${security.remember-me.token-validity-seconds}")
    private int rememberMeTokenValiditySeconds;
    
    @Value("${security.remember-me.parameter}")
    private String rememberMeParameter;
    
    @Value("${security.session.maximum-sessions}")
    private int maximumSessions;
    
    @Value("${security.hsts.max-age-seconds}")
    private long hstsMaxAgeSeconds;
    
    @Autowired
    private CsrfTokenRepository csrfTokenRepository;
    
    @Autowired
    private CustomLogoutSuccessHandler logoutSuccessHandler;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    /**
     * SessionRegistryのBean定義
     * セッション管理に使用されるSessionRegistryを提供します。
     * @return SessionRegistry
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 管理者用セキュリティフィルターチェーンの設定
     * 優先順位を高く設定して、/admin/** のリクエストを先に処理する
     * @param http HttpSecurityオブジェクト
     * @return SecurityFilterChain
     * @throws Exception 設定エラー時
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")  // このフィルターチェーンは /admin/** のみに適用
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/admin/login").permitAll()  // ログイン画面へのアクセスを許可
                .requestMatchers("/admin/**").hasRole("ADMIN")  // その他の管理者ページはROLE_ADMINが必要
            )
            .exceptionHandling((exception) -> exception
                .authenticationEntryPoint(customAuthenticationEntryPoint)  // カスタム認証エントリーポイント
                .accessDeniedHandler(customAccessDeniedHandler)  // カスタムアクセス拒否ハンドラ
            )
            .formLogin((form) -> form
                .loginPage("/admin/login")  // 管理者用ログイン画面
                .loginProcessingUrl("/admin/login")  // 管理者用ログイン処理URL
                .defaultSuccessUrl("/admin/inventory")  // ログイン成功時のリダイレクト先（管理者用在庫管理）
                .failureUrl("/admin/login?error")  // ログイン失敗時のリダイレクト先
                .permitAll()
            )
            .logout((logout) -> logout
                .logoutUrl("/logout")  // ログアウト処理のURL
                .logoutSuccessHandler(logoutSuccessHandler)  // カスタムログアウト成功ハンドラー
                .deleteCookies("JSESSIONID", "remember-me")  // クッキーの完全削除
                .invalidateHttpSession(true)  // セッションの無効化
                .clearAuthentication(true)  // 認証情報のクリア
                .permitAll()
            )
            .rememberMe((remember) -> {
                remember
                    .key(rememberMeKey)                          // Remember-Meトークンのキー（環境変数から取得）
                    .tokenValiditySeconds(rememberMeTokenValiditySeconds)  // トークンの有効期限（プロパティから取得）
                    .rememberMeParameter(rememberMeParameter)    // パラメータ名（プロパティから取得）
                    .useSecureCookie(rememberMeUseSecureCookie)  // 環境変数で制御（開発: false, 本番: true）
                    .alwaysRemember(false);                      // デフォルトでRemember-Meを無効化
                logger.info("管理者用Remember-Me設定: useSecureCookie={}, tokenValiditySeconds={}, parameter={}", 
                    rememberMeUseSecureCookie, rememberMeTokenValiditySeconds, rememberMeParameter);
            })
            .csrf((csrf) -> csrf
                .csrfTokenRepository(csrfTokenRepository)  // CSRF保護の明示的な設定（セッションベース）
            )
            .sessionManagement((session) -> {
                session
                    .sessionFixation().migrateSession()   // セッション固定攻撃対策
                    .invalidSessionUrl("/admin/login?invalid")  // 無効なセッション時のリダイレクト先
                    .maximumSessions(maximumSessions)     // 同時ログイン数の制限（プロパティから取得）
                    .maxSessionsPreventsLogin(false)      // 新しいログインを許可（古いセッションを無効化）
                    .expiredUrl("/admin/login?expired")   // セッション期限切れ時のリダイレクト先
                    .sessionRegistry(sessionRegistry());  // SessionRegistryを明示的に指定
                logger.info("管理者用セッション管理設定: maximumSessions={}", maximumSessions);
            })
            .headers((headers) -> headers
                .contentSecurityPolicy((csp) -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' https://cdn.jsdelivr.net; " +
                        "style-src 'self' https://cdn.jsdelivr.net; " +
                        "img-src 'self' data:; " +
                        "font-src 'self' https://cdn.jsdelivr.net; " +
                        "report-uri /csp-violation-report-endpoint")
                )
                .frameOptions((frame) -> frame.deny())  // クリックジャッキング対策
                .xssProtection((xss) -> xss.disable())  // 最新ブラウザでは非推奨のため無効化
                .httpStrictTransportSecurity((hsts) -> {
                    hsts
                        .maxAgeInSeconds(hstsMaxAgeSeconds)  // HSTS有効期間（プロパティから取得）
                        .includeSubDomains(true)             // サブドメインも含む
                        .preload(true);                      // HSTSプリロードリスト用
                    logger.info("管理者用HSTS設定: maxAgeSeconds={}", hstsMaxAgeSeconds);
                })
                .referrerPolicy((referrer) -> referrer
                    .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)  // Referrer-Policy設定
                )
            );

        return http.build();
    }
    
    /**
     * 一般ユーザー用セキュリティフィルターチェーンの設定
     * @param http HttpSecurityオブジェクト
     * @return SecurityFilterChain
     * @throws Exception 設定エラー時
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        http
           .authorizeHttpRequests((requests) -> requests
           .requestMatchers("/css/**", "/images/**", "/js/**", "/storage/**").permitAll()  // 静的リソースへのアクセスを許可
           .requestMatchers("/login").permitAll() // 一般ユーザー用ログイン画面へのアクセスを許可
           .requestMatchers("/error").permitAll() // エラーページへのアクセスを許可
           .requestMatchers("/csp-violation-report-endpoint").permitAll() // CSP違反レポートエンドポイント
           .requestMatchers("/users/password").authenticated() // パスワード変更は認証済みユーザーのみ
           .anyRequest().authenticated()  // 上記以外のURLは認証が必要
           )
           .exceptionHandling((exception) -> exception
               .authenticationEntryPoint(customAuthenticationEntryPoint)  // カスタム認証エントリーポイント
               .accessDeniedHandler(customAccessDeniedHandler)  // カスタムアクセス拒否ハンドラ
           )
           .formLogin((form) -> form
               .loginPage("/login")  // 一般ユーザー用ログイン画面のURL
               .loginProcessingUrl("/login")  // ログイン処理のURL
               .defaultSuccessUrl("/inventory")  // ログイン成功時のリダイレクト先
               .failureUrl("/login?error")  // ログイン失敗時のリダイレクト先
               .permitAll()
           )
           .logout((logout) -> logout
               .logoutUrl("/logout")  // ログアウト処理のURL
               .logoutSuccessHandler(logoutSuccessHandler)  // カスタムログアウト成功ハンドラー
               .deleteCookies("JSESSIONID", "remember-me")  // クッキーの完全削除
               .invalidateHttpSession(true)  // セッションの無効化
               .clearAuthentication(true)  // 認証情報のクリア
               .permitAll()
           )
           .rememberMe((remember) -> {
               remember
                   .key(rememberMeKey)                          // Remember-Meトークンのキー（環境変数から取得）
                   .tokenValiditySeconds(rememberMeTokenValiditySeconds)  // トークンの有効期限（プロパティから取得）
                   .rememberMeParameter(rememberMeParameter)    // パラメータ名（プロパティから取得）
                   .useSecureCookie(rememberMeUseSecureCookie)  // 環境変数で制御（開発: false, 本番: true）
                   .alwaysRemember(false);                      // デフォルトでRemember-Meを無効化
               logger.info("一般ユーザー用Remember-Me設定: useSecureCookie={}, tokenValiditySeconds={}, parameter={}", 
                   rememberMeUseSecureCookie, rememberMeTokenValiditySeconds, rememberMeParameter);
           })
           .csrf((csrf) -> csrf
               .csrfTokenRepository(csrfTokenRepository)  // CSRF保護の明示的な設定（セッションベース）
               .ignoringRequestMatchers("/csp-violation-report-endpoint")  // CSPレポートエンドポイントをCSRF保護から除外
           )
           .sessionManagement((session) -> {
               session
                   .sessionFixation().migrateSession()   // セッション固定攻撃対策
                   .invalidSessionUrl("/login?invalid")  // 無効なセッション時のリダイレクト先
                   .maximumSessions(maximumSessions)     // 同時ログイン数の制限（プロパティから取得）
                   .maxSessionsPreventsLogin(false)      // 新しいログインを許可（古いセッションを無効化）
                   .expiredUrl("/login?expired")         // セッション期限切れ時のリダイレクト先
                   .sessionRegistry(sessionRegistry());  // SessionRegistryを明示的に指定
               logger.info("一般ユーザー用セッション管理設定: maximumSessions={}", maximumSessions);
           })
           .headers((headers) -> headers
               .contentSecurityPolicy((csp) -> csp
                   .policyDirectives("default-src 'self'; " +
                       "script-src 'self' https://cdn.jsdelivr.net; " +
                       "style-src 'self' https://cdn.jsdelivr.net; " +
                       "img-src 'self' data:; " +
                       "font-src 'self' https://cdn.jsdelivr.net; " +
                       "report-uri /csp-violation-report-endpoint")
               )
               .frameOptions((frame) -> frame.deny())  // クリックジャッキング対策
               .xssProtection((xss) -> xss.disable())  // 最新ブラウザでは非推奨のため無効化
               .httpStrictTransportSecurity((hsts) -> {
                   hsts
                       .maxAgeInSeconds(hstsMaxAgeSeconds)  // HSTS有効期間（プロパティから取得）
                       .includeSubDomains(true)             // サブドメインも含む
                       .preload(true);                      // HSTSプリロードリスト用
                   logger.info("一般ユーザー用HSTS設定: maxAgeSeconds={}", hstsMaxAgeSeconds);
               })
               .referrerPolicy((referrer) -> referrer
                   .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)  // Referrer-Policy設定
               )

           );

       return http.build();
   }
   
   /**
    * パスワードエンコーダーの設定
    * BCryptの強度を12に設定（セキュリティ強化）
    * @return BCryptPasswordEncoder
    */
   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder(12);
   }

}
