package com.inventory.inventory_management.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.inventory_management.dto.request.UserSearchCriteriaDto;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.form.UserCreateForm;
import com.inventory.inventory_management.form.UserEditForm;
import com.inventory.inventory_management.service.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用ユーザー管理コントローラー
 * ユーザーの一覧表示・編集・論理削除を担当する
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // =========================================================
    // 一覧
    // =========================================================

    /**
     * ユーザー一覧画面を表示する
     *
     * @param criteria 検索条件 DTO（クエリパラメータから自動バインド）
     * @param model    モデル
     * @return admin/users.html
     */
    @GetMapping
    public String showUsers(UserSearchCriteriaDto criteria, Model model) {
        try {
            // ページ番号の補正
            if (criteria.getPage() < 0) {
                log.warn("無効なページ番号: page={}", criteria.getPage());
                criteria.setPage(0);
            }

            log.debug("ユーザー一覧画面を表示: {}", criteria);

            Page<User> userPage = adminUserService.searchUsers(criteria);

            int[] pagingInfo = adminUserService.calculatePagingInfo(
                    criteria.getPage(), userPage.getSize(), userPage.getTotalElements());

            model.addAttribute("userPage",          userPage);
            model.addAttribute("users",             userPage.getContent());
            model.addAttribute("criteria",          criteria);
            model.addAttribute("allRoles",          adminUserService.getAllRoles());
            model.addAttribute("currentPage",       criteria.getPage());
            model.addAttribute("totalPages",        userPage.getTotalPages());
            model.addAttribute("totalElements",     userPage.getTotalElements());
            model.addAttribute("currentPageNumber", criteria.getPage() + 1);
            model.addAttribute("pageSize",          userPage.getSize());
            model.addAttribute("startItem",         pagingInfo[0]);
            model.addAttribute("endItem",           pagingInfo[1]);

            log.debug("ユーザー一覧取得完了: 全{}件 (ページ {}/{})",
                    userPage.getTotalElements(), criteria.getPage() + 1, userPage.getTotalPages());

            return "admin/users";
        } catch (Exception e) {
            log.error("ユーザー一覧画面表示時にエラーが発生: error={}", e.getMessage(), e);
            model.addAttribute("errorMessage", "ユーザー情報の取得に失敗しました。");
            return "error";
        }
    }

    // =========================================================
    // 新規登録
    // =========================================================

    /**
     * ユーザー新規登録フォーム画面を表示する
     *
     * @param model モデル
     * @return admin/user-create.html
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        log.debug("ユーザー新規登録フォームを表示");
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new UserCreateForm());
        }
        model.addAttribute("roles", adminUserService.getAllRoles());
        return "admin/user-create";
    }

    /**
     * 新規ユーザーを登録する
     *
     * @param form               新規登録フォーム（バリデーション済み）
     * @param bindingResult      バリデーション結果
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return ユーザー一覧画面へリダイレクト、またはエラー時は登録画面へリダイレクト
     */
    @PostMapping("/create")
    public String createUser(
            @ModelAttribute("createForm") @Valid UserCreateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // パスワード一致チェック
        if (!bindingResult.hasFieldErrors("confirmPassword")
                && form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "パスワードが一致しません");
        }

        if (bindingResult.hasErrors()) {
            log.warn("ユーザー新規登録バリデーションエラー: errors={}", bindingResult.getErrorCount());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.createForm", bindingResult);
            redirectAttributes.addFlashAttribute("createForm", form);
            redirectAttributes.addFlashAttribute("error", "入力内容を確認してください。");
            return "redirect:/admin/users/create";
        }

        try {
            log.info("ユーザー新規登録開始: username={}", form.getUsername());
            User saved = adminUserService.createUser(form);
            log.info("ユーザー新規登録完了: userId={}, username={}", saved.getId(), saved.getUsername());
            redirectAttributes.addFlashAttribute("message",
                    "ユーザー「" + saved.getUsername() + "」を登録しました。");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー新規登録エラー: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("createForm", form);
            return "redirect:/admin/users/create";
        } catch (Exception e) {
            log.error("ユーザー新規登録時にエラーが発生: error={}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "ユーザー登録時にエラーが発生しました。");
            redirectAttributes.addFlashAttribute("createForm", form);
            return "redirect:/admin/users/create";
        }
    }

    // =========================================================
    // 編集
    // =========================================================

    /**
     * ユーザー編集フォーム画面を表示する
     *
     * @param userId ユーザーID
     * @param model  モデル
     * @return admin/user-edit.html
     */
    @GetMapping("/{userId}/edit")
    public String showEditForm(@PathVariable("userId") Integer userId, Model model) {
        try {
            log.debug("ユーザー編集フォームを表示: userId={}", userId);

            User user = adminUserService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: ID=" + userId));

            // フラッシュ経由でフォームが戻っていない場合は User から初期値を生成
            if (!model.containsAttribute("editForm")) {
                UserEditForm form = new UserEditForm();
                form.setUsername(user.getUsername());
                form.setEmail(user.getEmail());
                form.setFullName(user.getFullName());
                form.setIsActive(user.getIsActive());
                // 現在の最初のロール ID をデフォルト値にセット
                if (!user.getUserRoles().isEmpty()) {
                    form.setRoleId(user.getUserRoles().get(0).getRole().getId());
                }
                model.addAttribute("editForm", form);
            }

            model.addAttribute("user",  user);
            model.addAttribute("roles", adminUserService.getAllRoles());

            return "admin/user-edit";
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー編集フォーム表示エラー: {}", e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("ユーザー編集フォーム表示時にエラーが発生: userId={}, error={}", userId, e.getMessage(), e);
            return "redirect:/admin/users";
        }
    }

    /**
     * ユーザー情報を更新する
     *
     * @param userId             対象ユーザーID
     * @param form               編集フォーム（バリデーション済み）
     * @param bindingResult      バリデーション結果
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return ユーザー一覧画面へリダイレクト、またはエラー時は編集画面へリダイレクト
     */
    @PostMapping("/{userId}/edit")
    public String updateUser(
            @PathVariable("userId") Integer userId,
            @ModelAttribute("editForm") @Valid UserEditForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("ユーザー編集バリデーションエラー: userId={}, errors={}", userId, bindingResult.getErrorCount());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.editForm", bindingResult);
            redirectAttributes.addFlashAttribute("editForm", form);
            redirectAttributes.addFlashAttribute("error", "入力内容を確認してください。");
            return "redirect:/admin/users/" + userId + "/edit";
        }

        try {
            log.info("ユーザー更新開始: userId={}", userId);

            User saved = adminUserService.updateUser(userId, form);

            log.info("ユーザー更新完了: userId={}, username={}", saved.getId(), saved.getUsername());
            redirectAttributes.addFlashAttribute("message",
                    "ユーザー「" + saved.getUsername() + "」の情報を更新しました。");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー更新エラー: userId={}, {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("editForm", form);
            return "redirect:/admin/users/" + userId + "/edit";
        } catch (Exception e) {
            log.error("ユーザー更新時にエラーが発生: userId={}, error={}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "ユーザー更新時にエラーが発生しました。");
            return "redirect:/admin/users/" + userId + "/edit";
        }
    }

    // =========================================================
    // 論理削除
    // =========================================================

    /**
     * ユーザーを論理削除（isActive = false）する
     *
     * @param userId             対象ユーザーID
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return ユーザー一覧画面へリダイレクト
     */
    @PostMapping("/{userId}/delete")
    public String deactivateUser(
            @PathVariable("userId") Integer userId,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("ユーザー論理削除リクエスト: userId={}", userId);

            User deactivated = adminUserService.deactivateUser(userId);

            log.info("ユーザー論理削除完了: userId={}, username={}", deactivated.getId(), deactivated.getUsername());
            redirectAttributes.addFlashAttribute("message",
                    "ユーザー「" + deactivated.getUsername() + "」を無効化しました。");
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー論理削除エラー: userId={}, {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("ユーザー論理削除時にエラーが発生: userId={}, error={}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "ユーザー削除時にエラーが発生しました。");
        }
        return "redirect:/admin/users";
    }
}
