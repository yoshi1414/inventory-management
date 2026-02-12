package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.service.InventoryService;

/**
 * 在庫管理システムの結合テストクラス
 * コントローラー → サービス → リポジトリ → データベースまでの
 * 一連の処理フローをテストします
 * 
 * このテストは以下を検証します:
 * 1. HTTPリクエストの受け付け（コントローラー層）
 * 2. ビジネスロジックの実行（サービス層）
 * 3. データアクセス（リポジトリ層）
 * 4. データベースへの永続化
 * 5. レスポンスの返却
 * 
 * @SpringBootTest により、実際のアプリケーションコンテキストが起動します
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("在庫管理システム 結合テスト")
public class InventoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    private MockMvc mockMvc;

    private Product testProduct1;
    private Product testProduct2;
    private Product testProduct3;

    /**
     * 各テスト前の初期化処理
     * テストデータをデータベースに登録
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 既存のテストデータをクリーンアップ
        productRepository.deleteAll();

        // テスト用商品1: 在庫あり(50個)
        testProduct1 = new Product();
        testProduct1.setProductCode("INT0001");
        testProduct1.setProductName("統合テスト商品A");
        testProduct1.setCategory("テストカテゴリ1");
        testProduct1.setStock(50);
        testProduct1.setPrice(new BigDecimal("10000.00"));
        testProduct1.setStatus("active");
        testProduct1.setDescription("結合テスト用商品A");
        testProduct1.setCreatedAt(LocalDateTime.now().minusDays(5));
        testProduct1.setUpdatedAt(LocalDateTime.now().minusDays(1));
        testProduct1 = productRepository.save(testProduct1);

        // テスト用商品2: 在庫不足(15個)
        testProduct2 = new Product();
        testProduct2.setProductCode("INT0002");
        testProduct2.setProductName("統合テスト商品B");
        testProduct2.setCategory("テストカテゴリ2");
        testProduct2.setStock(15);
        testProduct2.setPrice(new BigDecimal("5000.00"));
        testProduct2.setStatus("active");
        testProduct2.setDescription("結合テスト用商品B");
        testProduct2.setCreatedAt(LocalDateTime.now().minusDays(3));
        testProduct2.setUpdatedAt(LocalDateTime.now().minusHours(12));
        testProduct2 = productRepository.save(testProduct2);

        // テスト用商品3: 在庫切れ(0個)
        testProduct3 = new Product();
        testProduct3.setProductCode("INT0003");
        testProduct3.setProductName("統合テスト商品C");
        testProduct3.setCategory("テストカテゴリ1");
        testProduct3.setStock(0);
        testProduct3.setPrice(new BigDecimal("3000.00"));
        testProduct3.setStatus("inactive");
        testProduct3.setDescription("結合テスト用商品C");
        testProduct3.setCreatedAt(LocalDateTime.now().minusDays(7));
        testProduct3.setUpdatedAt(LocalDateTime.now().minusDays(2));
        testProduct3 = productRepository.save(testProduct3);
    }

    // ========== コントローラー → サービス → リポジトリ → DB の結合テスト ==========

    /**
     * Test: コントローラーからDBまでの全商品取得フロー
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】コントローラー経由で全商品を取得し、DBから正しくデータが返される")
    void testEndToEnd_GetAllProducts() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("productPage"))
                .andExpect(model().attributeExists("lowStockCount"))
                .andExpect(model().attributeExists("outOfStockCount"))
                .andReturn();

        // モデルから商品リストを取得してDBの内容と一致することを確認
        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(3);
        assertThat(productPage.getContent())
            .extracting(Product::getProductCode)
            .containsExactlyInAnyOrder("INT0001", "INT0002", "INT0003");
    }

    /**
     * Test: コントローラー → サービス → リポジトリのキーワード検索フロー
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】キーワード検索でコントローラーからDBまで正しく処理される")
    void testEndToEnd_SearchByKeyword() throws Exception {
        // Act & Assert - "商品A"で検索
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("search", "商品A"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andExpect(model().attributeExists("products"))
                .andReturn();

        // DBから正しくフィルタリングされたデータが返ることを確認
        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        assertThat(productPage.getContent().get(0).getProductName()).contains("商品A");
        
        // サービス層経由でも同じ結果が得られることを確認
        org.springframework.data.domain.Page<Product> serviceResult = 
            inventoryService.searchProducts("商品A", null, null, null, "name", 0);
        assertThat(serviceResult.getContent()).hasSize(1);
    }

    /**
     * Test: カテゴリフィルタリングの結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】カテゴリフィルタでコントローラーからDBまで正しく処理される")
    void testEndToEnd_FilterByCategory() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("category", "テストカテゴリ1"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(2); // testProduct1とtestProduct3
        assertThat(productPage.getContent())
            .allMatch(p -> p.getCategory().equals("テストカテゴリ1"));

        // 直接DBに問い合わせて一致することを確認
        long dbCount = productRepository.findBySearchConditions(
            null, "テストカテゴリ1", null, 
            org.springframework.data.domain.PageRequest.of(0, 20))
            .getTotalElements();
        assertThat(dbCount).isEqualTo(2);
    }

    /**
     * Test: 在庫状態フィルタリングの結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫切れフィルタでコントローラーからDBまで正しく処理される")
    void testEndToEnd_FilterByStockStatus_Out() throws Exception {
        // Act & Assert - 在庫切れで検索
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("stock", "out"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        assertThat(productPage.getContent().get(0).getStock()).isEqualTo(0);
        assertThat(productPage.getContent().get(0).getProductCode()).isEqualTo("INT0003");

        // リポジトリ層で直接検証
        long outOfStockCount = productRepository.countOutOfStock();
        assertThat(outOfStockCount).isEqualTo(1);
    }

    /**
     * Test: 在庫不足フィルタリングの結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫不足フィルタでコントローラーからDBまで正しく処理される")
    void testEndToEnd_FilterByStockStatus_Low() throws Exception {
        // Act & Assert - 在庫不足で検索
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("stock", "low"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        assertThat(productPage.getContent().get(0).getStock())
            .isGreaterThan(0)
            .isLessThanOrEqualTo(20);

        // サービス層とリポジトリ層でも確認
        long lowStockCount = inventoryService.getLowStockCount();
        assertThat(lowStockCount).isEqualTo(1);
        
        long dbLowStockCount = productRepository.countLowStock();
        assertThat(dbLowStockCount).isEqualTo(1);
    }

    /**
     * Test: 在庫充分フィルタリングの結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫充分フィルタでコントローラーからDBまで正しく処理される")
    void testEndToEnd_FilterByStockStatus_Sufficient() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("stock", "sufficient"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        assertThat(productPage.getContent().get(0).getStock()).isGreaterThan(20);
        assertThat(productPage.getContent().get(0).getProductCode()).isEqualTo("INT0001");
    }

    /**
     * Test: ソート機能の結合テスト（商品名順）
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品名ソートでコントローラーからDBまで正しく処理される")
    void testEndToEnd_SortByName() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(3);
        
        // 商品名がソートされていることを確認
        java.util.List<String> productNames = productPage.getContent().stream()
            .map(Product::getProductName)
            .toList();
        assertThat(productNames).isSorted();
    }

    /**
     * Test: ソート機能の結合テスト（在庫数順）
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫数ソートでコントローラーからDBまで正しく処理される")
    void testEndToEnd_SortByStock() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("sort", "stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        
        // 在庫数がソートされていることを確認
        java.util.List<Integer> stocks = productPage.getContent().stream()
            .map(Product::getStock)
            .toList();
        assertThat(stocks).isSorted();
        assertThat(stocks).containsExactly(0, 15, 50); // 昇順
    }

    /**
     * Test: 複合条件での結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】複合条件(キーワード+カテゴリ+ステータス+在庫+ソート)でE2Eテスト")
    void testEndToEnd_ComplexSearch() throws Exception {
        // Act & Assert - 複数の条件を組み合わせて検索
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("search", "統合")
                .param("category", "テストカテゴリ1")
                .param("status", "active")
                .param("stock", "sufficient")
                .param("sort", "stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        
        Product found = productPage.getContent().get(0);
        assertThat(found.getProductName()).contains("統合");
        assertThat(found.getCategory()).isEqualTo("テストカテゴリ1");
        assertThat(found.getStatus()).isEqualTo("active");
        assertThat(found.getStock()).isGreaterThan(20);
    }

    /**
     * Test: ページネーションの結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】ページネーションでコントローラーからDBまで正しく処理される")
    void testEndToEnd_Pagination() throws Exception {
        // 追加のテストデータを作成（合計で21件以上にする）
        for (int i = 4; i <= 25; i++) {
            Product product = new Product();
            product.setProductCode(String.format("INT%04d", i));
            product.setProductName("ページングテスト商品" + i);
            product.setCategory("テストカテゴリ3");
            product.setStock(i * 10);
            product.setPrice(new BigDecimal("1000.00"));
            product.setStatus("active");
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        }

        // Act & Assert - 1ページ目
        MvcResult result1 = mockMvc.perform(get("/inventory")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> page0 = 
            (org.springframework.data.domain.Page<Product>) result1.getModelAndView().getModel().get("productPage");

        assertThat(page0).isNotNull();
        assertThat(page0.getNumber()).isEqualTo(0);
        assertThat(page0.getContent()).hasSizeLessThanOrEqualTo(20);
        assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(25);

        // Act & Assert - 2ページ目
        MvcResult result2 = mockMvc.perform(get("/inventory")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> page1 = 
            (org.springframework.data.domain.Page<Product>) result2.getModelAndView().getModel().get("productPage");

        assertThat(page1).isNotNull();
        assertThat(page1.getNumber()).isEqualTo(1);
        
        // 1ページ目と2ページ目で異なる商品が返ることを確認
        assertThat(page0.getContent())
            .doesNotContainAnyElementsOf(page1.getContent());
    }

    /**
     * Test: 在庫カウント情報がモデルに正しく設定される
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫カウント情報がコントローラーからDBまで正しく計算される")
    void testEndToEnd_StockCounts() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("lowStockCount"))
                .andExpect(model().attributeExists("outOfStockCount"))
                .andReturn();

        // モデルから取得
        Long lowStockCount = (Long) result.getModelAndView().getModel().get("lowStockCount");
        Long outOfStockCount = (Long) result.getModelAndView().getModel().get("outOfStockCount");

        assertThat(lowStockCount).isEqualTo(1); // testProduct2
        assertThat(outOfStockCount).isEqualTo(1); // testProduct3

        // サービス層で直接確認
        assertThat(inventoryService.getLowStockCount()).isEqualTo(1);
        assertThat(inventoryService.getOutOfStockCount()).isEqualTo(1);

        // リポジトリ層で直接確認
        assertThat(productRepository.countLowStock()).isEqualTo(1);
        assertThat(productRepository.countOutOfStock()).isEqualTo(1);
    }

    /**
     * Test: 認証が必要なエンドポイントでのセキュリティ結合テスト
     */
    @Test
    @DisplayName("【結合】認証なしでアクセスするとログイン画面にリダイレクトされる")
    void testEndToEnd_SecurityIntegration_NoAuth() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/inventory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Test: データベースからのデータ取得とモデルへの反映
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】DBから取得したデータが正しくモデルに反映される")
    void testEndToEnd_DatabaseToModel() throws Exception {
        // 新しい商品をDBに追加
        Product newProduct = new Product();
        newProduct.setProductCode("INT9999");
        newProduct.setProductName("新規追加商品");
        newProduct.setCategory("新規カテゴリ");
        newProduct.setStock(99);
        newProduct.setPrice(new BigDecimal("9999.00"));
        newProduct.setStatus("active");
        newProduct.setCreatedAt(LocalDateTime.now());
        newProduct.setUpdatedAt(LocalDateTime.now());
        productRepository.save(newProduct);

        // Act & Assert - 追加した商品が検索結果に含まれることを確認
        MvcResult result = mockMvc.perform(get("/inventory")
                .param("search", "新規追加"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
            (org.springframework.data.domain.Page<Product>) result.getModelAndView().getModel().get("productPage");

        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent()).hasSize(1);
        
        Product foundProduct = productPage.getContent().get(0);
        assertThat(foundProduct.getProductCode()).isEqualTo("INT9999");
        assertThat(foundProduct.getProductName()).isEqualTo("新規追加商品");
        assertThat(foundProduct.getStock()).isEqualTo(99);
        
        // DBから直接取得して確認
        Product dbProduct = productRepository.findByProductCode("INT9999");
        assertThat(dbProduct).isNotNull();
        assertThat(dbProduct.getProductName()).isEqualTo(foundProduct.getProductName());
    }

    /**
     * Test: サービス層とリポジトリ層の整合性確認
     */
    @Test
    @DisplayName("【結合】サービス層とリポジトリ層の結果が整合している")
    void testEndToEnd_ServiceRepositoryConsistency() {
        // サービス層経由で取得
        org.springframework.data.domain.Page<Product> serviceResult = 
            inventoryService.searchProducts(null, null, "active", null, "name", 0);

        // リポジトリ層から直接取得
        org.springframework.data.domain.Page<Product> repoResult = 
            productRepository.findBySearchConditions(
                null, null, "active", 
                org.springframework.data.domain.PageRequest.of(0, 20, 
                    org.springframework.data.domain.Sort.by("productName")));

        // 結果が一致することを確認
        assertThat(serviceResult.getTotalElements())
            .isEqualTo(repoResult.getTotalElements());
        assertThat(serviceResult.getContent().size())
            .isEqualTo(repoResult.getContent().size());
    }
}
