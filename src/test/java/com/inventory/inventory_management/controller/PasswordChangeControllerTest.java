package com.inventory.inventory_management.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
 * PasswordChangeControllerのテスト
 * 
 * パスワード変更機能のバリデーション・セキュリティテストを実施します。
 * 
 * 以下の項目を検証:
 * - 入力フィールドのバリデーション（空欄・文字数・型チェック）
 * - パスワード要件の検証（大文字・小文字・数字・特殊文字含有）
 * - セキュリティチェック（CSRF保護、未認証時のアクセス制御）
 * - ビジネスロジックエラー（パスワード不一致・現在のパスワード誤り）
 * 
 * データベース永続化を検証するテストは
 * PasswordChangeControllerIntegrationTest.java に配置します（integration パッケージ）。
 * 
 * @SpringBootTest により、実際のアプリケーションコンテキストが起動されます。
 * @Transactional により、テスト終了後に自動的にロールバックされます。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordChangeController - パスワード変更機能のテスト")
class PasswordChangeControllerTest {
    
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
     * 
     * テストユーザー「testuser」は初期データ（data-test.sql）から自動化読み込も
     * されますが、テスト毎に一意のスキーマが作成されるため、setUp() で
     * 明示的に作成して確実性を確保しています。
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
    
    @Test
    @DisplayName("正常系: パスワード変更画面を表示")
    void showPasswordChangePage_Success() throws Exception {
        mockMvc.perform(get("/users/password")
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("password-change"));
    }
    
    @Test
    @DisplayName("異常系: 未認証ユーザーはパスワード変更画面にアクセスできない")
    void showPasswordChangePage_Unauthorized() throws Exception {
        mockMvc.perform(get("/users/password"))
                .andExpect(status().is3xxRedirection());
    }
    
    @Test
    @DisplayName("異常系: 現在のパスワードが未入力")
    void changePassword_CurrentPasswordEmpty() throws Exception {
        // Given
        String currentPassword = "";
        String newPassword = "NewPass123!";
        String confirmPassword = "NewPass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "現在のパスワードを入力してください"));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードが未入力")
    void changePassword_NewPasswordEmpty() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "";
        String confirmPassword = "NewPass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "新しいパスワードを入力してください"));
    }
    
    @Test
    @DisplayName("異常系: 確認用パスワードが未入力")
    void changePassword_ConfirmPasswordEmpty() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        String confirmPassword = "";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "確認用パスワードを入力してください"));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードと確認用パスワードが一致しない")
    void changePassword_PasswordMismatch() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        String confirmPassword = "DifferentPass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "新しいパスワードと確認用パスワードが一致しません"));
    }
    
    @Test
    @DisplayName("異常系: 現在のパスワードが正しくない")
    void changePassword_CurrentPasswordIncorrect() throws Exception {
        // Given
        String currentPassword = "WrongPassword123!";
        String newPassword = "NewPass123!";
        String confirmPassword = "NewPass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "現在のパスワードが正しくありません"));
    }
    
    @Test
    @DisplayName("異常系: パスワードが要件を満たさない - 8文字未満")
    void changePassword_PasswordTooShort() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "Short1!";
        String confirmPassword = "Short1!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードは8文字以上である必要があります"));
    }
    
    @Test
    @DisplayName("異常系: CSRF トークンなしでリクエストを送信")
    void changePassword_WithoutCsrfToken() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        String confirmPassword = "NewPass123!";
        
        // When & Then
        // CSRFトークンなしの場合、302リダイレクト（認証エラー）を返す
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                // CSRFトークンを付けない
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection());
    }
    
    @Test
    @DisplayName("異常系: パスワードに大文字が含まれない")
    void changePassword_NoUpperCase() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "newpass123!";
        String confirmPassword = "newpass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードに英大文字（A-Z）を含める必要があります"));
    }
    
    @Test
    @DisplayName("異常系: パスワードに小文字が含まれない")
    void changePassword_NoLowerCase() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NEWPASS123!";
        String confirmPassword = "NEWPASS123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードに英小文字（a-z）を含める必要があります"));
    }
    
    @Test
    @DisplayName("異常系: パスワードに数字が含まれない")
    void changePassword_NoDigit() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass!";
        String confirmPassword = "NewPass!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードに数字（0-9）を含める必要があります"));
    }
    
    @Test
    @DisplayName("異常系: パスワードに特殊文字が含まれない")
    void changePassword_NoSpecialCharacter() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123";
        String confirmPassword = "NewPass123";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードに特殊文字（!@#$%^&*）を含める必要があります"));
    }
    
    @Test
    @DisplayName("異常系: パスワードが72バイトを超える - BCryptの制限")
    void changePassword_PasswordTooLong() throws Exception {
        // Given
        // BCryptは72バイトまでしかサポートしないため、それを超えるパスワードはエラーになる
        String currentPassword = "OldPass123!";
        String newPassword = "A" + "a1!" + "x".repeat(70); // 72バイト超過
        String confirmPassword = newPassword;
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "password cannot be more than 72 bytes"));
    }
    
    @Test
    @DisplayName("異常系: パスワードが空白文字のみ")
    void changePassword_OnlyWhitespace() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "     ";
        String confirmPassword = "     ";
        
        // When & Then
        // 空白文字のみは8文字未満なので、エラーメッセージは最小文字数チェックのものになる
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "パスワードは8文字以上である必要があります"));
    }
    
    @Test
    @DisplayName("異常系: 前回と同じパスワードに変更")
    void changePassword_SameAsPreviousPassword() throws Exception {
        // Given
        String currentPassword = "OldPass123!";
        String newPassword = "OldPass123!"; // 前回と同じ
        String confirmPassword = "OldPass123!";
        
        // When & Then
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("errorMessage", "新しいパスワードは現在のパスワードと異なる必要があります"));
    }
    
    @Test
    @DisplayName("正常系: パスワード変更が成功する")
    void changePassword_Success() throws Exception {
        // Given: 正しい現在のパスワードと、要件を満たす新しいパスワード
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass456!";
        String confirmPassword = "NewPass456!";
        
        // When & Then: パスワード変更が成功
        mockMvc.perform(post("/users/password")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/password"))
                .andExpect(flash().attribute("successMessage", "パスワードを変更しました"));
    }
}
