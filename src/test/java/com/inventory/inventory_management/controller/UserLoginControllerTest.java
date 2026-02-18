package com.inventory.inventory_management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * UserLoginControllerのテストクラス
 * Spring TestとMockMvcを使用してコントローラーの動作をテストします
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserLoginControllerTest {

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
     * 未ログインの場合でもログイン画面が正常に表示される
     */
    @Test
    public void 未ログインの場合でもログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    /**
     * 一般ユーザーとしてログイン済みの場合でもログイン画面にアクセスできる
     */
    @Test
    @WithUserDetails("testuser")
    public void 一般ユーザーとしてログイン済みの場合でもログイン画面にアクセスできる() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    /**
     * 管理者としてログイン済みの場合でもログイン画面にアクセスできる
     */
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    public void 管理者としてログイン済みの場合でもログイン画面にアクセスできる() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    /**
     * エラーパラメータ付きでもログイン画面が正常に表示される
     */
    @Test
    public void エラーパラメータ付きでもログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    /**
     * ログアウトパラメータ付きでもログイン画面が正常に表示される
     */
    @Test
    public void ログアウトパラメータ付きでもログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    // ========== 異常系テスト ==========

    /**
     * PUTメソッドでのアクセスは許可されない
     */
    @Test
    public void PUTメソッドでのアクセスは許可されない() throws Exception {
        mockMvc.perform(put("/login"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * DELETEメソッドでのアクセスは許可されない
     */
    @Test
    public void DELETEメソッドでのアクセスは許可されない() throws Exception {
        mockMvc.perform(delete("/login"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * 存在しないサブパスへのアクセスは404エラーとなる
     */
    @Test
    public void 存在しないサブパスへのアクセスは404エラーとなる() throws Exception {
        mockMvc.perform(get("/login/nonexistent"))
               .andExpect(status().isNotFound());
    }

    /**
     * XSSを含むパラメータでも安全に処理される
     */
    @Test
    public void XSSを含むパラメータでも安全に処理される() throws Exception {
        mockMvc.perform(get("/login").param("error", "<script>alert('XSS')</script>"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    // ========== セキュリティヘッダーテスト ==========

    /**
     * セキュリティヘッダーが正しく設定されている
     */
    @Test
    public void セキュリティヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(header().exists("X-Content-Type-Options"))
               .andExpect(header().exists("X-Frame-Options"))
               .andExpect(header().doesNotExist("X-XSS-Protection"));
    }

    /**
     * Content-Typeヘッダーが正しく設定されている
     */
    @Test
    public void ContentTypeヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
               .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset")));
    }

    // ========== POSTログイン処理テスト ==========

    /**
     * 正しい認証情報でのPOSTログインが成功する
     */
    @Test
    public void 正しい認証情報でのPOSTログインが成功する() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "testuser")
                       .param("password", "password")
                       .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/inventory"));
    }

    /**
     * 誤った認証情報でのPOSTログインが失敗する
     */
    @Test
    public void 誤った認証情報でのPOSTログインが失敗する() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "testuser")
                       .param("password", "wrongpassword")
                       .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * 存在しないユーザーでのPOSTログインが失敗する
     */
    @Test
    public void 存在しないユーザーでのPOSTログインが失敗する() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "nonexistentuser")
                       .param("password", "password")
                       .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * 空のユーザー名でのPOSTログインが失敗する
     */
    @Test
    public void 空のユーザー名でのPOSTログインが失敗する() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "")
                       .param("password", "password")
                       .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * 空のパスワードでのPOSTログインが失敗する
     */
    @Test
    public void 空のパスワードでのPOSTログインが失敗する() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "testuser")
                       .param("password", "")
                       .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    // ========== CSRFテスト ==========

    /**
     * CSRFトークンなしのPOSTリクエストは拒否される
     */
    @Test
    public void CSRFトークンなしのPOSTリクエストは拒否される() throws Exception {
        mockMvc.perform(post("/login")
                       .param("username", "testuser")
                       .param("password", "password"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * PUTリクエストでCSRFトークンを付与しても許可されない
     */
    @Test
    public void PUTリクエストでCSRFトークンを付与しても許可されない() throws Exception {
        mockMvc.perform(put("/login").with(csrf()))
               .andExpect(status().isInternalServerError());
    }

    /**
     * DELETEリクエストでCSRFトークンを付与しても許可されない
     */
    @Test
    public void DELETEリクエストでCSRFトークンを付与しても許可されない() throws Exception {
        mockMvc.perform(delete("/login").with(csrf()))
               .andExpect(status().isInternalServerError());
    }

    // ========== 文字エンコーディングテスト ==========

    /**
     * 日本語パラメータが正しく処理される
     */
    @Test
    public void 日本語パラメータが正しく処理される() throws Exception {
        mockMvc.perform(get("/login")
                       .param("error", "ログインに失敗しました")
                       .characterEncoding("UTF-8"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    /**
     * 特殊文字を含むパラメータが正しく処理される
     */
    @Test
    public void 特殊文字を含むパラメータが正しく処理される() throws Exception {
        mockMvc.perform(get("/login")
                       .param("error", "!@#$%^&*()_+-={}[]|:;\"'<>,.?/"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }
}
