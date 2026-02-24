package com.inventory.inventory_management.security;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * ログイン試行回数を管理するサービス
 * ブルートフォース攻撃を防ぐため、ログイン失敗回数を追跡します。
 */
@Service
@Slf4j
public class LoginAttemptService {
    
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
        int prev = removed != null ? removed.getAttempts() : 0;
        log.debug("loginSucceeded: clearing attempts for username={}, previousAttempts={}", key, prev);
        // ブロック解除された場合のみログ出力（リスナーで基本ログは記録済み）
        if (removed != null && removed.getAttempts() >= MAX_ATTEMPT) {
            log.info("ブロック解除: username={}, 以前の試行回数={}", key, removed.getAttempts());
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
                log.debug("loginFailed: username={}, previousAttempts=0, newAttempts=1, timestamp={}", key, now);
                return new LoginAttempt(1, now);
            } else {
                int newAttempts = attempt.getAttempts() + 1;
                // 詳細デバッグ出力
                log.debug("loginFailed: username={}, previousAttempts={}, newAttempts={}, timestamp={}", key, attempt.getAttempts(), newAttempts, now);
                // 警告レベルに達した場合のみログ出力
                if (newAttempts >= MAX_ATTEMPT - 1) {
                    log.warn("ログイン失敗回数が警告レベル: username={}, attempts={}", key, newAttempts);
                }
                // ブロック時は ERROR レベルで記録
                if (newAttempts >= MAX_ATTEMPT) {
                    log.error("アカウントブロック: username={}, attempts={}", key, newAttempts);
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
            log.debug("isBlocked: username={}, attempts=0 -> not blocked", key);
            return false;
        }
        
        // 24時間以上経過している場合は削除
        if (attempt.getLastAttempt().plusHours(24).isBefore(LocalDateTime.now())) {
            attemptsCache.remove(key);
            log.info("ブロック期限切れ、解除: username={}", key);
            return false;
        }
        
        boolean blocked = attempt.getAttempts() >= MAX_ATTEMPT;
        if (blocked) {
            log.warn("ブロック中のアクセス試行: username={}, attempts={}", key, attempt.getAttempts());
        } else {
            log.debug("isBlocked: username={}, attempts={} -> not blocked", key, attempt.getAttempts());
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
        int attempts = attempt != null ? attempt.getAttempts() : 0;
        log.debug("getAttempts: username={}, attempts={}", key, attempts);
        return attempts;
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
            log.info("期限切れログイン試行エントリをクリーンアップ: {}件削除", sizeBefore - sizeAfter);
        }
    }
}
