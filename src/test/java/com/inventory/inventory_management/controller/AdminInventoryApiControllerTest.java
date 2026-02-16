package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.inventory.inventory_management.dto.request.UpdateStockRequest;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.service.AdminInventoryService;

/**
 * AdminInventoryApiControllerのユニットテスト
 * Mockitoを使用してAPIレスポンスの分岐を検証
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInventoryApiController ユニットテスト")
class AdminInventoryApiControllerTest {

    @Mock
    private AdminInventoryService adminInventoryService;

    @InjectMocks
    private AdminInventoryApiController adminInventoryApiController;

    /**
     * 在庫更新が成功した場合に200 OKと成功レスポンスを返すことを検証
     */
    @Test
    @DisplayName("updateStock: 成功時は200を返す")
    void updateStock_Success_ReturnsOk() {
        UpdateStockRequest request = new UpdateStockRequest(1, "in", 5, "入荷");

        Product updated = new Product();
        updated.setId(1);
        updated.setProductName("商品A");
        updated.setStock(15);

        when(adminInventoryService.updateStock(1, "in", 5, "入荷")).thenReturn(updated);

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.updateStock(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("5個の入庫が完了しました。", response.getBody().get("message"));
    }

    /**
     * 在庫更新で不正引数が発生した場合に400を返すことを検証
     */
    @Test
    @DisplayName("updateStock: 不正引数時は400を返す")
    void updateStock_IllegalArgument_ReturnsBadRequest() {
        UpdateStockRequest request = new UpdateStockRequest(1, "in", 5, null);
        when(adminInventoryService.updateStock(anyInt(), any(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("不正なリクエスト"));

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.updateStock(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("不正なリクエスト", response.getBody().get("message"));
    }

    /**
     * 在庫更新で業務エラーが発生した場合に409を返すことを検証
     */
    @Test
    @DisplayName("updateStock: 業務エラー時は409を返す")
    void updateStock_IllegalState_ReturnsConflict() {
        UpdateStockRequest request = new UpdateStockRequest(1, "out", 99, null);
        when(adminInventoryService.updateStock(anyInt(), any(), anyInt(), any()))
                .thenThrow(new IllegalStateException("在庫不足"));

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.updateStock(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("在庫不足", response.getBody().get("message"));
    }

    /**
     * 在庫履歴取得で商品が見つからない場合に404を返すことを検証
     */
    @Test
    @DisplayName("getStockHistory: 商品未存在時は404を返す")
    void getStockHistory_ProductNotFound_ReturnsNotFound() {
        when(adminInventoryService.getProductById(10)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.getStockHistory(10, 5);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
    }

    /**
     * 在庫履歴取得が成功した場合に200と履歴件数を返すことを検証
     */
    @Test
    @DisplayName("getStockHistory: 成功時は200を返す")
    void getStockHistory_Success_ReturnsOk() {
        Product product = new Product();
        product.setId(1);
        product.setProductName("商品A");
        product.setStock(20);

        StockTransaction t1 = new StockTransaction();
        StockTransaction t2 = new StockTransaction();

        when(adminInventoryService.getProductById(1)).thenReturn(Optional.of(product));
        when(adminInventoryService.getStockTransactions(1, 2)).thenReturn(List.of(t1, t2));

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.getStockHistory(1, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(2, response.getBody().get("totalCount"));
    }

    /**
     * 商品削除が成功した場合に200を返すことを検証
     */
    @Test
    @DisplayName("deleteProduct: 成功時は200を返す")
    void deleteProduct_Success_ReturnsOk() {
        Product product = new Product();
        product.setId(5);
        product.setProductName("削除対象");

        when(adminInventoryService.deleteProduct(5)).thenReturn(product);

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.deleteProduct(5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("商品を削除しました（論理削除）", response.getBody().get("message"));
    }

    /**
     * 商品復元で業務エラーが発生した場合に409を返すことを検証
     */
    @Test
    @DisplayName("restoreProduct: 業務エラー時は409を返す")
    void restoreProduct_IllegalState_ReturnsConflict() {
        when(adminInventoryService.restoreProduct(eq(6))).thenThrow(new IllegalStateException("復元不可"));

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.restoreProduct(6);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("復元不可", response.getBody().get("message"));
    }

    /**
     * 商品復元で予期せぬ例外が発生した場合に500を返すことを検証
     */
    @Test
    @DisplayName("restoreProduct: 予期せぬ例外時は500を返す")
    void restoreProduct_Exception_ReturnsInternalServerError() {
        when(adminInventoryService.restoreProduct(eq(7))).thenThrow(new RuntimeException("DBエラー"));

        ResponseEntity<Map<String, Object>> response = adminInventoryApiController.restoreProduct(7);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertNotNull(response.getBody().get("message"));
    }
}
