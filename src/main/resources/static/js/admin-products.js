/**
 * 商品管理画面用 JavaScript
 * admin/products.html で使用
 */

/** 削除モーダルの Bootstrap インスタンス */
let deleteModal = null;

document.addEventListener('DOMContentLoaded', function () {

    // 削除モーダル初期化
    const deleteModalElement = document.getElementById('deleteModal');
    if (deleteModalElement) {
        deleteModal = bootstrap.Modal.getOrCreateInstance(deleteModalElement);
    }

    // 削除ボタン: イベントデリゲーション（.product-delete-trigger クラスを持つボタン）
    document.querySelectorAll('.product-delete-trigger').forEach(function (trigger) {
        trigger.addEventListener('click', function () {
            const productId   = this.dataset.productId;
            const productName = this.dataset.productName;

            // モーダル内の表示を更新
            document.getElementById('deleteProductName').textContent = productName;

            // 削除フォームのアクションを動的に設定
            document.getElementById('deleteForm').action = '/admin/products/' + productId + '/delete';

            // モーダルを表示
            if (deleteModal) {
                deleteModal.show();
            }
        });
    });

});
