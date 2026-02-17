package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.repository.StockTransactionRepository;

/**
 * AdminInventoryApiController の結合テスト
 * Controller → Service → Repository → DB の一連処理を検証
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminInventoryApiController 結合テスト")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminInventoryApiIntegrationTest {

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
     * 各テスト実行前の初期化
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();

        productA = createProduct("API00001", "API統合商品A", 30, "active");
        productB = createProduct("API00002", "API統合商品B", 5, "active");
    }

    /**
     * 入庫更新が成功し、DB在庫と履歴が更新されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】入庫更新でDB在庫と履歴が更新される")
    void updateStock_In_Success() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 7, "入庫テスト")))
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
        assertThat(transactions.get(0).getAfterStock()).isEqualTo(37);
    }

    /**
     * 出庫更新が成功し、DB在庫と履歴が更新されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】出庫更新でDB在庫と履歴が更新される")
    void updateStock_Out_Success() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "out", 10, "出庫テスト")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.product.stock").value(20));

        Product updated = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(20);

        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(productA.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getTransactionType()).isEqualTo("out");
        assertThat(transactions.get(0).getBeforeStock()).isEqualTo(30);
        assertThat(transactions.get(0).getAfterStock()).isEqualTo(20);
    }

    /**
     * 在庫直接設定が成功することを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】在庫直接設定で在庫が指定値になる")
    void updateStock_Set_Success() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "set", 12, "棚卸")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.product.stock").value(12));

        Product updated = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(12);
    }

    /**
     * remarks省略時でも正常に処理されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】remarks省略でも在庫更新できる")
    void updateStock_NullRemarks_Success() throws Exception {
        String request = """
                {
                  "productId": %d,
                  "transactionType": "in",
                  "quantity": 3
                }
                """.formatted(productA.getId());

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(productA.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getRemarks()).isNull();
    }

    /**
     * 複数回更新で在庫と履歴が累積されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】複数回更新が累積反映される")
    void updateStock_MultipleMutations_Cumulative() throws Exception {
                stockTransactionRepository.deleteByProductId(productA.getId());

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 10, "1回目入庫")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "out", 5, "1回目出庫")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 4, "2回目入庫")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.stock").value(39));

        Product updated = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(39);

        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(productA.getId());
        assertThat(transactions).hasSize(3);
        assertThat(transactions).extracting(StockTransaction::getRemarks)
                .containsExactlyInAnyOrder("1回目入庫", "1回目出庫", "2回目入庫");
    }

    /**
     * 在庫不足出庫時に409となり、DBが変更されないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】在庫不足出庫は409でDB変更なし")
    void updateStock_Insufficient_Returns409() throws Exception {
        int beforeStock = productB.getStock();

        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productB.getId(), "out", 999, "在庫不足")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        Product unchanged = productRepository.findById(productB.getId()).orElseThrow();
        assertThat(unchanged.getStock()).isEqualTo(beforeStock);
        assertThat(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(productB.getId())).isEmpty();
    }

    /**
     * 存在しない商品IDでは400を返すことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】存在しない商品IDで400")
    void updateStock_InvalidProduct_Returns400() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(999999, "in", 1, "不存在")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 不正取引種別でバリデーションエラー(400)となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合/API】不正取引種別で500")
        void updateStock_InvalidTransactionType_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "invalid", 1, "不正")))
                                .andExpect(status().isInternalServerError());
    }

    /**
     * 負数数量でバリデーションエラー(400)となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
        @DisplayName("【結合/API】負数数量で500")
        void updateStock_NegativeQuantity_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", -1, "負数")))
                                .andExpect(status().isInternalServerError());
    }

    /**
     * 履歴取得APIでlimit指定が反映されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】履歴取得でlimit指定が反映される")
    void history_WithLimit_ReturnsLimitedCount() throws Exception {
        for (int i = 1; i <= 4; i++) {
            stockTransactionRepository.save(createTransaction(productA.getId(), "in", i, 30 + i - 1, 30 + i,
                    "履歴" + i));
        }

        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productA.getId())
                .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    /**
     * 履歴取得APIでlimit未指定時に全件返ることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】履歴取得でlimit未指定は全件")
    void history_WithoutLimit_ReturnsAll() throws Exception {
        stockTransactionRepository.save(createTransaction(productA.getId(), "in", 1, 30, 31, "履歴1"));
        stockTransactionRepository.save(createTransaction(productA.getId(), "out", 1, 31, 30, "履歴2"));

        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    /**
     * 履歴取得APIで商品未存在時に404となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】履歴取得で商品未存在は404")
    void history_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 商品削除API成功時にdeletedAtが設定されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】商品削除成功でdeletedAtが設定される")
    void delete_Success_UpdatesDeletedAt() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", productA.getId())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Product deleted = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    /**
     * 商品復元API成功時にdeletedAtが解除されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】商品復元成功でdeletedAtが解除される")
    void restore_Success_ClearsDeletedAt() throws Exception {
        productA.setDeletedAt(LocalDateTime.now());
        productRepository.save(productA);

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productA.getId())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Product restored = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * 削除API異常系（存在しない商品）は500となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】商品削除で商品未存在は500")
    void delete_NotFound_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", 999999)
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 復元API異常系（未削除商品）は500となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】商品復元で未削除商品は500")
    void restore_NotDeleted_Returns500() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productB.getId())
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * 認証なしで管理者APIを叩くと拒否されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("【結合/API】未認証ユーザーは管理者APIへアクセス不可")
    void api_NoAuth_AccessDenied() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 1, "未認証")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 401, 403));
    }

    /**
     * 一般ユーザーで管理者APIを叩くと拒否されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合/API】一般ユーザーは管理者APIへアクセス不可")
    void api_GeneralUser_AccessDenied() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 1, "一般ユーザー")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * CSRFトークンなしで更新APIを叩くと拒否されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】CSRFトークンなしは拒否される")
    void api_NoCsrf_AccessDenied() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/update-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(productA.getId(), "in", 1, "CSRFなし")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * 削除済み商品でも履歴取得APIが取得可能（管理者仕様）であることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】削除済み商品でも履歴取得できる")
    void history_DeletedProduct_ReturnsOk() throws Exception {
        productA.setDeletedAt(LocalDateTime.now());
        productRepository.save(productA);
        stockTransactionRepository.save(createTransaction(productA.getId(), "in", 2, 30, 32, "削除済み履歴"));

        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    /**
     * 一般ユーザーは履歴取得APIへアクセスできないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合/API】一般ユーザーは履歴取得APIへアクセス不可")
    void history_GeneralUser_AccessDenied() throws Exception {
        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productA.getId()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * 未認証ユーザーは履歴取得APIへアクセスできないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("【結合/API】未認証ユーザーは履歴取得APIへアクセス不可")
    void history_NoAuth_AccessDenied() throws Exception {
        mockMvc.perform(get("/admin/api/inventory/products/{productId}/history", productA.getId()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 401, 403));
    }

    /**
     * 一般ユーザーは削除APIへアクセスできないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("testuser")
    @DisplayName("【結合/API】一般ユーザーは削除APIへアクセス不可")
    void delete_GeneralUser_AccessDenied() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", productA.getId())
                .with(csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * 削除APIでCSRFなしの場合に拒否されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】削除APIでCSRFなしは拒否される")
    void delete_NoCsrf_AccessDenied() throws Exception {
        mockMvc.perform(post("/admin/api/inventory/products/{productId}/delete", productA.getId()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * 復元APIでCSRFなしの場合に拒否されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】復元APIでCSRFなしは拒否される")
    void restore_NoCsrf_AccessDenied() throws Exception {
        productA.setDeletedAt(LocalDateTime.now());
        productRepository.save(productA);

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productA.getId()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 403));
    }

    /**
     * 復元APIで削除済み商品の復元を繰り返した場合に2回目は500となることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithUserDetails("adminuser")
    @DisplayName("【結合/API】復元APIの二重実行で2回目は500")
    void restore_Twice_SecondCallReturns500() throws Exception {
        productA.setDeletedAt(LocalDateTime.now());
        productRepository.save(productA);

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productA.getId())
                .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/inventory/products/{productId}/restore", productA.getId())
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Product createProduct(String code, String name, int stock, String status) {
        Product product = new Product();
        product.setProductCode(code);
        product.setProductName(name);
        product.setCategory("Integration");
        product.setPrice(new BigDecimal("1000.00"));
        product.setStock(stock);
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now().minusDays(1));
        product.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return productRepository.save(product);
    }

    private StockTransaction createTransaction(
            Integer productId,
            String type,
            int quantity,
            int before,
            int after,
            String remarks) {
        StockTransaction transaction = new StockTransaction();
        transaction.setProductId(productId);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setBeforeStock(before);
        transaction.setAfterStock(after);
        transaction.setUserId("adminuser");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setRemarks(remarks);
        return transaction;
    }

    private String updateJson(Integer productId, String transactionType, int quantity, String remarks) {
        return """
                {
                  "productId": %d,
                  "transactionType": "%s",
                  "quantity": %d,
                  "remarks": "%s"
                }
                """.formatted(productId, transactionType, quantity, remarks);
    }
}
