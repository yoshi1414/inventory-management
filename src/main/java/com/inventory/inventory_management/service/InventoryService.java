package com.inventory.inventory_management.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.entity.StockTransaction;
import com.inventory.inventory_management.repository.ProductRepository;
import com.inventory.inventory_management.repository.StockTransactionRepository;

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
    private final StockTransactionRepository stockTransactionRepository;

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

    /**
     * 在庫を更新（入庫・出庫）
     * @param productId 商品ID
     * @param transactionType 取引種別（"in": 入庫、"out": 出庫）
     * @param quantity 数量
     * @param remarks 備考
     * @return 更新後の商品エンティティ
     * @throws IllegalArgumentException 不正な引数の場合
     * @throws IllegalStateException 在庫不足の場合
     */
    @Transactional
    public Product updateStock(Integer productId, String transactionType, Integer quantity, String remarks) {
        try {
            log.info("在庫更新開始: productId={}, type={}, quantity={}", productId, transactionType, quantity);

            // バリデーション
            if (productId == null || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("商品IDまたは数量が不正です");
            }

            if (!"in".equals(transactionType) && !"out".equals(transactionType)) {
                throw new IllegalArgumentException("取引種別が不正です（in/outのみ）");
            }

            // 商品を取得
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + productId));

            // 削除済み商品チェック
            if (product.getDeletedAt() != null) {
                throw new IllegalStateException("削除済みの商品です");
            }

            // 変更前在庫数を記録
            Integer beforeStock = product.getStock();
            Integer afterStock;

            // 在庫数を更新
            if ("in".equals(transactionType)) {
                // 入庫
                afterStock = beforeStock + quantity;
                log.debug("入庫: {} → {}", beforeStock, afterStock);
            } else {
                // 出庫
                if (beforeStock < quantity) {
                    throw new IllegalStateException("在庫が不足しています（現在: " + beforeStock + "個）");
                }
                afterStock = beforeStock - quantity;
                log.debug("出庫: {} → {}", beforeStock, afterStock);
            }

            // 商品の在庫数を更新
            product.setStock(afterStock);
            product.setUpdatedAt(LocalDateTime.now());
            Product savedProduct = productRepository.save(product);

            // 在庫変動履歴を記録
            StockTransaction transaction = new StockTransaction();
            transaction.setProductId(productId);
            transaction.setTransactionType(transactionType);
            transaction.setQuantity(quantity);
            transaction.setBeforeStock(beforeStock);
            transaction.setAfterStock(afterStock);
            transaction.setUserId(getCurrentUserId());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setRemarks(remarks);
            stockTransactionRepository.save(transaction);

            log.info("在庫更新完了: productId={}, before={}, after={}", productId, beforeStock, afterStock);
            return savedProduct;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("在庫更新エラー: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("在庫更新時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            throw new RuntimeException("在庫更新に失敗しました", e);
        }
    }

    /**
     * 商品詳細をIDで取得
     * @param productId 商品ID
     * @return 商品エンティティ（Optional）
     */
    public Optional<Product> getProductById(Integer productId) {
        try {
            log.debug("商品詳細取得: productId={}", productId);
            
            if (productId == null) {
                log.warn("商品IDがnullです");
                return Optional.empty();
            }
            
            Optional<Product> product = productRepository.findById(productId);
            
            // 削除済み商品は返さない
            if (product.isPresent() && product.get().getDeletedAt() != null) {
                log.debug("削除済み商品のため取得不可: productId={}", productId);
                return Optional.empty();
            }
            
            log.debug("商品取得成功: productId={}", productId);
            return product;
            
        } catch (Exception e) {
            log.error("商品取得時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 商品の入出庫履歴を取得（最新N件）
     * @param productId 商品ID
     * @param limit 取得件数（nullの場合は全件取得）
     * @return 在庫変動履歴リスト
     */
    public List<StockTransaction> getStockTransactions(Integer productId, Integer limit) {
        try {
            log.debug("在庫履歴取得: productId={}, limit={}", productId, limit);
            
            if (productId == null) {
                log.warn("商品IDがnullです");
                return Collections.emptyList();
            }
            
            List<StockTransaction> transactions = 
                    stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(productId);
            
            // 件数制限を適用
            if (limit != null && limit > 0 && transactions.size() > limit) {
                transactions = transactions.subList(0, limit);
            }
            
            log.debug("在庫履歴取得成功: productId={}, 件数={}", productId, transactions.size());
            return transactions;
            
        } catch (Exception e) {
            log.error("在庫履歴取得時にエラーが発生: productId={}, error={}", productId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 現在のログインユーザーIDを取得
     * @return ユーザーID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
            return "system";
        } catch (Exception e) {
            log.warn("ユーザーID取得エラー: {}", e.getMessage());
            return "unknown";
        }
    }
}
