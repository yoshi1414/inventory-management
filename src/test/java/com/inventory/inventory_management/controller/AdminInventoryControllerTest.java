package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.inventory.inventory_management.dto.request.SearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.service.AdminInventoryService;

/**
 * AdminInventoryControllerのユニットテスト
 * Mockitoを使用してコントローラーの振る舞いを検証
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInventoryController ユニットテスト")
class AdminInventoryControllerTest {

    @Mock
    private AdminInventoryService adminInventoryService;

    @InjectMocks
    private AdminInventoryController adminInventoryController;

    /**
     * showInventoryが正常に一覧画面を返し、在庫件数をモデルへ設定することを検証
     */
    @Test
    @DisplayName("正常系: 一覧画面が表示されモデルに検索結果が設定される")
    void showInventory_正常系_一覧表示成功() {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setSearch("テスト");
        criteria.setCategory("Electronics");
        criteria.setStatus("active");
        criteria.setStock("low");
        criteria.setSort("name");
        criteria.setPage(0);
        criteria.setIncludeDeleted(true);

        Product product = new Product();
        product.setId(1);
        product.setProductName("テスト商品");

        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(adminInventoryService.searchProducts(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(productPage);
        when(adminInventoryService.getLowStockCount()).thenReturn(5L);
        when(adminInventoryService.getOutOfStockCount()).thenReturn(2L);

        Model model = new ExtendedModelMap();

        String viewName = adminInventoryController.showInventory(criteria, model);

        assertEquals("admin/inventory", viewName);
        assertNotNull(model.getAttribute("productPage"));
        assertNotNull(model.getAttribute("products"));
        assertEquals(5L, model.getAttribute("lowStockCount"));
        assertEquals(2L, model.getAttribute("outOfStockCount"));

        verify(adminInventoryService, times(1)).getLowStockCount();
        verify(adminInventoryService, times(1)).getOutOfStockCount();
    }

    /**
     * 負のページ番号が指定された場合に0へ補正されることを検証
     */
    @Test
    @DisplayName("正常系: 負のページ番号は0へ補正される")
    void showInventory_正常系_負のページ番号を補正() {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setPage(-1);

        Page<Product> productPage = new PageImpl<>(List.of());
        when(adminInventoryService.searchProducts(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(productPage);
        when(adminInventoryService.getLowStockCount()).thenReturn(0L);
        when(adminInventoryService.getOutOfStockCount()).thenReturn(0L);

        Model model = new ExtendedModelMap();

        String viewName = adminInventoryController.showInventory(criteria, model);

        assertEquals("admin/inventory", viewName);
        assertEquals(0, criteria.getPage());

        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(adminInventoryService).searchProducts(
                any(), any(), any(), any(), any(), pageCaptor.capture(), anyBoolean());
        assertEquals(0, pageCaptor.getValue());
    }

    /**
     * 検索キーワードがトリムされてサービスへ渡されることを検証
     */
    @Test
    @DisplayName("正常系: 検索キーワードの前後空白がトリムされる")
    void showInventory_正常系_検索キーワードをトリム() {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        criteria.setSearch("  ノートPC  ");

        Page<Product> productPage = new PageImpl<>(List.of());
        when(adminInventoryService.searchProducts(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(productPage);
        when(adminInventoryService.getLowStockCount()).thenReturn(0L);
        when(adminInventoryService.getOutOfStockCount()).thenReturn(0L);

        Model model = new ExtendedModelMap();

        String viewName = adminInventoryController.showInventory(criteria, model);

        assertEquals("admin/inventory", viewName);
        assertEquals("ノートPC", criteria.getSearch());

        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminInventoryService).searchProducts(
                searchCaptor.capture(), any(), any(), any(), any(), anyInt(), anyBoolean());
        assertEquals("ノートPC", searchCaptor.getValue());
    }

    /**
     * 例外発生時にerror画面へ遷移し、エラーメッセージが設定されることを検証
     */
    @Test
    @DisplayName("異常系: サービス例外時はerror画面を返す")
    void showInventory_異常系_例外発生時はerror() {
        SearchCriteriaDto criteria = new SearchCriteriaDto();
        Model model = new ExtendedModelMap();

        when(adminInventoryService.searchProducts(any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("DBエラー"));

        String viewName = adminInventoryController.showInventory(criteria, model);

        assertEquals("error", viewName);
        Object errorMessage = model.getAttribute("errorMessage");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.toString().contains("在庫情報の取得に失敗しました。"));
    }
}
