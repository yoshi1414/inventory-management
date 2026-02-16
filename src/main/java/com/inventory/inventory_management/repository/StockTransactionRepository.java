package com.inventory.inventory_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventory.inventory_management.entity.StockTransaction;

/**
 * 在庫変動履歴リポジトリ
 * 在庫変動履歴データのCRUD操作を提供
 */
@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Integer> {

    /**
     * 商品IDで在庫変動履歴を検索（日時降順）
     * @param productId 商品ID
     * @return 在庫変動履歴リスト
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.productId = :productId ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByProductIdOrderByTransactionDateDesc(@Param("productId") Integer productId);

    /**
     * 商品IDと取引種別で在庫変動履歴を検索
     * @param productId 商品ID
     * @param transactionType 取引種別（in/out）
     * @return 在庫変動履歴リスト
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.productId = :productId AND st.transactionType = :transactionType ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByProductIdAndTransactionType(
        @Param("productId") Integer productId,
        @Param("transactionType") String transactionType
    );

    /**
     * 商品IDで在庫変動履歴を削除
     * @param productId 商品ID
     */
    void deleteByProductId(Integer productId);
}
