package com.inventory.inventory_management.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * UserDetailsServiceImplのテストクラス
 * ログイン認証のコアロジックをテストします
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl単体テスト")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private Role testRole;
    private UserRole testUserRole;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        // テスト用ロールの作成
        testRole = new Role();
        testRole.setId(1);
        testRole.setRoleName("ROLE_USER");
        testRole.setDescription("一般ユーザー");
        testRole.setCreatedAt(LocalDateTime.now());

        // テスト用ユーザーの作成
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        testUser.setEmail("testuser@example.com");
        testUser.setFullName("テストユーザー");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // テスト用ユーザーロールの作成
        testUserRole = new UserRole();
        testUserRole.setId(1);
        testUserRole.setUser(testUser);
        testUserRole.setRole(testRole);
        testUserRole.setAssignedAt(LocalDateTime.now());

        testUser.setUserRoles(Arrays.asList(testUserRole));
    }

    /**
     * 正常系：ユーザー名が存在する場合、正しいUserDetailsを返す
     */
    @Test
    @DisplayName("正常系：有効なユーザー名でUserDetailsを取得できる")
    void testLoadUserByUsername_Success() {
        // given
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }

    /**
     * 正常系：ロール情報が正しく取得できる
     */
    @Test
    @DisplayName("正常系：ロール情報が正しく取得できる")
    void testLoadUserByUsername_WithRoles() {
        // given
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertNotNull(userDetails.getAuthorities());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }

    /**
     * 正常系：ROLE_プレフィックスが自動追加される
     */
    @Test
    @DisplayName("正常系：ROLE_プレフィックスがないロールに自動追加される")
    void testLoadUserByUsername_RolePrefixAdded() {
        // given
        testRole.setRoleName("USER"); // プレフィックスなし
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }

    /**
     * 正常系：複数のロールを持つユーザー
     */
    @Test
    @DisplayName("正常系：複数のロールを持つユーザーのロール情報が正しく取得できる")
    void testLoadUserByUsername_WithMultipleRoles() {
        // given
        Role adminRole = new Role();
        adminRole.setId(2);
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setDescription("管理者");
        adminRole.setCreatedAt(LocalDateTime.now());

        UserRole adminUserRole = new UserRole();
        adminUserRole.setId(2);
        adminUserRole.setUser(testUser);
        adminUserRole.setRole(adminRole);
        adminUserRole.setAssignedAt(LocalDateTime.now());

        testUser.setUserRoles(Arrays.asList(testUserRole, adminUserRole));

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }

    /**
     * 異常系：ユーザー名が存在しない場合、UsernameNotFoundExceptionをスロー
     */
    @Test
    @DisplayName("異常系：存在しないユーザー名でUsernameNotFoundExceptionがスローされる")
    void testLoadUserByUsername_UserNotFound() {
        // given
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("nonexistent")).thenReturn(Optional.empty());

        // when & then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertEquals("ログインに失敗しました。ユーザー名またはパスワードが正しくありません。", exception.getMessage());

        verify(loginAttemptService, times(1)).isBlocked("nonexistent");
        verify(userRepository, times(1)).findByUsernameWithRoles("nonexistent");
    }

    /**
     * 異常系：ブロックされているユーザーの場合、UsernameNotFoundExceptionをスロー
     */
    @Test
    @DisplayName("異常系：ブロックされているユーザーでUsernameNotFoundExceptionがスローされる")
    void testLoadUserByUsername_UserBlocked() {
        // given
        when(loginAttemptService.isBlocked("testuser")).thenReturn(true);

        // when & then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("testuser")
        );

        assertEquals("ログインに失敗しました。ユーザー名またはパスワードが正しくありません。", exception.getMessage());

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, never()).findByUsernameWithRoles(anyString());
    }

    /**
     * 異常系：無効なユーザー（is_active=false）
     */
    @Test
    @DisplayName("正常系：無効なユーザー（is_active=false）の場合でもUserDetailsは取得できる")
    void testLoadUserByUsername_InactiveUser() {
        // given
        testUser.setIsActive(false);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled()); // is_active=falseなのでisEnabled()もfalse

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }

    /**
     * 異常系：ロールが割り当てられていないユーザー
     */
    @Test
    @DisplayName("正常系：ロールが割り当てられていないユーザーでも取得できる")
    void testLoadUserByUsername_NoRoles() {
        // given
        testUser.setUserRoles(new ArrayList<>());
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // then
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().isEmpty());

        verify(loginAttemptService, times(1)).isBlocked("testuser");
        verify(userRepository, times(1)).findByUsernameWithRoles("testuser");
    }
}
