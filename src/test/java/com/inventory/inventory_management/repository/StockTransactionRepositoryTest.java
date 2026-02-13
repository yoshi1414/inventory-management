package com.inventory.inventory_management.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockTransactionRepositoryの統合テスト
 * @SpringBootTestを使用したリポジトリ層のテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("StockTransactionRepository 統合テスト")
class StockTransactionRepositoryTest {

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    private Integer testProduct1Id;
    private Integer testProduct2Id;
    private StockTransaction transaction1;
    private StockTransaction transaction2;
    private StockTransaction transaction3;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        // テストデータをクリア
        stockTransactionRepository.deleteAll();
        productRepository.deleteAll();

        // テスト商品を作成
        Product product1 = new Product();
        product1.setProductCode("TEST0001");
        product1.setProductName("テスト商品1");
        product1.setCategory("Electronics");
        product1.setPrice(new BigDecimal("10000.00"));
        product1.setStock(50);
        product1.setStatus("active");
        product1.setCreatedAt(LocalDateTime.now());
        product1.setUpdatedAt(LocalDateTime.now());
        Product savedProduct1 = productRepository.save(product1);
        testProduct1Id = savedProduct1.getId();

        Product product2 = new Product();
        product2.setProductCode("TEST0002");
        product2.setProductName("テスト商品2");
        product2.setCategory("Electronics");
        product2.setPrice(new BigDecimal("20000.00"));
        product2.setStock(30);
        product2.setStatus("active");
        product2.setCreatedAt(LocalDateTime.now());
        product2.setUpdatedAt(LocalDateTime.now());
        Product savedProduct2 = productRepository.save(product2);
        testProduct2Id = savedProduct2.getId();

        // テスト在庫変動履歴1: 入庫
        transaction1 = new StockTransaction();
        transaction1.setProductId(testProduct1Id);
        transaction1.setTransactionType("in");
        transaction1.setQuantity(10);
        transaction1.setBeforeStock(50);
        transaction1.setAfterStock(60);
        transaction1.setUserId("testuser1");
        transaction1.setTransactionDate(LocalDateTime.now().minusDays(2));
        transaction1.setRemarks("テスト入庫1");
        stockTransactionRepository.save(transaction1);

        // テスト在庫変動履歴2: 出庫
        transaction2 = new StockTransaction();
        transaction2.setProductId(testProduct1Id);
        transaction2.setTransactionType("out");
        transaction2.setQuantity(5);
        transaction2.setBeforeStock(60);
        transaction2.setAfterStock(55);
        transaction2.setUserId("testuser1");
        transaction2.setTransactionDate(LocalDateTime.now().minusDays(1));
        transaction2.setRemarks("テスト出庫");
        stockTransactionRepository.save(transaction2);

