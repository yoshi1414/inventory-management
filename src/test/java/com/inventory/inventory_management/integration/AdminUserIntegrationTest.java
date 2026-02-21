package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;
import com.inventory.inventory_management.repository.RoleRepository;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * AdminUserController の結合テスト
 * Controller → Service → Repository → DB の一連処理を検証する
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminUserController 結合テスト")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminUserIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private MockMvc mockMvc;

    private User targetUser;

    /**
     * 各テスト実行前の初期化
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role userRole = roleRepository.findByRoleName("ROLE_USER").orElseThrow();
        targetUser = createUserWithRole("int-user-01", "int-user-01@example.com", "統合テストユーザー", userRole, true);
    }

    /**
     * ユーザー一覧画面が正常表示され、モデルに一覧が設定されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー一覧を表示できる")
    void showUsers_Success() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("userPage"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("criteria"));

        User saved = userRepository.findByUsername("int-user-01").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("int-user-01@example.com");
    }

        /**
         * roleId 指定でロール絞り込み検索ができることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithMockUser(username = "adminuser", roles = {"ADMIN"})
        @DisplayName("【結合】roleId指定で一般ユーザー検索ができる")
        @SuppressWarnings("unchecked")
        void showUsers_FilterByRoleId_Success() throws Exception {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();
        createUserWithRole("int-admin-01", "int-admin-01@example.com", "統合テスト管理者", adminRole, true);

        Role userRole = roleRepository.findByRoleName("ROLE_USER").orElseThrow();

        MvcResult result = mockMvc.perform(get("/admin/users")
                .param("roleId", String.valueOf(userRole.getId())))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/users"))
            .andExpect(model().attributeExists("users"))
            .andReturn();

        List<User> users = (List<User>) result.getModelAndView().getModel().get("users");
        assertThat(users).isNotEmpty();
        assertThat(users)
            .allMatch(u -> u.getUserRoles().stream().anyMatch(ur -> "ROLE_USER".equals(ur.getRole().getRoleName())));
        assertThat(users)
            .noneMatch(u -> u.getUserRoles().stream().anyMatch(ur -> "ROLE_ADMIN".equals(ur.getRole().getRoleName())));
        }

    /**
     * ユーザー編集画面が正常表示されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー編集画面を表示できる")
    void showEditForm_Success() throws Exception {
        mockMvc.perform(get("/admin/users/{id}/edit", targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("editForm"))
                .andExpect(model().attributeExists("roles"));
    }

    /**
     * ユーザー新規登録画面が正常表示されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー新規登録画面を表示できる")
    void showCreateForm_Success() throws Exception {
        mockMvc.perform(get("/admin/users/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-create"))
                .andExpect(model().attributeExists("createForm"))
                .andExpect(model().attributeExists("roles"));
    }

    /**
     * ユーザー新規登録でDBにユーザーとロールが登録されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー新規登録でDBが更新される")
    void createUser_Success() throws Exception {
        Role userRole = roleRepository.findByRoleName("ROLE_USER").orElseThrow();

        mockMvc.perform(post("/admin/users/create")
                .with(csrf())
                .param("username", "int-create-user")
                .param("email", "int-create-user@example.com")
                .param("fullName", "統合テスト新規ユーザー")
                .param("password", "Password123")
                .param("confirmPassword", "Password123")
                .param("roleId", String.valueOf(userRole.getId()))
                .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User created = userRepository.findByUsernameWithRoles("int-create-user").orElseThrow();
        assertThat(created.getEmail()).isEqualTo("int-create-user@example.com");
        assertThat(created.getFullName()).isEqualTo("統合テスト新規ユーザー");
        assertThat(created.getIsActive()).isTrue();
        assertThat(created.getUserRoles()).hasSize(1);
        assertThat(created.getUserRoles().get(0).getRole().getRoleName()).isEqualTo("ROLE_USER");
    }

    /**
     * ユーザー編集でDBが更新されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー編集でDBが更新される")
    void updateUser_Success() throws Exception {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();

        mockMvc.perform(post("/admin/users/{id}/edit", targetUser.getId())
                .with(csrf())
                .param("username", "int-user-01-updated")
                .param("email", "int-user-01-updated@example.com")
                .param("fullName", "統合テストユーザー更新")
                .param("isActive", "true")
                .param("roleId", String.valueOf(adminRole.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findByIdWithRoles(targetUser.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("int-user-01-updated");
        assertThat(updated.getEmail()).isEqualTo("int-user-01-updated@example.com");
        assertThat(updated.getFullName()).isEqualTo("統合テストユーザー更新");
        assertThat(updated.getIsActive()).isTrue();
        assertThat(updated.getUserRoles()).hasSize(1);
        assertThat(updated.getUserRoles().get(0).getRole().getRoleName()).isEqualTo("ROLE_ADMIN");
    }

    /**
     * ユーザー削除（論理削除）で isActive=false になることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】ユーザー削除でisActiveがfalseになる")
    void deactivateUser_Success() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/delete", targetUser.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User deactivated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(deactivated.getIsActive()).isFalse();
    }

    /**
     * テスト用ユーザーを作成する
     *
     * @param username ユーザー名
     * @param email メールアドレス
     * @param fullName フルネーム
     * @param role ロール
     * @param isActive 有効フラグ
     * @return 保存済みユーザー
     */
    private User createUserWithRole(String username, String email, String fullName, Role role, boolean isActive) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        user.setEmail(email);
        user.setFullName(fullName);
        user.setIsActive(isActive);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(saved);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());
        saved.getUserRoles().add(userRole);

        return userRepository.save(saved);
    }
}
