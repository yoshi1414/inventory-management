package com.inventory.inventory_management.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品エンティティ
 * productsテーブルに対応
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /**
     * 商品ID（主キー）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 商品コード（8桁、ユニーク）
     */
    @Column(name = "product_code", length = 8, nullable = false, unique = true)
    private String productCode;

    /**
     * 商品名
     */
    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    /**
     * カテゴリ
     */
    @Column(name = "category", length = 50, nullable = false)
    private String category;

    /**
     * SKU（Stock Keeping Unit）
     */
    @Column(name = "sku", length = 50, unique = true)
    private String sku;

    /**
     * 価格
     */
    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    /**
     * 在庫数
     */
    @Column(name = "stock", nullable = false)
    private Integer stock;

    /**
     * ステータス（active/inactive）
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * 商品説明
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 評価（0.0-5.0）
     */
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    /**
     * 保証期間（月数）
     */
    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    /**
     * 寸法
     */
    @Column(name = "dimensions", length = 100)
    private String dimensions;

    /**
     * バリエーション（色/サイズ）
     */
    @Column(name = "variations", length = 200)
    private String variations;

    /**
     * 製造日
     */
    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    /**
     * 有効期限
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * タグ
     */
    @Column(name = "tags", length = 200)
    private String tags;

    /**
     * 作成日時
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新日時
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 削除日時（論理削除用）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 在庫状態を取得
     * @return 在庫状態（"out": 0個、"low": 1-20個、"sufficient": 21個以上）
     */
    public String getStockStatus() {
        if (stock == null || stock == 0) {
            return "out";
        } else if (stock <= 20) {
            return "low";
        } else {
            return "sufficient";
        }
    }

    /**
     * 在庫不足かどうかを判定
     * @return true: 在庫不足（20個以下）、false: 在庫十分
     */
    public boolean isLowStock() {
        return stock != null && stock > 0 && stock <= 20;
    }

    /**
     * 在庫切れかどうかを判定
     * @return true: 在庫切れ（0個）、false: 在庫あり
     */
    public boolean isOutOfStock() {
        return stock == null || stock == 0;
    }
}
