package com.inventory.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

/**
 * 一般ユーザー用在庫一覧画面のコントローラー
 */
@Slf4j
@Controller
public class InventoryController {

    /**
     * 一般ユーザー用在庫一覧画面を表示
     * @param search 検索キーワード
     * @param category カテゴリー
     * @param status ステータス
     * @param stock 在庫状況
     * @param sort ソート順
     * @param model モデル
     * @return inventory.html
     */
    @GetMapping("/inventory")
    public String showProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "stock", required = false) String stock,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        try {
            log.debug("在庫一覧画面を表示: search={}, category={}, status={}, stock={}, sort={}", 
                        search, category, status, stock, sort);
            model.addAttribute("products", mockProducts());
            model.addAttribute("search", search);
            model.addAttribute("category", category);
            model.addAttribute("status", status);
            model.addAttribute("stock", stock);
            model.addAttribute("sort", sort);

            return "inventory";
        } catch (Exception e) {
            log.error("在庫一覧画面表示時にエラーが発生: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * モック商品データを返す（開発中）
     * @return モック商品データ
     */
    private Object mockProducts() {
        return null;
    }
}
