package com.inventory.inventory_management.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品クイック登録フォーム（最小限）
 * products.html の一覧画面内クイック登録専用
 */
@Data
public class ProductQuickForm {

    /** 商品名（必須） */
    @NotBlank(message = "商品名は必須です")
    private String productName;

    /** カテゴリ（必須） */
    @NotBlank(message = "カテゴリは必須です")
    private String category;

    /** 価格（必須0以上） */
    @NotNull(message = "価格は必須です")
    @DecimalMin(value = "0.00", message = "価格は0以上で入力してください")
    private BigDecimal price;

    /** 在庫数（必須0以上） */
    @NotNull(message = "在庫数は必須です")
    @Min(value = 0, message = "在庫数は0以上で入力してください")
    private Integer stockQuantity;

    /** ステータス（active / inactive） */
    private String status;
}