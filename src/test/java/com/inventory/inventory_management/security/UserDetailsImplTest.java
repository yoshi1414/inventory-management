package com.inventory.inventory_management.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;

/**
 * UserDetailsImplのテストクラス
 * UserDetails実装の動作をテストします
 */
@DisplayName("UserDetailsImpl単体テスト")
class UserDetailsImplTest {

    private User testUser;
    private Collection<GrantedAuthority> authorities;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
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

        // テスト用ロールの作成
        Role testRole = new Role();
        testRole.setId(1);
        testRole.setRoleName("ROLE_USER");
        testRole.setDescription("一般ユーザー");
        testRole.setCreatedAt(LocalDateTime.now());

        UserRole testUserRole = new UserRole();
        testUserRole.setId(1);
        testUserRole.setUser(testUser);
        testUserRole.setRole(testRole);
        testUserRole.setAssignedAt(LocalDateTime.now());

        testUser.setUserRoles(Arrays.asList(testUserRole));

        // 権限のコレクションを作成
        authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 正常系：getUsernameが正しい値を返す
     */
    @Test
    @DisplayName("正常系：getUsername()が正しい値を返す")
    void testGetUsername() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        String username = userDetails.getUsername();

        // then
        assertEquals("testuser", username);
    }

    /**
     * 正常系：getPasswordが正しい値を返す
     */
    @Test
    @DisplayName("正常系：getPassword()が正しい値を返す")
    void testGetPassword() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        String password = userDetails.getPassword();

        // then
        assertEquals("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG", password);
    }

    /**
     * 正常系：getAuthoritiesが正しい権限リストを返す
     */
    @Test
    @DisplayName("正常系：getAuthorities()が正しい権限リストを返す")
    void testGetAuthorities() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        Collection<? extends GrantedAuthority> result = userDetails.getAuthorities();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    /**
     * 正常系：複数の権限を持つ場合
     */
    @Test
    @DisplayName("正常系：複数の権限を正しく返す")
    void testGetAuthorities_MultipleRoles() {
        // given
        authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        Collection<? extends GrantedAuthority> result = userDetails.getAuthorities();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(result.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * 正常系：isEnabledがユーザーのis_activeを反映する（true）
     */
    @Test
    @DisplayName("正常系：isEnabled()がis_active=trueを反映する")
    void testIsEnabled_True() {
        // given
        testUser.setIsActive(true);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        boolean enabled = userDetails.isEnabled();

        // then
        assertTrue(enabled);
    }

    /**
     * 正常系：isEnabledがユーザーのis_activeを反映する（false）
     */
    @Test
    @DisplayName("正常系：isEnabled()がis_active=falseを反映する")
    void testIsEnabled_False() {
        // given
        testUser.setIsActive(false);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        boolean enabled = userDetails.isEnabled();

        // then
        assertFalse(enabled);
    }

    /**
     * 正常系：isAccountNonExpiredが常にtrueを返す
     */
    @Test
    @DisplayName("正常系：isAccountNonExpired()が常にtrueを返す")
    void testIsAccountNonExpired() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        boolean accountNonExpired = userDetails.isAccountNonExpired();

        // then
        assertTrue(accountNonExpired);
    }

    /**
     * 正常系：isAccountNonLockedが常にtrueを返す
     */
    @Test
    @DisplayName("正常系：isAccountNonLocked()が常にtrueを返す")
    void testIsAccountNonLocked() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        boolean accountNonLocked = userDetails.isAccountNonLocked();

        // then
        assertTrue(accountNonLocked);
    }

    /**
     * 正常系：isCredentialsNonExpiredが常にtrueを返す
     */
    @Test
    @DisplayName("正常系：isCredentialsNonExpired()が常にtrueを返す")
    void testIsCredentialsNonExpired() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        boolean credentialsNonExpired = userDetails.isCredentialsNonExpired();

        // then
        assertTrue(credentialsNonExpired);
    }

    /**
     * 正常系：getUserメソッドで元のUserエンティティを取得できる
     */
    @Test
    @DisplayName("正常系：getUser()で元のUserエンティティを取得できる")
    void testGetUser() {
        // given
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, authorities);

        // when
        User user = userDetails.getUser();

        // then
        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
        assertEquals(testUser.getUsername(), user.getUsername());
        assertEquals(testUser.getEmail(), user.getEmail());
        assertEquals(testUser.getFullName(), user.getFullName());
    }

    /**
     * 正常系：権限が空のコレクションでも動作する
     */
    @Test
    @DisplayName("正常系：権限が空のコレクションでも動作する")
    void testWithEmptyAuthorities() {
        // given
        Collection<GrantedAuthority> emptyAuthorities = Arrays.asList();
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser, emptyAuthorities);

        // when
        Collection<? extends GrantedAuthority> result = userDetails.getAuthorities();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
