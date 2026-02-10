package com.inventory.inventory_management.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LoginAttemptServiceのテストクラス
 * ログイン試行回数管理とブロック機能をテストします
 */
@DisplayName("LoginAttemptService単体テスト")
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    /**
     * 正常系：初期状態ではブロックされていない
     */
    @Test
    @DisplayName("正常系：初期状態ではブロックされていない")
    void testIsBlocked_InitiallyNotBlocked() {
        // when
        boolean isBlocked = loginAttemptService.isBlocked("testuser");

        // then
        assertFalse(isBlocked);
    }

    /**
     * 正常系：ログイン失敗時、試行回数が増加する
     */
    @Test
    @DisplayName("正常系：ログイン失敗時、試行回数が記録される")
    void testLoginFailed_IncreasesAttemptCount() {
        // given
        String username = "testuser";

        // when
        loginAttemptService.loginFailed(username);

        // then
        assertFalse(loginAttemptService.isBlocked(username)); // まだブロックされていない
    }

    /**
     * 正常系：5回失敗でブロックされる
     */
    @Test
    @DisplayName("正常系：5回の失敗でユーザーがブロックされる")
    void testLoginFailed_BlocksAfterFiveAttempts() {
        // given
        String username = "testuser";

        // when
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(username);
        }

        // then
        assertTrue(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：4回失敗ではブロックされない
     */
    @Test
    @DisplayName("正常系：4回の失敗ではブロックされない")
    void testLoginFailed_NotBlockedAfterFourAttempts() {
        // given
        String username = "testuser";

        // when
        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(username);
        }

        // then
        assertFalse(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：6回以上失敗してもブロックされ続ける
     */
    @Test
    @DisplayName("正常系：6回以上失敗してもブロックされ続ける")
    void testLoginFailed_StaysBlockedAfterMoreThanFiveAttempts() {
        // given
        String username = "testuser";

        // when
        for (int i = 0; i < 10; i++) {
            loginAttemptService.loginFailed(username);
        }

        // then
        assertTrue(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：ログイン成功時、試行回数がリセットされる
     */
    @Test
    @DisplayName("正常系：ログイン成功時、試行回数がリセットされる")
    void testLoginSucceeded_ResetsAttemptCount() {
        // given
        String username = "testuser";
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        // when
        loginAttemptService.loginSucceeded(username);

        // then
        assertFalse(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：ブロック状態からログイン成功で解除される
     */
    @Test
    @DisplayName("正常系：ブロック状態からログイン成功で解除される")
    void testLoginSucceeded_UnblocksUser() {
        // given
        String username = "testuser";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(username);
        }
        assertTrue(loginAttemptService.isBlocked(username));

        // when
        loginAttemptService.loginSucceeded(username);

        // then
        assertFalse(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：異なるユーザーは独立して管理される
     */
    @Test
    @DisplayName("正常系：異なるユーザーは独立して管理される")
    void testMultipleUsers_IndependentTracking() {
        // given
        String user1 = "user1";
        String user2 = "user2";

        // when
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(user1);
        }
        loginAttemptService.loginFailed(user2);

        // then
        assertTrue(loginAttemptService.isBlocked(user1));
        assertFalse(loginAttemptService.isBlocked(user2));
    }

    /**
     * 正常系：ログイン成功後、再度失敗のカウントが始まる
     */
    @Test
    @DisplayName("正常系：ログイン成功後、再度失敗のカウントが始まる")
    void testLoginFailed_AfterSuccess() {
        // given
        String username = "testuser";
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginSucceeded(username);

        // when
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        // then
        assertFalse(loginAttemptService.isBlocked(username)); // まだ2回なのでブロックされない
    }

    /**
     * 正常系：null または空文字のユーザー名でも動作する
     */
    @Test
    @DisplayName("正常系：空文字のユーザー名でも動作する")
    void testWithEmptyUsername() {
        // given
        String username = "";

        // when
        loginAttemptService.loginFailed(username);

        // then
        assertFalse(loginAttemptService.isBlocked(username));
    }

    /**
     * 正常系：ログイン試行がない状態でログイン成功を呼んでもエラーにならない
     */
    @Test
    @DisplayName("正常系：ログイン試行がない状態でログイン成功を呼んでもエラーにならない")
    void testLoginSucceeded_WithoutPriorAttempts() {
        // given
        String username = "testuser";

        // when & then
        assertDoesNotThrow(() -> loginAttemptService.loginSucceeded(username));
        assertFalse(loginAttemptService.isBlocked(username));
    }
}
