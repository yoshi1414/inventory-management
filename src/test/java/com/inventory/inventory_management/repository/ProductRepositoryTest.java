package com.inventory.inventory_management.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Product;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductRepositoryの統合テスト
 * JPA機能とカスタムクエリの検証
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ProductRepository 統合テスト")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    /**
     * 各テストメソッド実行前の初期化処理
     * テスト用データを作成
     */
    @BeforeEach
    void setUp() {
        // テストデータをクリア（外部キー制約を考慮して、先にstock_transactionsを削除）
        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();
        productRepository.flush();

        // テスト商品1: 在庫十分（Electronics）
        product1 = new Product();
        product1.setProductCode("TEST001");
        product1.setProductName("テストノートPC");
        product1.setCategory("Electronics");
        product1.setSku("SKU-TEST-001");
        product1.setPrice(new BigDecimal("120000.00"));
        product1.setStock(50);
        product1.setStatus("active");
        product1.setDescription("高性能ノートPC");
        product1.setRating(new BigDecimal("4.5"));
        product1.setWarrantyMonths(24);
        product1.setDimensions("30x20x2cm");
        product1.setVariations("Silver/Black");
        product1.setManufacturingDate(LocalDate.of(2025, 1, 1));
        product1.setTags("laptop,computer");
        product1.setCreatedAt(LocalDateTime.now());
        product1.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product1);

        // テスト商品2: 在庫不足（Clothing）
        product2 = new Product();
        product2.setProductCode("TEST002");
        product2.setProductName("テストTシャツ");
        product2.setCategory("Clothing");
        product2.setSku("SKU-TEST-002");
        product2.setPrice(new BigDecimal("2500.00"));
        product2.setStock(10);
        product2.setStatus("active");
        product2.setDescription("コットン100%");
        product2.setRating(new BigDecimal("4.0"));
        product2.setWarrantyMonths(0);
        product2.setDimensions("Free");
        product2.setVariations("Red/Blue/Green,S/M/L");
        product2.setManufacturingDate(LocalDate.of(2025, 2, 1));
        product2.setTags("tshirt,clothing");
        product2.setCreatedAt(LocalDateTime.now());
        product2.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product2);

        // テスト商品3: 在庫切れ（Home Appliances）
        product3 = new Product();
        product3.setProductCode("TEST003");
        product3.setProductName("テスト掃除機");
        product3.setCategory("Home Appliances");
        product3.setSku("SKU-TEST-003");
        product3.setPrice(new BigDecimal("35000.00"));
        product3.setStock(0);
        product3.setStatus("inactive");
        product3.setDescription("強力吸引掃除機");
        product3.setRating(new BigDecimal("3.8"));
        product3.setWarrantyMonths(12);
        product3.setDimensions("25x30x100cm");
        product3.setVariations("White");
        product3.setManufacturingDate(LocalDate.of(2024, 12, 1));
        product3.setTags("cleaner,appliance");
        product3.setCreatedAt(LocalDateTime.now());
        product3.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product3);
    }

    /**
     * 各テストメソッド実行後のクリーンアップ処理
     * テストデータを削除してデータベ ースをクリアする
     */
    @AfterEach
    void tearDown() {
        // 外部キー制約を考慮して、先にstock_transactionsを削除
        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();
    }

    /**
     * 商品コードで商品を検索できることを検証
     */
    @Test
    @DisplayName("商品コードで商品を検索できる")
    void findByProductCode_Success() {
        // When: 商品コードで検索
        Product found = productRepository.findByProductCode("TEST001");

        // Then: 正しい商品が取得できる
        assertNotNull(found);
        assertEquals("TEST001", found.getProductCode());
        assertEquals("テストノートPC", found.getProductName());
        assertEquals(50, found.getStock());
    }

    /**
     * 存在しない商品コードで検索した場合、nullが返されることを検証
     */
    @Test
    @DisplayName("存在しない商品コードで検索するとnullが返される")
    void findByProductCode_NotFound() {
        // When: 存在しない商品コードで検索
        Product found = productRepository.findByProductCode("NOTEXIST");

        // Then: nullが返される
        assertNull(found);
    }

    /**
     * キーワードなしで全商品を検索できることを検証
     */
    @Test
    @DisplayName("キーワードなしで全商品を検索できる")
    void findBySearchConditions_WithoutKeyword() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: キーワードなしで検索
        Page<Product> result = productRepository.findBySearchConditions(null, null, null, pageable);

        // Then: 全商品が取得できる
        assertEquals(3, result.getTotalElements());
    }

    /**
     * 商品名で部分一致検索できることを検証
     */
    @Test
    @DisplayName("商品名で部分一致検索できる")
    void findBySearchConditions_WithKeyword() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: "ノート"で検索
        Page<Product> result = productRepository.findBySearchConditions("ノート", null, null, pageable);

        // Then: ノートPCのみ取得できる
        assertEquals(1, result.getTotalElements());
        assertEquals("テストノートPC", result.getContent().get(0).getProductName());
    }

    /**
     * カテゴリでフィルタリングできることを検証
     */
    @Test
    @DisplayName("カテゴリでフィルタリングできる")
    void findBySearchConditions_WithCategory() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: Electronicsカテゴリで検索
        Page<Product> result = productRepository.findBySearchConditions(null, "Electronics", null, pageable);

        // Then: Electronics商品のみ取得できる
        assertEquals(1, result.getTotalElements());
        assertEquals("Electronics", result.getContent().get(0).getCategory());
    }

    /**
     * ステータスでフィルタリングできることを検証
     */
    @Test
    @DisplayName("ステータスでフィルタリングできる")
    void findBySearchConditions_WithStatus() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: activeステータスで検索
        Page<Product> result = productRepository.findBySearchConditions(null, null, "active", pageable);

        // Then: active商品のみ取得できる
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(p -> "active".equals(p.getStatus())));
    }

    /**
     * 複合条件で検索できることを検証
     */
    @Test
    @DisplayName("複合条件で検索できる")
    void findBySearchConditions_WithMultipleConditions() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: キーワード、カテゴリ、ステータスを指定して検索
        Page<Product> result = productRepository.findBySearchConditions(
                "テスト", "Electronics", "active", pageable);

        // Then: 条件に合致する商品のみ取得できる
        assertEquals(1, result.getTotalElements());
        Product found = result.getContent().get(0);
        assertEquals("TEST001", found.getProductCode());
        assertEquals("Electronics", found.getCategory());
        assertEquals("active", found.getStatus());
    }

    /**
     * 在庫不足の商品数をカウントできることを検証
     */
    @Test
    @DisplayName("在庫不足の商品数をカウントできる")
    void countLowStock_Success() {
        // When: 在庫不足商品数を取得
        long count = productRepository.countLowStock();

        // Then: 在庫が1-20個の商品がカウントされる
        assertEquals(1, count);
    }

    /**
     * 在庫切れの商品数をカウントできることを検証
     */
    @Test
    @DisplayName("在庫切れの商品数をカウントできる")
    void countOutOfStock_Success() {
        // When: 在庫切れ商品数を取得
        long count = productRepository.countOutOfStock();

        // Then: 在庫が0個の商品がカウントされる
        assertEquals(1, count);
    }

    /**
     * 在庫数範囲でフィルタリングできることを検証
     */
    @Test
    @DisplayName("在庫数範囲でフィルタリングできる")
    void findBySearchConditionsWithStock_Success() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: 在庫数1-20の範囲で検索
        Page<Product> result = productRepository.findBySearchConditionsWithStock(
                null, null, null, 1, 20, pageable);

        // Then: 在庫不足商品のみ取得できる
        assertEquals(1, result.getTotalElements());
        Product found = result.getContent().get(0);
        assertTrue(found.getStock() >= 1 && found.getStock() <= 20);
    }

    /**
     * 在庫数が0の商品を検索できることを検証
     */
    @Test
    @DisplayName("在庫数が0の商品を検索できる")
    void findBySearchConditionsWithStock_OutOfStock() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: 在庫数0で検索
        Page<Product> result = productRepository.findBySearchConditionsWithStock(
                null, null, null, 0, 0, pageable);

        // Then: 在庫切れ商品のみ取得できる
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getContent().get(0).getStock());
    }

    /**
     * 在庫数が21以上の商品を検索できることを検証
     */
    @Test
    @DisplayName("在庫数が21以上の商品を検索できる")
    void findBySearchConditionsWithStock_Sufficient() {
        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: 在庫数21以上で検索
        Page<Product> result = productRepository.findBySearchConditionsWithStock(
                null, null, null, 21, null, pageable);

        // Then: 在庫十分な商品のみ取得できる
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getStock() >= 21);
    }

    /**
     * 全カテゴリを取得できることを検証
     */
    @Test
    @DisplayName("全カテゴリを取得できる")
    void findAllCategories_Success() {
        // When: 全カテゴリを取得
        List<String> categories = productRepository.findAllCategories();

        // Then: 重複なく全カテゴリが取得できる
        assertEquals(3, categories.size());
        assertTrue(categories.contains("Electronics"));
        assertTrue(categories.contains("Clothing"));
        assertTrue(categories.contains("Home Appliances"));
    }

    /**
     * 論理削除された商品は検索結果に含まれないことを検証
     */
    @Test
    @DisplayName("論理削除された商品は検索結果に含まれない")
    void findBySearchConditions_ExcludesSoftDeleted() {
        // Given: product1を論理削除
        product1.setDeletedAt(LocalDateTime.now());
        productRepository.save(product1);

        // Given: ページング設定
        Pageable pageable = PageRequest.of(0, 20);

        // When: 全商品を検索
        Page<Product> result = productRepository.findBySearchConditions(null, null, null, pageable);

        // Then: 削除されていない商品のみ取得できる
        assertEquals(2, result.getTotalElements());
        assertFalse(result.getContent().stream().anyMatch(p -> "TEST001".equals(p.getProductCode())));
    }

    /**
     * ページングが正しく動作することを検証
     */
    @Test
    @DisplayName("ページングが正しく動作する")
    void findBySearchConditions_Pagination() {
        // Given: ページサイズ2で1ページ目を取得
        Pageable pageable = PageRequest.of(0, 2);

        // When: 全商品を検索
        Page<Product> result = productRepository.findBySearchConditions(null, null, null, pageable);

        // Then: ページング情報が正しい
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertTrue(result.hasNext());
    }

    /**
     * 管理者用検索では論理削除済み商品も含まれることを検証
     */
    @Test
    @DisplayName("管理者用検索は論理削除済み商品を含む")
    void findBySearchConditionsIncludingDeleted_IncludesSoftDeleted() {
        // Given: product1を論理削除
        product1.setDeletedAt(LocalDateTime.now());
        productRepository.save(product1);

        Pageable pageable = PageRequest.of(0, 20);

        // When: 削除済み含む検索を実行
        Page<Product> result = productRepository.findBySearchConditionsIncludingDeleted(
                null, null, null, null, null, pageable);

        // Then: 削除済みも含めて3件取得できる
        assertEquals(3, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> "TEST001".equals(p.getProductCode())));
    }

    /**
     * 管理者用検索で在庫条件が適用されることを検証
     */
    @Test
    @DisplayName("管理者用検索で在庫条件を指定できる")
    void findBySearchConditionsIncludingDeleted_WithStockRange() {
        Pageable pageable = PageRequest.of(0, 20);

        // When: 在庫1-20で検索
        Page<Product> result = productRepository.findBySearchConditionsIncludingDeleted(
                null, null, null, 1, 20, pageable);

        // Then: 在庫不足の商品のみ取得
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST002", result.getContent().get(0).getProductCode());
    }
}
