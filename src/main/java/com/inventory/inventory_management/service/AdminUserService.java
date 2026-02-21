package com.inventory.inventory_management.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventory.inventory_management.dto.request.UserSearchCriteriaDto;
import com.inventory.inventory_management.entity.Role;
import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.entity.UserRole;
import com.inventory.inventory_management.form.UserCreateForm;
import com.inventory.inventory_management.form.UserEditForm;
import com.inventory.inventory_management.repository.RoleRepository;
import com.inventory.inventory_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用ユーザー管理サービス
 * ユーザーの一覧取得・編集・論理削除を担当する
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // 検索・一覧
    // =========================================================

    /**
     * 検索条件に基づいてユーザーをページング検索する
     *
     * @param criteria 検索条件 DTO
     * @return ユーザーのページ
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(UserSearchCriteriaDto criteria) {
        // ページ番号の補正
        int page = Math.max(criteria.getPage(), 0);

        // ソート設定
        Sort sort = buildSort(criteria.getSort());

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);

        // ステータスフィルター
        Boolean isActive = null;
        if ("active".equals(criteria.getStatus())) {
            isActive = Boolean.TRUE;
        } else if ("inactive".equals(criteria.getStatus())) {
            isActive = Boolean.FALSE;
        }

        // キーワードのトリム
        String keyword = (criteria.getSearch() != null && !criteria.getSearch().isBlank())
                ? criteria.getSearch().trim()
                : null;

        log.debug("ユーザー検索実行: keyword={}, isActive={}, roleId={}, sort={}, page={}", keyword, isActive, criteria.getRoleId(), criteria.getSort(), page);

        return userRepository.searchUsers(keyword, isActive, criteria.getRoleId(), pageable);
    }

    /**
     * ページング情報（開始/終了インデックス）を計算する
     *
     * @param page          ページ番号（0始まり）
     * @param pageSize      ページサイズ
     * @param totalElements 総件数
     * @return [startItem, endItem]
     */
    public int[] calculatePagingInfo(int page, int pageSize, long totalElements) {
        int startItem = (int) (page * (long) pageSize + 1);
        int endItem = (int) Math.min(startItem + pageSize - 1, totalElements);
        return new int[]{startItem, endItem};
    }

    // =========================================================
    // 取得
    // =========================================================

    /**
     * ユーザーをIDでロール情報込みに取得する
     *
     * @param id ユーザーID
     * @return ユーザー（存在しない場合は空の Optional）
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Integer id) {
        return userRepository.findByIdWithRoles(id);
    }

    /**
     * 全ロールを取得する（編集フォームのロール選択用）
     *
     * @return ロールリスト
     */
    @Transactional(readOnly = true)
    public java.util.List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // =========================================================
    // 新規登録
    // =========================================================

    /**
     * 新規ユーザーを登録する
     *
     * @param form 新規登録フォーム
     * @return 登録されたユーザー
     * @throws IllegalArgumentException ユーザー名・メールアドレス重複、またはロールが見つからない場合
     */
    @Transactional
    public User createUser(UserCreateForm form) {
        log.info("ユーザー新規登録開始: username={}", form.getUsername());

        // ユーザー名重複チェック
        userRepository.findByUsername(form.getUsername())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("このユーザー名はすでに使用されています: " + form.getUsername());
                });

        // メールアドレス重複チェック
        userRepository.findByEmail(form.getEmail())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("このメールアドレスはすでに使用されています: " + form.getEmail());
                });

        // ロールを取得
        Role role = roleRepository.findById(form.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("ロールが見つかりません: ID=" + form.getRoleId()));

        // ユーザーエンティティ生成
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setFullName(form.getFullName());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setIsActive(form.getIsActive());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // ロール紐付け
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(now);
        user.getUserRoles().add(userRole);

        User saved = userRepository.save(user);
        log.info("ユーザー新規登録完了: userId={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    // =========================================================
    // 更新
    // =========================================================

    /**
     * ユーザー情報を更新する
     *
     * @param id   対象ユーザーID
     * @param form 編集フォーム
     * @return 更新後のユーザー
     * @throws IllegalArgumentException ユーザー・ロールが見つからない場合、またはユーザー名/メール重複の場合
     */
    @Transactional
    public User updateUser(Integer id, UserEditForm form) {
        log.info("ユーザー更新開始: userId={}", id);

        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: ID=" + id));

        // ユーザー名重複チェック（自分自身は除く）
        userRepository.findByUsername(form.getUsername())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("このユーザー名はすでに使用されています: " + form.getUsername());
                });

        // メールアドレス重複チェック（自分自身は除く）
        userRepository.findByEmail(form.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("このメールアドレスはすでに使用されています: " + form.getEmail());
                });

        // ロールを取得
        Role newRole = roleRepository.findById(form.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("ロールが見つかりません: ID=" + form.getRoleId()));

        // フィールド更新
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setFullName(form.getFullName());
        user.setIsActive(form.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());

        // ロール更新（既存ロールと異なる場合のみ更新）
        if (user.getUserRoles().stream()
                .noneMatch(ur -> ur.getRole().getId().equals(form.getRoleId()))) {
            user.getUserRoles().clear();
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(newRole);
            userRole.setAssignedAt(LocalDateTime.now());
            user.getUserRoles().add(userRole);
            log.debug("ユーザーロール更新: userId={}, roleId={}", id, form.getRoleId());
        } else {
            log.debug("ユーザーロール変更なし: userId={}, roleId={}", id, form.getRoleId());
        }

        User saved = userRepository.save(user);
        log.info("ユーザー更新完了: userId={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    // =========================================================
    // 論理削除
    // =========================================================

    /**
     * ユーザーを論理削除（isActive = false）する
     * ログイン中のユーザー自身は削除不可
     *
     * @param id 対象ユーザーID
     * @return 無効化後のユーザー
     * @throws IllegalArgumentException ユーザーが見つからない場合、または自分自身を削除しようとした場合
     */
    @Transactional
    public User deactivateUser(Integer id) {
        log.info("ユーザー論理削除開始: userId={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: ID=" + id));

        // 自分自身の削除防止
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && user.getUsername().equals(auth.getName())) {
            throw new IllegalArgumentException("ログイン中のユーザー自身は削除できません");
        }

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("このユーザーはすでに無効化されています: ID=" + id);
        }

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("ユーザー論理削除完了: userId={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    // =========================================================
    // 内部ユーティリティ
    // =========================================================

    /**
     * ソートキー文字列から Sort オブジェクトを生成する
     *
     * @param sortKey ソートキー
     * @return Sort オブジェクト
     */
    private Sort buildSort(String sortKey) {
        if (sortKey == null) {
            return Sort.by(Sort.Direction.ASC, "username");
        }
        return switch (sortKey) {
            case "username_desc" -> Sort.by(Sort.Direction.DESC, "username");
            case "email"         -> Sort.by(Sort.Direction.ASC,  "email");
            case "email_desc"    -> Sort.by(Sort.Direction.DESC, "email");
            case "created"       -> Sort.by(Sort.Direction.ASC,  "createdAt");
            case "created_desc"  -> Sort.by(Sort.Direction.DESC, "createdAt");
            default              -> Sort.by(Sort.Direction.ASC,  "username");
        };
    }
}
