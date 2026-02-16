package com.inventory.inventory_management.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * PasswordChangeControllerの結合テスト
 * 
 * コントローラー → サービス → リポジトリ → データベースの
 * 完全なエンドツーエンドテストを実施します。
 * 
 * データベースの実際の状態変化を検証するテストを含みます。
 * 
 * @SpringBootTest により、実際のアプリケーションコンテキストが起動され、
 * 依存性注入、UserRepository、PasswordEncoder など実装が使用されます。
 * 
 * @Transactional により、テスト終了後に自動的にロールバックされるため、
 * テストの分離性が確保されます。
 * 
 * スキーマは spring.jpa.hibernate.ddl-auto=create-drop により、
 * 各テストメソッド前に自動作成され、テスト後に削除されます。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordChangeController - パスワード変更結合テスト")
public class PasswordChangeControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private MockMvc mockMvc;
    
    private User testUser;
    
    /**
     * テスト前の初期化処理
     * MockMvcの設定とテストユーザーの作成を行います
     * 
     * @Transactional のため、テスト終了後に自動的にロールバックされます
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
                // テストユーザーを初期化（既存がある場合は更新）
                testUser = userRepository.findByUsername("testuser").orElseGet(User::new);
                testUser.setUsername("testuser");
                testUser.setPassword(passwordEncoder.encode("OldPass123!"));
                testUser.setEmail("testuser@example.com");
                testUser.setFullName("Test User");
                testUser.setIsActive(true);
                if (testUser.getCreatedAt() == null) {
                        testUser.setCreatedAt(LocalDateTime.now());
                }
                testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);
    }
    
    // ========== コントローラー → サービス → リポジトリ → DB の結合テスト ==========

    /**
     * Test: コントローラーからDBまでの完全なパスワード変更フロー
     * 
     * HTTPリクエスト → ビジネスロジック処理 → DB永続化 の各層を検証
     * DB検証により、パスワードが実際にデータベースに保存されたことを確認
     */
    @Test
    @DisplayName("【結合】パスワード変更が成功し、DBに正しく保存される")
    void changePassword_Success_Integration() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        String confirmPassword = "NewPass123!";
        
        // When - パスワード変更リクエストを実行（Controller → Service → Repository）
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                // Then - HTTPレスポンス検証
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("successMessage", "パスワードを変更しました"));
        
        // DB検証：パスワードが実際に変更されているか確認（InventoryIntegrationTest パターン）
        User updatedUser = userRepository.findByUsername("testuser")
                .orElseThrow(() -> new AssertionError("ユーザーが見つかりません"));
        
        // 新しいパスワードで暗号化されていることを確認
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword()))
                .as("新しいパスワードが正しく保存されている")
                .isTrue();
        
        // 旧パスワードではマッチしないことを確認
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword()))
                .as("旧パスワードは無効になっている")
                .isFalse();
        
        // 更新日時が更新されているか確認
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
    }
}
