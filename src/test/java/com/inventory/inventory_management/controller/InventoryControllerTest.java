package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.repository.StockTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * InventoryControllerのテストクラス
 * Spring TestとMockMvcを使用してコントローラーの動作をテストします
 */
@SpringBootTest
@ActiveProfiles("test")
public class InventoryControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private MockMvc mockMvc;
    
    private Integer testProductId;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // テストデータ準備（ユニークなproduct_codeを生成）
        String uniqueProductCode = "TEST" + String.format("%04d", System.currentTimeMillis() % 10000);
        Product product = new Product();
        product.setProductCode(uniqueProductCode);
        product.setProductName("テスト商品");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("10000.00"));
        product.setStock(50);
        product.setStatus("active");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        testProductId = savedProduct.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();
    }

    // ========== 正常系テスト ==========

    /**
     * 認証なしでアクセスするとログイン画面にリダイレクトされる
     */
    @Test
    public void 認証なしでアクセスするとログイン画面にリダイレクトされる() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login"));
    }

    /**
     * 一般ユーザーとして在庫一覧画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 一般ユーザーとして在庫一覧画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attributeExists("products"))
               .andExpect(model().attributeExists("productPage"))
               .andExpect(model().attributeExists("lowStockCount"))
               .andExpect(model().attributeExists("outOfStockCount"));
    }

    /**
     * 管理者として在庫一覧画面が正常に表示される
     */
    @Test
    @WithUserDetails("adminuser")
    public void 管理者として在庫一覧画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attributeExists("products"));
    }

    // ========== 検索・フィルタリングテスト ==========

    /**
     * 検索キーワードでフィルタリングできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 検索キーワードでフィルタリングできる() throws Exception {
        mockMvc.perform(get("/inventory").param("search", "ノートPC"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("search", "ノートPC"));
    }

    /**
     * カテゴリでフィルタリングできる
     */
    @Test
    @WithUserDetails("testuser")
    public void カテゴリでフィルタリングできる() throws Exception {
        mockMvc.perform(get("/inventory").param("category", "Electronics"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("category", "Electronics"));
    }

    /**
     * ステータスでフィルタリングできる
     */
    @Test
    @WithUserDetails("testuser")
    public void ステータスでフィルタリングできる() throws Exception {
        mockMvc.perform(get("/inventory").param("status", "active"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("status", "active"));
    }

    /**
     * 在庫状態でフィルタリングできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫状態でフィルタリングできる() throws Exception {
        mockMvc.perform(get("/inventory").param("stock", "low"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("stock", "low"));
    }

    /**
     * 複数の条件を組み合わせてフィルタリングできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 複数の条件を組み合わせてフィルタリングできる() throws Exception {
        mockMvc.perform(get("/inventory")
                       .param("search", "テスト")
                       .param("category", "Electronics")
                       .param("status", "active")
                       .param("stock", "sufficient"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("search", "テスト"))
               .andExpect(model().attribute("category", "Electronics"))
               .andExpect(model().attribute("status", "active"))
               .andExpect(model().attribute("stock", "sufficient"));
    }

    // ========== ソート・ページングテスト ==========

    /**
     * 商品名順でソートできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 商品名順でソートできる() throws Exception {
        mockMvc.perform(get("/inventory").param("sort", "name"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("sort", "name"));
    }

    /**
     * 在庫数順でソートできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫数順でソートできる() throws Exception {
        mockMvc.perform(get("/inventory").param("sort", "stock"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("sort", "stock"));
    }

    /**
     * 更新日順でソートできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 更新日順でソートできる() throws Exception {
        mockMvc.perform(get("/inventory").param("sort", "updated"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("sort", "updated"));
    }

    /**
     * ページングが正常に機能する
     */
    @Test
    @WithUserDetails("testuser")
    public void ページングが正常に機能する() throws Exception {
        mockMvc.perform(get("/inventory").param("page", "1"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * ページ番号0で1ページ目が表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void ページ番号0で1ページ目が表示される() throws Exception {
        mockMvc.perform(get("/inventory").param("page", "0"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("currentPage", 0));
    }

    /**
     * デフォルトパラメータで正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void デフォルトパラメータで正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("sort", "name"))
               .andExpect(model().attribute("currentPage", 0));
    }

    // ========== 異常系テスト ==========

    /**
     * POSTメソッドでのアクセスは許可されない
     */
    @Test
    public void POSTメソッドでのアクセスは許可されない() throws Exception {
        mockMvc.perform(post("/inventory"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * PUTメソッドでのアクセスは許可されない
     */
    @Test
    public void PUTメソッドでのアクセスは許可されない() throws Exception {
        mockMvc.perform(put("/inventory"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * DELETEメソッドでのアクセスは許可されない
     */
    @Test
    public void DELETEメソッドでのアクセスは許可されない() throws Exception {
        mockMvc.perform(delete("/inventory"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * 存在しないサブパスへのアクセスは404エラーとなる
     */
    @Test
    @WithUserDetails("testuser")
    public void 存在しないサブパスへのアクセスは404エラーとなる() throws Exception {
        mockMvc.perform(get("/inventory/nonexistent"))
               .andExpect(status().isNotFound());
    }

    /**
     * 負のページ番号ではエラーになる
     */
    @Test
    @WithUserDetails("testuser")
    public void 負のページ番号ではエラーになる() throws Exception {
        mockMvc.perform(get("/inventory").param("page", "-1"))
               .andExpect(status().isOk())
               .andExpect(view().name("error"));
    }

    /**
     * 不正なソート値でもデフォルト値が適用される
     */
    @Test
    @WithUserDetails("testuser")
    public void 不正なソート値でもデフォルト値が適用される() throws Exception {
        mockMvc.perform(get("/inventory").param("sort", "invalid"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("sort", "invalid"));
    }

    /**
     * XSSを含むパラメータでも安全に処理される
     */
    @Test
    @WithUserDetails("testuser")
    public void XSSを含むパラメータでも安全に処理される() throws Exception {
        mockMvc.perform(get("/inventory").param("search", "<script>alert('XSS')</script>"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"));
    }

    // ========== セキュリティヘッダーテスト ==========

    /**
     * セキュリティヘッダーが正しく設定されている
     */
    @Test
    @WithUserDetails("testuser")
    public void セキュリティヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(header().exists("X-Content-Type-Options"))
               .andExpect(header().exists("X-Frame-Options"));
    }

    /**
     * Content-Typeヘッダーが正しく設定されている
     */
    @Test
    @WithUserDetails("testuser")
    public void ContentTypeヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
               .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset")));
    }

    // ========== 文字エンコーディングテスト ==========

    /**
     * 日本語パラメータが正しく処理される
     */
    @Test
    @WithUserDetails("testuser")
    public void 日本語パラメータが正しく処理される() throws Exception {
        mockMvc.perform(get("/inventory")
                       .param("search", "日本語商品名")
                       .characterEncoding("UTF-8"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attribute("search", "日本語商品名"));
    }

    /**
     * 特殊文字を含むパラメータが正しく処理される
     */
    @Test
    @WithUserDetails("testuser")
    public void 特殊文字を含むパラメータが正しく処理される() throws Exception {
        mockMvc.perform(get("/inventory")
                       .param("search", "!@#$%^&*()_+-={}[]|:;\"'<>,.?/"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"));
    }

    /**
     * 空の検索キーワードでも正常に動作する
     */
    @Test
    @WithUserDetails("testuser")
    public void 空の検索キーワードでも正常に動作する() throws Exception {
        mockMvc.perform(get("/inventory").param("search", ""))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"));
    }

    // ========== パフォーマンステスト ==========

    /**
     * 大量のパラメータでも正常に処理される
     */
    @Test
    @WithUserDetails("testuser")
    public void 大量のパラメータでも正常に処理される() throws Exception {
        mockMvc.perform(get("/inventory")
                       .param("search", "test")
                       .param("category", "Electronics")
                       .param("status", "active")
                       .param("stock", "sufficient")
                       .param("sort", "name")
                       .param("page", "0"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"));
    }

    /**
     * 検索結果が0件でもエラーにならない
     */
    @Test
    @WithUserDetails("testuser")
    public void 検索結果が0件でもエラーにならない() throws Exception {
        mockMvc.perform(get("/inventory").param("search", "存在しない商品名XYZ12345"))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory"))
               .andExpect(model().attributeExists("products"));
    }

    // ========== モデル属性テスト ==========

    /**
     * 必須のモデル属性がすべて存在する
     */
    @Test
    @WithUserDetails("testuser")
    public void 必須のモデル属性がすべて存在する() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("productPage"))
               .andExpect(model().attributeExists("products"))
               .andExpect(model().attributeExists("lowStockCount"))
               .andExpect(model().attributeExists("outOfStockCount"))
               .andExpect(model().attributeExists("totalPages"))
               .andExpect(model().attributeExists("totalElements"))
               .andExpect(model().attributeExists("currentPageNumber"))
               .andExpect(model().attributeExists("startItem"))
               .andExpect(model().attributeExists("endItem"));
    }

    /**
     * ページング情報が正しく設定されている
     */
    @Test
    @WithUserDetails("testuser")
    public void ページング情報が正しく設定されている() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("currentPage"))
               .andExpect(model().attributeExists("totalPages"))
               .andExpect(model().attributeExists("pageSize"));
    }

    /**
     * 在庫アラート情報が正しく設定されている
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫アラート情報が正しく設定されている() throws Exception {
        mockMvc.perform(get("/inventory"))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("lowStockCount"))
               .andExpect(model().attributeExists("outOfStockCount"));
    }

    // ========== URLパラメータ保持テスト ==========

    /**
     * 検索条件がモデルに保持される
     */
    @Test
    @WithUserDetails("testuser")
    public void 検索条件がモデルに保持される() throws Exception {
        mockMvc.perform(get("/inventory")
                       .param("search", "test")
                       .param("category", "Electronics")
                       .param("status", "active")
                       .param("stock", "low")
                       .param("sort", "stock")
                       .param("page", "2"))
               .andExpect(status().isOk())
               .andExpect(model().attribute("search", "test"))
               .andExpect(model().attribute("category", "Electronics"))
               .andExpect(model().attribute("status", "active"))
               .andExpect(model().attribute("stock", "low"))
               .andExpect(model().attribute("sort", "stock"))
               .andExpect(model().attribute("currentPage", 2));
    }

    // ========== 在庫更新APIテスト ==========

    /**
     * 入庫処理が正常に実行される
     */
    @Test
    @WithUserDetails("testuser")
    public void 入庫処理が正常に実行される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫処理テスト"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.message").exists())
               .andExpect(jsonPath("$.product.id").value(testProductId))
               .andExpect(jsonPath("$.product.stock").exists());
        
        // データベースで在庫変動履歴を確認
        var transactions = stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(testProductId);
        assertEquals(1, transactions.size());
        assertEquals("in", transactions.get(0).getTransactionType());
        assertEquals(10, transactions.get(0).getQuantity());
        assertEquals("入庫処理テスト", transactions.get(0).getRemarks());
    }

    /**
     * 出庫処理が正常に実行される
     */
    @Test
    @WithUserDetails("testuser")
    public void 出庫処理が正常に実行される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "出庫処理テスト"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.message").exists());
        
        // データベースで在庫変動履歴を確認
        var transactions = stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(testProductId);
        assertEquals(1, transactions.size());
        assertEquals("out", transactions.get(0).getTransactionType());
        assertEquals(5, transactions.get(0).getQuantity());
        assertEquals("出庫処理テスト", transactions.get(0).getRemarks());
    }

    /**
     * remarksがnullでも在庫更新が正常に実行される
     */
    @Test
    @WithUserDetails("testuser")
    public void remarksがnullでも在庫更新が正常に実行される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.message").exists());
        
        // データベースで在庫変動履歴を確認
        var transactions = stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(testProductId);
        assertEquals(1, transactions.size());
        assertNull(transactions.get(0).getRemarks());
    }

    /**
     * 商品IDが指定されていない場合、エラーが返される
     */
    @Test
    @WithUserDetails("testuser")
    public void 商品IDが指定されていない場合エラーが返される() throws Exception {
        String requestBody = """
            {
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫処理"
            }
            """;

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 不正な取引種別の場合、エラーが返される
     */
    @Test
    @WithUserDetails("testuser")
    public void 不正な取引種別の場合エラーが返される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "invalid",
                "quantity": 10,
                "remarks": "不正な処理"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 数量が0以下の場合、エラーが返される
     */
    @Test
    @WithUserDetails("testuser")
    public void 数量が0以下の場合エラーが返される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 0,
                "remarks": "入庫処理"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.success").value(false))
               .andExpect(jsonPath("$.message").exists());
    }

    /**
     * CSRFトークンなしでリクエストすると403エラーが返される
     */
    @Test
    @WithUserDetails("testuser")
    public void CSRFトークンなしでリクエストすると403エラーが返される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫処理"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * 認証なしで在庫更新APIにアクセスするとリダイレクトまたは403エラーが返される
     */
    @Test
    public void 認証なしで在庫更新APIにアクセスするとリダイレクトまたは403エラーが返される() throws Exception {
        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫処理"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * 在庫更新後、商品の在庫数が正確に更新される
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫更新後商品の在庫数が正確に更新される() throws Exception {
        // 初期在庫数を確認
        Product initialProduct = productRepository.findById(testProductId).orElseThrow();
        int initialStock = initialProduct.getStock();

        String requestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 15,
                "remarks": "在庫数検証テスト"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.product.stock").value(initialStock + 15));

        // データベースで在庫数を確認
        Product updatedProduct = productRepository.findById(testProductId).orElseThrow();
        assertEquals(initialStock + 15, updatedProduct.getStock());
    }

    /**
     * 複数回の在庫更新が正確に反映される
     */
    @Test
    @WithUserDetails("testuser")
    public void 複数回の在庫更新が正確に反映される() throws Exception {
        // 初期在庫数を確認
        Product initialProduct = productRepository.findById(testProductId).orElseThrow();
        int initialStock = initialProduct.getStock();

        // 1回目: 入庫10個
        String requestBody1 = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "1回目入庫"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody1))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.product.stock").value(initialStock + 10));

        // 2回目: 出庫5個
        String requestBody2 = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "2回目出庫"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(requestBody2))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.product.stock").value(initialStock + 5));

        // 最終的な在庫数を確認
        Product finalProduct = productRepository.findById(testProductId).orElseThrow();
        assertEquals(initialStock + 5, finalProduct.getStock());

        // 在庫変動履歴を確認
        var transactions = stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(testProductId);
        assertEquals(2, transactions.size());
        assertEquals("out", transactions.get(0).getTransactionType());
        assertEquals("in", transactions.get(1).getTransactionType());
    }

    // ========== 商品詳細画面テスト ==========

    /**
     * 一般ユーザーとして商品詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 一般ユーザーとして商品詳細画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"))
               .andExpect(model().attributeExists("transactions"));
    }

    /**
     * 管理者として商品詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("adminuser")
    public void 管理者として商品詳細画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"))
               .andExpect(model().attributeExists("transactions"));
    }

    /**
     * 商品詳細画面に商品情報が正しく表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 商品詳細画面に商品情報が正しく表示される() throws Exception {
        Product expectedProduct = productRepository.findById(testProductId).orElseThrow();

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attribute("product", expectedProduct));
    }

    /**
     * 商品詳細画面に入出庫履歴が表示される（最新3件）
     */
    @Test
    @WithUserDetails("testuser")
    public void 商品詳細画面に入出庫履歴が表示される() throws Exception {
        // 在庫変動履歴を作成（4件）
        for (int i = 0; i < 4; i++) {
            String requestBody = String.format("""
                {
                    "productId": %d,
                    "transactionType": "in",
                    "quantity": 5,
                    "remarks": "テスト履歴%d"
                }
                """, testProductId, i + 1);

            mockMvc.perform(post("/api/inventory/update-stock")
                           .with(csrf())
                           .contentType(MediaType.APPLICATION_JSON)
                           .content(requestBody))
                   .andExpect(status().isOk());
        }

        // 商品詳細画面を表示（最新3件のみ表示されるはず）
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("transactions"));

        // 履歴件数が3件であることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        @SuppressWarnings("unchecked")
        var transactions = (java.util.List<?>) result.getModelAndView().getModel().get("transactions");
        assertEquals(3, transactions.size(), "最新3件のみ表示されるべき");
    }

    /**
     * 存在しない商品IDでアクセスするとエラー画面が表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 存在しない商品IDでアクセスするとエラー画面が表示される() throws Exception {
        Integer nonExistentId = 999999;

        mockMvc.perform(get("/inventory/products/{id}", nonExistentId))
               .andExpect(status().isOk())
               .andExpect(view().name("error"))
               .andExpect(model().attributeExists("errorMessage"));
    }

    /**
     * 0以下の商品IDでアクセスするとエラー画面が表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void ゼロ以下の商品IDでアクセスするとエラー画面が表示される() throws Exception {
        mockMvc.perform(get("/inventory/products/{id}", 0))
               .andExpect(status().isOk())
               .andExpect(view().name("error"))
               .andExpect(model().attributeExists("errorMessage"));

        mockMvc.perform(get("/inventory/products/{id}", -1))
               .andExpect(status().isOk())
               .andExpect(view().name("error"))
               .andExpect(model().attributeExists("errorMessage"));
    }

    /**
     * 削除済み商品にアクセスするとエラー画面が表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 削除済み商品にアクセスするとエラー画面が表示される() throws Exception {
        // 商品を論理削除
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("error"))
               .andExpect(model().attributeExists("errorMessage"));
    }

    /**
     * 認証なしで商品詳細画面にアクセスするとログイン画面にリダイレクトされる
     */
    @Test
    public void 認証なしで商品詳細画面にアクセスするとログイン画面にリダイレクトされる() throws Exception {
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login"));
    }

    /**
     * 入出庫履歴がない商品でも商品詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 入出庫履歴がない商品でも商品詳細画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"))
               .andExpect(model().attributeExists("transactions"));

        // 履歴が空のリストであることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        @SuppressWarnings("unchecked")
        var transactions = (java.util.List<?>) result.getModelAndView().getModel().get("transactions");
        assertTrue(transactions.isEmpty(), "履歴がない場合は空のリストが返されるべき");
    }

    /**
     * 在庫切れ商品の詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫切れ商品の詳細画面が正常に表示される() throws Exception {
        // 在庫を0にする
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setStock(0);
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"));

        // 在庫が0であることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        Product returnedProduct = (Product) result.getModelAndView().getModel().get("product");
        assertEquals(0, returnedProduct.getStock(), "在庫数が0であるべき");
    }

    /**
     * 在庫不足商品（1-20個）の詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫不足商品の詳細画面が正常に表示される() throws Exception {
        // 在庫を10にする
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setStock(10);
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"));

        // 在庫が10であることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        Product returnedProduct = (Product) result.getModelAndView().getModel().get("product");
        assertEquals(10, returnedProduct.getStock(), "在庫数が10であるべき");
        assertTrue(returnedProduct.getStock() > 0 && returnedProduct.getStock() <= 20, "在庫不足状態であるべき");
    }

    /**
     * 在庫十分商品（21個以上）の詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 在庫十分商品の詳細画面が正常に表示される() throws Exception {
        // 在庫を100にする
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setStock(100);
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"));

        // 在庫が100であることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        Product returnedProduct = (Product) result.getModelAndView().getModel().get("product");
        assertEquals(100, returnedProduct.getStock(), "在庫数が100であるべき");
        assertTrue(returnedProduct.getStock() > 20, "在庫十分状態であるべき");
    }

    /**
     * 入庫と出庫が混在する履歴が正しく表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 入庫と出庫が混在する履歴が正しく表示される() throws Exception {
        // 在庫を十分な量に設定
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setStock(50);
        productRepository.save(product);

        // 入庫を記録
        String inRequestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "in",
                "quantity": 10,
                "remarks": "入庫テスト"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(inRequestBody))
               .andExpect(status().isOk());

        // 出庫を記録
        String outRequestBody = String.format("""
            {
                "productId": %d,
                "transactionType": "out",
                "quantity": 5,
                "remarks": "出庫テスト"
            }
            """, testProductId);

        mockMvc.perform(post("/api/inventory/update-stock")
                       .with(csrf())
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(outRequestBody))
               .andExpect(status().isOk());

        // 商品詳細画面を表示
        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("transactions"));

        // 履歴に入庫と出庫の両方が含まれることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        @SuppressWarnings("unchecked")
        var transactions = (java.util.List<StockTransaction>) result.getModelAndView().getModel().get("transactions");
        assertFalse(transactions.isEmpty(), "履歴が存在するべき");
        
        // 入庫と出庫の両方の種別が存在することを確認
        boolean hasIn = transactions.stream().anyMatch(t -> "in".equals(t.getTransactionType()));
        boolean hasOut = transactions.stream().anyMatch(t -> "out".equals(t.getTransactionType()));
        assertTrue(hasIn, "入庫履歴が含まれているべき");
        assertTrue(hasOut, "出庫履歴が含まれているべき");
    }

    /**
     * 販売停止商品（inactive）の詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void 販売停止商品の詳細画面が正常に表示される() throws Exception {
        // ステータスをinactiveにする
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setStatus("inactive");
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"));

        // ステータスがinactiveであることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        Product returnedProduct = (Product) result.getModelAndView().getModel().get("product");
        assertEquals("inactive", returnedProduct.getStatus(), "ステータスがinactiveであるべき");
    }

    /**
     * オプション項目がnullの商品でも詳細画面が正常に表示される
     */
    @Test
    @WithUserDetails("testuser")
    public void オプション項目がnullの商品でも詳細画面が正常に表示される() throws Exception {
        // オプション項目をnullにする
        Product product = productRepository.findById(testProductId).orElseThrow();
        product.setWarrantyMonths(null);
        product.setDimensions(null);
        product.setVariations(null);
        product.setManufacturingDate(null);
        product.setExpirationDate(null);
        product.setTags(null);
        productRepository.save(product);

        mockMvc.perform(get("/inventory/products/{id}", testProductId))
               .andExpect(status().isOk())
               .andExpect(view().name("inventory-detail"))
               .andExpect(model().attributeExists("product"));

        // オプション項目がnullであることを確認
        var result = mockMvc.perform(get("/inventory/products/{id}", testProductId))
                           .andReturn();
        Product returnedProduct = (Product) result.getModelAndView().getModel().get("product");
        assertNull(returnedProduct.getWarrantyMonths(), "保証期間がnullであるべき");
        assertNull(returnedProduct.getDimensions(), "寸法がnullであるべき");
        assertNull(returnedProduct.getVariations(), "バリエーションがnullであるべき");
        assertNull(returnedProduct.getManufacturingDate(), "製造日がnullであるべき");
        assertNull(returnedProduct.getExpirationDate(), "有効期限がnullであるべき");
        assertNull(returnedProduct.getTags(), "タグがnullであるべき");
    }
}
