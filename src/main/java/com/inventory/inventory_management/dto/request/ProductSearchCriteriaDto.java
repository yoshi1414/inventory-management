package com.inventory.inventory_management.dto.request;

import lombok.Data;

/**
 * 商品管理画面の検索条件を保持するリクエスト DTO
 * キーワード・カテゴリ・ステータス・ソート・ページ番号を一括管理する
 */
@Data
public class ProductSearchCriteriaDto {

    /** 検索キーワード（商品名または商品コード） */
    private String search;

    /** カテゴリフィルター */
    private String category;

    /** ステータスフィルター（active / inactive） */
    private String status;

    /**
     * ソート順
     * name / name_desc / price / price_desc / stock / stock_desc / updated
     */
    private String sort = "name";

    /** ページ番号（0始まり） */
    private int page = 0;

    /** 削除済み商品を含めるかどうか */
    private boolean includeDeleted = false;
}
