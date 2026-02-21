package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.inventory.inventory_management.dto.request.UserSearchCriteriaDto;
import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;
import com.inventory.inventory_management.form.UserCreateForm;
import com.inventory.inventory_management.form.UserEditForm;
import com.inventory.inventory_management.service.AdminUserService;

/**
 * AdminUserController のユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController ユニットテスト")
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("showUsers: 正常系で一覧画面を返す")
    void showUsers_Success() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setPage(0);

        Page<User> page = new PageImpl<>(List.of(new User()));
        when(adminUserService.searchUsers(criteria)).thenReturn(page);
        when(adminUserService.calculatePagingInfo(0, page.getSize(), page.getTotalElements()))
                .thenReturn(new int[]{1, 1});

        Model model = new ExtendedModelMap();

        String view = adminUserController.showUsers(criteria, model);

        assertEquals("admin/users", view);
        assertNotNull(model.getAttribute("users"));
        assertEquals(1, ((List<?>) model.getAttribute("users")).size());
    }

    @Test
    @DisplayName("showUsers: ロール一覧をモデルに設定する")
    void showUsers_SetsAllRoles() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setPage(0);

        Page<User> page = new PageImpl<>(List.of(new User()));
        when(adminUserService.searchUsers(criteria)).thenReturn(page);
        when(adminUserService.calculatePagingInfo(0, page.getSize(), page.getTotalElements()))
                .thenReturn(new int[]{1, 1});

        Role userRole = new Role();
        userRole.setId(1);
        userRole.setRoleName("ROLE_USER");
        when(adminUserService.getAllRoles()).thenReturn(List.of(userRole));

        Model model = new ExtendedModelMap();

        String view = adminUserController.showUsers(criteria, model);

        assertEquals("admin/users", view);
        assertNotNull(model.getAttribute("allRoles"));
        assertEquals(1, ((List<?>) model.getAttribute("allRoles")).size());
    }

    @Test
    @DisplayName("showUsers: 負のページ番号は0に補正される")
    void showUsers_WhenPageIsNegative_CorrectsToZero() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setPage(-3);

        Page<User> page = new PageImpl<>(List.of(new User()));
        when(adminUserService.searchUsers(criteria)).thenReturn(page);
        when(adminUserService.calculatePagingInfo(0, page.getSize(), page.getTotalElements()))
                .thenReturn(new int[]{1, 1});
        when(adminUserService.getAllRoles()).thenReturn(List.of());

        Model model = new ExtendedModelMap();

        String view = adminUserController.showUsers(criteria, model);

        assertEquals("admin/users", view);
        assertEquals(0, criteria.getPage());
        verify(adminUserService).calculatePagingInfo(0, page.getSize(), page.getTotalElements());
    }

    @Test
    @DisplayName("showUsers: 例外発生時はerror画面を返す")
    void showUsers_WhenException_ReturnsErrorView() {
        UserSearchCriteriaDto criteria = new UserSearchCriteriaDto();
        criteria.setPage(0);
        when(adminUserService.searchUsers(criteria)).thenThrow(new RuntimeException("DB error"));

        Model model = new ExtendedModelMap();

        String view = adminUserController.showUsers(criteria, model);

        assertEquals("error", view);
        assertEquals("ユーザー情報の取得に失敗しました。", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("showEditForm: 正常系で編集画面を返す")
    void showEditForm_Success() {
        User user = buildUser(1, "testuser", "test@example.com", "Test User", true, "ROLE_USER", 1);

        when(adminUserService.getUserById(1)).thenReturn(Optional.of(user));

        Role role = new Role();
        role.setId(1);
        role.setRoleName("ROLE_USER");
        when(adminUserService.getAllRoles()).thenReturn(List.of(role));

        Model model = new ExtendedModelMap();

        String view = adminUserController.showEditForm(1, model);

        assertEquals("admin/user-edit", view);
        assertNotNull(model.getAttribute("editForm"));
        assertNotNull(model.getAttribute("roles"));
    }

    @Test
    @DisplayName("showCreateForm: 正常系で新規登録画面を返す")
    void showCreateForm_Success() {
        Role role = new Role();
        role.setId(1);
        role.setRoleName("ROLE_USER");
        when(adminUserService.getAllRoles()).thenReturn(List.of(role));

        Model model = new ExtendedModelMap();

        String view = adminUserController.showCreateForm(model);

        assertEquals("admin/user-create", view);
        assertNotNull(model.getAttribute("createForm"));
        assertNotNull(model.getAttribute("roles"));
    }

    @Test
    @DisplayName("createUser: パスワード不一致時は登録画面へリダイレクト")
    void createUser_WhenPasswordMismatch_RedirectsToCreate() {
        UserCreateForm form = new UserCreateForm();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setFullName("New User");
        form.setPassword("Password123");
        form.setConfirmPassword("Password999");
        form.setRoleId(1);
        form.setIsActive(true);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "createForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminUserController.createUser(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/users/create", view);
        assertEquals("入力内容を確認してください。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createUser: 正常登録で一覧へリダイレクト")
    void createUser_Success() {
        UserCreateForm form = new UserCreateForm();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setFullName("New User");
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");
        form.setRoleId(1);
        form.setIsActive(true);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "createForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        User saved = buildUser(30, "newuser", "new@example.com", "New User", true, "ROLE_USER", 1);
        when(adminUserService.createUser(any(UserCreateForm.class))).thenReturn(saved);

        String view = adminUserController.createUser(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/users", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("newuser"));
    }

    @Test
    @DisplayName("showEditForm: 対象ユーザー不存在時は一覧へリダイレクト")
    void showEditForm_WhenUserNotFound_RedirectsToUsers() {
        when(adminUserService.getUserById(999)).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();
        String view = adminUserController.showEditForm(999, model);

        assertEquals("redirect:/admin/users", view);
    }

    @Test
    @DisplayName("updateUser: バリデーションエラー時は編集画面へリダイレクト")
    void updateUser_WhenValidationError_RedirectsToEdit() {
        UserEditForm form = new UserEditForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "editForm");
        bindingResult.rejectValue("username", "NotBlank", "必須");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminUserController.updateUser(10, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/users/10/edit", view);
        assertEquals("入力内容を確認してください。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("updateUser: 正常更新で一覧へリダイレクト")
    void updateUser_Success() {
        UserEditForm form = new UserEditForm();
        form.setUsername("updated");
        form.setEmail("updated@example.com");
        form.setFullName("Updated User");
        form.setIsActive(true);
        form.setRoleId(1);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "editForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        User saved = buildUser(10, "updated", "updated@example.com", "Updated User", true, "ROLE_USER", 1);
        when(adminUserService.updateUser(anyInt(), any(UserEditForm.class))).thenReturn(saved);

        String view = adminUserController.updateUser(10, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/users", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("updated"));
    }

    @Test
    @DisplayName("deactivateUser: 正常系で一覧へリダイレクト")
    void deactivateUser_Success() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        User deactivated = buildUser(20, "inactive-user", "i@example.com", "Inactive User", false, "ROLE_USER", 1);
        when(adminUserService.deactivateUser(20)).thenReturn(deactivated);

        String view = adminUserController.deactivateUser(20, redirectAttributes);

        assertEquals("redirect:/admin/users", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("inactive-user"));
    }

    private User buildUser(
            Integer id,
            String username,
            String email,
            String fullName,
            Boolean isActive,
            String roleName,
            Integer roleId) {

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setIsActive(isActive);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        Role role = new Role();
        role.setId(roleId);
        role.setRoleName(roleName);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());

        user.getUserRoles().add(userRole);
        return user;
    }
}
