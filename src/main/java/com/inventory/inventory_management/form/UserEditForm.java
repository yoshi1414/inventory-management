package com.inventory.inventory_management.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ユーザー編集フォーム
 * 管理者がユーザー情報を編集する際に使用するフォームクラス
 * パスワード変更は既存の /users/password 機能で対応するため除外
 */
@Data
public class UserEditForm {

    /** ユーザー名 */
    @NotBlank(message = "ユーザー名は必須です")
    @Size(max = 50, message = "ユーザー名は50文字以内で入力してください")
    private String username;

    /** メールアドレス */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 100, message = "メールアドレスは100文字以内で入力してください")
    private String email;

    /** フルネーム */
    @NotBlank(message = "フルネームは必須です")
    @Size(max = 100, message = "フルネームは100文字以内で入力してください")
    private String fullName;

    /** アクティブフラグ（有効: true / 無効: false） */
    @NotNull(message = "ステータスは必須です")
    private Boolean isActive;

    /** 割り当てるロールID */
    @NotNull(message = "ロールは必須です")
    private Integer roleId;
}
