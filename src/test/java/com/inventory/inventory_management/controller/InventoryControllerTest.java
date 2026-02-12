package com.inventory.inventory_management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * InventoryControllerのテストクラス
 * Spring TestとMockMvcを使用してコントローラーの動作をテストします
 */
@SpringBootTest
@ActiveProfiles("test")
public class InventoryControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
}
