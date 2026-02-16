package com.inventory.inventory_management.security;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * AuthenticationFailureListenerのテストクラス
 * ログイン失敗時のイベント処理をテストします
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFailureListener単体テスト")
class AuthenticationFailureListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationFailureListener authenticationFailureListener;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    private Authentication authentication;
    private BadCredentialsException badCredentialsException;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken("testuser", "wrongpassword");
        badCredentialsException = new BadCredentialsException("Bad credentials");
    }

    /**
     * 正常系：認証失敗イベント発生時、loginFailedが呼ばれる
     */
    @Test
    @DisplayName("正常系：認証失敗イベント発生時、loginFailedが呼ばれる")
    void testOnApplicationEvent_Failure() {
        // given
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("testuser");
    }

    /**
     * 正常系：WebAuthenticationDetails付きの認証失敗イベント
     */
    @Test
    @DisplayName("正常系：WebAuthenticationDetails付きの認証失敗イベント")
    void testOnApplicationEvent_WithWebAuthenticationDetails() {
        // given
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn("192.168.1.100");
        when(webAuthenticationDetails.getSessionId()).thenReturn("SESSION456");
        
        authentication = new UsernamePasswordAuthenticationToken("testuser", "wrongpassword");
        ((UsernamePasswordAuthenticationToken) authentication).setDetails(webAuthenticationDetails);
        
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("testuser");
        verify(webAuthenticationDetails, times(1)).getRemoteAddress();
        verify(webAuthenticationDetails, times(1)).getSessionId();
    }

    /**
     * 正常系：異なるユーザー名での認証失敗
     */
    @Test
    @DisplayName("正常系：異なるユーザー名での認証失敗")
    void testOnApplicationEvent_DifferentUsername() {
        // given
        authentication = new UsernamePasswordAuthenticationToken("admin", "wrongpassword");
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("admin");
    }

    /**
     * 正常系：管理者ユーザーでの認証失敗
     */
    @Test
    @DisplayName("正常系：管理者ユーザー（adminuser）での認証失敗")
    void testOnApplicationEvent_AdminUserFailure() {
        // given
        authentication = new UsernamePasswordAuthenticationToken("adminuser", "wrongpassword");
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("adminuser");
    }

    /**
     * 正常系：複数回の認証失敗イベント
     */
    @Test
    @DisplayName("正常系：複数回の認証失敗イベントが発生した場合")
    void testOnApplicationEvent_MultipleFailures() {
        // given
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);
        authenticationFailureListener.onApplicationEvent(event);
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(3)).loginFailed("testuser");
    }

    /**
     * 異常系：LoginAttemptServiceが例外をスローした場合でもエラーにならない
     */
    @Test
    @DisplayName("異常系：LoginAttemptServiceが例外をスローしても処理が継続する")
    void testOnApplicationEvent_LoginAttemptServiceThrowsException() {
        // given
        doThrow(new RuntimeException("Test exception")).when(loginAttemptService).loginFailed(anyString());
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when & then
        // 例外がスローされずに処理が完了することを確認
        authenticationFailureListener.onApplicationEvent(event);
        
        verify(loginAttemptService, times(1)).loginFailed("testuser");
    }

    /**
     * 正常系：Details情報がnullの場合でも処理が継続する
     */
    @Test
    @DisplayName("正常系：Details情報がnullの場合でも処理が継続する")
    void testOnApplicationEvent_NullDetails() {
        // given
        authentication = new UsernamePasswordAuthenticationToken("testuser", "wrongpassword");
        ((UsernamePasswordAuthenticationToken) authentication).setDetails(null);
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("testuser");
    }

    /**
     * 正常系：Details情報がWebAuthenticationDetails以外の型の場合
     */
    @Test
    @DisplayName("正常系：Details情報がWebAuthenticationDetails以外の型の場合でも処理が継続する")
    void testOnApplicationEvent_NonWebAuthenticationDetails() {
        // given
        authentication = new UsernamePasswordAuthenticationToken("testuser", "wrongpassword");
        ((UsernamePasswordAuthenticationToken) authentication).setDetails("SomeOtherDetails");
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("testuser");
    }

    /**
     * 正常系：空のユーザー名での認証失敗
     */
    @Test
    @DisplayName("正常系：空のユーザー名での認証失敗")
    void testOnApplicationEvent_EmptyUsername() {
        // given
        authentication = new UsernamePasswordAuthenticationToken("", "wrongpassword");
        AuthenticationFailureBadCredentialsEvent event = 
            new AuthenticationFailureBadCredentialsEvent(authentication, badCredentialsException);

        // when
        authenticationFailureListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginFailed("");
    }
}
