package com.inventory.inventory_management.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.inventory.inventory_management.dto.request.SearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
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
}
