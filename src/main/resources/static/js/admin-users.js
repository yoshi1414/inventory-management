/**
 * admin-users.js
 * 管理者用ユーザー管理画面の JavaScript
 * ユーザー削除確認モーダルの制御を担当する
 */

document.addEventListener('DOMContentLoaded', function () {

    // =========================================================
    // 削除確認モーダル
    // =========================================================

    const deleteModal    = document.getElementById('deleteModal');
    const deleteForm     = document.getElementById('deleteForm');
    const deleteUsername = document.getElementById('deleteUsername');
    const deleteUserId   = document.getElementById('deleteUserId');

    if (!deleteModal || !deleteForm) {
        return;
    }

    const modalInstance = new bootstrap.Modal(deleteModal);

    // 削除トリガーボタン（一覧の各行）のクリックイベント
    document.querySelectorAll('.user-delete-trigger').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const userId   = this.getAttribute('data-user-id');
            const username = this.getAttribute('data-username');

            // モーダル内に対象ユーザー情報をセット
            if (deleteUsername) deleteUsername.textContent = username;
            if (deleteUserId)   deleteUserId.textContent   = userId;

            // 削除フォームの送信先を動的に設定
            deleteForm.action = '/admin/users/' + userId + '/delete';

            // モーダルを表示
            modalInstance.show();
        });
    });
});
