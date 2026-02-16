// 共通JavaScriptファイル

document.addEventListener('DOMContentLoaded', function () {
  // モーダルのイベントリスナーを設定
  const stockLinks = document.querySelectorAll('[data-bs-toggle="modal"]');
  stockLinks.forEach(link => {
    link.addEventListener('click', function (event) {
      event.stopPropagation(); // 親要素のクリックイベントを防止
    });
  });

  /**
   * エラーページの戻るボタン処理
   */
  const backButton = document.getElementById('backButton');
  if (backButton) {
    backButton.addEventListener('click', function() {
      window.history.back();
    });
  }
});