        // テスト在庫変動履歴3: 別商品の入庫
        transaction3 = new StockTransaction();
        transaction3.setProductId(testProduct2Id);
        transaction3.setTransactionType("in");
        transaction3.setQuantity(20);
        transaction3.setBeforeStock(0);
        transaction3.setAfterStock(20);
        transaction3.setUserId("testuser2");
        transaction3.setTransactionDate(LocalDateTime.now());
        transaction3.setRemarks("テスト入庫2");
        stockTransactionRepository.save(transaction3);
    }

    /**
     * 商品IDで在庫変動履歴を取得できることを検証
     */
    @Test
    @DisplayName("商品IDで在庫変動履歴を取得できる")
    void findByProductIdOrderByTransactionDateDesc_Success() {
        // When: 商品ID=testProduct1Idの履歴を取得
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1Id);

        // Then: 2件取得でき、日時降順でソートされている
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        assertEquals("out", transactions.get(0).getTransactionType()); // 最新が先頭
        assertEquals("in", transactions.get(1).getTransactionType());
        assertTrue(transactions.get(0).getTransactionDate()
                .isAfter(transactions.get(1).getTransactionDate()));
        
        // remarksフィールドが正しく取得される
        assertEquals("テスト出庫", transactions.get(0).getRemarks());
        assertEquals("テスト入庫1", transactions.get(1).getRemarks());
    }

    /**
     * 存在しない商品IDで検索した場合、空リストが返されることを検証
     */
    @Test
    @DisplayName("存在しない商品IDで検索すると空リストが返される")
    void findByProductIdOrderByTransactionDateDesc_NotFound() {
        // When: 存在しない商品IDで検索
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(999);

        // Then: 空リストが返される
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    /**
     * 商品IDと取引種別で在庫変動履歴を取得できることを検証
     */
    @Test
    @DisplayName("商品IDと取引種別で在庫変動履歴を取得できる")
    void findByProductIdAndTransactionType_Success() {
        // When: 商品ID=testProduct1Id、取引種別=inで検索
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdAndTransactionType(testProduct1Id, "in");

        // Then: 1件取得できる
        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("in", transactions.get(0).getTransactionType());
        assertEquals(10, transactions.get(0).getQuantity());
    }

    /**
     * 商品IDと取引種別（出庫）で在庫変動履歴を取得できることを検証
     */
    @Test
    @DisplayName("商品IDと取引種別（出庫）で在庫変動履歴を取得できる")
    void findByProductIdAndTransactionType_Out_Success() {
        // When: 商品ID=testProduct1Id、取引種別=outで検索
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdAndTransactionType(testProduct1Id, "out");

        // Then: 1件取得できる
        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("out", transactions.get(0).getTransactionType());
        assertEquals(5, transactions.get(0).getQuantity());
    }

    /**
     * 該当する取引種別が存在しない場合、空リストが返されることを検証
     */
    @Test
    @DisplayName("該当する取引種別が存在しない場合、空リストが返される")
    void findByProductIdAndTransactionType_NotFound() {
        // When: 商品ID=testProduct2Id、取引種別=outで検索（存在しない）
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdAndTransactionType(testProduct2Id, "out");

        // Then: 空リストが返される
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    /**
     * 在庫変動履歴を保存できることを検証
     */
    @Test
    @DisplayName("在庫変動履歴を保存できる")
    void save_Success() {
        // Given: 新しい商品を作成
        Product product3 = new Product();
        product3.setProductCode("TEST0003");
        product3.setProductName("テスト商品3");
        product3.setCategory("Electronics");
        product3.setPrice(new BigDecimal("30000.00"));
        product3.setStock(0);
        product3.setStatus("active");
        product3.setCreatedAt(LocalDateTime.now());
        product3.setUpdatedAt(LocalDateTime.now());
        Product savedProduct3 = productRepository.save(product3);
        
        // Given: 新しい在庫変動履歴
        StockTransaction newTransaction = new StockTransaction();
        newTransaction.setProductId(savedProduct3.getId());
        newTransaction.setTransactionType("in");
        newTransaction.setQuantity(100);
        newTransaction.setBeforeStock(0);
        newTransaction.setAfterStock(100);
        newTransaction.setUserId("testuser3");
        newTransaction.setTransactionDate(LocalDateTime.now());
        newTransaction.setRemarks("新規入庫");

        // When: 保存
        StockTransaction saved = stockTransactionRepository.save(newTransaction);

        // Then: 保存され、IDが付与される
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(savedProduct3.getId(), saved.getProductId());
        assertEquals("in", saved.getTransactionType());
        assertEquals(100, saved.getQuantity());
    }

    /**
     * 在庫変動履歴をすべて取得できることを検証
     */
    @Test
    @DisplayName("在庫変動履歴をすべて取得できる")
    void findAll_Success() {
        // When: すべての履歴を取得
        List<StockTransaction> transactions = stockTransactionRepository.findAll();

        // Then: 3件取得できる
        assertNotNull(transactions);
        assertEquals(3, transactions.size());
    }

    /**
     * 在庫変動履歴を削除できることを検証
     */
    @Test
    @DisplayName("在庫変動履歴を削除できる")
    void delete_Success() {
        // Given: 削除する履歴のID
        Integer idToDelete = transaction1.getId();

        // When: 削除
        stockTransactionRepository.deleteById(idToDelete);

        // Then: 削除される
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(testProduct1Id);
        assertEquals(1, transactions.size()); // 2件から1件に減る
    }

    /**
     * 在庫変動履歴の件数を取得できることを検証
     */
    @Test
    @DisplayName("在庫変動履歴の件数を取得できる")
    void count_Success() {
        // When: 件数を取得
        long count = stockTransactionRepository.count();

        // Then: 3件
        assertEquals(3, count);
    }

    /**
     * remarks=nullの在庫変動履歴を保存・取得できることを検証
     */
    @Test
    @DisplayName("remarks=nullの在庫変動履歴を保存・取得できる")
    void save_WithNullRemarks_Success() {
        // Given: 新しい商品を作成
        Product product4 = new Product();
        product4.setProductCode("TEST0004");
        product4.setProductName("テスト商品4");
        product4.setCategory("Electronics");
        product4.setPrice(new BigDecimal("40000.00"));
        product4.setStock(0);
        product4.setStatus("active");
        product4.setCreatedAt(LocalDateTime.now());
        product4.setUpdatedAt(LocalDateTime.now());
        Product savedProduct4 = productRepository.save(product4);
        
        // Given: remarks=nullの在庫変動履歴
        StockTransaction newTransaction = new StockTransaction();
        newTransaction.setProductId(savedProduct4.getId());
        newTransaction.setTransactionType("in");
        newTransaction.setQuantity(50);
        newTransaction.setBeforeStock(0);
        newTransaction.setAfterStock(50);
        newTransaction.setUserId("testuser4");
        newTransaction.setTransactionDate(LocalDateTime.now());
        newTransaction.setRemarks(null); // remarks=null

        // When: 保存
        StockTransaction saved = stockTransactionRepository.save(newTransaction);

        // Then: 保存され、remarks=nullで取得できる
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertNull(saved.getRemarks());

        // データベースから再取得して確認
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(savedProduct4.getId());
        assertEquals(1, transactions.size());
        assertNull(transactions.get(0).getRemarks());
    }

    /**
     * remarks=空文字の在庫変動履歴を保存・取得できることを検証
     */
    @Test
    @DisplayName("remarks=空文字の在庫変動履歴を保存・取得できる")
    void save_WithEmptyRemarks_Success() {
        // Given: 新しい商品を作成
        Product product5 = new Product();
        product5.setProductCode("TEST0005");
        product5.setProductName("テスト商品5");
        product5.setCategory("Electronics");
        product5.setPrice(new BigDecimal("50000.00"));
        product5.setStock(0);
        product5.setStatus("active");
        product5.setCreatedAt(LocalDateTime.now());
        product5.setUpdatedAt(LocalDateTime.now());
        Product savedProduct5 = productRepository.save(product5);
        
        // Given: remarks=""の在庫変動履歴
        StockTransaction newTransaction = new StockTransaction();
        newTransaction.setProductId(savedProduct5.getId());
        newTransaction.setTransactionType("out");
        newTransaction.setQuantity(10);
        newTransaction.setBeforeStock(100);
        newTransaction.setAfterStock(90);
        newTransaction.setUserId("testuser5");
        newTransaction.setTransactionDate(LocalDateTime.now());
        newTransaction.setRemarks(""); // remarks=空文字

        // When: 保存
        StockTransaction saved = stockTransactionRepository.save(newTransaction);

        // Then: 保存され、remarks=""で取得できる
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("", saved.getRemarks());

        // データベースから再取得して確認
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(savedProduct5.getId());
        assertEquals(1, transactions.size());
        assertEquals("", transactions.get(0).getRemarks());
    }

    /**
     * 長いremarksを保存・取得できることを検証
     */
    @Test
    @DisplayName("長いremarksを保存・取得できる")
    void save_WithLongRemarks_Success() {
        // Given: 新しい商品を作成
        Product product6 = new Product();
        product6.setProductCode("TEST0006");
        product6.setProductName("テスト商品6");
        product6.setCategory("Electronics");
        product6.setPrice(new BigDecimal("60000.00"));
        product6.setStock(0);
        product6.setStatus("active");
        product6.setCreatedAt(LocalDateTime.now());
        product6.setUpdatedAt(LocalDateTime.now());
        Product savedProduct6 = productRepository.save(product6);
        
        // Given: 長いremarksの在庫変動履歴（255文字）
        String longRemarks = "A".repeat(255);
        StockTransaction newTransaction = new StockTransaction();
        newTransaction.setProductId(savedProduct6.getId());
        newTransaction.setTransactionType("in");
        newTransaction.setQuantity(100);
        newTransaction.setBeforeStock(0);
        newTransaction.setAfterStock(100);
        newTransaction.setUserId("testuser6");
        newTransaction.setTransactionDate(LocalDateTime.now());
        newTransaction.setRemarks(longRemarks);

        // When: 保存
        StockTransaction saved = stockTransactionRepository.save(newTransaction);

        // Then: 保存され、長いremarksで取得できる
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(255, saved.getRemarks().length());
        assertEquals(longRemarks, saved.getRemarks());

        // データベースから再取得して確認
        List<StockTransaction> transactions = stockTransactionRepository
                .findByProductIdOrderByTransactionDateDesc(savedProduct6.getId());
        assertEquals(1, transactions.size());
        assertEquals(longRemarks, transactions.get(0).getRemarks());
    }
}
