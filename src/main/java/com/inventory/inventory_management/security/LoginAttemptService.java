package com.inventory.inventory_management.security;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ログイン試行回数を管理するサービス
 * ブルートフォース攻撃を防ぐため、ログイン失敗回数を追跡します。
 */
@Service
public class LoginAttemptService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    
    /** 最大試行回数 */
    private static final int MAX_ATTEMPT = 5;
    
    /** ログイン試行を記録するマップ */
    private final ConcurrentHashMap<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();
    
    /**
     * ログイン試行情報を保持するクラス
     */
    private static class LoginAttempt {
        private int attempts;
        private LocalDateTime lastAttempt;
        
        public LoginAttempt(int attempts, LocalDateTime lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
        
        public int getAttempts() {
            return attempts;
        }
        
        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }
    }
    
    /**
     * ログイン成功時に呼び出す
     * キャッシュから試行回数を削除
     * @param key ユーザーを識別するキー（通常はユーザー名）
     */
    public void loginSucceeded(String key) {
        LoginAttempt removed = attemptsCache.remove(key);
        // ブロック解除された場合のみログ出力（リスナーで基本ログは記録済み）
        if (removed != null && removed.getAttempts() >= MAX_ATTEMPT) {
            logger.info("ブロック解除: username={}, 以前の試行回数={}", key, removed.getAttempts());
        }
    }
    
    /**
     * ログイン失敗時に呼び出す
     * 試行回数をインクリメント
     * @param key ユーザーを識別するキー（通常はユーザー名）
     */
    public void loginFailed(String key) {
        LocalDateTime now = LocalDateTime.now();
        attemptsCache.compute(key, (k, attempt) -> {
            if (attempt == null) {
                // 初回失敗：基本ログはリスナーで記録済み、DEBUG レベルで試行回数のみ記録
                logger.debug("ログイン試行回数記録開始: username={}, attempts=1", key);
                return new LoginAttempt(1, now);
            } else {
                int newAttempts = attempt.getAttempts() + 1;
                // 警告レベルに達した場合のみログ出力
                if (newAttempts >= MAX_ATTEMPT - 1) {
                    logger.warn("ログイン失敗回数が警告レベル: username={}, attempts={}", key, newAttempts);
                }
                // ブロック時は ERROR レベルで記録
                if (newAttempts >= MAX_ATTEMPT) {
                    logger.error("アカウントブロック: username={}, attempts={}", key, newAttempts);
                }
                return new LoginAttempt(newAttempts, now);
            }
        });
    }
    
    /**
     * ユーザーがブロックされているかチェック
     * @param key ユーザーを識別するキー（通常はユーザー名）
     * @return ブロックされている場合はtrue
     */
    public boolean isBlocked(String key) {
        LoginAttempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            return false;
        }
        
        // 24時間以上経過している場合は削除
        if (attempt.getLastAttempt().plusHours(24).isBefore(LocalDateTime.now())) {
            attemptsCache.remove(key);
            logger.info("ブロック期限切れ、解除: username={}", key);
            return false;
        }
        
        boolean blocked = attempt.getAttempts() >= MAX_ATTEMPT;
        if (blocked) {
            logger.warn("ブロック中のアクセス試行: username={}, attempts={}", key, attempt.getAttempts());
        }
        return blocked;
    }
    
    /**
     * 現在の試行回数を取得
     * @param key ユーザーを識別するキー
     * @return 試行回数
     */
    public int getAttempts(String key) {
        LoginAttempt attempt = attemptsCache.get(key);
        return attempt != null ? attempt.getAttempts() : 0;
    }
    
    /**
     * 24時間以上経過したエントリを定期的にクリーンアップ
     * 毎時間実行
     */
    @Scheduled(fixedRate = 3600000) // 1時間ごと
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        int sizeBefore = attemptsCache.size();
        attemptsCache.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().plusHours(24).isBefore(now)
        );
        int sizeAfter = attemptsCache.size();
        if (sizeBefore > sizeAfter) {
            logger.info("期限切れログイン試行エントリをクリーンアップ: {}件削除", sizeBefore - sizeAfter);
        }
    }
}
