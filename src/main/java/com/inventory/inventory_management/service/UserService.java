package com.inventory.inventory_management.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * ユーザーサービス
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
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
}
