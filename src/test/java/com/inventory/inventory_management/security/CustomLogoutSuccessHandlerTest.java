package com.inventory.inventory_management.security;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CustomLogoutSuccessHandler の単体テストクラス
 * 
 * <p>
 * ログアウト成功時の処理について、ロール情報に基づいた
 * リダイレクト先の判定をテストします。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomLogoutSuccessHandler - ログアウト成功ハンドラー")
class CustomLogoutSuccessHandlerTest {
    
    @InjectMocks
    private CustomLogoutSuccessHandler handler;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    /**
     * 【単体】管理者（ROLE_ADMIN）ログアウト時は /admin/login?logout にリダイレクト
     * <p>
     * 条件: ROLE_ADMIN ロールを持つユーザーでログアウト
     * 期待値: /admin/login?logout へリダイレクト
     * </p>
     */
    @Test
    @DisplayName("【単体】ROLE_ADMIN ユーザーは /admin/login?logout にリダイレクト")
    void testAdminLogoutRedirectsToAdminLogin() throws IOException, ServletException {
        // Arrange: ROLE_ADMIN を持つ Authentication を作成
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("adminuser");
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        
        // Act: ログアウト成功処理を実行
        handler.onLogoutSuccess(request, response, auth);
        
        // Assert: /admin/login?logout へリダイレクトされることを確認
        verify(response).sendRedirect("/admin/login?logout");
    }
    
    /**
     * 【単体】一般ユーザー（ROLE_USER）ログアウト時は /login?logout にリダイレクト
     * <p>
     * 条件: ROLE_USER ロールを持つユーザーでログアウト
     * 期待値: /login?logout へリダイレクト
     * </p>
     */
    @Test
    @DisplayName("【単体】ROLE_USER ユーザーは /login?logout にリダイレクト")
    void testUserLogoutRedirectsToUserLogin() throws IOException, ServletException {
        // Arrange: ROLE_USER を持つ Authentication を作成
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        
        // Act: ログアウト成功処理を実行
        handler.onLogoutSuccess(request, response, auth);
        
        // Assert: /login?logout へリダイレクトされることを確認
        verify(response).sendRedirect("/login?logout");
    }
    
    /**
     * 【単体】複数ロール（ROLE_ADMIN + ROLE_USER）の場合
     * <p>
     * 条件: ROLE_ADMIN と ROLE_USER 両方を持つユーザーでログアウト
     * 期待値: /admin/login?logout へリダイレクト （ROLE_ADMIN が優先）
     * </p>
     */
    @Test
    @DisplayName("【単体】ROLE_ADMIN と ROLE_USER 両方の場合は /admin/login?logout にリダイレクト")
    void testMultipleRolesWithAdminRedirectsToAdminLogin() throws IOException, ServletException {
        // Arrange: ROLE_ADMIN と ROLE_USER を持つ Authentication を作成
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("adminuser");
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        
        // Act: ログアウト成功処理を実行
        handler.onLogoutSuccess(request, response, auth);
        
        // Assert: /admin/login?logout へリダイレクトされることを確認（ROLE_ADMIN が優先）
        verify(response).sendRedirect("/admin/login?logout");
    }
    
    /**
     * 【単体】認証情報が null の場合
     * <p>
     * 条件: 認証情報が null でログアウト
     * 期待値: /login?logout へリダイレクト （デフォルト値）
     * </p>
     */
    @Test
    @DisplayName("【単体】認証情報が null の場合は /login?logout にリダイレクト")
    void testNullAuthenticationRedirectsToDefaultLogin() throws IOException, ServletException {
        // Act: ログアウト成功処理を実行
        handler.onLogoutSuccess(request, response, null);
        
        // Assert: /login?logout へリダイレクトされることを確認（デフォルト値）
        verify(response).sendRedirect("/login?logout");
    }

    /**
     * 【単体】認証情報のユーザー名が null の場合
     * <p>
     * 条件: authentication は存在するが getName() が null
     * 期待値: /login?logout へリダイレクト
     * </p>
     */
    @Test
    @DisplayName("【単体】ユーザー名が null の場合は /login?logout にリダイレクト")
    void testNullUsernameRedirectsToDefaultLogin() throws IOException, ServletException {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);

        handler.onLogoutSuccess(request, response, auth);

        verify(response).sendRedirect("/login?logout");
    }

    /**
     * 【単体】セッションが存在する場合のログ出力分岐
     * <p>
     * 条件: authentication が有効で session も存在
     * 期待値: /login?logout へリダイレクト
     * </p>
     */
    @Test
    @DisplayName("【単体】セッションありの一般ユーザーは /login?logout にリダイレクト")
    void testUserLogoutWithSessionRedirectsToUserLogin() throws IOException, ServletException {
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        Authentication auth = mock(Authentication.class);
        jakarta.servlet.http.HttpSession session = mock(jakarta.servlet.http.HttpSession.class);
        when(auth.getName()).thenReturn("user");
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-1");

        handler.onLogoutSuccess(request, response, auth);

        verify(response).sendRedirect("/login?logout");
    }
    
    /**
     * 【単体】例外発生時のエラーハンドリング
     * <p>
     * 条件: リダイレクト処理中に例外が発生
     * 期待値: キャッチされて、エラー時のリダイレクト処理が実行される
     * </p>
     */
    @Test
    @DisplayName("【単体】例外発生時は /login?error にリダイレクト")
    void testExceptionDuringLogoutRedirectsToErrorPage() throws IOException, ServletException {
        // Arrange: ROLE_USER を持つ Authentication を作成
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        
        // リダイレクト処理で例外発生時の処理を2段階で行う
        // 1回目の呼び出しで /login?logout に遷移しようとして例外発生
        // 2回目の呼び出しで /login?error に遷移
        doThrow(new IOException("Test exception")).when(response).sendRedirect("/login?logout");
        doNothing().when(response).sendRedirect("/login?error");
        
        // Act: ログアウト成功処理を実行
        handler.onLogoutSuccess(request, response, auth);
        
        // Assert: /login?error へリダイレクトされることを確認
        verify(response).sendRedirect("/login?error");
    }
}
