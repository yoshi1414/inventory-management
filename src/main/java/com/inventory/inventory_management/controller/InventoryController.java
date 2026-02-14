package com.inventory.inventory_management.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 一般ユーザー用在庫一覧画面のコントローラー
 * 商品の検索・フィルタリング・ソート・ページング機能を提供
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 一般ユーザー用在庫一覧画面を表示
     * @param search 検索キーワード（商品名）
     * @param category カテゴリー
     * @param status ステータス（active/inactive）
     * @param stock 在庫状況（all/sufficient/low/out）
     * @param sort ソート順（name/stock/updated）
     * @param page ページ番号（0始まり）
     * @param model モデル
     * @return inventory.html
     */
    @GetMapping("/inventory")
    public String showProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "stock", required = false) String stock,
            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            Model model) {

        try {
            // ページ番号のバリデーション
            if (page < 0) {
                log.warn("負のページ番号が指定されました: page={}", page);
                model.addAttribute("errorMessage", "ページ番号は0以上である必要があります。");
                return "error";
            }
            
            // 検索キーワードのトリム処理（前後のスペースを削除）
            if (search != null && !search.isEmpty()) {
                search = search.trim();
            }
            
            log.debug("在庫一覧画面を表示: search={}, category={}, status={}, stock={}, sort={}, page={}", 
                        search, category, status, stock, sort, page);

            // 商品検索
            Page<Product> productPage = inventoryService.searchProducts(
                    search, category, status, stock, sort, page);

            // 在庫不足・在庫切れ商品数を取得
            long lowStockCount = inventoryService.getLowStockCount();
            long outOfStockCount = inventoryService.getOutOfStockCount();

            // モデルに追加
            model.addAttribute("productPage", productPage);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            
            // 検索条件を保持
            model.addAttribute("search", search);
            model.addAttribute("category", category);
            model.addAttribute("status", status);
            model.addAttribute("stock", stock);
            model.addAttribute("sort", sort);
            model.addAttribute("currentPage", page);

            // ページング情報
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalElements", productPage.getTotalElements());
            model.addAttribute("currentPageNumber", page + 1);
            model.addAttribute("pageSize", productPage.getSize());
            
            // 表示範囲計算
            int startItem = page * productPage.getSize() + 1;
            int endItem = Math.min(startItem + productPage.getSize() - 1, (int) productPage.getTotalElements());
            model.addAttribute("startItem", startItem);
            model.addAttribute("endItem", endItem);

            log.debug("検索結果: 全{}件中{}-{}件を表示 (ページ{}/{})", 
                    productPage.getTotalElements(), startItem, endItem, 
                    page + 1, productPage.getTotalPages());

            return "inventory";

        } catch (Exception e) {
            log.error("在庫一覧画面表示時にエラーが発生: error={}", e.getMessage(), e);
            model.addAttribute("errorMessage", "在庫情報の取得に失敗しました。");
            return "error";
        }
    }

    /**
     * 在庫を更新（入庫・出庫）
     * @param request リクエストボディ（productId, transactionType, quantity, remarks）
     * @return ResponseEntity（成功/エラー情報）
     */
    @PostMapping("/api/inventory/update-stock")
    public ResponseEntity<Map<String, Object>> updateStock(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // リクエストパラメータを取得
            Integer productId = (Integer) request.get("productId");
            String transactionType = (String) request.get("transactionType");
            Integer quantity = (Integer) request.get("quantity");
            String remarks = (String) request.get("remarks");

            log.info("在庫更新リクエスト: productId={}, type={}, quantity={}", productId, transactionType, quantity);

            // バリデーション
            if (productId == null) {
                response.put("success", false);
                response.put("message", "商品IDが指定されていません。");
                return ResponseEntity.badRequest().body(response);
            }

            if (transactionType == null || (!transactionType.equals("in") && !transactionType.equals("out"))) {
                response.put("success", false);
                response.put("message", "操作種別が不正です。");
                return ResponseEntity.badRequest().body(response);
            }

            if (quantity == null || quantity <= 0) {
                response.put("success", false);
                response.put("message", "数量は1以上を入力してください。");
                return ResponseEntity.badRequest().body(response);
            }

            // 在庫更新
            Product updatedProduct = inventoryService.updateStock(productId, transactionType, quantity, remarks);

            // 成功レスポンス
            response.put("success", true);
            response.put("message", transactionType.equals("in") ? 
                    quantity + "個の入庫が完了しました。" : 
                    quantity + "個の出庫が完了しました。");
            response.put("product", Map.of(
                    "id", updatedProduct.getId(),
                    "productName", updatedProduct.getProductName(),
                    "stock", updatedProduct.getStock()
            ));

            log.info("在庫更新成功: productId={}, newStock={}", productId, updatedProduct.getStock());
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
     * 一般ユーザー用商品詳細画面を表示
     * @param id 商品ID
     * @param model モデル
     * @return inventory-detail.html または error.html
     */
    @GetMapping("/inventory/products/{id}")
    public String showProductDetail(@PathVariable("id") Integer id, Model model) {
        try {
            log.debug("商品詳細画面を表示: productId={}", id);

            // 商品IDのバリデーション
            if (id == null || id <= 0) {
                log.warn("不正な商品IDが指定されました: id={}", id);
                model.addAttribute("errorMessage", "不正な商品IDです。");
                return "error";
            }

            // 商品情報を取得
            Optional<Product> productOpt = inventoryService.getProductById(id);
            if (productOpt.isEmpty()) {
                log.warn("商品が見つかりませんでした: productId={}", id);
                model.addAttribute("errorMessage", "商品が見つかりません。");
                return "error";
            }

            Product product = productOpt.get();

            // 入出庫履歴を取得（最新3件）
            List<StockTransaction> transactions = inventoryService.getStockTransactions(id, 3);

            // モデルに追加
            model.addAttribute("product", product);
            model.addAttribute("transactions", transactions);

            log.debug("商品詳細取得成功: productId={}, productName={}", id, product.getProductName());
            
            // 一般ユーザー用テンプレートを返す
            return "inventory-detail";

        } catch (Exception e) {
            log.error("商品詳細画面表示時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "商品詳細情報の取得に失敗しました。");
            return "error";
        }
    }
}
