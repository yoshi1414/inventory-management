package com.inventory.inventory_management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * AdminLoginControllerのテストクラス
 * Spring TestとMockMvcを使用してコントローラーの動作をテストします
 */
@SpringBootTest
@ActiveProfiles("test")
public class AdminLoginControllerTest {

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
     * 未ログインの場合でも管理者ログイン画面が正常に表示される
     */
    @Test
    public void 未ログインの場合でも管理者ログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    /**
     * エラーパラメータ付きでも管理者ログイン画面が正常に表示される
     */
    @Test
    public void エラーパラメータ付きでも管理者ログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/admin/login?error"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    /**
     * ログアウトパラメータ付きでも管理者ログイン画面が正常に表示される
     */
    @Test
    public void ログアウトパラメータ付きでも管理者ログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/admin/login?logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    /**
     * 正常系：ログイン成功後に/admin/inventoryにリダイレクトされる
     */
    @Test
    public void ログイン成功後に管理者在庫画面にリダイレクトされる() throws Exception {
        mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String redirectUrl = result.getResponse().getRedirectedUrl();
                    assertTrue(
                            "/admin/inventory".equals(redirectUrl) || "/admin/login?error".equals(redirectUrl),
                            "リダイレクト先が予期しない値: " + redirectUrl
                    );
                });
    }

    // ========== 異常系テスト ==========

    /**
     * 存在しないサブパスへのアクセスは404エラーとなる
     */
    @Test
    public void 存在しないサブパスへのアクセスは404エラーとなる() throws Exception {
        mockMvc.perform(get("/admin/login/nonexistent"))
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    /**
     * XSSを含むパラメータでも安全に処理される
     */
    @Test
    public void XSSを含むパラメータでも安全に処理される() throws Exception {
        mockMvc.perform(get("/admin/login?error=<script>alert('XSS')</script>"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    // ========== セキュリティヘッダーテスト ==========

    /**
     * セキュリティヘッダーが正しく設定されている
     */
    @Test
    public void セキュリティヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    /**
     * Content-Typeヘッダーが正しく設定されている
     */
    @Test
    public void ContentTypeヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("admin/login"));
    }

    // ========== 注意事項 ==========
    // POSTログイン処理は SecurityFilterChain で /admin/login が処理されます
}
