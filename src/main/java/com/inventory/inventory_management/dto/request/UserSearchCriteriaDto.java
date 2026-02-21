package com.inventory.inventory_management.dto.request;

import lombok.Data;

/**
 * ユーザー管理画面の検索条件を保持するリクエスト DTO
 * キーワード・ステータス・ソート・ページ番号を一括管理する
 */
@Data
public class UserSearchCriteriaDto {

    /** 検索キーワード（ユーザー名・メールアドレス・フルネーム） */
    private String search;

    /**
     * ステータスフィルター
     * active: 有効のみ / inactive: 無効のみ / 空文字・null: 全件
     */
    private String status;

    /**
     * ソート順
     * username / username_desc / email / email_desc / created / created_desc
     */
    private String sort = "username";

    /** ロール ID フィルター（null の場合は全ロール対象） */
    private Integer roleId;

    /** ページ番号（0始まり） */
    private int page = 0;
}
