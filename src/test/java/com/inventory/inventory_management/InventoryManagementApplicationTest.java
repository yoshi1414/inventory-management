package com.inventory.inventory_management;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
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

    /**
     * アプリケーションクラスに必要なアノテーションが付与されていることを検証
     */
    @Test
    @DisplayName("構成: 必須アノテーションが付与されている")
    void applicationClass_HasRequiredAnnotations() {
        assertTrue(InventoryManagementApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(InventoryManagementApplication.class.isAnnotationPresent(EnableScheduling.class));
        assertTrue(InventoryManagementApplication.class.isAnnotationPresent(EnableAsync.class));
    }

    /**
     * mainメソッドがSpringApplication.runを呼び出すことを検証
     */
    @Test
    @DisplayName("main: SpringApplication.runが呼び出される")
    void main_InvokesSpringApplicationRun() {
        String[] args = new String[]{"--test"};
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            InventoryManagementApplication.main(args);
            mocked.verify(() -> SpringApplication.run(InventoryManagementApplication.class, args));
        }
    }
}
