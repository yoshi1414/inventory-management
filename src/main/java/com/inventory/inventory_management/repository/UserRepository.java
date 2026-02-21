package com.inventory.inventory_management.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventory.inventory_management.entity.User;

/**
 * ユーザーリポジトリ
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    /**
     * メールアドレスでユーザーを検索
     * @param email メールアドレス
     * @return ユーザー（存在しない場合は空の Optional）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * ユーザー名でユーザーを検索
     * @param username ユーザー名
     * @return ユーザー（存在しない場合は空の Optional）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * ユーザー名でユーザーを検索（ロール情報を含む）
     * LazyInitializationException対策のため、JOIN FETCHでロール情報を同時に取得
     * @param username ユーザー名
     * @return ユーザー（存在しない場合は空の Optional）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    /**
     * キーワード・アクティブ状態・ロール ID でユーザーをページング検索する
     * キーワードはユーザー名・メールアドレス・フルネームを対象とする
     * @param keyword 検索キーワード（null または空文字の場合は全件対象）
     * @param isActive アクティブフラグ（null の場合は全件対象）
     * @param roleId ロール ID（null の場合は全件対象）
     * @param pageable ページング情報
     * @return ユーザーのページ
     */
    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.fullName LIKE %:keyword%) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive) " +
           "AND (:roleId IS NULL OR EXISTS (SELECT 1 FROM u.userRoles ur2 WHERE ur2.role.id = :roleId))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u WHERE " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        " u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.fullName LIKE %:keyword%) " +
                        "AND (:isActive IS NULL OR u.isActive = :isActive) " +
                        "AND (:roleId IS NULL OR EXISTS (SELECT 1 FROM u.userRoles ur2 WHERE ur2.role.id = :roleId))")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            @Param("roleId") Integer roleId,
            Pageable pageable);

    /**
     * IDでユーザーをロール情報込みで取得する
     * @param id ユーザーID
     * @return ユーザー（存在しない場合は空の Optional）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Integer id);
}
