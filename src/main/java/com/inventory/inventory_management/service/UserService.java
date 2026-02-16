package com.inventory.inventory_management.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * ユーザーサービス
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * メールアドレスでユーザーを検索
     * @param email メールアドレス
     * @return ユーザー（存在しない場合は空の Optional）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        try {
            logger.debug("メールアドレスでユーザーを検索: email={}", email);
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            logger.error("メールアドレスでのユーザー検索に失敗: email={}, error={}", email, e.getMessage());
            throw e;
        }
    }
    
    /**
     * ユーザー名でユーザーを検索
     * @param username ユーザー名
     * @return ユーザー（存在しない場合は空の Optional）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        try {
            logger.debug("ユーザー名でユーザーを検索: username={}", username);
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            logger.error("ユーザー名でのユーザー検索に失敗: username={}, error={}", username, e.getMessage());
            throw e;
        }
    }
    
    /**
     * ユーザーを保存
     * @param user ユーザー
     * @return 保存されたユーザー
     */
    @Transactional
    public User save(User user) {
        try {
            logger.info("ユーザーを保存: username={}", user.getUsername());
            User savedUser = userRepository.save(user);
            logger.info("ユーザー保存成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
            return savedUser;
        } catch (Exception e) {
            logger.error("ユーザー保存に失敗: username={}, error={}", user.getUsername(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * パスワード変更
     * @param username ユーザー名
     * @param currentPassword 現在のパスワード
     * @param newPassword 新しいパスワード
     * @throws IllegalArgumentException 現在のパスワードが間違っている場合
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        logger.info("パスワード変更処理開始: username={}", username);
        
        try {
            // ユーザーを取得
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("ユーザーが見つかりません: username={}", username);
                    return new IllegalArgumentException("ユーザーが見つかりません");
                });
            
            // 現在のパスワードを検証
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                logger.warn("現在のパスワードが正しくありません: username={}", username);
                throw new IllegalArgumentException("現在のパスワードが正しくありません");
            }
            
            // 新しいパスワードのバリデーション
            validatePassword(newPassword);
            
            // 同じパスワードを使用していないか確認
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                logger.warn("新しいパスワードが現在のパスワードと同じです: username={}", username);
                throw new IllegalArgumentException("新しいパスワードは現在のパスワードと異なる必要があります");
            }
            
            // パスワードを暗号化して更新
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            
            // 保存
            userRepository.save(user);
            
            logger.info("パスワード変更成功: username={}", username);
            
        } catch (IllegalArgumentException e) {
            // 予想されるエラーはそのまま再スロー
            throw e;
        } catch (Exception e) {
            logger.error("パスワード変更処理中にエラーが発生: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("パスワード変更に失敗しました", e);
        }
    }
    
    /**
     * パスワードのバリデーション
     * @param password パスワード
     * @throws IllegalArgumentException パスワードが要件を満たさない場合
     */
    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("パスワードを入力してください");
        }
        
        // 最小文字数チェック
        if (password.length() < 8) {
            throw new IllegalArgumentException("パスワードは8文字以上である必要があります");
        }
        
        // 英大文字を含むかチェック
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("パスワードに英大文字（A-Z）を含める必要があります");
        }
        
        // 英小文字を含むかチェック
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("パスワードに英小文字（a-z）を含める必要があります");
        }
        
        // 数字を含むかチェック
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("パスワードに数字（0-9）を含める必要があります");
        }
        
        // 特殊文字を含むかチェック
        if (!password.matches(".*[!@#$%^&*].*")) {
            throw new IllegalArgumentException("パスワードに特殊文字（!@#$%^&*）を含める必要があります");
        }
    }
}
