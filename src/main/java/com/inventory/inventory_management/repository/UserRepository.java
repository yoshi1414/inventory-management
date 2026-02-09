package com.inventory.inventory_management.repository;

import java.util.Optional;

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
}
