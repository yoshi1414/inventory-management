package com.inventory.inventory_management.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryServiceのユニットテスト
 * Mockitoを使用したビジネスロジックの検証
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService ユニットテスト")
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product1;
    private Product product2;

    /**
     * 各テストメソッド実行前の初期化処理
     */
    @BeforeEach
    void setUp() {
        // テスト商品1: 在庫十分
        product1 = new Product();
        product1.setId(1);
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
        product1.setCreatedAt(LocalDateTime.now());
        product1.setUpdatedAt(LocalDateTime.now());

        // テスト商品2: 在庫不足
        product2 = new Product();
        product2.setId(2);
        product2.setProductCode("TEST002");
        product2.setProductName("テストTシャツ");
        product2.setCategory("Clothing");
        product2.setSku("SKU-TEST-002");
        product2.setPrice(new BigDecimal("2500.00"));
        product2.setStock(10);
        product2.setStatus("active");
        product2.setDescription("コットン100%");
        product2.setRating(new BigDecimal("4.0"));
        product2.setCreatedAt(LocalDateTime.now());
        product2.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 商品検索が成功することを検証
     */
    @Test
    @DisplayName("商品検索が成功する")
    void searchProducts_Success() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product1, product2));
        when(productRepository.findBySearchConditions(
                anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 商品を検索
        Page<Product> result = inventoryService.searchProducts(
                "テスト", "Electronics", "active", "all", "name", 0);

        // Then: 検索結果が返される
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(productRepository, times(1)).findBySearchConditions(
                anyString(), anyString(), anyString(), any(Pageable.class));
    }

    /**
     * 在庫状態フィルタ"out"で検索できることを検証
     */
    @Test
    @DisplayName("在庫状態フィルタ'out'で検索できる")
    void searchProducts_WithStockFilterOut() {
        // Given: 在庫切れ商品
        Product outOfStockProduct = new Product();
        outOfStockProduct.setStock(0);
        outOfStockProduct.setProductCode("OUT001");
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(outOfStockProduct));
        
        when(productRepository.findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(0), eq(0), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 在庫切れでフィルタ
        Page<Product> result = inventoryService.searchProducts(
                null, null, null, "out", "name", 0);

        // Then: findBySearchConditionsWithStockが呼ばれる
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getContent().get(0).getStock());
        verify(productRepository, times(1)).findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(0), eq(0), any(Pageable.class));
    }

    /**
     * 在庫状態フィルタ"low"で検索できることを検証
     */
    @Test
    @DisplayName("在庫状態フィルタ'low'で検索できる")
    void searchProducts_WithStockFilterLow() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product2));
        when(productRepository.findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(1), eq(20), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 在庫不足でフィルタ
        Page<Product> result = inventoryService.searchProducts(
                null, null, null, "low", "name", 0);

        // Then: 在庫不足商品が取得できる
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getStock() >= 1 
                && result.getContent().get(0).getStock() <= 20);
        verify(productRepository, times(1)).findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(1), eq(20), any(Pageable.class));
    }

    /**
     * 在庫状態フィルタ"sufficient"で検索できることを検証
     */
    @Test
    @DisplayName("在庫状態フィルタ'sufficient'で検索できる")
    void searchProducts_WithStockFilterSufficient() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product1));
        when(productRepository.findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(21), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 在庫十分でフィルタ
        Page<Product> result = inventoryService.searchProducts(
                null, null, null, "sufficient", "name", 0);

        // Then: 在庫十分な商品が取得できる
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getStock() >= 21);
        verify(productRepository, times(1)).findBySearchConditionsWithStock(
                isNull(), isNull(), isNull(), eq(21), isNull(), any(Pageable.class));
    }

    /**
     * ソート順"name"で正しくソートされることを検証
     */
    @Test
    @DisplayName("ソート順'name'で正しくソートされる")
    void searchProducts_SortByName() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product1, product2));
        when(productRepository.findBySearchConditions(
                isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 商品名順でソート
        inventoryService.searchProducts(null, null, null, "all", "name", 0);

        // Then: 商品名の昇順ソートが適用される
        verify(productRepository).findBySearchConditions(
                isNull(), isNull(), isNull(), 
                argThat(pageable -> pageable.getSort().equals(Sort.by(Sort.Direction.ASC, "productName"))));
    }

    /**
     * ソート順"stock"で正しくソートされることを検証
     */
    @Test
    @DisplayName("ソート順'stock'で正しくソートされる")
    void searchProducts_SortByStock() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product2, product1));
        when(productRepository.findBySearchConditions(
                isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 在庫数順でソート
        inventoryService.searchProducts(null, null, null, "all", "stock", 0);

        // Then: 在庫数の昇順ソートが適用される
        verify(productRepository).findBySearchConditions(
                isNull(), isNull(), isNull(), 
                argThat(pageable -> pageable.getSort().equals(Sort.by(Sort.Direction.ASC, "stock"))));
    }

    /**
     * ソート順"updated"で正しくソートされることを検証
     */
    @Test
    @DisplayName("ソート順'updated'で正しくソートされる")
    void searchProducts_SortByUpdated() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product1, product2));
        when(productRepository.findBySearchConditions(
                isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 更新日順でソート
        inventoryService.searchProducts(null, null, null, "all", "updated", 0);

        // Then: 更新日の降順ソートが適用される
        verify(productRepository).findBySearchConditions(
                isNull(), isNull(), isNull(), 
                argThat(pageable -> pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "updatedAt"))));
    }

    /**
     * ページングが正しく動作することを検証
     */
    @Test
    @DisplayName("ページングが正しく動作する")
    void searchProducts_Pagination() {
        // Given: モックの設定
        Page<Product> mockPage = new PageImpl<>(Arrays.asList(product1));
        when(productRepository.findBySearchConditions(
                isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When: 2ページ目を取得
        inventoryService.searchProducts(null, null, null, "all", "name", 1);

        // Then: ページング情報が正しく渡される
        verify(productRepository).findBySearchConditions(
                isNull(), isNull(), isNull(), 
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 20));
    }

    /**
     * 在庫不足商品数を取得できることを検証
     */
    @Test
    @DisplayName("在庫不足商品数を取得できる")
    void getLowStockCount_Success() {
        // Given: モックの設定
        when(productRepository.countLowStock()).thenReturn(5L);

        // When: 在庫不足商品数を取得
        long count = inventoryService.getLowStockCount();

        // Then: 正しい件数が返される
        assertEquals(5L, count);
        verify(productRepository, times(1)).countLowStock();
    }

    /**
     * 在庫切れ商品数を取得できることを検証
     */
    @Test
    @DisplayName("在庫切れ商品数を取得できる")
    void getOutOfStockCount_Success() {
        // Given: モックの設定
        when(productRepository.countOutOfStock()).thenReturn(3L);

        // When: 在庫切れ商品数を取得
        long count = inventoryService.getOutOfStockCount();

        // Then: 正しい件数が返される
        assertEquals(3L, count);
        verify(productRepository, times(1)).countOutOfStock();
    }

    /**
     * 商品IDで商品を取得できることを検証
     */
    @Test
    @DisplayName("商品IDで商品を取得できる")
    void getProductById_Success() {
        // Given: モックの設定
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));

        // When: 商品IDで商品を取得
        Product result = inventoryService.getProductById(1);

        // Then: 正しい商品が返される
        assertNotNull(result);
        assertEquals("TEST001", result.getProductCode());
        verify(productRepository, times(1)).findById(1);
    }

    /**
     * 存在しない商品IDで検索した場合、nullが返されることを検証
     */
    @Test
    @DisplayName("存在しない商品IDで検索するとnullが返される")
    void getProductById_NotFound() {
        // Given: モックの設定
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // When: 存在しない商品IDで検索
        Product result = inventoryService.getProductById(999);

        // Then: nullが返される
        assertNull(result);
        verify(productRepository, times(1)).findById(999);
    }

    /**
     * 商品コードで商品を取得できることを検証
     */
    @Test
    @DisplayName("商品コードで商品を取得できる")
    void getProductByCode_Success() {
        // Given: モックの設定
        when(productRepository.findByProductCode("TEST001")).thenReturn(product1);

        // When: 商品コードで商品を取得
        Product result = inventoryService.getProductByCode("TEST001");

        // Then: 正しい商品が返される
        assertNotNull(result);
        assertEquals("TEST001", result.getProductCode());
        verify(productRepository, times(1)).findByProductCode("TEST001");
    }

    /**
     * 存在しない商品コードで検索した場合、nullが返されることを検証
     */
    @Test
    @DisplayName("存在しない商品コードで検索するとnullが返される")
    void getProductByCode_NotFound() {
        // Given: モックの設定
        when(productRepository.findByProductCode("NOTEXIST")).thenReturn(null);

        // When: 存在しない商品コードで検索
        Product result = inventoryService.getProductByCode("NOTEXIST");

        // Then: nullが返される
        assertNull(result);
        verify(productRepository, times(1)).findByProductCode("NOTEXIST");
    }

    /**
     * リポジトリで例外が発生した場合、RuntimeExceptionがスローされることを検証
     */
    @Test
    @DisplayName("リポジトリで例外が発生した場合、RuntimeExceptionがスローされる")
    void searchProducts_ThrowsException() {
        // Given: モックで例外をスロー
        when(productRepository.findBySearchConditions(
                anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then: RuntimeExceptionがスローされる
        assertThrows(RuntimeException.class, () -> {
            inventoryService.searchProducts("test", null, null, "all", "name", 0);
        });
    }

    /**
     * 在庫不足カウント時に例外が発生した場合、0が返されることを検証
     */
    @Test
    @DisplayName("在庫不足カウント時に例外が発生した場合、0が返される")
    void getLowStockCount_ThrowsException() {
        // Given: モックで例外をスロー
        when(productRepository.countLowStock()).thenThrow(new RuntimeException("Database error"));

        // When: 在庫不足商品数を取得
        long count = inventoryService.getLowStockCount();

        // Then: 0が返される
        assertEquals(0L, count);
    }

    /**
     * 在庫切れカウント時に例外が発生した場合、0が返されることを検証
     */
    @Test
    @DisplayName("在庫切れカウント時に例外が発生した場合、0が返される")
    void getOutOfStockCount_ThrowsException() {
        // Given: モックで例外をスロー
        when(productRepository.countOutOfStock()).thenThrow(new RuntimeException("Database error"));

        // When: 在庫切れ商品数を取得
        long count = inventoryService.getOutOfStockCount();

        // Then: 0が返される
        assertEquals(0L, count);
    }
}
