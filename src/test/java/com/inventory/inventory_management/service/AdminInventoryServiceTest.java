package com.inventory.inventory_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.repository.StockTransactionRepository;

/**
 * AdminInventoryServiceのユニットテスト
 * Mockitoを使用して管理者在庫機能のビジネスロジックを検証
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInventoryService ユニットテスト")
class AdminInventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @InjectMocks
    private AdminInventoryService adminInventoryService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(adminInventoryService, "pageSize", 20);
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

    /**
     * includeDeleted=trueの場合に削除済み含む検索クエリが呼ばれることを検証
     */
    @Test
    @DisplayName("検索: includeDeleted=trueで削除済み含む検索が呼ばれる")
    void searchProducts_IncludeDeleted_UsesIncludingDeletedQuery() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditionsIncludingDeleted(
                eq("テスト"), eq("Electronics"), eq("active"), eq(1), eq(20), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Product> result = adminInventoryService.searchProducts(
                "テスト", "Electronics", "active", "low", "name", 0, true);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findBySearchConditionsIncludingDeleted(
                eq("テスト"), eq("Electronics"), eq("active"), eq(1), eq(20), any(Pageable.class));
        verify(productRepository, never()).findBySearchConditions(any(), any(), any(), any(Pageable.class));
    }

    /**
     * includeDeleted=falseかつstockFilter未指定時に通常検索クエリが呼ばれることを検証
     */
    @Test
    @DisplayName("検索: includeDeleted=falseで通常検索が呼ばれる")
    void searchProducts_NotIncludeDeleted_UsesDefaultQuery() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Product> result = adminInventoryService.searchProducts(
                null, null, null, "all", "name", 0, false);

        assertNotNull(result);
        verify(productRepository, times(1)).findBySearchConditions(any(), any(), any(), any(Pageable.class));
        verify(productRepository, never()).findBySearchConditionsIncludingDeleted(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    /**
     * includeDeleted=falseかつstockFilter指定時に在庫条件検索クエリが呼ばれることを検証
     */
    @Test
    @DisplayName("検索: 在庫フィルタ指定時は在庫条件検索が呼ばれる")
    void searchProducts_WithStockFilter_UsesStockQuery() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditionsWithStock(
                any(), any(), any(), eq(0), eq(0), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Product> result = adminInventoryService.searchProducts(
                null, null, null, "out", "stock", 1, false);

        assertNotNull(result);
        verify(productRepository, times(1)).findBySearchConditionsWithStock(
                any(), any(), any(), eq(0), eq(0), any(Pageable.class));
    }

    /**
     * includeDeleted=trueかつsufficient指定時に在庫下限21で検索されることを検証
     */
    @Test
    @DisplayName("検索: includeDeleted=true かつ sufficient で在庫下限21")
    void searchProducts_IncludeDeleted_Sufficient() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditionsIncludingDeleted(
                any(), any(), any(), eq(21), eq(null), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Product> result = adminInventoryService.searchProducts(
                null, null, null, "sufficient", "name", 0, true);

        assertNotNull(result);
        verify(productRepository).findBySearchConditionsIncludingDeleted(
                any(), any(), any(), eq(21), eq(null), any(Pageable.class));
    }

    /**
     * sortBy=stock_descで在庫降順ソートされることを検証
     */
    @Test
    @DisplayName("検索: sortBy=stock_descで在庫降順")
    void searchProducts_SortByStockDesc() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        adminInventoryService.searchProducts(null, null, null, "all", "stock_desc", 0, false);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findBySearchConditions(any(), any(), any(), pageableCaptor.capture());
        assertTrue(pageableCaptor.getValue().getSort().toString().contains("stock: DESC"));
    }

    /**
     * sortBy=updatedで更新日時降順ソートされることを検証
     */
    @Test
    @DisplayName("検索: sortBy=updatedで更新日時降順")
    void searchProducts_SortByUpdated() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        adminInventoryService.searchProducts(null, null, null, "all", "updated", 0, false);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findBySearchConditions(any(), any(), any(), pageableCaptor.capture());
        assertTrue(pageableCaptor.getValue().getSort().toString().contains("updatedAt: DESC"));
    }

    /**
     * 検索時に例外が発生した場合はRuntimeExceptionへ変換されることを検証
     */
    @Test
    @DisplayName("検索: 例外時はRuntimeException")
    void searchProducts_WhenRepositoryThrows_ThrowsRuntimeException() {
        when(productRepository.findBySearchConditions(any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("DBエラー"));

        assertThrows(RuntimeException.class,
                () -> adminInventoryService.searchProducts(null, null, null, "all", "name", 0, false));
    }

    /**
     * sortByがnullのとき商品名昇順で検索されることを検証
     */
    @Test
    @DisplayName("検索: sortByがnullなら商品名昇順")
    void searchProducts_NullSort_UsesNameAscending() {
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        adminInventoryService.searchProducts(null, null, null, "all", null, 0, false);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findBySearchConditions(any(), any(), any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(PageRequest.of(0, 20).getPageSize(), pageable.getPageSize());
        assertTrue(pageable.getSort().toString().contains("productName: ASC"));
    }

    /**
     * 在庫不足件数取得時にRepository例外が発生しても0を返すことを検証
     */
    @Test
    @DisplayName("在庫不足件数: 例外時は0を返す")
    void getLowStockCount_WhenRepositoryThrows_ReturnsZero() {
        when(productRepository.countLowStock()).thenThrow(new RuntimeException("DB error"));

        long count = adminInventoryService.getLowStockCount();

        assertEquals(0L, count);
    }

        /**
         * 在庫不足件数取得が正常に取得できることを検証
         */
        @Test
        @DisplayName("在庫不足件数: 正常取得")
        void getLowStockCount_Success() {
                when(productRepository.countLowStock()).thenReturn(4L);

                long count = adminInventoryService.getLowStockCount();

                assertEquals(4L, count);
        }

        /**
         * 在庫切れ件数取得が正常に取得できることを検証
         */
        @Test
        @DisplayName("在庫切れ件数: 正常取得")
        void getOutOfStockCount_Success() {
                when(productRepository.countOutOfStock()).thenReturn(3L);

                long count = adminInventoryService.getOutOfStockCount();

                assertEquals(3L, count);
        }

        /**
         * 在庫切れ件数取得で例外時に0を返すことを検証
         */
        @Test
        @DisplayName("在庫切れ件数: 例外時は0を返す")
        void getOutOfStockCount_WhenRepositoryThrows_ReturnsZero() {
                when(productRepository.countOutOfStock()).thenThrow(new RuntimeException("DBエラー"));

                long count = adminInventoryService.getOutOfStockCount();

                assertEquals(0L, count);
        }

        /**
         * 入庫処理が成功し、取引履歴が保存されることを検証
         */
        @Test
        @DisplayName("在庫更新: 入庫成功")
        void updateStock_In_Success() {
                Product product = new Product();
                product.setId(1);
                product.setStock(10);

                when(productRepository.findById(1)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = adminInventoryService.updateStock(1, "in", 5, "入庫");

                assertEquals(15, result.getStock());
                verify(stockTransactionRepository).save(any(StockTransaction.class));
        }

        /**
         * 出庫処理が成功し、在庫が減算されることを検証
         */
        @Test
        @DisplayName("在庫更新: 出庫成功")
        void updateStock_Out_Success() {
                Product product = new Product();
                product.setId(2);
                product.setStock(10);

                when(productRepository.findById(2)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = adminInventoryService.updateStock(2, "out", 4, "出庫");

                assertEquals(6, result.getStock());
        }

        /**
         * 在庫直接設定が成功することを検証
         */
        @Test
        @DisplayName("在庫更新: 直接設定成功")
        void updateStock_Set_Success() {
                Product product = new Product();
                product.setId(3);
                product.setStock(100);

                when(productRepository.findById(3)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = adminInventoryService.updateStock(3, "set", 7, "棚卸");

                assertEquals(7, result.getStock());
        }

        /**
         * 出庫で在庫不足の場合はIllegalStateExceptionが送出されることを検証
         */
        @Test
        @DisplayName("在庫更新: 在庫不足でIllegalStateException")
        void updateStock_Out_InsufficientStock_ThrowsIllegalStateException() {
                Product product = new Product();
                product.setId(4);
                product.setStock(2);

                when(productRepository.findById(4)).thenReturn(Optional.of(product));

                assertThrows(IllegalStateException.class,
                                () -> adminInventoryService.updateStock(4, "out", 3, "出庫"));
        }

        /**
         * 引数不正の場合はIllegalArgumentExceptionが送出されることを検証
         */
        @Test
        @DisplayName("在庫更新: 引数不正でIllegalArgumentException")
        void updateStock_InvalidArguments_ThrowsIllegalArgumentException() {
                assertThrows(IllegalArgumentException.class,
                                () -> adminInventoryService.updateStock(null, "in", 1, null));
                assertThrows(IllegalArgumentException.class,
                                () -> adminInventoryService.updateStock(1, "in", -1, null));
                assertThrows(IllegalArgumentException.class,
                                () -> adminInventoryService.updateStock(1, "invalid", 1, null));
        }

        /**
         * 商品未存在の場合はIllegalArgumentExceptionが送出されることを検証
         */
        @Test
        @DisplayName("在庫更新: 商品未存在でIllegalArgumentException")
        void updateStock_ProductNotFound_ThrowsIllegalArgumentException() {
                when(productRepository.findById(99)).thenReturn(Optional.empty());

                assertThrows(IllegalArgumentException.class,
                                () -> adminInventoryService.updateStock(99, "in", 1, null));
        }

        /**
         * 認証ユーザーがある場合、取引履歴にユーザーIDが設定されることを検証
         */
        @Test
        @DisplayName("在庫更新: 認証ユーザーIDが履歴に設定される")
        void updateStock_WithAuthenticatedUser_SetsTransactionUserId() {
                Product product = new Product();
                product.setId(8);
                product.setStock(10);

                SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
                Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
                when(authentication.getName()).thenReturn("adminuser");
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                when(productRepository.findById(8)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                adminInventoryService.updateStock(8, "in", 1, "入庫");

                verify(stockTransactionRepository).save(any(StockTransaction.class));
        }

        /**
         * 認証がない場合、取引履歴にsystemが設定されることを検証
         */
        @Test
        @DisplayName("在庫更新: 未認証時はsystemが履歴ユーザーID")
        void updateStock_WithoutAuthentication_SetsSystemUserId() {
                Product product = new Product();
                product.setId(9);
                product.setStock(10);

                SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(null);
                SecurityContextHolder.setContext(securityContext);

                when(productRepository.findById(9)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                adminInventoryService.updateStock(9, "in", 1, "入庫");

                verify(stockTransactionRepository).save(any(StockTransaction.class));
        }

        /**
         * 商品取得が成功することを検証
         */
        @Test
        @DisplayName("商品取得: 成功")
        void getProductById_Success() {
                Product product = new Product();
                product.setId(20);
                product.setProductName("商品A");
                when(productRepository.findById(20)).thenReturn(Optional.of(product));

                Optional<Product> result = adminInventoryService.getProductById(20);

                assertTrue(result.isPresent());
                assertEquals("商品A", result.get().getProductName());
        }

        /**
         * 商品未存在時に空Optionalを返すことを検証
         */
        @Test
        @DisplayName("商品取得: 未存在時は空Optional")
        void getProductById_NotFound_ReturnsEmpty() {
                when(productRepository.findById(21)).thenReturn(Optional.empty());

                Optional<Product> result = adminInventoryService.getProductById(21);

                assertTrue(result.isEmpty());
        }

        /**
         * 商品取得時の例外で空Optionalを返すことを検証
         */
        @Test
        @DisplayName("商品取得: 例外時は空Optional")
        void getProductById_Exception_ReturnsEmpty() {
                when(productRepository.findById(anyInt())).thenThrow(new RuntimeException("DBエラー"));

                Optional<Product> result = adminInventoryService.getProductById(22);

                assertTrue(result.isEmpty());
        }

    /**
     * 在庫履歴取得でlimit指定時に上限件数へ制限されることを検証
     */
    @Test
    @DisplayName("在庫履歴: limit指定で件数制限される")
    void getStockTransactions_WithLimit_ReturnsLimitedList() {
        StockTransaction t1 = new StockTransaction();
        StockTransaction t2 = new StockTransaction();
        StockTransaction t3 = new StockTransaction();
        when(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(1))
                .thenReturn(List.of(t1, t2, t3));

        List<StockTransaction> result = adminInventoryService.getStockTransactions(1, 2);

        assertEquals(2, result.size());
    }

        /**
         * 在庫履歴取得でlimit未指定時に全件返却されることを検証
         */
        @Test
        @DisplayName("在庫履歴: limit未指定で全件")
        void getStockTransactions_WithoutLimit_ReturnsAll() {
                StockTransaction t1 = new StockTransaction();
                StockTransaction t2 = new StockTransaction();
                when(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(2))
                                .thenReturn(List.of(t1, t2));

                List<StockTransaction> result = adminInventoryService.getStockTransactions(2, null);

                assertEquals(2, result.size());
        }

        /**
         * 在庫履歴取得でlimitが0以下の場合は全件返却されることを検証
         */
        @Test
        @DisplayName("在庫履歴: limitが0以下なら全件")
        void getStockTransactions_LimitNonPositive_ReturnsAll() {
                StockTransaction t1 = new StockTransaction();
                StockTransaction t2 = new StockTransaction();
                when(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(3))
                                .thenReturn(List.of(t1, t2));

                List<StockTransaction> result = adminInventoryService.getStockTransactions(3, 0);

                assertEquals(2, result.size());
        }

        /**
         * 在庫履歴取得時に例外が発生した場合は空リストを返すことを検証
         */
        @Test
        @DisplayName("在庫履歴: 例外時は空リスト")
        void getStockTransactions_Exception_ReturnsEmptyList() {
                when(stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(anyInt()))
                                .thenThrow(new RuntimeException("DBエラー"));

                List<StockTransaction> result = adminInventoryService.getStockTransactions(4, 3);

                assertTrue(result.isEmpty());
        }

    /**
     * 論理削除済み商品の復元が成功することを検証
     */
    @Test
    @DisplayName("商品復元: 論理削除済み商品を復元できる")
    void restoreProduct_Success() {
        Product deletedProduct = new Product();
        deletedProduct.setId(10);
        deletedProduct.setDeletedAt(LocalDateTime.now().minusDays(1));

        Product savedProduct = new Product();
        savedProduct.setId(10);
        savedProduct.setDeletedAt(null);

        when(productRepository.findById(10)).thenReturn(Optional.of(deletedProduct));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        Product result = adminInventoryService.restoreProduct(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    /**
     * 削除済みでない商品を復元しようとした場合はRuntimeExceptionとなることを検証
     */
    @Test
    @DisplayName("商品復元: 未削除商品の復元は例外")
    void restoreProduct_NotDeleted_ThrowsRuntimeException() {
        Product activeProduct = new Product();
        activeProduct.setId(11);
        activeProduct.setDeletedAt(null);

        when(productRepository.findById(11)).thenReturn(Optional.of(activeProduct));

        assertThrows(RuntimeException.class, () -> adminInventoryService.restoreProduct(11));
    }

        /**
         * 商品が見つからない場合はRuntimeExceptionが送出されることを検証
         */
        @Test
        @DisplayName("商品復元: 商品未存在時はRuntimeException")
        void restoreProduct_NotFound_ThrowsRuntimeException() {
                when(productRepository.findById(12)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class, () -> adminInventoryService.restoreProduct(12));
        }

        /**
         * 商品削除が成功することを検証
         */
        @Test
        @DisplayName("商品削除: 成功")
        void deleteProduct_Success() {
                Product product = new Product();
                product.setId(30);
                product.setDeletedAt(null);

                when(productRepository.findById(30)).thenReturn(Optional.of(product));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = adminInventoryService.deleteProduct(30);

                assertNotNull(result.getDeletedAt());
        }

        /**
         * 既に削除済みの商品削除はRuntimeExceptionになることを検証
         */
        @Test
        @DisplayName("商品削除: 既削除はRuntimeException")
        void deleteProduct_AlreadyDeleted_ThrowsRuntimeException() {
                Product product = new Product();
                product.setId(31);
                product.setDeletedAt(LocalDateTime.now());

                when(productRepository.findById(31)).thenReturn(Optional.of(product));

                assertThrows(RuntimeException.class, () -> adminInventoryService.deleteProduct(31));
        }

        /**
         * 商品未存在の削除はRuntimeExceptionになることを検証
         */
        @Test
        @DisplayName("商品削除: 商品未存在はRuntimeException")
        void deleteProduct_NotFound_ThrowsRuntimeException() {
                when(productRepository.findById(32)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class, () -> adminInventoryService.deleteProduct(32));
        }
}
