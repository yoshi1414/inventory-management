package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * 管理者テンプレートのログアウトモーダル構成を検証する単体テスト
 */
@DisplayName("管理者テンプレート ログアウトモーダル単体テスト")
class AdminLogoutTemplateTest {

    /**
     * 在庫管理テンプレートにログアウトモーダル構成が含まれることを検証
     * @throws IOException テンプレート読み込み時の例外
     */
    @Test
    @DisplayName("admin/inventory.html にログアウトモーダル構成が存在する")
    void inventoryTemplate_ContainsLogoutModal() throws IOException {
        assertLogoutModalElements("templates/admin/inventory.html");
    }

    /**
     * 在庫詳細テンプレートにログアウトモーダル構成が含まれることを検証
     * @throws IOException テンプレート読み込み時の例外
     */
    @Test
    @DisplayName("admin/inventory-detail.html にログアウトモーダル構成が存在する")
    void inventoryDetailTemplate_ContainsLogoutModal() throws IOException {
        assertLogoutModalElements("templates/admin/inventory-detail.html");
    }

    /**
     * 商品新規登録テンプレートにログアウトモーダル構成が含まれることを検証
     * @throws IOException テンプレート読み込み時の例外
     */
    @Test
    @DisplayName("admin/product-create.html にログアウトモーダル構成が存在する")
    void productCreateTemplate_ContainsLogoutModal() throws IOException {
        assertLogoutModalElements("templates/admin/product-create.html");
    }

    /**
     * 商品編集テンプレートにログアウトモーダル構成が含まれることを検証
     * @throws IOException テンプレート読み込み時の例外
     */
    @Test
    @DisplayName("admin/product-edit.html にログアウトモーダル構成が存在する")
    void productEditTemplate_ContainsLogoutModal() throws IOException {
        assertLogoutModalElements("templates/admin/product-edit.html");
    }

    /**
     * 商品詳細テンプレートにログアウトモーダル構成が含まれることを検証
     * @throws IOException テンプレート読み込み時の例外
     */
    @Test
    @DisplayName("admin/product-detail.html にログアウトモーダル構成が存在する")
    void productDetailTemplate_ContainsLogoutModal() throws IOException {
        assertLogoutModalElements("templates/admin/product-detail.html");
    }

    /**
     * ログアウトモーダル関連要素がテンプレートに存在することを検証
     * @param templatePath クラスパス上のテンプレートパス
     * @throws IOException テンプレート読み込み時の例外
     */
    private void assertLogoutModalElements(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        assertTrue(html.contains("data-bs-target=\"#logoutModal\""),
                "ログアウトモーダルのトリガーが存在しません: " + templatePath);
        assertTrue(html.contains("id=\"logoutModal\""),
                "ログアウトモーダル本体が存在しません: " + templatePath);
        assertTrue(html.contains("action=\"/logout\""),
                "ログアウトPOSTフォームが存在しません: " + templatePath);
        assertTrue(html.contains("th:name=\"${_csrf.parameterName}\""),
                "CSRFトークン項目が存在しません: " + templatePath);
    }
}
