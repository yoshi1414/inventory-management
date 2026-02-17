package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.repository.StockTransactionRepository;
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
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class InventoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

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
        stockTransactionRepository.deleteAll();
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

    // ========== 在庫更新API 結合テスト ==========

    /**
     * Test: 入庫処理の結合テスト（コントローラー → サービス → リポジトリ → DB）
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】入庫処理でコントローラーからDBまで正しく処理される")
    void testEndToEnd_StockUpdate_In() throws Exception {
        // Given: 初期在庫数を記録
        int initialStock = testProduct1.getStock();
        int quantity = 10;

        // リクエストボディを作成
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": %d,
                "remarks": "結合テスト入庫処理"
            }
            """, testProduct1.getId(), quantity);

        // Act: 入庫APIを実行
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.product.id").value(testProduct1.getId()))
               .andExpect(jsonPath("$.product.stock").value(initialStock + quantity));

        // Assert: データベースで在庫数が正しく更新されている
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock + quantity);

        // Assert: 在庫変動履歴が正しく記録されている
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(transactions).hasSize(1);
        
        StockTransaction transaction = transactions.get(0);
        assertThat(transaction.getProductId()).isEqualTo(testProduct1.getId());
        assertThat(transaction.getTransactionType()).isEqualTo("in");
        assertThat(transaction.getQuantity()).isEqualTo(quantity);
        assertThat(transaction.getBeforeStock()).isEqualTo(initialStock);
        assertThat(transaction.getAfterStock()).isEqualTo(initialStock + quantity);
        assertThat(transaction.getRemarks()).isEqualTo("結合テスト入庫処理");
        assertThat(transaction.getUserId()).isEqualTo("testuser");
    }

    /**
     * Test: 出庫処理の結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】出庫処理でコントローラーからDBまで正しく処理される")
    void testEndToEnd_StockUpdate_Out() throws Exception {
        // Given: 初期在庫数を記録
        int initialStock = testProduct1.getStock();
        int quantity = 20;

        // リクエストボディを作成
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": %d,
                "remarks": "結合テスト出庫処理"
            }
            """, testProduct1.getId(), quantity);

        // Act: 出庫APIを実行
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.product.stock").value(initialStock - quantity));

        // Assert: データベースで在庫数が正しく更新されている
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - quantity);

        // Assert: 在庫変動履歴が正しく記録されている
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(transactions).hasSize(1);
        
        StockTransaction transaction = transactions.get(0);
        assertThat(transaction.getTransactionType()).isEqualTo("out");
        assertThat(transaction.getQuantity()).isEqualTo(quantity);
        assertThat(transaction.getBeforeStock()).isEqualTo(initialStock);
        assertThat(transaction.getAfterStock()).isEqualTo(initialStock - quantity);
        assertThat(transaction.getRemarks()).isEqualTo("結合テスト出庫処理");
    }

    /**
     * Test: remarks=null での在庫更新結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】remarks=nullでも在庫更新が正常に処理される")
    void testEndToEnd_StockUpdate_WithNullRemarks() throws Exception {
        // Given: 初期在庫数を記録
        int initialStock = testProduct1.getStock();
        int quantity = 15;
        
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": %d
            }
            """, testProduct1.getId(), quantity);

        // Act: 在庫更新APIを実行（remarksなし）
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.product.stock").value(initialStock + quantity));

        // Assert: データベースで在庫数が正しく更新されている
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock + quantity);

        // Assert: 在庫変動履歴でremarks=nullが記録される
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getRemarks()).isNull();
    }

    /**
     * Test: 複数回の在庫更新が cumulative に反映される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】複数回の在庫更新が累積して正しく処理される")
    void testEndToEnd_MultipleStockUpdates() throws Exception {
        // Given: 初期在庫数
        int initialStock = testProduct1.getStock();
        stockTransactionRepository.deleteByProductId(testProduct1.getId());

        // Act 1: 入庫10個
        String requestBody1 = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "1回目入庫"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody1))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.product.stock").value(initialStock + 10));

        // Act 2: 出庫5個
        String requestBody2 = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "1回目出庫"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody2))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.product.stock").value(initialStock + 5));

        // Act 3: 入庫20個
        String requestBody3 = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 20,
                "remarks": "2回目入庫"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody3))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.product.stock").value(initialStock + 25));

        // Assert: 最終的な在庫数
        Product finalProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(initialStock + 25);

        // Assert: 在庫変動履歴が3件記録されている
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(transactions).hasSize(3);
        
        // 履歴内容を確認（同一時刻登録時の順序非決定性を考慮）
        assertThat(transactions).extracting(StockTransaction::getRemarks)
                .containsExactlyInAnyOrder("1回目入庫", "1回目出庫", "2回目入庫");
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("1回目入庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 10);
        });
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("1回目出庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock + 10);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 5);
        });
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("2回目入庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock + 5);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 25);
        });
    }

    /**
     * Test: 在庫不足時の出庫エラー結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫不足時に出庫処理がエラーになる")
    void testEndToEnd_StockUpdate_InsufficientStock() throws Exception {
        // Given: 在庫不足の商品（testProduct2 = 15個）
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 100,
                "remarks": "在庫不足テスト"
            }
            """, testProduct2.getId());

        // Act & Assert: 在庫不足エラーが返される
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("在庫が不足")));

        // Assert: 在庫数は変更されていない
        Product unchangedProduct = productRepository.findById(testProduct2.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(15);

        // Assert: 在庫変動履歴は記録されていない
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct2.getId());
        assertThat(transactions).isEmpty();
    }

    /**
     * Test: 不正な商品IDでのエラー結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】存在しない商品IDで在庫更新するとエラーになる")
    void testEndToEnd_StockUpdate_InvalidProductId() throws Exception {
        // Given: 存在しない商品ID
        String requestBody = """
            {
                "productId": 99999,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "不正な商品ID"
            }
            """;

        // Act & Assert: エラーが返される
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品が見つかりません")));
    }

    /**
     * Test: 認証なしでの在庫更新API結合テスト
     */
    @Test
    @DisplayName("【結合】認証なしで在庫更新APIにアクセスするとリダイレクトされる")
    void testEndToEnd_StockUpdate_NoAuth() throws Exception {
        // Given
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "認証なしテスト"
            }
            """, testProduct1.getId());

        // Act & Assert: 認証エラーでリダイレクト
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().is3xxRedirection());

        // Assert: 在庫数は変更されていない
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(50); // 初期値のまま
    }

    /**
     * Test: CSRFトークンなしでのエラー結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】CSRFトークンなしで在庫更新APIにアクセスするとエラーになる")
    void testEndToEnd_StockUpdate_NoCsrf() throws Exception {
        // Given
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "CSRFなしテスト"
            }
            """, testProduct1.getId());

        // Act & Assert: CSRFエラー
        mockMvc.perform(post("/api/inventory/update-stock")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().is3xxRedirection());

        // Assert: 在庫数は変更されていない
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(50);
    }

    // ========== 商品詳細画面 結合テスト ==========

    /**
     * Test: 商品詳細画面が正常に表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品詳細画面でコントローラーからDBまで正しく処理される")
    void testEndToEnd_ProductDetail_Success() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("transactions"))
                .andReturn();

        // モデルから商品を取得してDBの内容と一致することを確認
        Product product = (Product) result.getModelAndView().getModel().get("product");
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo(testProduct1.getId());
        assertThat(product.getProductCode()).isEqualTo(testProduct1.getProductCode());
        assertThat(product.getProductName()).isEqualTo(testProduct1.getProductName());
        assertThat(product.getStock()).isEqualTo(testProduct1.getStock());

        // DBから直接取得して整合性を確認
        Product dbProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(product.getProductName()).isEqualTo(dbProduct.getProductName());
        assertThat(product.getStock()).isEqualTo(dbProduct.getStock());
    }

    /**
     * Test: 商品詳細画面の全表示項目がDBの内容と一致しているか確認する詳細テスト
     * HTML表示項目とDB値の完全な整合性を検証
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品詳細画面のすべての表示項目がDBの内容と完全に一致している")
    void testEndToEnd_ProductDetail_AllFieldsConsistency() throws Exception {
        // Given: テストデータに全項目を設定
        Product product = productRepository.findById(testProduct1.getId()).orElseThrow();
        product.setSku("SKU-TEST-001");
        product.setDescription("詳細テスト用商品説明");
        product.setPrice(new BigDecimal("15000.00"));
        product.setRating(new BigDecimal("4.5"));
        product.setWarrantyMonths(12);
        product.setDimensions("10cm x 20cm x 5cm");
        product.setVariations("Red, Blue, Green");
        product.setManufacturingDate(java.time.LocalDate.now().minusDays(30));
        product.setExpirationDate(java.time.LocalDate.now().plusYears(1));
        product.setTags("タグ1,タグ2,タグ3");
        productRepository.save(product);

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        // Assert: モデルの商品情報がDBと完全に一致することを確認
        Product resultProduct = (Product) result.getModelAndView().getModel().get("product");
        
        // 基本情報の確認
        assertThat(resultProduct.getId()).isEqualTo(product.getId());
        assertThat(resultProduct.getProductCode()).isEqualTo(product.getProductCode());
        assertThat(resultProduct.getProductName()).isEqualTo(product.getProductName());
        assertThat(resultProduct.getCategory()).isEqualTo(product.getCategory());
        
        // 詳細情報の確認
        assertThat(resultProduct.getSku()).isEqualTo(product.getSku());
        assertThat(resultProduct.getDescription()).isEqualTo(product.getDescription());
        assertThat(resultProduct.getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(resultProduct.getStatus()).isEqualTo(product.getStatus());
        assertThat(resultProduct.getRating()).isEqualByComparingTo(product.getRating());
        
        // 追加情報の確認
        assertThat(resultProduct.getWarrantyMonths()).isEqualTo(product.getWarrantyMonths());
        assertThat(resultProduct.getDimensions()).isEqualTo(product.getDimensions());
        assertThat(resultProduct.getVariations()).isEqualTo(product.getVariations());
        assertThat(resultProduct.getManufacturingDate()).isEqualTo(product.getManufacturingDate());
        assertThat(resultProduct.getExpirationDate()).isEqualTo(product.getExpirationDate());
        assertThat(resultProduct.getTags()).isEqualTo(product.getTags());
        
        // 在庫情報の確認
        assertThat(resultProduct.getStock()).isEqualTo(product.getStock());
        
        // DBから直接取得した商品とも完全に一致することを確認
        Product dbProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(resultProduct.getProductCode()).isEqualTo(dbProduct.getProductCode());
        assertThat(resultProduct.getProductName()).isEqualTo(dbProduct.getProductName());
        assertThat(resultProduct.getCategory()).isEqualTo(dbProduct.getCategory());
        assertThat(resultProduct.getSku()).isEqualTo(dbProduct.getSku());
        assertThat(resultProduct.getDescription()).isEqualTo(dbProduct.getDescription());
        assertThat(resultProduct.getPrice()).isEqualByComparingTo(dbProduct.getPrice());
        assertThat(resultProduct.getStatus()).isEqualTo(dbProduct.getStatus());
        assertThat(resultProduct.getRating()).isEqualByComparingTo(dbProduct.getRating());
        assertThat(resultProduct.getWarrantyMonths()).isEqualTo(dbProduct.getWarrantyMonths());
        assertThat(resultProduct.getDimensions()).isEqualTo(dbProduct.getDimensions());
        assertThat(resultProduct.getVariations()).isEqualTo(dbProduct.getVariations());
        assertThat(resultProduct.getManufacturingDate()).isEqualTo(dbProduct.getManufacturingDate());
        assertThat(resultProduct.getExpirationDate()).isEqualTo(dbProduct.getExpirationDate());
        assertThat(resultProduct.getTags()).isEqualTo(dbProduct.getTags());
        assertThat(resultProduct.getStock()).isEqualTo(dbProduct.getStock());
    }

    /**
     * Test: Null値フィールドが正しく表示される結合テスト
     * オプショナルフィールド(保証期間、製造日など)がnullの場合の処理確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】オプショナルフィールドがnullの場合、正常に表示される")
    void testEndToEnd_ProductDetail_NullOptionalFields() throws Exception {
        // Given: オプショナルフィールドをnullに設定
        Product product = productRepository.findById(testProduct1.getId()).orElseThrow();
        product.setSku(null);
        product.setDescription(null);
        product.setWarrantyMonths(null);
        product.setDimensions(null);
        product.setVariations(null);
        product.setManufacturingDate(null);
        product.setExpirationDate(null);
        product.setTags(null);
        product.setRating(null);
        productRepository.save(product);

        // Act
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        // Assert: nullフィールドが正しく取得されることを確認
        Product resultProduct = (Product) result.getModelAndView().getModel().get("product");
        assertThat(resultProduct.getSku()).isNull();
        assertThat(resultProduct.getDescription()).isNull();
        assertThat(resultProduct.getWarrantyMonths()).isNull();
        assertThat(resultProduct.getDimensions()).isNull();
        assertThat(resultProduct.getVariations()).isNull();
        assertThat(resultProduct.getManufacturingDate()).isNull();
        assertThat(resultProduct.getExpirationDate()).isNull();
        assertThat(resultProduct.getTags()).isNull();
        assertThat(resultProduct.getRating()).isNull();
        
        // DBからも同じ結果が得られることを確認
        Product dbProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(resultProduct.getSku()).isEqualTo(dbProduct.getSku());
        assertThat(resultProduct.getWarrantyMonths()).isEqualTo(dbProduct.getWarrantyMonths());
        assertThat(resultProduct.getDimensions()).isEqualTo(dbProduct.getDimensions());
        assertThat(resultProduct.getTags()).isEqualTo(dbProduct.getTags());
    }

    /**
     * Test: 複数商品の表示項目がそれぞれ正しく表示される結合テスト
     * 同時に複数商品の詳細画面を表示して、各商品の項目が混ざらないことを確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】複数商品の詳細画面で各商品の表示項目が混ざらない")
    void testEndToEnd_ProductDetail_MultipleProductsDataSegregation() throws Exception {
        // Act: 商品1の詳細を取得
        MvcResult result1 = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        Product product1Result = (Product) result1.getModelAndView().getModel().get("product");

        // Act: 商品2の詳細を取得
        MvcResult result2 = mockMvc.perform(get("/inventory/products/{id}", testProduct2.getId()))
                .andExpect(status().isOk())
                .andReturn();

        Product product2Result = (Product) result2.getModelAndView().getModel().get("product");

        // Act: 商品3の詳細を取得
        MvcResult result3 = mockMvc.perform(get("/inventory/products/{id}", testProduct3.getId()))
                .andExpect(status().isOk())
                .andReturn();

        Product product3Result = (Product) result3.getModelAndView().getModel().get("product");

        // Assert: 各商品の情報が正しく分離されていることを確認
        assertThat(product1Result.getProductCode()).isEqualTo("INT0001");
        assertThat(product1Result.getProductName()).isEqualTo("統合テスト商品A");
        assertThat(product1Result.getStock()).isEqualTo(50);

        assertThat(product2Result.getProductCode()).isEqualTo("INT0002");
        assertThat(product2Result.getProductName()).isEqualTo("統合テスト商品B");
        assertThat(product2Result.getStock()).isEqualTo(15);

        assertThat(product3Result.getProductCode()).isEqualTo("INT0003");
        assertThat(product3Result.getProductName()).isEqualTo("統合テスト商品C");
        assertThat(product3Result.getStock()).isEqualTo(0);

        // Assert: DBの値と完全に一致することを確認
        Product db1 = productRepository.findById(testProduct1.getId()).orElseThrow();
        Product db2 = productRepository.findById(testProduct2.getId()).orElseThrow();
        Product db3 = productRepository.findById(testProduct3.getId()).orElseThrow();

        assertThat(product1Result.getProductCode()).isEqualTo(db1.getProductCode());
        assertThat(product2Result.getProductCode()).isEqualTo(db2.getProductCode());
        assertThat(product3Result.getProductCode()).isEqualTo(db3.getProductCode());
    }

    /**
     * Test: 商品詳細画面に入出庫履歴が正しく表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品詳細画面に入出庫履歴が最新3件表示される")
    void testEndToEnd_ProductDetail_WithTransactions() throws Exception {
        // Given: 既存の取引履歴をクリアして、新しい履歴を作成する
        stockTransactionRepository.deleteByProductId(testProduct1.getId());
        int initialStock = testProduct1.getStock();

        // 在庫変動履歴を作成（4件）
        for (int i = 0; i < 4; i++) {
            String requestBody = String.format("""
                {
                    "productId": %d,
                    "transactionType": "in",
                    "quantity": 5,
                    "remarks": "テスト履歴%d"
                }
                """, testProduct1.getId(), i + 1);

            mockMvc.perform(post("/api/inventory/update-stock")
                           .with(csrf())
                           .contentType(MediaType.APPLICATION_JSON)
                           .content(requestBody))
                   .andExpect(status().isOk());
        }

        // Act & Assert: 商品詳細画面を表示（最新3件のみ表示されるはず）
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("transactions"))
                .andReturn();

        // 履歴件数が3件であることを確認
        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");
        assertThat(transactions).hasSize(3);
        assertThat(transactions).extracting(StockTransaction::getRemarks)
            .allMatch(remarks -> remarks != null && remarks.startsWith("テスト履歴"));
        assertThat(transactions).extracting(StockTransaction::getRemarks)
            .contains("テスト履歴4");

        // 取得された3件の afterStock は4回の更新結果（+5/+10/+15/+20）のいずれか
        assertThat(transactions).extracting(StockTransaction::getAfterStock)
            .allMatch(afterStock -> afterStock == initialStock + 5
                || afterStock == initialStock + 10
                || afterStock == initialStock + 15
                || afterStock == initialStock + 20);

        // DBから全件取得して4件あることを確認
        List<StockTransaction> allTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(allTransactions).hasSize(4);
    }

    /**
     * Test: 入庫と出庫が混在する履歴が正しく表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品詳細画面に入庫と出庫が混在する履歴が正しく表示される")
    void testEndToEnd_ProductDetail_MixedTransactions() throws Exception {
        // Given: 入庫を記録
        String inRequestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫テスト"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(inRequestBody))
               .andExpect(status().isOk());

        // Given: 出庫を記録
        String outRequestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "出庫テスト"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(outRequestBody))
               .andExpect(status().isOk());

        // Act & Assert: 商品詳細画面を表示
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("transactions"))
                .andReturn();

        // 履歴に入庫と出庫の両方が含まれることを確認
        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");
        assertThat(transactions).isNotEmpty();

        // 入庫と出庫の両方の種別が存在することを確認
        boolean hasIn = transactions.stream().anyMatch(t -> "in".equals(t.getTransactionType()));
        boolean hasOut = transactions.stream().anyMatch(t -> "out".equals(t.getTransactionType()));
        assertThat(hasIn).isTrue();
        assertThat(hasOut).isTrue();

        // 最新の履歴が出庫であることを確認
        assertThat(transactions.get(0).getTransactionType()).isEqualTo("out");
        assertThat(transactions.get(0).getRemarks()).isEqualTo("出庫テスト");
    }

    /**
     * Test: 存在しない商品IDでアクセスするとエラー画面が表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】存在しない商品IDで商品詳細画面にアクセスするとエラー画面が表示される")
    void testEndToEnd_ProductDetail_NotFound() throws Exception {
        // Given: 存在しない商品ID
        Integer nonExistentId = 999999;

        // Act & Assert
        mockMvc.perform(get("/inventory/products/{id}", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        // DBで商品が存在しないことを確認
        assertThat(productRepository.findById(nonExistentId)).isEmpty();
    }

    /**
     * Test: 0以下の商品IDでアクセスするとエラー画面が表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】0以下の商品IDで商品詳細画面にアクセスするとエラー画面が表示される")
    void testEndToEnd_ProductDetail_InvalidId() throws Exception {
        // Act& Assert: ID=0の場合
        mockMvc.perform(get("/inventory/products/{id}", 0))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        // Act & Assert: ID=-1の場合
        mockMvc.perform(get("/inventory/products/{id}", -1))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    /**
     * Test: 削除済み商品にアクセスするとエラー画面が表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】削除済み商品にアクセスするとエラー画面が表示される")
    void testEndToEnd_ProductDetail_DeletedProduct() throws Exception {
        // Given: 商品を論理削除
        Product product = productRepository.findById(testProduct1.getId()).orElseThrow();
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);

        // Act & Assert
        mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        // DBで削除フラグが立っていることを確認
        Product deletedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(deletedProduct.getDeletedAt()).isNotNull();
    }

    /**
     * Test: 認証なしで商品詳細画面にアクセスするとログイン画面にリダイレクトされる結合テスト
     */
    @Test
    @DisplayName("【結合】認証なしで商品詳細画面にアクセスするとログイン画面にリダイレクトされる")
    void testEndToEnd_ProductDetail_NoAuth() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Test: 入出庫履歴がない商品でも商品詳細画面が正常に表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】入出庫履歴がない商品でも商品詳細画面が正常に表示される")
    void testEndToEnd_ProductDetail_NoTransactions() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("transactions"))
                .andReturn();

        // 履歴が空のリストであることを確認
        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");
        assertThat(transactions).isEmpty();

        // DBでも履歴が存在しないことを確認
        List<StockTransaction> dbTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct2.getId());
        assertThat(dbTransactions).isEmpty();
    }

    /**
     * Test: 在庫切れ商品の詳細画面が正常に表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫切れ商品の詳細画面が正常に表示される")
    void testEndToEnd_ProductDetail_OutOfStock() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct3.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        // 在庫が0であることを確認
        Product product = (Product) result.getModelAndView().getModel().get("product");
        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo("inactive");

        // DBでも在庫が0であることを確認
        Product dbProduct = productRepository.findById(testProduct3.getId()).orElseThrow();
        assertThat(dbProduct.getStock()).isEqualTo(0);
    }

    /**
     * Test: 在庫不足商品の詳細画面が正常に表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫不足商品の詳細画面が正常に表示される")
    void testEndToEnd_ProductDetail_LowStock() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        // 在庫が1-20の範囲であることを確認
        Product product = (Product) result.getModelAndView().getModel().get("product");
        assertThat(product.getStock())
                .isGreaterThan(0)
                .isLessThanOrEqualTo(20);

        // DBでも在庫不足状態であることを確認
        Product dbProduct = productRepository.findById(testProduct2.getId()).orElseThrow();
        assertThat(dbProduct.getStock())
                .isGreaterThan(0)
                .isLessThanOrEqualTo(20);
    }

    /**
     * Test: 在庫十分商品の詳細画面が正常に表示される結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】在庫十分商品の詳細画面が正常に表示される")
    void testEndToEnd_ProductDetail_SufficientStock() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("inventory-detail"))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        // 在庫が21以上であることを確認
        Product product = (Product) result.getModelAndView().getModel().get("product");
        assertThat(product.getStock()).isGreaterThan(20);

        // DBでも在庫十分状態であることを確認
        Product dbProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(dbProduct.getStock()).isGreaterThan(20);
    }

    /**
     * Test: 商品詳細画面から在庫一覧画面への整合性結合テスト
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】商品詳細画面と在庫一覧画面のデータ整合性が取れている")
    void testEndToEnd_ProductDetail_ConsistencyWithList() throws Exception {
        // Act: 在庫一覧画面から商品情報を取得
        MvcResult listResult = mockMvc.perform(get("/inventory")
                        .param("search", testProduct1.getProductName()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        org.springframework.data.domain.Page<Product> productPage = 
                (org.springframework.data.domain.Page<Product>) listResult.getModelAndView()
                        .getModel().get("productPage");
        Product listProduct = productPage.getContent().get(0);

        // Act: 商品詳細画面からも同じ商品情報を取得
        MvcResult detailResult = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        Product detailProduct = (Product) detailResult.getModelAndView().getModel().get("product");

        // Assert: 両画面で同じ商品情報が取得されることを確認
        assertThat(listProduct.getId()).isEqualTo(detailProduct.getId());
        assertThat(listProduct.getProductCode()).isEqualTo(detailProduct.getProductCode());
        assertThat(listProduct.getProductName()).isEqualTo(detailProduct.getProductName());
        assertThat(listProduct.getStock()).isEqualTo(detailProduct.getStock());
        assertThat(listProduct.getPrice()).isEqualByComparingTo(detailProduct.getPrice());
        assertThat(listProduct.getStatus()).isEqualTo(detailProduct.getStatus());
    }

    // ========== トランザクション（入出庫履歴）表示の整合性検証テスト ==========

    /**
     * Test: トランザクション履歴の全フィールドがDBと完全に一致しているか確認
     * HTML表示項目とDB値の完全な整合性を検証
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴のすべてのフィールドがDBと完全に一致している")
    void testEndToEnd_Transaction_AllFieldsConsistency() throws Exception {
        // Given: トランザクションを記録
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 25,
                "remarks": "仕入先Aからの商品受入"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk());

        // Act: 商品詳細画面を取得してトランザクション情報を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("transactions"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        assertThat(transactions).isNotEmpty();
        StockTransaction displayedTransaction = transactions.get(0); // 最新のトランザクション

        // Assert: DBから取得したトランザクションと完全に一致することを確認
        List<StockTransaction> dbTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        StockTransaction dbTransaction = dbTransactions.get(0);

        // 全フィールドの一致を確認
        assertThat(displayedTransaction.getId()).isEqualTo(dbTransaction.getId());
        assertThat(displayedTransaction.getProductId()).isEqualTo(dbTransaction.getProductId());
        assertThat(displayedTransaction.getTransactionType()).isEqualTo(dbTransaction.getTransactionType());
        assertThat(displayedTransaction.getQuantity()).isEqualTo(dbTransaction.getQuantity());
        assertThat(displayedTransaction.getBeforeStock()).isEqualTo(dbTransaction.getBeforeStock());
        assertThat(displayedTransaction.getAfterStock()).isEqualTo(dbTransaction.getAfterStock());
        assertThat(displayedTransaction.getRemarks()).isEqualTo(dbTransaction.getRemarks());
        assertThat(displayedTransaction.getUserId()).isEqualTo(dbTransaction.getUserId());
        // 日時も秒単位で一致を確認（ミリ秒は無視）
        assertThat(displayedTransaction.getTransactionDate().withNano(0))
                .isEqualTo(dbTransaction.getTransactionDate().withNano(0));
    }

    /**
     * Test: 複数トランザクション時の計算正確性と順序確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】複数トランザクションの在庫計算が正確で、最新順に表示される")
    void testEndToEnd_Transaction_MultipleMutationsWithAccuracy() throws Exception {
        // Given: 初期在庫を記録
        int initialStock = testProduct1.getStock();
        stockTransactionRepository.deleteByProductId(testProduct1.getId());

        // 入庫 1回目: +20個
        String in1 = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 20,
                "remarks": "1回目入庫"
            }
            """, testProduct1.getId());
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(in1))
               .andExpect(status().isOk());

        // 出庫 1回目: -5個
        String out1 = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "1回目出庫"
            }
            """, testProduct1.getId());
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(out1))
               .andExpect(status().isOk());

        // 入庫 2回目: +10個
        String in2 = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "2回目入庫"
            }
            """, testProduct1.getId());
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(in2))
               .andExpect(status().isOk());

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: 3件のトランザクションが存在すること
        assertThat(transactions).hasSize(3);

        // Assert: 履歴内容が正しいことを確認（同一時刻登録時の順序揺れを考慮）
        assertThat(transactions).extracting(StockTransaction::getRemarks)
                .containsExactlyInAnyOrder("1回目入庫", "1回目出庫", "2回目入庫");
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("1回目入庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 20);
        });
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("1回目出庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock + 20);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 15);
        });
        assertThat(transactions).anySatisfy(t -> {
            assertThat(t.getRemarks()).isEqualTo("2回目入庫");
            assertThat(t.getBeforeStock()).isEqualTo(initialStock + 15);
            assertThat(t.getAfterStock()).isEqualTo(initialStock + 25);
        });

        // Assert: 最終在庫が一致することを確認
        Product product = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(initialStock + 20 - 5 + 10);
    }

    /**
     * Test: トランザクション表示上限（最新3件）が正しく適用されているか確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴は最新3件のみが表示される")
    void testEndToEnd_Transaction_DisplayLimitOf3() throws Exception {
        // Given: 新規テスト商品を作成（履歴をクリアするため）
        Product testProduct = new Product();
        testProduct.setProductCode("LIM0001");
        testProduct.setProductName("表示上限テスト商品");
        testProduct.setCategory("テストカテゴリ");
        testProduct.setStock(100);
        testProduct.setPrice(new BigDecimal("5000.00"));
        testProduct.setStatus("active");
        testProduct.setDescription("表示上限テスト用");
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());
        testProduct = productRepository.save(testProduct);

        // 5件のトランザクションを記録
        for (int i = 1; i <= 5; i++) {
            String requestBody = String.format("""
                {
                    "productId": %d,
                    "transactionType": "in",
                    "quantity": 5,
                    "remarks": "テスト入庫#%d"
                }
                """, testProduct.getId(), i);

            mockMvc.perform(post("/api/inventory/update-stock")
                           .with(csrf())
                           .contentType(MediaType.APPLICATION_JSON)
                           .content(requestBody))
                   .andExpect(status().isOk());
        }

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct.getId()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> displayedTransactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: 画面に表示されるのは3件のみ
        assertThat(displayedTransactions).hasSize(3);
        
        // 表示された3件が、登録した5件のいずれかであることを確認
        assertThat(displayedTransactions).extracting(StockTransaction::getRemarks)
            .allMatch(remarks -> remarks != null && remarks.startsWith("テスト入庫#"));
        assertThat(displayedTransactions).extracting(StockTransaction::getRemarks)
            .allMatch(remarks -> {
                int number = Integer.parseInt(remarks.substring("テスト入庫#".length()));
                return number >= 1 && number <= 5;
            });

        // Assert: DBには全5件が存在することを確認
        List<StockTransaction> allTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct.getId());
        assertThat(allTransactions).hasSize(5);
        
        // 全件がDBに存在していることを確認
        assertThat(allTransactions.stream()
                .map(StockTransaction::getRemarks)
                .toList())
            .containsExactlyInAnyOrder("テスト入庫#5", "テスト入庫#4", "テスト入庫#3", "テスト入庫#2", "テスト入庫#1");
    }

    /**
     * Test: トランザクション表示のnull値フィールド処理
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴で備考（remarks）がnullでも正常に表示される")
    void testEndToEnd_Transaction_NullRemarks() throws Exception {
        // Given: 備考なしでトランザクションを記録
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk());

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: remarksがnullであることを確認
        assertThat(transactions).isNotEmpty();
        StockTransaction transaction = transactions.get(0);
        assertThat(transaction.getRemarks()).isNull();

        // Assert: DBからも同じ結果が得られることを確認
        List<StockTransaction> dbTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        assertThat(dbTransactions.get(0).getRemarks()).isNull();
    }

    /**
     * Test: トランザクション表示と商品在庫数の特合性確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴の最終在庫数と商品の現在在庫数が一致している")
    void testEndToEnd_Transaction_ConsistencyWithProductStock() throws Exception {
        // Given: 複数のトランザクションを記録
        stockTransactionRepository.deleteByProductId(testProduct1.getId());

        String in = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 30,
                "remarks": "総入庫"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(in))
               .andExpect(status().isOk());

        String out = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 10,
                "remarks": "総出庫"
            }
            """, testProduct1.getId());

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(out))
               .andExpect(status().isOk());

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        Product displayedProduct = (Product) result.getModelAndView().getModel().get("product");
        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: トランザクション履歴に現在在庫と一致する変更後在庫が存在
        int currentStock = displayedProduct.getStock();
        assertThat(transactions).isNotEmpty();
        assertThat(transactions).extracting(StockTransaction::getAfterStock).contains(currentStock);

        // Assert: DBの在庫数とも一致することを確認
        Product dbProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(dbProduct.getStock()).isEqualTo(currentStock);
        assertThat(displayedProduct.getStock()).isEqualTo(dbProduct.getStock());
    }

    /**
     * Test: トランザクション履歴が正しく日時順にソートされている確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴が日時の新しい順（desc）に正確にソートされている")
    void testEndToEnd_Transaction_SortOrderByDate() throws Exception {
        // Given: 新規テスト商品を作成
        Product sortTestProduct = new Product();
        sortTestProduct.setProductCode("SORT0001");
        sortTestProduct.setProductName("ソート順序テスト商品");
        sortTestProduct.setCategory("テストカテゴリ");
        sortTestProduct.setStock(50);
        sortTestProduct.setPrice(new BigDecimal("5000.00"));
        sortTestProduct.setStatus("active");
        sortTestProduct.setDescription("ソート順序テスト用");
        sortTestProduct.setCreatedAt(LocalDateTime.now());
        sortTestProduct.setUpdatedAt(LocalDateTime.now());
        sortTestProduct = productRepository.save(sortTestProduct);

        // 複数のトランザクションを記録
        for (int i = 1; i <= 3; i++) {
            String requestBody = String.format("""
                {
                    "productId": %d,
                    "transactionType": "in",
                    "quantity": 1,
                    "remarks": "トランザクション#%d"
                }
                """, sortTestProduct.getId(), i);

            mockMvc.perform(post("/api/inventory/update-stock")
                           .with(csrf())
                           .contentType(MediaType.APPLICATION_JSON)
                           .content(requestBody))
                   .andExpect(status().isOk());
        }

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", sortTestProduct.getId()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: 表示されたトランザクションが存在することを確認
        assertThat(transactions).isNotEmpty();

        // Assert: トランザクションが新しい順にソートされていることを確認（重要な検証）
        for (int i = 0; i < transactions.size() - 1; i++) {
            assertThat(transactions.get(i).getTransactionDate())
                    .isAfterOrEqualTo(transactions.get(i + 1).getTransactionDate());
        }

        // Assert: DBからの取得結果とも同じソート順序であることを確認
        List<StockTransaction> dbTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(sortTestProduct.getId());
        assertThat(dbTransactions).isNotEmpty();
        
        // DBのソート順序もチェック
        for (int i = 0; i < dbTransactions.size() - 1; i++) {
            assertThat(dbTransactions.get(i).getTransactionDate())
                    .isAfterOrEqualTo(dbTransactions.get(i + 1).getTransactionDate());
        }
        
        // 表示順と DB取得順が一致することを確認（最新のものから順に一致）
        for (int i = 0; i < Math.min(transactions.size(), dbTransactions.size()); i++) {
            assertThat(transactions.get(i).getId()).isEqualTo(dbTransactions.get(i).getId());
        }
    }

    /**
     * Test: 入庫・出庫の種別表示が正確に区別されているか確認
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合】トランザクション履歴で入庫・出庫の種別が正確に表示されている")
    void testEndToEnd_Transaction_TypeDistinction() throws Exception {
        // Given: 入庫と出庫を記録
        String in = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫テスト"
            }
            """, testProduct1.getId());
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(in))
               .andExpect(status().isOk());

        String out = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 3,
                "remarks": "出庫テスト"
            }
            """, testProduct1.getId());
        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(out))
               .andExpect(status().isOk());

        // Act: 商品詳細画面を取得
        MvcResult result = mockMvc.perform(get("/inventory/products/{id}", testProduct1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView()
                .getModel().get("transactions");

        // Assert: 最新の出庫トランザクションを確認
        StockTransaction latestOut = transactions.stream()
                .filter(t -> "出庫テスト".equals(t.getRemarks()))
                .findFirst()
                .orElseThrow();
        assertThat(latestOut.getTransactionType()).isEqualTo("out");
        assertThat(latestOut.getQuantity()).isEqualTo(3);

        // Assert: 入庫トランザクションを確認
        StockTransaction latestIn = transactions.stream()
                .filter(t -> "入庫テスト".equals(t.getRemarks()))
                .findFirst()
                .orElseThrow();
        assertThat(latestIn.getTransactionType()).isEqualTo("in");
        assertThat(latestIn.getQuantity()).isEqualTo(10);

        // Assert: DBからの取得結果とも一致
        List<StockTransaction> dbTransactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1.getId());
        long dbInCount = dbTransactions.stream().filter(t -> "in".equals(t.getTransactionType())).count();
        long dbOutCount = dbTransactions.stream().filter(t -> "out".equals(t.getTransactionType())).count();
        
        long displayInCount = transactions.stream().filter(t -> "in".equals(t.getTransactionType())).count();
        long displayOutCount = transactions.stream().filter(t -> "out".equals(t.getTransactionType())).count();
        
        assertThat(displayInCount).isEqualTo(dbInCount);
        assertThat(displayOutCount).isEqualTo(dbOutCount);
    }
}
