package com.inventory.inventory_management.entity;

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
 * 在庫変動履歴エンティティ
 * stock_transactionsテーブルに対応
 */
@Entity
@Table(name = "stock_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {

    /**
     * トランザクションID（主キー）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 商品ID（外部キー）
     */
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    /**
     * 取引種別（in: 入庫、out: 出庫）
     */
    @Column(name = "transaction_type", length = 10, nullable = false)
    private String transactionType;

    /**
     * 数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 変更前在庫数
     */
    @Column(name = "before_stock", nullable = false)
    private Integer beforeStock;

    /**
     * 変更後在庫数
     */
    @Column(name = "after_stock", nullable = false)
    private Integer afterStock;

    /**
     * 実行ユーザーID
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /**
     * 取引日時
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /**
     * 備考
     */
    @Column(name = "remarks", length = 255)
    private String remarks;
}
