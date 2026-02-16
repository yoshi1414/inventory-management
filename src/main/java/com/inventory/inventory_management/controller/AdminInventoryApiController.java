package com.inventory.inventory_management.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.inventory_management.dto.request.UpdateStockRequest;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.service.AdminInventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用在庫管理API コントローラー
 * 在庫更新・履歴取得・商品削除復元などのAPI処理を提供
 */
@Slf4j
@RestController
@RequestMapping("/admin/api/inventory")
@RequiredArgsConstructor
public class AdminInventoryApiController {

    private final AdminInventoryService adminInventoryService;

    /**
     * 在庫を更新（入庫・出庫・在庫数直接設定）
     * @param request 在庫更新リクエスト（UpdateStockRequest）
     * @return ResponseEntity（成功/エラー情報）
     */
    @PostMapping("/update-stock")
    public ResponseEntity<Map<String, Object>> updateStock(@Valid @RequestBody UpdateStockRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("在庫更新リクエスト: productId={}, type={}, quantity={}", 
                    request.getProductId(), request.getTransactionType(), request.getQuantity());

            // 在庫更新
            Product updatedProduct = adminInventoryService.updateStock(
                    request.getProductId(), 
                    request.getTransactionType(), 
                    request.getQuantity(), 
                    request.getRemarks());

            // 成功レスポンス
            String message;
            switch (request.getTransactionType()) {
                case "in":
                    message = request.getQuantity() + "個の入庫が完了しました。";
                    break;
                case "out":
                    message = request.getQuantity() + "個の出庫が完了しました。";
                    break;
                case "set":
                    message = "在庫数を" + request.getQuantity() + "個に設定しました。";
                    break;
                default:
                    message = "在庫更新が完了しました。";
            }

            response.put("success", true);
            response.put("message", message);
            response.put("product", Map.of(
                    "id", updatedProduct.getId(),
                    "productName", updatedProduct.getProductName(),
                    "stock", updatedProduct.getStock()
            ));

            log.info("在庫更新成功: productId={}, newStock={}", 
                    request.getProductId(), updatedProduct.getStock());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("在庫更新バリデーションエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            log.warn("在庫更新ビジネスエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("在庫更新時にエラーが発生: error={}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "在庫更新に失敗しました。システム管理者に連絡してください。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 商品の入出庫履歴を取得
     * @param productId 商品ID
     * @param limit 取得件数（デフォルト：全件）
     * @return ResponseEntity（履歴リスト）
     */
    @GetMapping("/products/{productId}/history")
    public ResponseEntity<Map<String, Object>> getStockHistory(
            @PathVariable("productId") Integer productId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("在庫履歴取得リクエスト: productId={}, limit={}", productId, limit);

            // 商品の存在チェック
            Optional<Product> product = adminInventoryService.getProductById(productId);
            if (product.isEmpty()) {
                response.put("success", false);
                response.put("message", "商品が見つかりません: ID=" + productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 在庫履歴を取得
            List<StockTransaction> transactions = adminInventoryService.getStockTransactions(productId, limit);

            response.put("success", true);
            response.put("product", Map.of(
                    "id", product.get().getId(),
                    "productName", product.get().getProductName(),
                    "stock", product.get().getStock()
            ));
            response.put("transactions", transactions);
            response.put("totalCount", transactions.size());

            log.debug("在庫履歴取得成功: productId={}, count={}", productId, transactions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("在庫履歴取得時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "在庫履歴の取得に失敗しました。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 商品を論理削除
     * @param productId 商品ID
     * @return ResponseEntity（成功/エラー情報）
     */
    @PostMapping("/products/{productId}/delete")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable("productId") Integer productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("商品削除リクエスト: productId={}", productId);

            Product deletedProduct = adminInventoryService.deleteProduct(productId);

            response.put("success", true);
            response.put("message", "商品を削除しました（論理削除）");
            response.put("product", Map.of(
                    "id", deletedProduct.getId(),
                    "productName", deletedProduct.getProductName()
            ));

            log.info("商品削除成功: productId={}", productId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("商品削除バリデーションエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("商品削除ビジネスエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("商品削除時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "商品削除に失敗しました。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 商品を論理削除から復元
     * @param productId 商品ID
     * @return ResponseEntity（成功/エラー情報）
     */
    @PostMapping("/products/{productId}/restore")
    public ResponseEntity<Map<String, Object>> restoreProduct(@PathVariable("productId") Integer productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("商品復元リクエスト: productId={}", productId);

            Product restoredProduct = adminInventoryService.restoreProduct(productId);

            response.put("success", true);
            response.put("message", "商品を復元しました");
            response.put("product", Map.of(
                    "id", restoredProduct.getId(),
                    "productName", restoredProduct.getProductName()
            ));

            log.info("商品復元成功: productId={}", productId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("商品復元バリデーションエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("商品復元ビジネスエラー: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("商品復元時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "商品復元に失敗しました。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
