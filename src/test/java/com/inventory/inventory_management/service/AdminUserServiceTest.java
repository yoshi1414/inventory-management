package com.inventory.inventory_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventory.inventory_management.dto.request.UserSearchCriteriaDto;
import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;
import com.inventory.inventory_management.form.UserCreateForm;
import com.inventory.inventory_management.form.UserEditForm;
import com.inventory.inventory_management.repository.RoleRepository;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * AdminUserService のユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService ユニットテスト")
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("searchUsers: active指定時にisActive=trueで検索される")
    void searchUsers_WithActiveFilter_UsesTrueFlag() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setSearch("  test ");
        criteria.setStatus("active");
        criteria.setSort("username");
        criteria.setPage(0);

        Page<User> expected = new PageImpl<>(List.of(new User()));
        when(userRepository.searchUsers(eq("test"), eq(Boolean.TRUE), eq(null), any(Pageable.class)))
                .thenReturn(expected);

        Page<User> actual = adminUserService.searchUsers(criteria);

        assertEquals(expected, actual);
        verify(userRepository).searchUsers(eq("test"), eq(Boolean.TRUE), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("searchUsers: inactive指定時にisActive=falseで検索される")
    void searchUsers_WithInactiveFilter_UsesFalseFlag() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setStatus("inactive");
        criteria.setSort("created_desc");
        criteria.setPage(1);

        Page<User> expected = new PageImpl<>(List.of(new User()));
        when(userRepository.searchUsers(eq(null), eq(Boolean.FALSE), eq(null), any(Pageable.class)))
                .thenReturn(expected);

        Page<User> actual = adminUserService.searchUsers(criteria);

        assertEquals(expected, actual);
        verify(userRepository).searchUsers(eq(null), eq(Boolean.FALSE), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("searchUsers: roleId指定時にロール条件で検索される")
    void searchUsers_WithRoleId_UsesRoleFilter() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setSearch("keyword");
        criteria.setRoleId(2);
        criteria.setSort("username_desc");
        criteria.setPage(0);

        Page<User> expected = new PageImpl<>(List.of(new User()));
        when(userRepository.searchUsers(eq("keyword"), eq(null), eq(2), any(Pageable.class)))
                .thenReturn(expected);

        Page<User> actual = adminUserService.searchUsers(criteria);

        assertEquals(expected, actual);
        verify(userRepository).searchUsers(eq("keyword"), eq(null), eq(2), any(Pageable.class));
    }

    @Test
    @DisplayName("createUser: 正常系でユーザーとロールが登録される")
    void createUser_Success_CreatesUserAndRole() {
        Role role = new Role();
        role.setId(1);
        role.setRoleName("ROLE_USER");

        UserCreateForm form = new UserCreateForm();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setFullName("New User");
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");
        form.setRoleId(1);
        form.setIsActive(true);

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(100);
            return saved;
        });

        User created = adminUserService.createUser(form);

        assertEquals(100, created.getId());
        assertEquals("newuser", created.getUsername());
        assertEquals("new@example.com", created.getEmail());
        assertEquals("encoded-password", created.getPassword());
        assertEquals(1, created.getUserRoles().size());
        assertEquals("ROLE_USER", created.getUserRoles().get(0).getRole().getRoleName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: ユーザー名重複時は例外")
    void createUser_WhenUsernameDuplicate_ThrowsException() {
        UserCreateForm form = new UserCreateForm();
        form.setUsername("dupuser");
        form.setEmail("new@example.com");
        form.setFullName("New User");
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");
        form.setRoleId(1);
        form.setIsActive(true);

        User duplicate = new User();
        duplicate.setId(2);
        duplicate.setUsername("dupuser");
        when(userRepository.findByUsername("dupuser")).thenReturn(Optional.of(duplicate));

        assertThrows(IllegalArgumentException.class, () -> adminUserService.createUser(form));
    }

    @Test
    @DisplayName("updateUser: 正常系でユーザー情報とロールが更新される")
    void updateUser_Success_UpdatesFieldsAndRole() {
        User user = new User();
        user.setId(1);
        user.setUsername("olduser");
        user.setEmail("old@example.com");
        user.setFullName("Old Name");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(2));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));

        Role oldRole = new Role();
        oldRole.setId(1);
        oldRole.setRoleName("ROLE_USER");

        UserRole existingUserRole = new UserRole();
        existingUserRole.setUser(user);
        existingUserRole.setRole(oldRole);
        existingUserRole.setAssignedAt(LocalDateTime.now().minusDays(2));
        user.getUserRoles().add(existingUserRole);

        Role newRole = new Role();
        newRole.setId(2);
        newRole.setRoleName("ROLE_ADMIN");

        UserEditForm form = new UserEditForm();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setFullName("New Name");
        form.setIsActive(false);
        form.setRoleId(2);

        when(userRepository.findByIdWithRoles(1)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(2)).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminUserService.updateUser(1, form);

        assertEquals("newuser", updated.getUsername());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New Name", updated.getFullName());
        assertFalse(updated.getIsActive());
        assertNotNull(updated.getUpdatedAt());
        assertEquals(1, updated.getUserRoles().size());
        assertEquals("ROLE_ADMIN", updated.getUserRoles().get(0).getRole().getRoleName());

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateUser: ユーザー名重複時は例外")
    void updateUser_WhenUsernameDuplicate_ThrowsException() {
        User target = new User();
        target.setId(1);

        User duplicate = new User();
        duplicate.setId(2);
        duplicate.setUsername("dupuser");

        UserEditForm form = new UserEditForm();
        form.setUsername("dupuser");
        form.setEmail("new@example.com");
        form.setFullName("Name");
        form.setIsActive(true);
        form.setRoleId(1);

        when(userRepository.findByIdWithRoles(1)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("dupuser")).thenReturn(Optional.of(duplicate));

        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUser(1, form));
    }

    @Test
    @DisplayName("updateUser: メールアドレス重複時は例外")
    void updateUser_WhenEmailDuplicate_ThrowsException() {
        User target = new User();
        target.setId(1);

        User duplicate = new User();
        duplicate.setId(2);
        duplicate.setEmail("dup@example.com");

        Role role = new Role();
        role.setId(1);
        role.setRoleName("ROLE_USER");

        UserEditForm form = new UserEditForm();
        form.setUsername("unique-name");
        form.setEmail("dup@example.com");
        form.setFullName("Name");
        form.setIsActive(true);
        form.setRoleId(1);

        when(userRepository.findByIdWithRoles(1)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("unique-name")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(duplicate));

        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUser(1, form));
    }

    @Test
    @DisplayName("deactivateUser: 自分自身の削除は例外")
    void deactivateUser_WhenSelfDelete_ThrowsException() {
        User loginUser = new User();
        loginUser.setId(1);
        loginUser.setUsername("adminuser");
        loginUser.setIsActive(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("adminuser", "N/A"));
        SecurityContextHolder.setContext(context);

        when(userRepository.findById(1)).thenReturn(Optional.of(loginUser));

        assertThrows(IllegalArgumentException.class, () -> adminUserService.deactivateUser(1));
    }

    @Test
    @DisplayName("deactivateUser: 正常系でisActive=falseになる")
    void deactivateUser_Success_SetsInactive() {
        User target = new User();
        target.setId(10);
        target.setUsername("targetuser");
        target.setIsActive(true);
        target.setUpdatedAt(LocalDateTime.now().minusDays(1));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("adminuser", "N/A"));
        SecurityContextHolder.setContext(context);

        when(userRepository.findById(10)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = adminUserService.deactivateUser(10);

        assertFalse(result.getIsActive());
        assertNotNull(result.getUpdatedAt());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("deactivateUser: 既に無効ユーザーの場合は例外")
    void deactivateUser_WhenAlreadyInactive_ThrowsException() {
        User target = new User();
        target.setId(10);
        target.setUsername("targetuser");
        target.setIsActive(false);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("adminuser", "N/A"));
        SecurityContextHolder.setContext(context);

        when(userRepository.findById(10)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class, () -> adminUserService.deactivateUser(10));
    }

    @Test
    @DisplayName("getAllRoles: リポジトリ結果を返す")
    void getAllRoles_ReturnsRepositoryResult() {
        Role roleUser = new Role();
        roleUser.setId(1);
        roleUser.setRoleName("ROLE_USER");

        Role roleAdmin = new Role();
        roleAdmin.setId(2);
        roleAdmin.setRoleName("ROLE_ADMIN");

        when(roleRepository.findAll()).thenReturn(List.of(roleUser, roleAdmin));

        List<Role> roles = adminUserService.getAllRoles();

        assertEquals(2, roles.size());
        assertEquals("ROLE_USER", roles.get(0).getRoleName());
        assertEquals("ROLE_ADMIN", roles.get(1).getRoleName());
    }

    @Test
    @DisplayName("calculatePagingInfo: 開始終了インデックスを計算できる")
    void calculatePagingInfo_ReturnsStartAndEnd() {
        int[] result = adminUserService.calculatePagingInfo(1, 20, 33);

        assertEquals(21, result[0]);
        assertEquals(33, result[1]);
    }
}
