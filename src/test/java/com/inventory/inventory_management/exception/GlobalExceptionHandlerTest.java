package com.inventory.inventory_management.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandlerのテストクラス
 * <p>
 * グローバル例外ハンドラーが各種HTTPエラーを正しく処理し、
 * 適切なエラーページへ誘導することを検証します。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GlobalExceptionHandlerTest.TestControllerConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    /**
     * 各テストの前に実行される初期化処理
     * <p>
     * MockMvcを手動でセットアップします。
     * </p>
     */
    @BeforeEach
    void setup() {
        // logger.info("MockMvcのセットアップを開始");
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        // logger.info("MockMvcのセットアップが完了");
    }

    /**
     * 404エラー（ページが見つからない）のテスト
     * <p>
     * 存在しないURLへアクセスした際、404エラーページが表示されることを検証します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("404エラー: 存在しないURLへのアクセス時にエラーページが表示される")
    @WithMockUser
    void testHandleNotFound() throws Exception {
        // logger.info("テスト実行: 404エラーハンドリング");

        mockMvc.perform(get("/nonexistent-url"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Not Found"))
                .andExpect(model().attributeExists("message", "path", "timestamp"));

        // logger.info("テスト完了: 404エラーが正しく処理されました");
    }

    /**
     * 403エラー（アクセス拒否）のテスト
     * <p>
     * 権限のないリソースへアクセスした際、403エラーページが表示されることを検証します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("403エラー: アクセス権限がない場合にエラーページが表示される")
    @WithMockUser
    void testHandleAccessDenied() throws Exception {
        // logger.info("テスト実行: 403エラーハンドリング");

        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 403))
                .andExpect(model().attribute("error", "Forbidden"))
                .andExpect(model().attribute("message", "このページにアクセスする権限がありません。"))
                .andExpect(model().attributeExists("path", "timestamp"));

        // logger.info("テスト完了: 403エラーが正しく処理されました");
    }

    /**
     * 400エラー（不正なリクエスト）のテスト
     * <p>
     * 不正な引数が渡された際、400エラーページが表示されることを検証します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("400エラー: 不正な引数が渡された場合にエラーページが表示される")
    @WithMockUser
    void testHandleIllegalArgument() throws Exception {
        // logger.info("テスト実行: 400エラーハンドリング");

        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 400))
                .andExpect(model().attribute("error", "Bad Request"))
                .andExpect(model().attribute("message", "不正なパラメータです"))
                .andExpect(model().attributeExists("path", "timestamp"));

        // logger.info("テスト完了: 400エラーが正しく処理されました");
    }

    /**
     * 500エラー（内部サーバーエラー）のテスト
     * <p>
     * 予期しない例外が発生した際、500エラーページが表示されることを検証します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("500エラー: 予期しない例外が発生した場合にエラーページが表示される")
    @WithMockUser
    void testHandleAllExceptions() throws Exception {
        // logger.info("テスト実行: 500エラーハンドリング");

        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("error", "Internal Server Error"))
                .andExpect(model().attribute("message", "システムエラーが発生しました。しばらく時間をおいてから再度お試しください。"))
                .andExpect(model().attributeExists("path", "timestamp"));

        // logger.info("テスト完了: 500エラーが正しく処理されました");
    }

    /**
     * エラーページに必要な属性が全て含まれているか検証するテスト
     * <p>
     * エラーページに表示するために必要な情報が適切にモデルに格納されていることを確認します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("エラーページに必要な属性が全て含まれている")
    @WithMockUser
    void testErrorPageAttributes() throws Exception {
        // logger.info("テスト実行: エラーページ属性の検証");

        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("status"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attributeExists("path"))
                .andExpect(model().attributeExists("timestamp"));

        // logger.info("テスト完了: 全ての必要な属性が存在します");
    }

    /**
     * 管理者ユーザー時にisAdminがtrueとなることを検証するテスト
     * <p>
     * エラー画面の「ログイン画面へ」ボタンが管理者ログイン画面へ遷移するための判定値を確認します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("管理者ユーザーでエラー時にisAdminがtrueになる")
    @WithMockUser(roles = "ADMIN")
    void testIsAdminAttributeForAdminUser() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("isAdmin", true));
    }

    /**
     * 一般ユーザー時にisAdminがfalseとなることを検証するテスト
     * <p>
     * エラー画面の「ログイン画面へ」ボタンが一般ユーザーログイン画面へ遷移するための判定値を確認します。
     * </p>
     *
     * @throws Exception テスト実行時の例外
     */
    @Test
    @DisplayName("一般ユーザーでエラー時にisAdminがfalseになる")
    @WithMockUser(roles = "USER")
    void testIsAdminAttributeForUser() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("isAdmin", false));
    }

    /**
     * テスト用のダミーコントローラー
     * <p>
     * 各種例外をスローして、GlobalExceptionHandlerの動作を検証するために使用します。
     * </p>
     */
    @TestConfiguration
    static class TestControllerConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        /**
         * AccessDeniedExceptionをスローするエンドポイント
         * <p>
         * 403エラーのテストに使用します。
         * </p>
         *
         * @throws AccessDeniedException アクセス拒否例外
         */
        @GetMapping("/test/access-denied")
        public void throwAccessDenied() {
            // logger.debug("テストエンドポイント: AccessDeniedExceptionをスロー");
            throw new AccessDeniedException("アクセスが拒否されました");
        }

        /**
         * IllegalArgumentExceptionをスローするエンドポイント
         * <p>
         * 400エラーのテストに使用します。
         * </p>
         *
         * @throws IllegalArgumentException 不正な引数例外
         */
        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgument() {
            // logger.debug("テストエンドポイント: IllegalArgumentExceptionをスロー");
            throw new IllegalArgumentException("不正なパラメータです");
        }

        /**
         * RuntimeExceptionをスローするエンドポイント
         * <p>
         * 500エラーのテストに使用します。
         * </p>
         *
         * @throws RuntimeException ランタイム例外
         */
        @GetMapping("/test/server-error")
        public void throwServerError() {
            // logger.debug("テストエンドポイント: RuntimeExceptionをスロー");
            throw new RuntimeException("予期しないエラーが発生しました");
        }
    }
}
