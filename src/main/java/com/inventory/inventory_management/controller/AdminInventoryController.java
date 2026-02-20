package com.inventory.inventory_management.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import com.inventory.inventory_management.dto.request.SearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.service.AdminInventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用在庫管理画面のコントローラー
 * 商品の検索・フィルタリング・ソート・ページング機能を提供
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    /**
     * 管理者用在庫一覧画面を表示
     * @param criteria 検索条件（SearchCriteriaDto）
     * @param model モデル
     * @return admin/inventory.html
     */
    @GetMapping("/inventory")
    public String showInventory(SearchCriteriaDto criteria, Model model) {

        try {
            // ページ番号のバリデーション
            if (criteria.getPage() < 0) {
                log.warn("無効なページ番号: page={}", criteria.getPage());
                criteria.setPage(0);
            }
            
            // 検索キーワードのトリム処理（前後のスペースを削除）
            if (criteria.getSearch() != null && !criteria.getSearch().isEmpty()) {
                criteria.setSearch(criteria.getSearch().trim());
            }
            
            log.debug("管理者用在庫一覧画面を表示: {}", criteria);

            // 商品検索（削除済み商品を含むオプション付き）
            Page<Product> productPage = adminInventoryService.searchProducts(
                    criteria.getSearch(),
                    criteria.getCategory(),
                    criteria.getStatus(),
                    criteria.getStock(),
                    criteria.getSort(),
                    criteria.getPage(),
                    criteria.isIncludeDeleted());

            // 在庫不足・在庫切れ商品数を取得
            long lowStockCount = adminInventoryService.getLowStockCount();
            long outOfStockCount = adminInventoryService.getOutOfStockCount();

            // モデルに追加
            model.addAttribute("productPage", productPage);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            
            // 検索条件を保持
            model.addAttribute("criteria", criteria);
            model.addAttribute("search", criteria.getSearch());
            model.addAttribute("category", criteria.getCategory());
            model.addAttribute("status", criteria.getStatus());
            model.addAttribute("stock", criteria.getStock());
            model.addAttribute("sort", criteria.getSort());
            model.addAttribute("includeDeleted", criteria.isIncludeDeleted());
            model.addAttribute("currentPage", criteria.getPage());

            // ページング情報
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalElements", productPage.getTotalElements());
            model.addAttribute("currentPageNumber", criteria.getPage() + 1);
            model.addAttribute("pageSize", productPage.getSize());
            
            // 表示範囲計算
            int startItem = criteria.getPage() * productPage.getSize() + 1;
            int endItem = Math.min(startItem + productPage.getSize() - 1, (int) productPage.getTotalElements());
            model.addAttribute("startItem", startItem);
            model.addAttribute("endItem", endItem);

            log.debug("検索結果: 全{}件中{}-{}件を表示 (ページ{}/{})", 
                    productPage.getTotalElements(), startItem, endItem, 
                    criteria.getPage() + 1, productPage.getTotalPages());

            return "admin/inventory";

        } catch (Exception e) {
            log.error("管理者用在庫一覧画面表示時にエラーが発生: error={}", e.getMessage(), e);
            model.addAttribute("errorMessage", "在庫情報の取得に失敗しました。");
            return "error";
        }
    }

    /**
     * 管理者用商品詳細画面を表示
     * @param id 商品ID
     * @param model モデル
     * @return admin/product-detail.html または error.html
     */
    @GetMapping("/inventory/products/{id}")
    public String showProductDetail(@PathVariable("id") Integer id,
                                    @RequestParam(value = "from", required = false) String from,
                                    Model model) {
        try {
            log.debug("管理者用商品詳細画面を表示: productId={}", id);

            // 商品IDのバリデーション
            if (id == null || id <= 0) {
                log.warn("不正な商品IDが指定されました: id={}", id);
                model.addAttribute("errorMessage", "不正な商品IDです。");
                return "error";
            }

            // 商品情報を取得（削除済み商品も含む）
            Optional<Product> productOpt = adminInventoryService.getProductById(id);
            if (productOpt.isEmpty()) {
                log.warn("商品が見つかりませんでした: productId={}", id);
                model.addAttribute("errorMessage", "商品が見つかりません。");
                return "error";
            }

            Product product = productOpt.get();

            // 入出庫履歴を取得（最新10件）
            List<StockTransaction> transactions = adminInventoryService.getStockTransactions(id, 10);

            // モデルに追加
            model.addAttribute("product", product);
            model.addAttribute("transactions", transactions);
            // 遷移元パラメータをテンプレートで使用するために追加
            model.addAttribute("from", from);
            // 戻り先（パンくずなど）を設定
            if (from != null && from.equals("products")) {
                model.addAttribute("parentLabel", "商品管理");
                model.addAttribute("parentUrl", "/admin/products");
            } else {
                // デフォルトは在庫管理
                model.addAttribute("parentLabel", "在庫管理");
                model.addAttribute("parentUrl", "/admin/inventory");
            }

            log.debug("商品詳細取得成功: productId={}, productName={}, deleted={}", 
                    id, product.getProductName(), product.getDeletedAt() != null);
            
            // 管理者用テンプレートを返す
            return "admin/inventory-detail";

        } catch (Exception e) {
            log.error("管理者用商品詳細画面表示時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "商品詳細情報の取得に失敗しました。");
            return "error";
        }
    }
}
