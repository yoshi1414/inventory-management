/**
 * 商品管理画面用 JavaScript
 * admin/products.html で使用
 */

/**
 * 削除確認モーダルを開く
 * @param {HTMLElement} btn - data-product-id / data-product-name を持つボタン要素
 */
function openDeleteModal(btn) {
    const productId   = btn.getAttribute('data-product-id');
    const productName = btn.getAttribute('data-product-name');

    // モーダル内の表示を更新
    document.getElementById('deleteProductName').textContent = productName;

    // 削除フォームのアクションを動的に設定
    document.getElementById('deleteForm').action = '/admin/products/' + productId + '/delete';

    // モーダルを表示
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}
