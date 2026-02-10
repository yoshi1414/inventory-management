package com.inventory.inventory_management.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Role;

/**
 * RoleRepositoryのテストクラス
 * データベースとの連携をテストする統合テストです
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("RoleRepository統合テスト")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * 正常系：ロール名でロールを検索できる
     */
    @Test
    @DisplayName("正常系：findByRoleName()でロールを取得できる")
    void testFindByRoleName_Success() {
        // given
        Role role = new Role();
        role.setRoleName("ROLE_TEST");
        role.setDescription("テスト用ロール");
        role.setCreatedAt(LocalDateTime.now());
        roleRepository.save(role);

        // when
        Optional<Role> result = roleRepository.findByRoleName("ROLE_TEST");

        // then
        assertTrue(result.isPresent());
        assertEquals("ROLE_TEST", result.get().getRoleName());
        assertEquals("テスト用ロール", result.get().getDescription());
    }

    /**
     * 正常系：存在しないロール名で検索すると空のOptionalが返る
     */
    @Test
    @DisplayName("正常系：存在しないロール名で検索するとOptional.emptyが返る")
    void testFindByRoleName_NotFound() {
        // when
        Optional<Role> result = roleRepository.findByRoleName("ROLE_NONEXISTENT");

        // then
        assertFalse(result.isPresent());
    }

    /**
     * 正常系：ロールを保存できる
     */
    @Test
    @DisplayName("正常系：ロールを保存できる")
    void testSave_Success() {
        // given
        Role newRole = new Role();
        newRole.setRoleName("ROLE_NEW");
        newRole.setDescription("新規ロール");
        newRole.setCreatedAt(LocalDateTime.now());

        // when
        Role savedRole = roleRepository.save(newRole);

        // then
        assertNotNull(savedRole.getId());
        assertEquals("ROLE_NEW", savedRole.getRoleName());
        
        // データベースから取得して確認
        Optional<Role> found = roleRepository.findByRoleName("ROLE_NEW");
        assertTrue(found.isPresent());
        assertEquals("新規ロール", found.get().getDescription());
    }

    /**
     * 正常系：ロールを更新できる
     */
    @Test
    @DisplayName("正常系：ロールを更新できる")
    void testUpdate_Success() {
        // given
        Role role = new Role();
        role.setRoleName("ROLE_UPDATE");
        role.setDescription("更新前");
        role.setCreatedAt(LocalDateTime.now());
        role = roleRepository.save(role);

        // when
        role.setDescription("更新後");
        Role updatedRole = roleRepository.save(role);

        // then
        assertEquals("更新後", updatedRole.getDescription());
        
        // データベースから取得して確認
        Optional<Role> found = roleRepository.findById(updatedRole.getId());
        assertTrue(found.isPresent());
        assertEquals("更新後", found.get().getDescription());
    }

    /**
     * 正常系：ロールを削除できる
     */
    @Test
    @DisplayName("正常系：ロールを削除できる")
    void testDelete_Success() {
        // given
        Role role = new Role();
        role.setRoleName("ROLE_DELETE");
        role.setDescription("削除テスト");
        role.setCreatedAt(LocalDateTime.now());
        role = roleRepository.save(role);
        Integer roleId = role.getId();

        // when
        roleRepository.delete(role);
        roleRepository.flush();

        // then
        Optional<Role> deletedRole = roleRepository.findById(roleId);
        assertFalse(deletedRole.isPresent());
    }

    /**
     * 正常系：すべてのロールを取得できる
     */
    @Test
    @DisplayName("正常系：findAll()ですべてのロールを取得できる")
    void testFindAll_Success() {
        // given
        Role role1 = new Role();
        role1.setRoleName("ROLE_FINDALL1");
        role1.setDescription("検索テスト1");
        role1.setCreatedAt(LocalDateTime.now());
        roleRepository.save(role1);

        Role role2 = new Role();
        role2.setRoleName("ROLE_FINDALL2");
        role2.setDescription("検索テスト2");
        role2.setCreatedAt(LocalDateTime.now());
        roleRepository.save(role2);

        // when
        List<Role> roles = roleRepository.findAll();

        // then
        assertNotNull(roles);
        assertTrue(roles.size() >= 2);
        assertTrue(roles.stream().anyMatch(r -> r.getRoleName().equals("ROLE_FINDALL1")));
        assertTrue(roles.stream().anyMatch(r -> r.getRoleName().equals("ROLE_FINDALL2")));
    }

    /**
     * 正常系：ロール数をカウントできる
     */
    @Test
    @DisplayName("正常系：count()でロール数を取得できる")
    void testCount_Success() {
        // given
        long beforeCount = roleRepository.count();
        
        Role role = new Role();
        role.setRoleName("ROLE_COUNT");
        role.setDescription("カウントテスト");
        role.setCreatedAt(LocalDateTime.now());
        roleRepository.save(role);

        // when
        long afterCount = roleRepository.count();

        // then
        assertEquals(beforeCount + 1, afterCount);
    }

    /**
     * 正常系：IDでロールを検索できる
     */
    @Test
    @DisplayName("正常系：findById()でロールを取得できる")
    void testFindById_Success() {
        // given
        Role role = new Role();
        role.setRoleName("ROLE_FINDBYID");
        role.setDescription("ID検索テスト");
        role.setCreatedAt(LocalDateTime.now());
        role = roleRepository.save(role);
        Integer roleId = role.getId();

        // when
        Optional<Role> result = roleRepository.findById(roleId);

        // then
        assertTrue(result.isPresent());
        assertEquals("ROLE_FINDBYID", result.get().getRoleName());
        assertEquals("ID検索テスト", result.get().getDescription());
    }

    /**
     * 正常系：存在しないIDで検索すると空のOptionalが返る
     */
    @Test
    @DisplayName("正常系：存在しないIDで検索するとOptional.emptyが返る")
    void testFindById_NotFound() {
        // when
        Optional<Role> result = roleRepository.findById(999999);

        // then
        assertFalse(result.isPresent());
    }
}
