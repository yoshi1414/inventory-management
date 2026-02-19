package com.inventory.inventory_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.inventory_management.dto.request.ProductSearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.form.ProductDetailForm;
import com.inventory.inventory_management.form.ProductQuickForm;
import com.inventory.inventory_management.repository.ProductRepository;

/**
 * AdminProductServiceのユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductService ユニットテスト")
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminProductService adminProductService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminProductService, "pageSize", 20);
    }

    @Test
    @DisplayName("searchProducts: 検索条件がリポジトリへ渡される")
    void searchProducts_DelegatesToRepository() {
        Page<Product> expected = new PageImpl<>(List.of(new Product()));
        when(productRepository.findBySearchConditions(anyString(), anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(expected);

        Page<Product> actual = adminProductService.searchProducts("test", "Electronics", "active", "name", 0);

        assertEquals(expected, actual);
        verify(productRepository, times(1))
                .findBySearchConditions(anyString(), anyString(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("getProductById: リポジトリ結果を返す")
    void getProductById_ReturnsRepositoryResult() {
        Product product = new Product();
        product.setId(1);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Optional<Product> result = adminProductService.getProductById(1);

        assertEquals(true, result.isPresent());
        assertEquals(1, result.get().getId());
        verify(productRepository).findById(1);
    }

    @Test
    @DisplayName("getAllCategories: カテゴリ一覧を返す")
    void getAllCategories_ReturnsCategories() {
        when(productRepository.findAllCategories()).thenReturn(List.of("Books", "Electronics"));

        List<String> categories = adminProductService.getAllCategories();

        assertEquals(2, categories.size());
        assertEquals("Books", categories.get(0));
        verify(productRepository).findAllCategories();
    }

    @Test
    @DisplayName("createProductQuick: 必須項目で商品が登録される")
    void createProductQuick_SavesProduct() {
        ProductQuickForm form = new ProductQuickForm();
        form.setProductName("  クイック商品  ");
        form.setCategory("Books");
        form.setPrice(new BigDecimal("1500"));
        form.setStockQuantity(null);
        form.setStatus(null);

        when(productRepository.findByProductCode(anyString())).thenReturn(null);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        Product saved = adminProductService.createProductQuick(form);

        assertEquals(10, saved.getId());
        assertEquals("クイック商品", saved.getProductName());
        assertEquals("Books", saved.getCategory());
        assertEquals(0, saved.getStock());
        assertEquals("active", saved.getStatus());
        assertNotNull(saved.getProductCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProductDetail: 詳細項目で商品が登録される")
    void createProductDetail_SavesDetailedProduct() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("  詳細商品  ");
        form.setCategory("Electronics");
        form.setSku(" ");
        form.setPrice(new BigDecimal("9999"));
        form.setStockQuantity(7);
        form.setStatus("inactive");
        form.setDescription("  説明  ");
        form.setWarrantyMonths(12);
        form.setDimensions("  10x10x10  ");
        form.setVariations("  Black  ");
        form.setManufacturingDate("2025-01-01");
        form.setExpirationDate("2026-01-01");
        form.setTags("  tag  ");

        when(productRepository.findByProductCode(anyString())).thenReturn(null);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(20);
            return saved;
        });

        Product saved = adminProductService.createProductDetail(form);

        assertEquals(20, saved.getId());
        assertEquals("詳細商品", saved.getProductName());
        assertNull(saved.getSku());
        assertEquals(LocalDate.of(2025, 1, 1), saved.getManufacturingDate());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getExpirationDate());
        assertEquals("説明", saved.getDescription());
        assertEquals("10x10x10", saved.getDimensions());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProductDetail: 商品未存在時は例外")
    void updateProductDetail_WhenNotFound_ThrowsException() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("更新");
        form.setCategory("Books");
        form.setPrice(new BigDecimal("100"));
        form.setStockQuantity(1);

        assertThrows(IllegalArgumentException.class,
                () -> adminProductService.updateProductDetail(999, form));
    }

    @Test
    @DisplayName("updateProductDetail: 商品更新が成功する")
    void updateProductDetail_Success() {
        Product product = new Product();
        product.setId(1);
        product.setProductName("旧商品");
        product.setCategory("Old");
        product.setPrice(new BigDecimal("100"));
        product.setStock(1);
        product.setStatus("active");
        product.setUpdatedAt(LocalDateTime.now().minusDays(1));

        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("  新商品  ");
        form.setCategory("New");
        form.setSku("SKU-1");
        form.setPrice(new BigDecimal("200"));
        form.setStockQuantity(5);
        form.setStatus("inactive");
        form.setDescription("説明");
        form.setWarrantyMonths(6);
        form.setManufacturingDate("2025-02-01");
        form.setExpirationDate("2026-02-01");
        form.setTags("tag");

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = adminProductService.updateProductDetail(1, form);

        assertEquals("新商品", saved.getProductName());
        assertEquals("New", saved.getCategory());
        assertEquals(5, saved.getStock());
        assertEquals("inactive", saved.getStatus());
        assertEquals(LocalDate.of(2025, 2, 1), saved.getManufacturingDate());
        assertNotNull(saved.getUpdatedAt());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("deleteProduct: 論理削除が実行される")
    void deleteProduct_SetsDeletedAt() {
        Product product = new Product();
        product.setId(1);
        product.setProductName("削除対象");

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminProductService.deleteProduct(1);

        assertNotNull(product.getDeletedAt());
        assertNotNull(product.getUpdatedAt());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("restoreProduct: deletedAtがnullへ更新される")
    void restoreProduct_ClearsDeletedAt() {
        Product product = new Product();
        product.setId(1);
        product.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminProductService.restoreProduct(1);

        assertNull(product.getDeletedAt());
        assertNotNull(product.getUpdatedAt());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("validateSearchCriteria: 不正ページ補正とキーワードtrim")
    void validateSearchCriteria_NormalizesCriteria() {
        ProductSearchCriteriaDto criteria = new ProductSearchCriteriaDto();
        criteria.setPage(-1);
        criteria.setSearch("  ノートPC  ");

        ProductSearchCriteriaDto result = adminProductService.validateSearchCriteria(criteria);

        assertEquals(0, result.getPage());
        assertEquals("ノートPC", result.getSearch());
    }

    @Test
    @DisplayName("calculatePagingInfo: 開始・終了件数を返す")
    void calculatePagingInfo_ReturnsStartAndEnd() {
        int[] paging = adminProductService.calculatePagingInfo(1, 20, 35);

        assertEquals(21, paging[0]);
        assertEquals(35, paging[1]);
    }

    /**
     * ProductからProductDetailFormへの全項目マッピングを検証する
     */
    @Test
    @DisplayName("createProductDetailForm: 全項目が正しく変換される")
    void createProductDetailForm_MapsAllFields() {
        Product product = new Product();
        product.setProductName("テスト商品");
        product.setCategory("Electronics");
        product.setSku("SKU-001");
        product.setPrice(new BigDecimal("12345"));
        product.setStock(15);
        product.setStatus("active");
        product.setDescription("説明テキスト");
        product.setWarrantyMonths(24);
        product.setDimensions("10x20x30");
        product.setVariations("Black,White");
        product.setManufacturingDate(LocalDate.of(2025, 1, 10));
        product.setExpirationDate(LocalDate.of(2027, 1, 10));
        product.setTags("tag1,tag2");

        ProductDetailForm form = adminProductService.createProductDetailForm(product);

        assertEquals("テスト商品", form.getProductName());
        assertEquals("Electronics", form.getCategory());
        assertEquals("SKU-001", form.getSku());
        assertEquals(new BigDecimal("12345"), form.getPrice());
        assertEquals(15, form.getStockQuantity());
        assertEquals("active", form.getStatus());
        assertEquals("説明テキスト", form.getDescription());
        assertEquals(24, form.getWarrantyMonths());
        assertEquals("10x20x30", form.getDimensions());
        assertEquals("Black,White", form.getVariations());
        assertEquals("2025-01-10", form.getManufacturingDate());
        assertEquals("2027-01-10", form.getExpirationDate());
        assertEquals("tag1,tag2", form.getTags());
        verifyNoInteractions(productRepository);
    }

    /**
     * 日付項目がnullの場合にフォーム側もnullになることを検証する
     */
    @Test
    @DisplayName("createProductDetailForm: 日付がnullの場合はnullのまま変換される")
    void createProductDetailForm_NullDates_MapsToNull() {
        Product product = new Product();
        product.setProductName("日付なし商品");
        product.setCategory("Books");
        product.setPrice(new BigDecimal("1000"));
        product.setStock(3);
        product.setStatus("inactive");
        product.setManufacturingDate(null);
        product.setExpirationDate(null);

        ProductDetailForm form = adminProductService.createProductDetailForm(product);

        assertNull(form.getManufacturingDate());
        assertNull(form.getExpirationDate());
        assertEquals("日付なし商品", form.getProductName());
        assertEquals("Books", form.getCategory());
        assertEquals(new BigDecimal("1000"), form.getPrice());
        assertEquals(3, form.getStockQuantity());
        assertEquals("inactive", form.getStatus());
        verifyNoInteractions(productRepository);
    }
}
