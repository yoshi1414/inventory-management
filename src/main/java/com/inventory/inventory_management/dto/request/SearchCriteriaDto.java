package com.inventory.inventory_management.dto.request;

import lombok.Data;

/**
 * 検索条件を保持するリクエストDTOクラス
 * 管理者用在庫管理画面での検索・フィルタリングパラメータを一括管理
 */
@Data
public class SearchCriteriaDto {
    /** 検索キーワード（商品名） */
    private String search;
    
    /** カテゴリー */
    private String category;
    
    /** ステータス（active/inactive/deleted） */
    private String status;
    
    /** 在庫状況（all/sufficient/low/out） */
    private String stock;
    
    /** ソート順（name/stock/stock_desc/updated） */
    private String sort = "name";
    
    /** 削除済み商品を含むかどうか */
    private boolean includeDeleted = false;
    
    /** ページ番号（0始まり） */
    private int page = 0;
}
