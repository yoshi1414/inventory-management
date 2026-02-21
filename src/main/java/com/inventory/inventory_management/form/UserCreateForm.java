package com.inventory.inventory_management.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ユーザー新規登録フォーム
 * 管理者が新規ユーザーを登録する際に使用するフォームクラス
 */
@Data
public class UserCreateForm {

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

    /** パスワード（8文字以上、英字と数字を含む） */
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8文字以上100文字以内で入力してください")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "パスワードは英字と数字を組み合わせてください"
    )
    private String password;

    /** パスワード（確認用） */
    @NotBlank(message = "パスワード（確認）は必須です")
    private String confirmPassword;

    /** 割り当てるロールID */
    @NotNull(message = "ロールは必須です")
    private Integer roleId;

    /** アクティブフラグ（有効: true / 無効: false） */
    @NotNull(message = "ステータスは必須です")
    private Boolean isActive = true;
}
