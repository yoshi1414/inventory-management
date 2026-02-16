package com.inventory.inventory_management.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CustomAuthenticationEntryPointの統合テストクラス
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CustomAuthenticationEntryPoint統合テスト")
class CustomAuthenticationEntryPointTest {

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

    /**
     * 未認証で実在する保護URLへアクセスした場合、ログイン画面へリダイレクトされることを検証します。
     *
     * @throws Exception テスト実行中の例外
     */
    @Test
    @DisplayName("未認証で実在する保護URLにアクセスするとログイン画面へリダイレクトされる")
    void 未認証で実在する保護urlにアクセスするとログイン画面へリダイレクトされる() throws Exception {
        mockMvc.perform(get("/inventory"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    /**
     * 未認証で存在しないURLへアクセスした場合、404を返すことを検証します。
     *
     * @throws Exception テスト実行中の例外
     */
    @Test
    @DisplayName("未認証で存在しないURLにアクセスすると404を返す")
    void 未認証で存在しないurlにアクセスすると404を返す() throws Exception {
        mockMvc.perform(get("/inventory/nonexistent-path"))
            .andExpect(status().isNotFound());
    }
}
