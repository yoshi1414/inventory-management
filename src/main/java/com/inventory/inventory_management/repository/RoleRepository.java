package com.inventory.inventory_management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventory.inventory_management.entity.Role;

/**
 * ロールリポジトリ
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    /**
     * ロール名でロールを検索
     * @param roleName ロール名
     * @return ロール（存在しない場合は空の Optional）
     */
    Optional<Role> findByRoleName(String roleName);
}
