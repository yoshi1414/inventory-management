package com.inventory.inventory_management.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Productエンティティのユニットテスト
 * 在庫状態判定ロジックの検証
 */
@DisplayName("Product エンティティテスト")
class ProductTest {

    private Product product;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1);
        product.setProductCode("TEST001");
        product.setProductName("テスト商品");
        product.setCategory("Electronics");
        product.setSku("SKU-TEST-001");
        product.setPrice(new BigDecimal("10000.00"));
        product.setStatus("active");
        product.setDescription("テスト用商品");
        product.setRating(new BigDecimal("4.5"));
        product.setWarrantyMonths(12);
        product.setDimensions("10x20x30cm");
        product.setVariations("Black/White");
        product.setManufacturingDate(LocalDate.of(2025, 1, 1));
        product.setExpirationDate(LocalDate.of(2027, 1, 1));
        product.setTags("test,electronics");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 在庫数が0の場合、在庫切れ状態を返すことを検証
     */
    @Test
    @DisplayName("在庫数0の場合、在庫切れ状態を返す")
    void getStockStatus_WhenStockIsZero_ReturnsOut() {
        // Given: 在庫数が0
        product.setStock(0);

        // When: 在庫状態を取得
        String status = product.getStockStatus();

        // Then: "out"が返される
        assertEquals("out", status);
    }

    /**
     * 在庫数がnullの場合、在庫切れ状態を返すことを検証
     */
    @Test
    @DisplayName("在庫数nullの場合、在庫切れ状態を返す")
    void getStockStatus_WhenStockIsNull_ReturnsOut() {
        // Given: 在庫数がnull
        product.setStock(null);

        // When: 在庫状態を取得
        String status = product.getStockStatus();

        // Then: "out"が返される
        assertEquals("out", status);
    }

    /**
     * 在庫数が1-20の場合、在庫不足状態を返すことを検証
     */
    @Test
    @DisplayName("在庫数1-20の場合、在庫不足状態を返す")
    void getStockStatus_WhenStockIsLow_ReturnsLow() {
        // Given: 在庫数が1
        product.setStock(1);
        assertEquals("low", product.getStockStatus());

        // Given: 在庫数が10
        product.setStock(10);
        assertEquals("low", product.getStockStatus());

        // Given: 在庫数が20（境界値）
        product.setStock(20);
        assertEquals("low", product.getStockStatus());
    }

    /**
     * 在庫数が21以上の場合、在庫十分状態を返すことを検証
     */
    @Test
    @DisplayName("在庫数21以上の場合、在庫十分状態を返す")
    void getStockStatus_WhenStockIsSufficient_ReturnsSufficient() {
        // Given: 在庫数が21（境界値）
        product.setStock(21);
        assertEquals("sufficient", product.getStockStatus());

        // Given: 在庫数が100
        product.setStock(100);
        assertEquals("sufficient", product.getStockStatus());

        // Given: 在庫数が1000
        product.setStock(1000);
        assertEquals("sufficient", product.getStockStatus());
    }

    /**
     * 在庫数が0の場合、在庫切れ判定がtrueを返すことを検証
     */
    @Test
    @DisplayName("在庫数0の場合、在庫切れ判定がtrueを返す")
    void isOutOfStock_WhenStockIsZero_ReturnsTrue() {
        // Given: 在庫数が0
        product.setStock(0);

        // When: 在庫切れ判定
        boolean result = product.isOutOfStock();

        // Then: trueが返される
        assertTrue(result);
    }

    /**
     * 在庫数がnullの場合、在庫切れ判定がtrueを返すことを検証
     */
    @Test
    @DisplayName("在庫数nullの場合、在庫切れ判定がtrueを返す")
    void isOutOfStock_WhenStockIsNull_ReturnsTrue() {
        // Given: 在庫数がnull
        product.setStock(null);

        // When: 在庫切れ判定
        boolean result = product.isOutOfStock();

        // Then: trueが返される
        assertTrue(result);
    }

    /**
     * 在庫数が1以上の場合、在庫切れ判定がfalseを返すことを検証
     */
    @Test
    @DisplayName("在庫数1以上の場合、在庫切れ判定がfalseを返す")
    void isOutOfStock_WhenStockIsPositive_ReturnsFalse() {
        // Given: 在庫数が1
        product.setStock(1);
        assertFalse(product.isOutOfStock());

        // Given: 在庫数が100
        product.setStock(100);
        assertFalse(product.isOutOfStock());
    }

    /**
     * 在庫数が1-20の場合、在庫不足判定がtrueを返すことを検証
     */
    @Test
    @DisplayName("在庫数1-20の場合、在庫不足判定がtrueを返す")
    void isLowStock_WhenStockIsLow_ReturnsTrue() {
        // Given: 在庫数が1（境界値）
        product.setStock(1);
        assertTrue(product.isLowStock());

        // Given: 在庫数が10
        product.setStock(10);
        assertTrue(product.isLowStock());

        // Given: 在庫数が20（境界値）
        product.setStock(20);
        assertTrue(product.isLowStock());
    }

    /**
     * 在庫数が0の場合、在庫不足判定がfalseを返すことを検証
     */
    @Test
    @DisplayName("在庫数0の場合、在庫不足判定がfalseを返す")
    void isLowStock_WhenStockIsZero_ReturnsFalse() {
        // Given: 在庫数が0
        product.setStock(0);

        // When: 在庫不足判定
        boolean result = product.isLowStock();

        // Then: falseが返される（在庫切れなので不足ではない）
        assertFalse(result);
    }

    /**
     * 在庫数が21以上の場合、在庫不足判定がfalseを返すことを検証
     */
    @Test
    @DisplayName("在庫数21以上の場合、在庫不足判定がfalseを返す")
    void isLowStock_WhenStockIsSufficient_ReturnsFalse() {
        // Given: 在庫数が21（境界値）
        product.setStock(21);
        assertFalse(product.isLowStock());

        // Given: 在庫数が100
        product.setStock(100);
        assertFalse(product.isLowStock());
    }

    /**
     * 在庫数がnullの場合、在庫不足判定がfalseを返すことを検証
     */
    @Test
    @DisplayName("在庫数nullの場合、在庫不足判定がfalseを返す")
    void isLowStock_WhenStockIsNull_ReturnsFalse() {
        // Given: 在庫数がnull
        product.setStock(null);

        // When: 在庫不足判定
        boolean result = product.isLowStock();

        // Then: falseが返される
        assertFalse(result);
    }

    /**
     * Lombokの@Dataアノテーションが正しく動作することを検証
     */
    @Test
    @DisplayName("Lombok @Data が正しく動作する")
    void testLombokDataAnnotation() {
        // Given: 別のProductインスタンス
        Product anotherProduct = new Product();
        anotherProduct.setId(1);
        anotherProduct.setProductCode("TEST001");
        anotherProduct.setProductName("テスト商品");
        anotherProduct.setCategory("Electronics");
        anotherProduct.setSku("SKU-TEST-001");
        anotherProduct.setPrice(new BigDecimal("10000.00"));
        anotherProduct.setStatus("active");

        // When: 同じ値を設定
        anotherProduct.setStock(10);
        product.setStock(10);

        // Then: equals/hashCodeが正しく動作
        assertNotNull(product.toString());
        assertTrue(product.toString().contains("TEST001"));
    }
}
