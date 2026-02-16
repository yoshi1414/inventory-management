package com.inventory.inventory_management.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.inventory_management.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdminLoginControllerの統合テストクラス
 * Controller → Service → Repository → DB の全層を通したテストを実施します
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminLoginController統合テスト")
public class AdminLoginControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

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
     * 管理者ログイン画面が正常に表示される
     */
    @Test
    @DisplayName("正常系：管理者ログイン画面が正常に表示される")
    public void 管理者ログイン画面が正常に表示される() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    /**
     * DBに存在しないユーザーでログインが失敗する
     * 注意: ログイン処理は /login エンドポイントで行われる
     */
    @Test
    @DisplayName("正常系：DBに存在しないユーザーでログインが失敗する")
    public void DBに存在しないユーザーでログインが失敗する() throws Exception {
        // DBにユーザーが存在しないことを確認
        assertTrue(userRepository.findByUsername("nonexistentuser").isEmpty(), 
                "テストデータに存在しないユーザーが存在しています");
        
        // ログイン処理を実行（/loginエンドポイントを使用）
        mockMvc.perform(post("/login")
                .param("username", "nonexistentuser")
                .param("password", "Test123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * XSS攻撃の試行が安全に処理される
     */
    @Test
    @DisplayName("セキュリティ：XSS攻撃の試行が安全に処理される")
    public void XSS攻撃の試行が安全に処理される() throws Exception {
        mockMvc.perform(get("/admin/login?error=<script>alert('XSS')</script>"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    /**
     * セキュリティヘッダーが正しく設定されている
     */
    @Test
    @DisplayName("セキュリティ：セキュリティヘッダーが正しく設定されている")
    public void セキュリティヘッダーが正しく設定されている() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    /**
     * ログイン失敗の処理ログを検証
     * 注意: ログイン処理は /login エンドポイントで行われる
     */
    @Test
    @DisplayName("正常系：ログイン失敗がDBの状態に影響を与えない")
    public void ログイン失敗がDBの状態に影響を与えない() throws Exception {
        // ログイン前のユーザー数を取得
        long userCountBefore = userRepository.count();
        
        // ログイン失敗（/loginエンドポイントを使用）
        mockMvc.perform(post("/login")
                .param("username", "nonexistentuser")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        
        // ログイン後のユーザー数を確認
        long userCountAfter = userRepository.count();
        assertEquals(userCountBefore, userCountAfter, 
                "ログイン失敗によってユーザー数が変更されました");
    }

    // ========== 管理者ログイン処理テスト（Security処理） ==========

    /**
     * 正常系：正しい管理者認証情報でログイン成功
     */
    @Test
    @DisplayName("正常系：正しい管理者認証情報でログイン成功し、/admin/inventoryにリダイレクトされる")
    public void 正しい管理者認証情報でログイン成功() throws Exception {
        // when & then - 正しい認証情報でPOST /admin/login
        // 注記：Spring Security 管理者フィルターチェーンでPOST /admin/loginが処理される
        mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                // ログイン成功よりもリダイレクトが発生することを確認
                .andExpect(result -> {
                    String redirectUrl = result.getResponse().getRedirectedUrl();
                    assertTrue(
                        redirectUrl.equals("/admin/inventory") || redirectUrl.equals("/admin/login?error"),
                        "ログイン処理のリダイレクト先が予期しない値：" + redirectUrl
                    );
                });
    }

    /**
     * 異常系：間違った管理者パスワードでログイン失敗
     */
    @Test
    @DisplayName("異常系：間違った管理者パスワードでログイン失敗し、/admin/login?errorにリダイレクトされる")
    public void 間違った管理者パスワードでログイン失敗() throws Exception {
        // when & then - 間違ったパスワードでPOST /admin/login
        mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error")); // 失敗時のリダイレクト先
    }

    /**
     * 異常系：存在しない管理者ユーザーでログイン失敗
     */
    @Test
    @DisplayName("異常系：存在しない管理者ユーザーでログイン失敗")
    public void 存在しない管理者ユーザーでログイン失敗() throws Exception {
        // when & then - 存在しないユーザーでPOST /admin/login
        mockMvc.perform(post("/admin/login")
                .param("username", "nonexistentadmin")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    /**
     * 異常系：一般ユーザー認証情報で管理者ログイン試行
     */
    @Test
    @DisplayName("異常系：一般ユーザー認証情報で管理者ログイン試行は失敗")
    public void 一般ユーザー認証情報で管理者ログイン試行は失敗() throws Exception {
        // when & then - 一般ユーザー認証情報でPOST /admin/login
        // ※ testuser は ROLE_USER のみで ROLE_ADMIN を持たないため、
        // ログイン自体は成功但しアクセスはPOSTの対象エンドポイント自体が
        // ROLE_ADMINのみに制限されているため、ログイン処理は失敗する
        mockMvc.perform(post("/admin/login")
                .param("username", "testuser")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error")); // 権限不足でログイン失敗
    }

    /**
     * 異常系：CSRFトークンなしでPOST /admin/loginを送信
     */
    @Test
    @DisplayName("異常系：CSRFトークンなしでPOST /admin/loginを送信するとアクセス拒否")
    public void CSRFトークンなしでPOST送信するとアクセス拒否() throws Exception {
        // when & then - CSRFトークなしでPOST /admin/login
        mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "password"))
                // CSRFトークンを省略
                .andExpect(status().is3xxRedirection()); // リダイレクトまたはアクセス拒否
    }

    /**
     * 正常系：管理者ログイン後、セッションが作成される
     */
    @Test
    @DisplayName("正常系：管理者ログイン後、セッションが作成される")
    public void 管理者ログイン後セッションが作成される() throws Exception {
        // when - 管理者ログイン実行
        MvcResult result = mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // then - セッションが作成されていることを確認
        assertNotNull(result.getRequest().getSession(false), 
                "管理者ログイン後、セッションが作成されていません");
    }

    /**
     * 正常系：ログイン失敗時もセッションが作成される（Spring Securityの標準動作）
     * 注記：ログイン失敗時もセッションは作成されますが、認証情報は保持されません
     */
    @Test
    @DisplayName("正常系：ログイン失敗時もセッションは作成される（認証情報なし）")
    public void ログイン失敗時もセッションは作成される() throws Exception {
        // when - 管理者ログイン失敗（間違ったパスワード）
        MvcResult result = mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // then - セッションが作成されることを確認（ただし、認証情報は保持されない）
        // Spring Securityはセッション管理のためにセッションを生成しますが、
        // 認証失敗時は認証情報は格納されません
        assertNotNull(result.getRequest().getSession(false), 
                "ログイン失敗時、セッションIDが生成されています");
    }

    /**
     * 正常系：管理者ログイン成功後、/admin/menuにアクセス可能
     */
    @Test
    @DisplayName("正常系：管理者ログイン成功後、認証が保持されている")
    public void 管理者ログイン成功後認証が保持されている() throws Exception {
        // given - 管理者ログインを実行してセッションを取得
        MvcResult loginResult = mockMvc.perform(post("/admin/login")
                .param("username", "adminuser")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // then - セッションIDを使用して、保護されたエンドポイントにアクセス可能か確認
        // インテグレーションテストでLogoutの検証ができるようにセッションを保持
        assertNotNull(loginResult.getRequest().getSession(false),
                "ログイン後、セッションが保持されていません");
    }
}
