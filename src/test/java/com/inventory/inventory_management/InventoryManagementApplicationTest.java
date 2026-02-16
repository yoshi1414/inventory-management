package com.inventory.inventory_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * InventoryManagementApplicationの起動テスト
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryManagementApplication 起動テスト")
class InventoryManagementApplicationTest {

    /**
     * Springアプリケーションコンテキストが正常に起動することを検証
     */
    @Test
    @DisplayName("コンテキストが正常起動する")
    void contextLoads() {
    }
}
