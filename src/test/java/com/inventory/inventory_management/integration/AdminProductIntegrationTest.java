package com.inventory.inventory_management.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.repository.ProductRepository;

/**
 * AdminProductController の結合テスト
 * Controller → Service → Repository → DB の一連処理を検証
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminProductController 結合テスト")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminProductIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    private Product baseProduct;

    /**
     * 各テスト実行前の初期化
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        productRepository.deleteAll();
        baseProduct = createProduct("PRD00001", "管理者商品A", "Electronics", "IT-SKU-BASE-01", 1200, 10, "active");
    }

    /**
     * 商品一覧画面が正常表示され、モデルに一覧が設定されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】管理者商品一覧を表示できる")
    void showAdminProducts_Success() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(model().attributeExists("productPage"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("quickForm"));

        List<Product> products = productRepository.findAll();
        assertThat(products).extracting(Product::getProductCode).contains("PRD00001");
    }

    /**
     * includeDeleted 未指定時は削除済み商品が一覧に表示されないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】商品一覧はデフォルトで削除済み商品を含めない")
    void showAdminProducts_DefaultExcludesDeleted() throws Exception {
        Product deletedProduct = createProduct("PRD00002", "削除済み統合商品", "Books", "IT-SKU-DEL-01", 980, 3, "active");
        deletedProduct.setDeletedAt(LocalDateTime.now().minusDays(1));
        productRepository.save(deletedProduct);

        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(content().string(not(containsString("削除済み統合商品"))));
    }

    /**
     * includeDeleted=true 指定時は削除済み商品が一覧に表示されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】商品一覧で削除済み商品を含めるチェック指定時に削除済み商品が表示される")
    void showAdminProducts_WithIncludeDeleted_ShowsDeletedProducts() throws Exception {
        Product deletedProduct = createProduct("PRD00003", "削除済み表示商品", "Books", "IT-SKU-DEL-02", 1080, 5, "active");
        deletedProduct.setDeletedAt(LocalDateTime.now().minusDays(1));
        productRepository.save(deletedProduct);

        mockMvc.perform(get("/admin/products")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(content().string(containsString("削除済み表示商品")))
                .andExpect(content().string(containsString("削除済")));
    }

        /**
         * 商品新規登録画面が正常表示されることを検証
         * @throws Exception テスト実行時の例外
         */
        @Test
        @WithMockUser(username = "adminuser", roles = {"ADMIN"})
        @DisplayName("【結合】商品新規登録画面を表示できる")
        void showCreateForm_Success() throws Exception {
                mockMvc.perform(get("/admin/products/create"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/product-create"))
                                .andExpect(model().attributeExists("detailForm"))
                                .andExpect(model().attributeExists("categories"));
        }

                    /**
                     * 商品管理画面（新規登録/編集）にログアウトモーダル要素が描画されることを検証
                     * @throws Exception テスト実行時の例外
                     */
                    @Test
                    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
                    @DisplayName("【結合】商品管理画面でログアウトモーダルが表示される")
                    void productPages_RenderLogoutModal() throws Exception {
                    String createHtml = mockMvc.perform(get("/admin/products/create"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/product-create"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                    assertThat(createHtml)
                        .contains("data-bs-target=\"#logoutModal\"")
                        .contains("id=\"logoutModal\"")
                        .contains("action=\"/logout\"")
                        .contains("name=\"_csrf\"");

                    String editHtml = mockMvc.perform(get("/admin/products/{id}/edit", baseProduct.getId()))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/product-edit"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                    assertThat(editHtml)
                        .contains("data-bs-target=\"#logoutModal\"")
                        .contains("id=\"logoutModal\"")
                        .contains("action=\"/logout\"")
                        .contains("name=\"_csrf\"");
                    }

    /**
     * クイック登録で商品がDBに保存されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】クイック登録で商品がDBへ保存される")
    void createProductQuick_Success() throws Exception {
        long beforeCount = productRepository.count();

        mockMvc.perform(post("/admin/products/quick-create")
                .with(csrf())
                .param("productName", "統合クイック商品")
                .param("category", "Books")
                .param("price", "1980.00")
                .param("stockQuantity", "25")
                .param("status", "active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        assertThat(productRepository.count()).isEqualTo(beforeCount + 1);
        Product saved = productRepository.findAll().stream()
                .filter(product -> "統合クイック商品".equals(product.getProductName()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getCategory()).isEqualTo("Books");
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("1980.00"));
        assertThat(saved.getStock()).isEqualTo(25);
        assertThat(saved.getDeletedAt()).isNull();
    }

    /**
     * 詳細登録で商品がDBに保存され、任意項目も反映されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】詳細登録で全項目がDBへ保存される")
    void createProductDetail_Success() throws Exception {
        mockMvc.perform(post("/admin/products/create")
                .with(csrf())
                .param("productName", "統合詳細商品")
                .param("category", "Home")
                .param("sku", "IT-SKU-DETAIL-01")
                .param("price", "3450.00")
                .param("stockQuantity", "11")
                .param("status", "inactive")
                .param("description", "詳細登録の説明")
                .param("warrantyMonths", "12")
                .param("dimensions", "10x20x30")
                .param("variations", "blue")
                .param("manufacturingDate", "2025-01-15")
                .param("expirationDate", "2027-01-15")
                .param("tags", "tag1,tag2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product saved = productRepository.findAll().stream()
                .filter(product -> "統合詳細商品".equals(product.getProductName()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getSku()).isEqualTo("IT-SKU-DETAIL-01");
        assertThat(saved.getStatus()).isEqualTo("inactive");
        assertThat(saved.getWarrantyMonths()).isEqualTo(12);
        assertThat(saved.getManufacturingDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(saved.getExpirationDate()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    /**
     * 編集画面表示で既存商品情報がモデルに設定されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】編集画面表示で既存商品情報を取得できる")
    void showEditForm_Success() throws Exception {
        mockMvc.perform(get("/admin/products/{id}/edit", baseProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-edit"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("detailForm"))
                .andExpect(model().attributeExists("categories"));
    }

    /**
     * 商品更新でDBの値が更新されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】商品更新でDBが更新される")
    void updateProduct_Success() throws Exception {
        mockMvc.perform(post("/admin/products/{id}/edit", baseProduct.getId())
                .with(csrf())
                .param("productName", "更新後商品")
                .param("category", "Updated")
                .param("sku", "IT-SKU-UPDATED-01")
                .param("price", "8800.00")
                .param("stockQuantity", "77")
                .param("status", "active")
                .param("description", "更新説明")
                .param("warrantyMonths", "24")
                .param("dimensions", "50x40x30")
                .param("variations", "black")
                .param("manufacturingDate", "2026-02-01")
                .param("expirationDate", "2028-02-01")
                .param("tags", "updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product updated = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(updated.getProductName()).isEqualTo("更新後商品");
        assertThat(updated.getCategory()).isEqualTo("Updated");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("8800.00"));
        assertThat(updated.getStock()).isEqualTo(77);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(baseProduct.getUpdatedAt());
    }

    /**
     * 論理削除で deletedAt が設定されることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】商品削除でdeletedAtが設定される")
    void deleteProduct_Success() throws Exception {
        mockMvc.perform(post("/admin/products/{id}/delete", baseProduct.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product deleted = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    /**
     * 論理削除済み商品を再度削除しようとした場合にエラーリダイレクトされることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】論理削除済み商品を再削除するとエラーリダイレクトされる")
    void deleteProduct_AlreadyDeleted_RedirectsWithError() throws Exception {
        baseProduct.setDeletedAt(LocalDateTime.now().minusDays(1));
        productRepository.save(baseProduct);

        mockMvc.perform(post("/admin/products/{id}/delete", baseProduct.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        // deletedAt は変わらず設定されたままであること
        Product product = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(product.getDeletedAt()).isNotNull();
    }

    /**
     * CSRFトークンなしで削除リクエストすると403が返ることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】CSRFトークンなしの削除リクエストは403になる")
    void deleteProduct_WithoutCsrf_Forbidden() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/products/{id}/delete", baseProduct.getId()))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(302, 403);

        // DBは変更されていないこと
        Product product = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(product.getDeletedAt()).isNull();
    }

    /**
     * 一般ユーザーが削除エンドポイントを叩いても実行されないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("【結合】一般ユーザーによる削除リクエストはリダイレクト/拒否される")
    void deleteProduct_UnauthorizedUser_Forbidden() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/products/{id}/delete", baseProduct.getId())
                .with(csrf()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(302, 403);

        // DBは変更されていないこと
        Product product = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(product.getDeletedAt()).isNull();
    }

    /**
     * 論理削除済み商品を復元できることを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @DisplayName("【結合】商品復元でdeletedAtが解除される")
    void restoreProduct_Success() throws Exception {
        baseProduct.setDeletedAt(LocalDateTime.now());
        productRepository.save(baseProduct);

        mockMvc.perform(post("/admin/products/{id}/restore", baseProduct.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product restored = productRepository.findById(baseProduct.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * 一般ユーザーは管理者商品画面にアクセスできないことを検証
     * @throws Exception テスト実行時の例外
     */
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("【結合】一般ユーザーは管理者商品画面へアクセス不可")
    void adminProducts_GeneralUser_AccessDenied() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/products"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(302, 403);
    }

    private Product createProduct(
            String productCode,
            String productName,
            String category,
            String sku,
            int price,
            int stock,
            String status) {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setCategory(category);
        product.setSku(sku);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(status);
        product.setDescription("統合テスト商品");
        product.setCreatedAt(LocalDateTime.now().minusDays(1));
        product.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return productRepository.save(product);
    }
}
