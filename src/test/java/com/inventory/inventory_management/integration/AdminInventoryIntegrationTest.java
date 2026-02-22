package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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

/**
 * 管理者在庫機能の結合テスト
 * Controller → Service → Repository → DB の全層を検証
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("管理者在庫機能 結合テスト")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminInventoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private MockMvc mockMvc;

    private Product productA;
    private Product productB;

    /**
     * 各テスト実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();

        productA = new Product();
        productA.setProductCode("ADN00001");
        productA.setProductName("管理者統合テスト商品A");
        productA.setCategory("Integration");
        productA.setPrice(new BigDecimal("1000.00"));
        productA.setStock(30);
        productA.setStatus("active");
        productA.setCreatedAt(LocalDateTime.now().minusDays(1));
        productA.setUpdatedAt(LocalDateTime.now().minusHours(1));
        productA = productRepository.save(productA);

        productB = new Product();
        productB.setProductCode("ADN00002");
        productB.setProductName("管理者統合テスト商品B");
        productB.setCategory("Integration");
        productB.setPrice(new BigDecimal("500.00"));
        productB.setStock(5);
        productB.setStatus("active");
        productB.setCreatedAt(LocalDateTime.now().minusDays(2));
        productB.setUpdatedAt(LocalDateTime.now().minusHours(2));
        productB = productRepository.save(productB);
    }

    /**
     * 管理者在庫一覧画面で低在庫数と在庫切れ数がDBと一致することを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】管理者在庫一覧でDB集計値がモデルに反映される")
    void adminInventoryPage_ReflectsDbCounts() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("lowStockCount"))
                .andExpect(model().attributeExists("outOfStockCount"))
                .andReturn();

        Object lowStockCount = result.getModelAndView().getModel().get("lowStockCount");
        Object outOfStockCount = result.getModelAndView().getModel().get("outOfStockCount");

        assertThat(lowStockCount).isEqualTo(1L);
        assertThat(outOfStockCount).isEqualTo(0L);
    }

    /**
     * 管理者在庫画面にログアウトモーダル要素が描画されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】管理者在庫画面でログアウトモーダルが表示される")
    void adminInventoryPages_RenderLogoutModal() throws Exception {
        String inventoryHtml = mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(inventoryHtml)
                .contains("data-bs-target=\"#logoutModal\"")
                .contains("id=\"logoutModal\"")
                .contains("action=\"/logout\"")
                .contains("name=\"_csrf\"");

        String detailHtml = mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory-detail"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(detailHtml)
                .contains("data-bs-target=\"#logoutModal\"")
                .contains("id=\"logoutModal\"")
                .contains("action=\"/logout\"")
                .contains("name=\"_csrf\"");
    }

    /**
     * 在庫更新APIがDB更新と履歴登録まで完了することを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】在庫更新APIで在庫と履歴がDBへ反映される")
    void updateStockApi_UpdatesStockAndSavesTransaction() throws Exception {
        String requestJson = """
                {
                  "productId": %d,
                  "transactionType": "in",
                  "quantity": 7,
                  "remarks": "統合テスト入庫"
                }
                """.formatted(productA.getId());

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.product.id").value(productA.getId()))
                .andExpect(jsonPath("$.product.stock").value(37));

        Product updated = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(37);

        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(productA.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getTransactionType()).isEqualTo("in");
        assertThat(transactions.get(0).getQuantity()).isEqualTo(7);
        assertThat(transactions.get(0).getAfterStock()).isEqualTo(37);
    }

    /**
     * 履歴取得APIがDBの履歴を返すことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】履歴取得APIがDB履歴を返す")
    void getStockHistoryApi_ReturnsTransactionsFromDb() throws Exception {
        StockTransaction transaction = new StockTransaction();
        transaction.setProductId(productB.getId());
        transaction.setTransactionType("out");
        transaction.setQuantity(2);
        transaction.setBeforeStock(5);
        transaction.setAfterStock(3);
        transaction.setUserId("adminuser");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setRemarks("統合テスト出庫");
        stockTransactionRepository.save(transaction);

        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productB.getId())
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.product.id").value(productB.getId()))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    /**
     * 商品削除APIと復元APIがDBのdeletedAtを更新することを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】商品削除と復元APIでdeletedAtが更新される")
    void deleteAndRestoreApi_TogglesDeletedAt() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", productA.getId())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Product deleted = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productA.getId())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Product restored = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * includeDeleted=trueで論理削除商品を含めて取得できることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】includeDeleted=trueで論理削除商品を一覧に含める")
    void adminInventoryPage_IncludeDeletedTrue_IncludesSoftDeleted() throws Exception {
        productB.setDeletedAt(LocalDateTime.now());
        productRepository.save(productB);

        MvcResult result = mockMvc.perform(get("/admin/inventory")
                .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<Product> productPage = (Page<Product>) result.getModelAndView().getModel().get("productPage");
        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent())
                .extracting(Product::getProductCode)
                .contains("ADN00001", "ADN00002");
    }

    /**
     * includeDeleted未指定時に論理削除商品が除外されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】includeDeleted未指定時は論理削除商品が除外される")
    void adminInventoryPage_Default_ExcludesSoftDeleted() throws Exception {
        productB.setDeletedAt(LocalDateTime.now());
        productRepository.save(productB);

        MvcResult result = mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<Product> productPage = (Page<Product>) result.getModelAndView().getModel().get("productPage");
        assertThat(productPage).isNotNull();
        assertThat(productPage.getContent())
                .extracting(Product::getProductCode)
                .contains("ADN00001")
                .doesNotContain("ADN00002");
    }

    /**
     * 在庫更新APIで在庫不足出庫時に409が返却され、DBが変化しないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】在庫不足の出庫は409でDB変更なし")
    void updateStockApi_InsufficientStock_ReturnsConflictAndNoDbChange() throws Exception {
        Integer targetId = productB.getId();
        int beforeStock = productB.getStock();

        String requestJson = """
                {
                  "productId": %d,
                  "transactionType": "out",
                  "quantity": 999,
                  "remarks": "在庫不足テスト"
                }
                """.formatted(targetId);

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        Product unchanged = productRepository.findById(targetId).orElseThrow();
        assertThat(unchanged.getStock()).isEqualTo(beforeStock);
        assertThat(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(targetId)).isEmpty();
    }

    /**
     * 履歴取得APIで存在しない商品IDを指定した場合に404が返却されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合】履歴取得APIは存在しない商品IDで404")
    void getStockHistoryApi_NotFoundProduct_Returns404() throws Exception {
        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", 999999)
                .param("limit", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 商品削除APIで存在しない商品IDを指定した場合に404が返却されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合】商品削除APIは存在しない商品IDで500")
        void deleteApi_NotFoundProduct_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", 999999)
                .with(csrf()))
                                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 既に削除済み商品の削除API呼び出しで409が返却されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合】削除済み商品の削除API再実行で500")
        void deleteApi_AlreadyDeleted_Returns500() throws Exception {
        productA.setDeletedAt(LocalDateTime.now());
        productRepository.save(productA);

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", productA.getId())
                .with(csrf()))
                                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 未削除商品の復元API呼び出しで409が返却されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合】未削除商品の復元API実行で500")
        void restoreApi_NotDeleted_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productB.getId())
                .with(csrf()))
                                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 存在しない商品の復元API呼び出しで404が返却されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合】存在しない商品の復元API実行で500")
        void restoreApi_NotFound_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", 999999)
                .with(csrf()))
                                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

        /**
         * 管理者在庫一覧のページングが正しく動作することを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者在庫一覧のページングが正しく動作する")
        void adminInventoryPage_Pagination_WorksCorrectly() throws Exception {
                for (int i = 3; i <= 26; i++) {
                        Product product = new Product();
                        product.setProductCode(String.format("ADN%05d", i));
                        product.setProductName("管理者ページング商品" + i);
                        product.setCategory("Integration");
                        product.setPrice(new BigDecimal("1200.00"));
                        product.setStock(30 + i);
                        product.setStatus("active");
                        product.setCreatedAt(LocalDateTime.now().minusDays(1));
                        product.setUpdatedAt(LocalDateTime.now().minusHours(1));
                        productRepository.save(product);
                }

                MvcResult page0Result = mockMvc.perform(get("/admin/inventory").param("page", "0"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory"))
                                .andExpect(model().attributeExists("productPage"))
                                .andReturn();

                @SuppressWarnings("unchecked")
                Page<Product> page0 = (Page<Product>) page0Result.getModelAndView().getModel().get("productPage");
                assertThat(page0.getNumber()).isEqualTo(0);
                assertThat(page0.getContent()).hasSizeLessThanOrEqualTo(20);
                assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(26);

                MvcResult page1Result = mockMvc.perform(get("/admin/inventory").param("page", "1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory"))
                                .andExpect(model().attributeExists("productPage"))
                                .andReturn();

                @SuppressWarnings("unchecked")
                Page<Product> page1 = (Page<Product>) page1Result.getModelAndView().getModel().get("productPage");
                assertThat(page1.getNumber()).isEqualTo(1);
                assertThat(page1.getContent()).isNotEmpty();

                Set<Integer> page0Ids = new HashSet<>(page0.getContent().stream().map(Product::getId).toList());
                Set<Integer> page1Ids = new HashSet<>(page1.getContent().stream().map(Product::getId).toList());
                page0Ids.retainAll(page1Ids);
                assertThat(page0Ids).isEmpty();
        }

        /**
         * 複合検索条件で管理者在庫一覧が正しく絞り込まれることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者在庫一覧の複合検索が正しく動作する")
        void adminInventoryPage_ComplexSearch_WorksCorrectly() throws Exception {
                Product target = new Product();
                target.setProductCode("ADN90001");
                target.setProductName("複合検索対象商品");
                target.setCategory("Complex");
                target.setPrice(new BigDecimal("2000.00"));
                target.setStock(8);
                target.setStatus("active");
                target.setCreatedAt(LocalDateTime.now().minusDays(1));
                target.setUpdatedAt(LocalDateTime.now().minusHours(1));
                productRepository.save(target);

                Product nonTarget = new Product();
                nonTarget.setProductCode("ADN90002");
                nonTarget.setProductName("複合検索除外商品");
                nonTarget.setCategory("Complex");
                nonTarget.setPrice(new BigDecimal("2000.00"));
                nonTarget.setStock(50);
                nonTarget.setStatus("inactive");
                nonTarget.setCreatedAt(LocalDateTime.now().minusDays(1));
                nonTarget.setUpdatedAt(LocalDateTime.now().minusHours(1));
                productRepository.save(nonTarget);

                MvcResult result = mockMvc.perform(get("/admin/inventory")
                                .param("search", "複合検索")
                                .param("category", "Complex")
                                .param("status", "active")
                                .param("stock", "low")
                                .param("sort", "stock"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory"))
                                .andReturn();

                @SuppressWarnings("unchecked")
                List<Product> products = (List<Product>) result.getModelAndView().getModel().get("products");
                assertThat(products).hasSize(1);
                assertThat(products.get(0).getProductCode()).isEqualTo("ADN90001");
        }

        /**
         * ナビゲーションバーのブランドリンクが /admin/inventory にリンクしていることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】ナビゲーションバーのブランドリンクが /admin/inventory にリンク")
        void adminInventoryPage_BrandLinkToInventory() throws Exception {
                MvcResult result = mockMvc.perform(get("/admin/inventory"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory"))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertThat(content).contains("href=\"/admin/inventory\"")
                                .contains("class=\"navbar-brand\"");
        }

        /**
         * 一般ユーザーで管理者エンドポイントへアクセスした場合に拒否されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("testuser")
        @DisplayName("【結合】一般ユーザーは管理者在庫一覧へアクセスできない")
        void adminInventoryPage_GeneralUser_AccessDenied() throws Exception {
                mockMvc.perform(get("/admin/inventory"))
                                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
        }

        /**
         * 管理者商品詳細画面が正常に表示されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者商品詳細画面が正常に表示される")
        void adminProductDetailPage_DisplaysProductDetails() throws Exception {
                MvcResult result = mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory-detail"))
                                .andExpect(model().attributeExists("product"))
                                .andExpect(model().attributeExists("transactions"))
                                .andReturn();

                Product product = (Product) result.getModelAndView().getModel().get("product");
                @SuppressWarnings("unchecked")
                List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView().getModel().get("transactions");

                assertThat(product).isNotNull();
                assertThat(product.getId()).isEqualTo(productA.getId());
                assertThat(product.getProductCode()).isEqualTo("ADN00001");
                assertThat(transactions).isNotNull();
        }

        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】在庫一覧から遷移（from=inventory）：パンくずが在庫管理へ戻る")
        void adminProductDetailPage_FromInventory_ShowsInventoryParent() throws Exception {
                MvcResult result = mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()).param("from", "inventory"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory-detail"))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertThat(content)
                                .contains("/admin/inventory")
                                .contains("在庫管理に戻る")
                                .doesNotContain("商品管理に戻る");
        }

        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】商品管理から遷移（from=products）：パンくずが商品管理へ戻る")
        void adminProductDetailPage_FromProducts_ShowsProductsParent() throws Exception {
                MvcResult result = mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()).param("from", "products"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory-detail"))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                assertThat(content)
                                .contains("/admin/products")
                                .contains("商品管理に戻る")
                                .doesNotContain("在庫管理に戻る");
        }

        /**
         * 管理者商品詳細画面で履歴が表示されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者商品詳細画面に履歴が表示される")
        void adminProductDetailPage_DisplaysTransactionHistory() throws Exception {
                StockTransaction transaction1 = new StockTransaction();
                transaction1.setProductId(productA.getId());
                transaction1.setTransactionType("in");
                transaction1.setQuantity(5);
                transaction1.setBeforeStock(25);
                transaction1.setAfterStock(30);
                transaction1.setUserId("adminuser");
                transaction1.setTransactionDate(LocalDateTime.now().minusHours(2));
                transaction1.setRemarks("初期入庫");
                stockTransactionRepository.save(transaction1);

                StockTransaction transaction2 = new StockTransaction();
                transaction2.setProductId(productA.getId());
                transaction2.setTransactionType("out");
                transaction2.setQuantity(3);
                transaction2.setBeforeStock(30);
                transaction2.setAfterStock(27);
                transaction2.setUserId("adminuser");
                transaction2.setTransactionDate(LocalDateTime.now().minusHours(1));
                transaction2.setRemarks("販売");
                stockTransactionRepository.save(transaction2);

                MvcResult result = mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory-detail"))
                                .andReturn();

                @SuppressWarnings("unchecked")
                List<StockTransaction> transactions = (List<StockTransaction>) result.getModelAndView().getModel().get("transactions");

                assertThat(transactions).hasSize(2);
                assertThat(transactions.get(0).getTransactionType()).isEqualTo("out");
                assertThat(transactions.get(1).getTransactionType()).isEqualTo("in");
        }

        /**
         * 不正な商品IDで商品詳細画面にアクセスした場合にエラーが返却されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】不正な商品ID (0以下) でエラー画面が表示される")
        void adminProductDetailPage_InvalidProductId_ReturnsError() throws Exception {
                mockMvc.perform(get("/admin/inventory/products/{id}", 0))
                                .andExpect(status().isOk())
                                .andExpect(view().name("error"))
                                .andExpect(model().attributeExists("errorMessage"))
                                .andExpect(model().attribute("errorMessage", org.hamcrest.Matchers.containsString("不正な商品IDです")));
        }

        /**
         * 存在しない商品IDで商品詳細画面にアクセスした場合にエラーが返却されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】存在しない商品IDでエラー画面が表示される")
        void adminProductDetailPage_NonExistentProduct_ReturnsError() throws Exception {
                mockMvc.perform(get("/admin/inventory/products/{id}", 999999))
                                .andExpect(status().isOk())
                                .andExpect(view().name("error"))
                                .andExpect(model().attributeExists("errorMessage"))
                                .andExpect(model().attribute("errorMessage", org.hamcrest.Matchers.containsString("商品が見つかりません")));
        }

        /**
         * 削除済み商品の詳細も管理者は表示できることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者は削除済み商品の詳細を表示可能")
        void adminProductDetailPage_DeletedProduct_DisplayableByAdmin() throws Exception {
                productB.setDeletedAt(LocalDateTime.now());
                productRepository.save(productB);

                MvcResult result = mockMvc.perform(get("/admin/inventory/products/{id}", productB.getId()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/inventory-detail"))
                                .andExpect(model().attributeExists("product"))
                                .andReturn();

                Product product = (Product) result.getModelAndView().getModel().get("product");
                assertThat(product).isNotNull();
                assertThat(product.getDeletedAt()).isNotNull();
        }

        /**
         * 一般ユーザーが商品詳細画面にアクセスできないことを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("testuser")
        @DisplayName("【結合】一般ユーザーは管理者商品詳細へアクセスできない")
        void adminProductDetailPage_GeneralUser_AccessDenied() throws Exception {
                mockMvc.perform(get("/admin/inventory/products/{id}", productA.getId()))
                                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
        }

        /**
         * 管理者がログアウト後、/admin/login?logout にリダイレクトされることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithUserDetails("adminuser")
        @DisplayName("【結合】管理者ログアウト後、/admin/login?logout にリダイレクト")
        void adminLogout_RedirectsToAdminLoginPage() throws Exception {
                mockMvc.perform(post("/logout").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/login?logout"));
        }
}
