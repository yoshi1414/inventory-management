package com.inventory.inventory_management.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 本番環境（prodプロファイル）で必須プロパティが未設定の場合に、
 * 起動時点で明確な例外を投げて停止させるバリデータ。
 * <p>
 * セキュリティ上、値そのものはログに出さず、未設定のキー名のみを報告する。
 * </p>
 */
@Component
@Profile("prod")
public class ProdRequiredPropertiesValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProdRequiredPropertiesValidator.class);

    private final Environment environment;

    /**
     * コンストラクタ。
     * 
     * @param environment Springの環境情報
     */
    public ProdRequiredPropertiesValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * アプリ起動時に必須プロパティの未設定を検出し、例外で停止させる。
     * 
     * @param args 起動引数
     */
    @Override
    public void run(ApplicationArguments args) {
        logger.info("本番環境の必須プロパティ検証を開始");
        List<String> missingKeys = new ArrayList<>();

        requireNonBlank("spring.datasource.username", missingKeys);
        requireNonBlank("spring.datasource.password", missingKeys);
        requireNonBlank("security.remember-me.key", missingKeys);
        
        // セキュリティ設定の必須プロパティを確認
        requireNonBlank("security.remember-me.token-validity-seconds", missingKeys);
        requireNonBlank("security.remember-me.parameter", missingKeys);
        requireNonBlank("security.session.maximum-sessions", missingKeys);
        requireNonBlank("security.hsts.max-age-seconds", missingKeys);
        
        // use-secure-cookieは本番環境でtrueであるべき
        requireBoolean("security.remember-me.use-secure-cookie", true, missingKeys);

        if (!missingKeys.isEmpty()) {
            logger.error("本番環境の必須プロパティ検証失敗: 未設定または不正なプロパティ={}", missingKeys);
            throw new IllegalStateException(
                    "prodプロファイルで必須プロパティが未設定または不正です: " + String.join(", ", missingKeys)
                            + ". 環境変数(MYSQL_USER, MYSQL_PASSWORD, SECURITY_REMEMBER_ME_KEY)を設定し、application-prod.propertiesを確認してください。"
            );
        }
        logger.info("本番環境の必須プロパティ検証完了: すべてのプロパティが正しく設定されています");
    }

    /**
     * 指定キーが未設定（null/空/空白のみ）の場合にmissingKeysへ追加する。
     * 
     * @param key プロパティキー
     * @param missingKeys 未設定キーの格納先
     */
    private void requireNonBlank(String key, List<String> missingKeys) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            missingKeys.add(key);
        }
    }
    
    /**
     * 指定キーのboolean値が期待値と一致しない場合にmissingKeysへ追加する。
     * 
     * @param key プロパティキー
     * @param expected 期待される値
     * @param missingKeys 不正なキーの格納先
     */
    private void requireBoolean(String key, boolean expected, List<String> missingKeys) {
        Boolean value = environment.getProperty(key, Boolean.class);
        if (value == null || value != expected) {
            missingKeys.add(key + "(期待値: " + expected + ", 実際: " + value + ")");
        }
    }
}
