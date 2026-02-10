package com.inventory.inventory_management.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;

/**
 * UserRepositoryのテストクラス
 * データベースとの連携をテストする統合テストです
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("UserRepository統合テスト")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Role testRole;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        // テスト用ロールの取得または作成（既存データとの重複を回避）
        Optional<Role> existingRole = roleRepository.findByRoleName("ROLE_USER");
        if (existingRole.isPresent()) {
            testRole = existingRole.get();
        } else {
            testRole = new Role();
            testRole.setRoleName("ROLE_USER");
            testRole.setDescription("一般ユーザー");
            testRole.setCreatedAt(LocalDateTime.now());
            testRole = roleRepository.save(testRole);
        }

        // テスト用ユーザーの取得または作成（既存データとの重複を回避）
        Optional<User> existingUser = userRepository.findByUsername("testuser");
        if (existingUser.isPresent()) {
            testUser = existingUser.get();
        } else {
            testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
            testUser.setEmail("testuser@example.com");
            testUser.setFullName("テストユーザー");
            testUser.setIsActive(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            testUser = userRepository.save(testUser);
        }

        // ユーザーとロールの関連がなければ作成
        boolean hasRole = testUser.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_USER"));
        if (!hasRole) {
            UserRole userRole = new UserRole();
            userRole.setUser(testUser);
            userRole.setRole(testRole);
            userRole.setAssignedAt(LocalDateTime.now());
            testUser.getUserRoles().add(userRole);
            userRepository.save(testUser);
        }
    }

    /**
     * 正常系：ユーザー名でユーザーを検索できる
     */
    @Test
    @DisplayName("正常系：findByUsername()でユーザーを取得できる")
    void testFindByUsername_Success() {
        // when
        Optional<User> result = userRepository.findByUsername("testuser");

        // then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("testuser@example.com", result.get().getEmail());
        assertEquals("テストユーザー", result.get().getFullName());
        assertTrue(result.get().getIsActive());
    }

    /**
     * 正常系：存在しないユーザー名で検索すると空のOptionalが返る
     */
    @Test
    @DisplayName("正常系：存在しないユーザー名で検索するとOptional.emptyが返る")
    void testFindByUsername_NotFound() {
        // when
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // then
        assertFalse(result.isPresent());
    }

    /**
     * 正常系：メールアドレスでユーザーを検索できる
     */
    @Test
    @DisplayName("正常系：findByEmail()でユーザーを取得できる")
    void testFindByEmail_Success() {
        // when
        Optional<User> result = userRepository.findByEmail("testuser@example.com");

        // then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("testuser@example.com", result.get().getEmail());
    }

    /**
     * 正常系：存在しないメールアドレスで検索すると空のOptionalが返る
     */
    @Test
    @DisplayName("正常系：存在しないメールアドレスで検索するとOptional.emptyが返る")
    void testFindByEmail_NotFound() {
        // when
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertFalse(result.isPresent());
    }

    /**
     * 正常系：findByUsernameWithRoles()でロール情報を含むユーザーを取得できる
     */
    @Test
    @DisplayName("正常系：findByUsernameWithRoles()でロール情報を含むユーザーを取得できる")
    void testFindByUsernameWithRoles_Success() {
        // when
        Optional<User> result = userRepository.findByUsernameWithRoles("testuser");

        // then
        assertTrue(result.isPresent());
        User user = result.get();
        assertEquals("testuser", user.getUsername());
        
        // ロール情報が取得できることを確認（LazyInitializationExceptionが発生しない）
        assertNotNull(user.getUserRoles());
        assertFalse(user.getUserRoles().isEmpty());
        assertEquals(1, user.getUserRoles().size());
        assertEquals("ROLE_USER", user.getUserRoles().get(0).getRole().getRoleName());
    }

    /**
     * 正常系：存在しないユーザー名でfindByUsernameWithRolesを呼ぶと空のOptionalが返る
     */
    @Test
    @DisplayName("正常系：存在しないユーザー名でfindByUsernameWithRolesを呼ぶとOptional.emptyが返る")
    void testFindByUsernameWithRoles_NotFound() {
        // when
        Optional<User> result = userRepository.findByUsernameWithRoles("nonexistent");

        // then
        assertFalse(result.isPresent());
    }

    /**
     * 正常系：ロールが割り当てられていないユーザーでもfindByUsernameWithRolesで取得できる
     */
    @Test
    @DisplayName("正常系：ロールが割り当てられていないユーザーでもfindByUsernameWithRolesで取得できる")
    void testFindByUsernameWithRoles_NoRoles() {
        // given - ロールなしユーザーを作成
        User userWithoutRole = new User();
        userWithoutRole.setUsername("noroleuser");
        userWithoutRole.setPassword("password");
        userWithoutRole.setEmail("norole@example.com");
        userWithoutRole.setFullName("ロールなしユーザー");
        userWithoutRole.setIsActive(true);
        userWithoutRole.setCreatedAt(LocalDateTime.now());
        userWithoutRole.setUpdatedAt(LocalDateTime.now());
        userRepository.save(userWithoutRole);

        // when
        Optional<User> result = userRepository.findByUsernameWithRoles("noroleuser");

        // then
        assertTrue(result.isPresent());
        User user = result.get();
        assertEquals("noroleuser", user.getUsername());
        assertNotNull(user.getUserRoles());
        assertTrue(user.getUserRoles().isEmpty()); // ロールがない
    }

    /**
     * 正常系：複数のロールを持つユーザーをfindByUsernameWithRolesで取得できる
     */
    @Test
    @DisplayName("正常系：複数のロールを持つユーザーをfindByUsernameWithRolesで取得できる")
    void testFindByUsernameWithRoles_MultipleRoles() {
        // given - 管理者ロールの取得または作成（既存データとの重複を回避）
        Role adminRole;
        Optional<Role> existingAdminRole = roleRepository.findByRoleName("ROLE_ADMIN");
        if (existingAdminRole.isPresent()) {
            adminRole = existingAdminRole.get();
        } else {
            adminRole = new Role();
            adminRole.setRoleName("ROLE_ADMIN");
            adminRole.setDescription("管理者");
            adminRole.setCreatedAt(LocalDateTime.now());
            adminRole = roleRepository.save(adminRole);
        }

        // 既存ユーザーに管理者ロールを追加
        User user = userRepository.findByUsername("testuser").get();
        UserRole adminUserRole = new UserRole();
        adminUserRole.setUser(user);
        adminUserRole.setRole(adminRole);
        adminUserRole.setAssignedAt(LocalDateTime.now());
        user.getUserRoles().add(adminUserRole);
        userRepository.save(user);

        // when
        Optional<User> result = userRepository.findByUsernameWithRoles("testuser");

        // then
        assertTrue(result.isPresent());
        User foundUser = result.get();
        assertEquals(2, foundUser.getUserRoles().size());
        
        // 両方のロールが取得できることを確認
        boolean hasUserRole = foundUser.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_USER"));
        boolean hasAdminRole = foundUser.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_ADMIN"));
        
        assertTrue(hasUserRole);
        assertTrue(hasAdminRole);
    }

    /**
     * 正常系：ユーザーを保存できる
     */
    @Test
    @DisplayName("正常系：ユーザーを保存できる")
    void testSave_Success() {
        // given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("newuser@example.com");
        newUser.setFullName("新規ユーザー");
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // when
        User savedUser = userRepository.save(newUser);

        // then
        assertNotNull(savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        
        // データベースから取得して確認
        Optional<User> found = userRepository.findByUsername("newuser");
        assertTrue(found.isPresent());
        assertEquals("newuser@example.com", found.get().getEmail());
    }

    /**
     * 正常系：ユーザーを削除できる
     */
    @Test
    @DisplayName("正常系：ユーザーを削除できる")
    void testDelete_Success() {
        // given
        Optional<User> user = userRepository.findByUsername("testuser");
        assertTrue(user.isPresent());

        // when
        userRepository.delete(user.get());
        userRepository.flush();

        // then
        Optional<User> deletedUser = userRepository.findByUsername("testuser");
        assertFalse(deletedUser.isPresent());
    }

    /**
     * 正常系：is_active=falseのユーザーも取得できる
     */
    @Test
    @DisplayName("正常系：is_active=falseのユーザーも取得できる")
    void testFindByUsername_InactiveUser() {
        // given - 無効なユーザーを作成
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setPassword("password");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setFullName("無効ユーザー");
        inactiveUser.setIsActive(false); // 無効
        inactiveUser.setCreatedAt(LocalDateTime.now());
        inactiveUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(inactiveUser);

        // when
        Optional<User> result = userRepository.findByUsername("inactiveuser");

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get().getIsActive());
    }
}
