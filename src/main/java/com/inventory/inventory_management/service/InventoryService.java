package com.inventory.inventory_management.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 在庫管理サービス
 * 商品の検索・フィルタリング・在庫状態の取得を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductRepository productRepository;

    /**
     * 1ページあたりの表示件数
     */
    private static final int PAGE_SIZE = 20;

    /**
     * 商品を検索（ページング対応）
     * @param keyword 商品名検索キーワード
     * @param category カテゴリ
     * @param status ステータス
     * @param stockFilter 在庫状態フィルタ（"all", "sufficient", "low", "out"）
     * @param sortBy ソート順（"name", "stock", "updated"）
     * @param page ページ番号（0始まり）
     * @return 検索結果のページ
     */
    public Page<Product> searchProducts(
            String keyword,
            String category,
            String status,
            String stockFilter,
            String sortBy,
            int page) {

        try {
            log.debug("商品検索: keyword={}, category={}, status={}, stockFilter={}, sortBy={}, page={}",
                    keyword, category, status, stockFilter, sortBy, page);

            // ソート条件を設定
            Sort sort = createSort(sortBy);
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);

            // 在庫状態フィルタに応じて検索
            Page<Product> result;
            if (stockFilter != null && !stockFilter.isEmpty() && !"all".equals(stockFilter)) {
                result = searchWithStockFilter(keyword, category, status, stockFilter, pageable);
            } else {
                result = productRepository.findBySearchConditions(keyword, category, status, pageable);
            }

            log.debug("検索結果: {}件", result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("商品検索時にエラーが発生: error={}", e.getMessage(), e);
            throw new RuntimeException("商品検索に失敗しました", e);
        }
    }

    /**
     * 在庫状態フィルタ付きで商品を検索
     * @param keyword 商品名検索キーワード
     * @param category カテゴリ
     * @param status ステータス
     * @param stockFilter 在庫状態フィルタ
     * @param pageable ページング情報
     * @return 検索結果のページ
     */
    private Page<Product> searchWithStockFilter(
            String keyword,
            String category,
            String status,
            String stockFilter,
            Pageable pageable) {

        Integer minStock = null;
        Integer maxStock = null;

        switch (stockFilter) {
            case "out":
                // 在庫なし：0個
                minStock = 0;
                maxStock = 0;
                break;
            case "low":
                // 在庫不足：1-20個
                minStock = 1;
                maxStock = 20;
                break;
            case "sufficient":
                // 在庫あり：21個以上
                minStock = 21;
                maxStock = null;
                break;
            default:
                // 全て
                break;
        }

        return productRepository.findBySearchConditionsWithStock(
                keyword, category, status, minStock, maxStock, pageable);
    }

    /**
     * ソート条件を作成
     * @param sortBy ソート種別
     * @return Sortオブジェクト
     */
    private Sort createSort(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "name";
        }

        switch (sortBy) {
            case "stock":
                // 在庫数順（昇順）
                return Sort.by(Sort.Direction.ASC, "stock");
            case "updated":
                // 更新日順（降順）
                return Sort.by(Sort.Direction.DESC, "updatedAt");
            case "name":
            default:
                // 商品名順（昇順）
                return Sort.by(Sort.Direction.ASC, "productName");
        }
    }

    /**
     * 在庫不足の商品数を取得
     * @return 在庫不足商品数（1-20個）
     */
    public long getLowStockCount() {
        try {
            long count = productRepository.countLowStock();
            log.debug("在庫不足商品数: {}件", count);
            return count;
        } catch (Exception e) {
            log.error("在庫不足商品数取得時にエラー: error={}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 在庫切れの商品数を取得
     * @return 在庫切れ商品数（0個）
     */
    public long getOutOfStockCount() {
        try {
            long count = productRepository.countOutOfStock();
            log.debug("在庫切れ商品数: {}件", count);
            return count;
        } catch (Exception e) {
            log.error("在庫切れ商品数取得時にエラー: error={}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 商品IDで商品を取得
     * @param id 商品ID
     * @return 商品エンティティ
     */
    public Product getProductById(Integer id) {
        try {
            log.debug("商品取得: id={}", id);
            return productRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("商品取得時にエラー: id={}, error={}", id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 商品コードで商品を取得
     * @param productCode 商品コード
     * @return 商品エンティティ
     */
    public Product getProductByCode(String productCode) {
        try {
            log.debug("商品取得: productCode={}", productCode);
            return productRepository.findByProductCode(productCode);
        } catch (Exception e) {
            log.error("商品取得時にエラー: productCode={}, error={}", productCode, e.getMessage(), e);
            return null;
        }
    }
}
