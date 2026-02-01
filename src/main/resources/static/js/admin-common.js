// 管理者専用の初期化処理
console.log('Admin common JS loaded');

// 削除モーダルの初期化
const deleteModalElement = document.getElementById('deleteModal');
if (deleteModalElement) {
    deleteModal = new bootstrap.Modal(deleteModalElement);
}
