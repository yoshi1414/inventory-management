package com.inventory.inventory_management.security;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collections;

/**
 * AuthenticationSuccessListenerのテストクラス
 * ログイン成功時のイベント処理をテストします
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationSuccessListener単体テスト")
class AuthenticationSuccessListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationSuccessListener authenticationSuccessListener;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    private Authentication authentication;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    /**
     * 正常系：認証成功イベント発生時、loginSucceededが呼ばれる
     */
    @Test
    @DisplayName("正常系：認証成功イベント発生時、loginSucceededが呼ばれる")
    void testOnApplicationEvent_Success() {
        // given
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("testuser");
    }

    /**
     * 正常系：WebAuthenticationDetails付きの認証成功イベント
     */
    @Test
    @DisplayName("正常系：WebAuthenticationDetails付きの認証成功イベント")
    void testOnApplicationEvent_WithWebAuthenticationDetails() {
        // given
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn("127.0.0.1");
        when(webAuthenticationDetails.getSessionId()).thenReturn("SESSION123");
        
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        ((UsernamePasswordAuthenticationToken) authentication).setDetails(webAuthenticationDetails);
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("testuser");
        verify(webAuthenticationDetails, times(1)).getRemoteAddress();
        verify(webAuthenticationDetails, times(1)).getSessionId();
    }

    /**
     * 正常系：異なるユーザー名での認証成功
     */
    @Test
    @DisplayName("正常系：異なるユーザー名での認証成功")
    void testOnApplicationEvent_DifferentUsername() {
        // given
        authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("admin");
    }

    /**
     * 正常系：管理者ユーザーでの認証成功
     */
    @Test
    @DisplayName("正常系：管理者ユーザー（adminuser）での認証成功")
    void testOnApplicationEvent_AdminUserSuccess() {
        // given
        authentication = new UsernamePasswordAuthenticationToken(
                "adminuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("adminuser");
    }

    /**
     * 異常系：LoginAttemptServiceが例外をスローした場合でもエラーにならない
     */
    @Test
    @DisplayName("異常系：LoginAttemptServiceが例外をスローしても処理が継続する")
    void testOnApplicationEvent_LoginAttemptServiceThrowsException() {
        // given
        doThrow(new RuntimeException("Test exception")).when(loginAttemptService).loginSucceeded(anyString());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when & then
        // 例外がスローされずに処理が完了することを確認
        authenticationSuccessListener.onApplicationEvent(event);
        
        verify(loginAttemptService, times(1)).loginSucceeded("testuser");
    }

    /**
     * 正常系：Details情報がnullの場合でも処理が継続する
     */
    @Test
    @DisplayName("正常系：Details情報がnullの場合でも処理が継続する")
    void testOnApplicationEvent_NullDetails() {
        // given
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        ((UsernamePasswordAuthenticationToken) authentication).setDetails(null);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("testuser");
    }

    /**
     * 正常系：Details情報がWebAuthenticationDetails以外の型の場合
     */
    @Test
    @DisplayName("正常系：Details情報がWebAuthenticationDetails以外の型の場合でも処理が継続する")
    void testOnApplicationEvent_NonWebAuthenticationDetails() {
        // given
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        ((UsernamePasswordAuthenticationToken) authentication).setDetails("SomeOtherDetails");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        authenticationSuccessListener.onApplicationEvent(event);

        // then
        verify(loginAttemptService, times(1)).loginSucceeded("testuser");
    }
}
