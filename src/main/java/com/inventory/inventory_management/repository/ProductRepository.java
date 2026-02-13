package com.inventory.inventory_management.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventory.inventory_management.entity.Product;

/**
 * 商品リポジトリ
 * 商品データのCRUD操作を提供
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /**
     * 商品コードで商品を検索
     * @param productCode 商品コード
     * @return 商品エンティティ
     */
    Product findByProductCode(String productCode);

    /**
     * 複合条件で商品を検索（ページング対応）
     * @param keyword 商品名検索キーワード（部分一致）
     * @param category カテゴリ
     * @param status ステータス
     * @param pageable ページング情報
     * @return 検索結果のページ
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status) AND " +
           "p.deletedAt IS NULL")
    Page<Product> findBySearchConditions(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * 在庫不足の商品数を取得（1-20個）
     * @return 在庫不足商品数
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock > 0 AND p.stock <= 20 AND p.deletedAt IS NULL")
    long countLowStock();

    /**
     * 在庫切れの商品数を取得（0個）
     * @return 在庫切れ商品数
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock = 0 AND p.deletedAt IS NULL")
    long countOutOfStock();

    /**
     * 在庫状態でフィルタリング（ページング対応）
     * @param keyword 商品名検索キーワード
     * @param category カテゴリ
     * @param status ステータス
     * @param minStock 最小在庫数
     * @param maxStock 最大在庫数
     * @param pageable ページング情報
     * @return 検索結果のページ
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status) AND " +
           "(:minStock IS NULL OR p.stock >= :minStock) AND " +
           "(:maxStock IS NULL OR p.stock <= :maxStock) AND " +
           "p.deletedAt IS NULL")
    Page<Product> findBySearchConditionsWithStock(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("status") String status,
        @Param("minStock") Integer minStock,
        @Param("maxStock") Integer maxStock,
        Pageable pageable
    );

    /**
     * 全カテゴリの一覧を取得
     * @return カテゴリリスト
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.deletedAt IS NULL ORDER BY p.category")
    List<String> findAllCategories();
}
