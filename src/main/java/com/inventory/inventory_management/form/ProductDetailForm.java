package com.inventory.inventory_management.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品詳細登録編集フォーム（全フィールド）
 * product-create.html / product-edit.html 専用
 */
@Data
public class ProductDetailForm {

    /** 商品名（必須255文字以内） */
    @NotBlank(message = "商品名は必須です")
    @Size(max = 255, message = "商品名は255文字以内で入力してください")
    private String productName;

    /** カテゴリ（必須100文字以内） */
    @NotBlank(message = "カテゴリは必須です")
    @Size(max = 100, message = "カテゴリは100文字以内で入力してください")
    private String category;

    /** SKU（任意100文字以内ユニーク） */
    @Size(max = 100, message = "SKUは100文字以内で入力してください")
    private String sku;

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

    /** 商品説明（任意1000文字以内） */
    @Size(max = 1000, message = "商品説明は1000文字以内で入力してください")
    private String description;

    /** 保証期間（月数任意0以上） */
    @Min(value = 0, message = "保証期間は0以上で入力してください")
    private Integer warrantyMonths;

    /** 寸法（任意200文字以内） */
    @Size(max = 200, message = "寸法は200文字以内で入力してください")
    private String dimensions;

    /** バリエーション（任意500文字以内） */
    @Size(max = 500, message = "バリエーションは500文字以内で入力してください")
    private String variations;

    /** 製造日（yyyy-MM-dd任意） */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "製造日はYYYY-MM-DD形式で入力してください")
    private String manufacturingDate;

    /** 有効期限（yyyy-MM-dd任意） */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "有効期限はYYYY-MM-DD形式で入力してください")
    private String expirationDate;

    /** タグ（任意500文字以内） */
    @Size(max = 500, message = "タグは500文字以内で入力してください")
    private String tags;
}