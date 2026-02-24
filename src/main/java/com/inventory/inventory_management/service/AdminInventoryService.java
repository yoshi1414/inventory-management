package com.inventory.inventory_management.service;

import java.time.LocalDateTime;
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
import org.springframework.beans.factory.annotation.Value;

/**
 * 管理者用在庫管理サービス
 * 商品の検索・フィルタリング・在庫状態の取得・一括操作を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInventoryService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Value("${inventory.page-size}")
    private int pageSize;

    /**
     * 商品を検索（管理者用：削除済み商品含む、ページング対応）
     * @param keyword 商品名検索キーワード
     * @param category カテゴリ
     * @param status ステータス
     * @param stockFilter 在庫状態フィルタ（"all", "sufficient", "low", "out"）
     * @param sortBy ソート順（"name", "stock", "updated"）
     * @param page ページ番号（0始まり）
     * @param includeDeleted 削除済み商品を含むかどうか
     * @return 検索結果のページ
     */
    public Page<Product> searchProducts(
            String keyword,
            String category,
            String status,
            String stockFilter,
            String sortBy,
            int page,
            boolean includeDeleted) {

        try {
            log.debug("管理者用商品検索: keyword={}, category={}, status={}, stockFilter={}, sortBy={}, page={}, includeDeleted={}",
                    keyword, category, status, stockFilter, sortBy, page, includeDeleted);

            // ソート条件を設定
            Sort sort = createSort(sortBy);
            Pageable pageable = PageRequest.of(page, pageSize, sort);

            // 削除済み商品を含むかどうかで検索を分岐
            Page<Product> result;
            if (includeDeleted) {
                // 削除済み商品を含む検索（管理者専用）
                result = searchProductsIncludingDeleted(keyword, category, status, stockFilter, pageable);
            } else {
                // 削除済み商品を除外した検索
                if (stockFilter != null && !stockFilter.isEmpty() && !"all".equals(stockFilter)) {
                    result = searchWithStockFilter(keyword, category, status, stockFilter, pageable);
                } else {
                    result = productRepository.findBySearchConditions(keyword, category, status, pageable);
                }
            }

            log.debug("検索結果: {}件", result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("管理者用商品検索時にエラーが発生: error={}", e.getMessage(), e);
            throw new RuntimeException("商品検索に失敗しました", e);
        }
    }

    /**
     * 削除済み商品を含む検索（管理者専用）
     * @param keyword 商品名検索キーワード
     * @param category カテゴリ
     * @param status ステータス
     * @param stockFilter 在庫状態フィルタ
     * @param pageable ページング情報
     * @return 検索結果のページ
     */
    private Page<Product> searchProductsIncludingDeleted(
            String keyword,
            String category,
            String status,
            String stockFilter,
            Pageable pageable) {

        Integer minStock = null;
        Integer maxStock = null;

        if (stockFilter != null && !stockFilter.isEmpty() && !"all".equals(stockFilter)) {
            switch (stockFilter) {
                case "out":
                    minStock = 0;
                    maxStock = 0;
                    break;
                case "low":
                    minStock = 1;
                    maxStock = 20;
                    break;
                case "sufficient":
                    minStock = 21;
                    maxStock = null;
                    break;
            }
        }

        // 削除済み商品を含む検索用クエリ（Repositoryに追加実装が必要）
        return productRepository.findBySearchConditionsIncludingDeleted(
                keyword, category, status, minStock, maxStock, pageable);
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
                minStock = 0;
                maxStock = 0;
                break;
            case "low":
                minStock = 1;
                maxStock = 20;
                break;
            case "sufficient":
                minStock = 21;
                maxStock = null;
                break;
            default:
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
                return Sort.by(Sort.Direction.ASC, "stock");
            case "stock_desc":
                return Sort.by(Sort.Direction.DESC, "stock");
            case "updated":
                return Sort.by(Sort.Direction.DESC, "updatedAt");
            case "name":
            default:
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
     * 在庫を更新（入庫・出庫・在庫数直接設定）
     * @param productId 商品ID
     * @param transactionType 取引種別（"in": 入庫、"out": 出庫、"set": 在庫数設定）
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
            if (productId == null || quantity == null || quantity < 0) {
                throw new IllegalArgumentException("商品IDまたは数量が不正です");
            }

            if (!"in".equals(transactionType) && !"out".equals(transactionType) && !"set".equals(transactionType)) {
                throw new IllegalArgumentException("取引種別が不正です（in/out/set）");
            }

            // 商品を取得
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: ID=" + productId));

            // 削除済み商品はチェックしない（管理者は削除済み商品も操作可能）

            int beforeStock = product.getStock();
            int afterStock;

            // 取引種別に応じて在庫数を更新
            switch (transactionType) {
                case "in":
                    // 入庫
                    afterStock = beforeStock + quantity;
                    break;
                case "out":
                    // 出庫
                    if (beforeStock < quantity) {
                        throw new IllegalStateException("在庫数が不足しています（現在: " + beforeStock + "個）");
                    }
                    afterStock = beforeStock - quantity;
                    break;
                case "set":
                    // 在庫数直接設定（管理者専用）
                    afterStock = quantity;
                    break;
                default:
                    throw new IllegalArgumentException("取引種別が不正です");
            }

            // 在庫数を更新
            product.setStock(afterStock);
            product.setUpdatedAt(LocalDateTime.now());
            Product savedProduct = productRepository.save(product);

            // 在庫変動履歴を記録
            StockTransaction transaction = new StockTransaction();
            transaction.setProductId(productId);
            
            // transaction_type='set'の場合、在庫増減方向で'in'/'out'に変換
            String transactionTypeForDb = transactionType;
            if ("set".equals(transactionType)) {
                if (afterStock > beforeStock) {
                    transactionTypeForDb = "in";
                } else if (afterStock < beforeStock) {
                    transactionTypeForDb = "out";
                } else {
                    log.warn("在庫変更なし: productId={}, beforeStock={}, afterStock={}", productId, beforeStock, afterStock);
                    transactionTypeForDb = "in"; // デフォルト
                }
                log.debug("transaction_type変換: set -> {}", transactionTypeForDb);
            }
            
            transaction.setTransactionType(transactionTypeForDb);
            transaction.setQuantity(quantity);
            transaction.setBeforeStock(beforeStock);
            transaction.setAfterStock(afterStock);
            transaction.setUserId(getCurrentUserId());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setRemarks(remarks);
            stockTransactionRepository.save(transaction);

            log.info("在庫更新成功: productId={}, before={}, after={}", productId, beforeStock, afterStock);
            return savedProduct;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("在庫更新バリデーションエラー: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("在庫更新時にエラーが発生: error={}", e.getMessage(), e);
            throw new RuntimeException("在庫更新に失敗しました", e);
        }
    }

    /**
     * 商品詳細をIDで取得（削除済み含む）
     * @param productId 商品ID
     * @return 商品エンティティ（Optional）
     */
    public Optional<Product> getProductById(Integer productId) {
        try {
            log.debug("商品詳細取得: productId={}", productId);

            Optional<Product> product = productRepository.findById(productId);

            if (product.isEmpty()) {
                log.warn("商品が見つかりません: productId={}", productId);
                return Optional.empty();
            }

            log.debug("商品取得成功: productId={}, productName={}", productId, product.get().getProductName());
            return product;

        } catch (Exception e) {
            log.error("商品詳細取得時にエラー: productId={}, error={}", productId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 商品の入出庫履歴を取得
     * @param productId 商品ID
     * @param limit 取得件数（nullの場合は全件取得）
     * @return 在庫変動履歴リスト
     */
    public List<StockTransaction> getStockTransactions(Integer productId, Integer limit) {
        try {
            log.debug("在庫履歴取得: productId={}, limit={}", productId, limit);

            List<StockTransaction> transactions = 
                    stockTransactionRepository.findByProductIdOrderByTransactionDateDesc(productId);

            if (limit != null && limit > 0 && transactions.size() > limit) {
                transactions = transactions.subList(0, limit);
            }

            log.debug("在庫履歴取得成功: {}件", transactions.size());
            return transactions;

        } catch (Exception e) {
            log.error("在庫履歴取得時にエラー: productId={}, error={}", productId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 商品を論理削除から復元
     * @param productId 商品ID
     * @return 復元後の商品エンティティ
     * @throws IllegalArgumentException 商品が見つからない場合
     */
    @Transactional
    public Product restoreProduct(Integer productId) {
        try {
            log.info("商品復元開始: productId={}", productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: ID=" + productId));

            if (product.getDeletedAt() == null) {
                throw new IllegalStateException("商品は削除されていません");
            }

            product.setDeletedAt(null);
            product.setUpdatedAt(LocalDateTime.now());
            Product restoredProduct = productRepository.save(product);

            log.info("商品復元成功: productId={}", productId);
            return restoredProduct;

        } catch (Exception e) {
            log.error("商品復元時にエラー: productId={}, error={}", productId, e.getMessage(), e);
            throw new RuntimeException("商品復元に失敗しました", e);
        }
    }

    /**
     * 商品を論理削除
     * @param productId 商品ID
     * @return 削除後の商品エンティティ
     * @throws IllegalArgumentException 商品が見つからない場合
     */
    @Transactional
    public Product deleteProduct(Integer productId) {
        try {
            log.info("商品削除開始: productId={}", productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: ID=" + productId));

            if (product.getDeletedAt() != null) {
                throw new IllegalStateException("商品は既に削除されています");
            }

            product.setDeletedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            Product deletedProduct = productRepository.save(product);

            log.info("商品削除成功: productId={}", productId);
            return deletedProduct;

        } catch (Exception e) {
            log.error("商品削除時にエラー: productId={}, error={}", productId, e.getMessage(), e);
            throw new RuntimeException("商品削除に失敗しました", e);
        }
    }

    /**
     * 現在のログインユーザーIDを取得
     * @return ユーザーID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null ? authentication.getName() : "system";
        } catch (Exception e) {
            log.warn("ユーザーID取得時にエラー: error={}", e.getMessage());
            return "system";
        }
    }
}
