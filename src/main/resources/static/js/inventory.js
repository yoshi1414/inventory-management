/**
 * 在庫管理画面用JavaScript
 * 在庫モーダルの入庫・出庫処理を管理
 */

document.addEventListener('DOMContentLoaded', function() {
    const stockModal = document.getElementById('stockModal');
    
    // モーダルが存在しない場合は処理を終了
    if (!stockModal) {
        return;
    }
    
    const stockInput = document.getElementById('stockInput');
    let currentProductId = null;
    let currentProductName = null;
    let currentStock = null;
    let stockUpdated = false; // 在庫更新成功フラグ

    // CSRFトークンをHTMLのdata属性から取得
    const csrfToken = document.body.getAttribute('data-csrf-token');
    const csrfHeader = document.body.getAttribute('data-csrf-header');

    /**
     * メッセージを表示する関数
     */
    function showMessage(message, type) {
        const messageDiv = document.getElementById('stockModalMessage');
        messageDiv.className = 'alert alert-' + type;
        messageDiv.textContent = message;
        messageDiv.classList.remove('d-none');
    }

    /**
     * メッセージを非表示にする関数
     */
    function hideMessage() {
        const messageDiv = document.getElementById('stockModalMessage');
        messageDiv.classList.add('d-none');
    }

    /**
     * モーダルが開かれたときの処理
     */
    stockModal.addEventListener('show.bs.modal', function(event) {
        const button = event.relatedTarget;
        currentProductId = button.getAttribute('data-product-id');
        currentProductName = button.getAttribute('data-product-name');
        currentStock = parseInt(button.getAttribute('data-current-stock'));

        // モーダルのタイトルを更新
        const modalTitle = stockModal.querySelector('.modal-title');
        modalTitle.textContent = '在庫管理 - ' + currentProductName + ' (現在: ' + currentStock + '個)';

        // 入力フィールドをリセット
        stockInput.value = '';
        document.getElementById('stockIn').checked = true;
        
        // メッセージを非表示
        hideMessage();
        
        // 在庫更新フラグをリセット
        stockUpdated = false;
    });

    /**
     * モーダルが閉じられた後の処理
     * 注: ページリロードではなく、DOM更新で在庫数を反映
     */
    stockModal.addEventListener('hidden.bs.modal', function() {
        // 在庫更新フラグをリセット
        stockUpdated = false;
    });

    /**
     * モーダルの更新ボタンクリック処理
     */
    const updateButton = stockModal.querySelector('.btn-primary');
    updateButton.addEventListener('click', function() {
        const operation = document.querySelector('input[name="stockOperation"]:checked').value;
        const amount = parseInt(stockInput.value);

        // バリデーション
        if (!amount || amount <= 0) {
            showMessage('有効な在庫数を入力してください。', 'warning');
            return;
        }

        // 出庫の場合、在庫数チェック
        if (operation === 'out' && amount > currentStock) {
            showMessage('出庫数は現在の在庫数（' + currentStock + '個）以下で入力してください。', 'warning');
            return;
        }
        
        // メッセージを非表示
        hideMessage();

        // 更新ボタンを無効化（二重送信防止）
        updateButton.disabled = true;
        updateButton.textContent = '更新中...';

        // サーバーへのリクエスト
        fetch('/api/inventory/update-stock', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                productId: parseInt(currentProductId),
                transactionType: operation,
                quantity: amount,
                remarks: (operation === 'in' ? '入庫' : '出庫') + '処理'
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 成功メッセージを表示
                showMessage(data.message + ' (新しい在庫数: ' + data.product.stock + '個)', 'success');
                
                // 在庫更新成功フラグを設定
                stockUpdated = true;
                
                // ボタンを再有効化
                updateButton.disabled = false;
                updateButton.textContent = '更新';
                
                // 入力フィールドとラジオボタンをリセット
                stockInput.value = '';
                document.getElementById('stockIn').checked = true;
                
                // 現在の在庫数を更新
                currentStock = data.product.stock;
                
                // モーダルのタイトルを更新
                const modalTitle = stockModal.querySelector('.modal-title');
                modalTitle.textContent = '在庫管理 - ' + currentProductName + ' (現在: ' + currentStock + '個)';
                
                // 【DOM更新】ページ上の在庫数表示を更新（リロードなし）
                const stockLink = document.querySelector('a.stock-link[data-product-id="' + currentProductId + '"]');
                if (stockLink) {
                    stockLink.textContent = data.product.stock;
                    stockLink.setAttribute('data-current-stock', data.product.stock);
                }
            } else {
                // エラーメッセージを表示
                showMessage('エラー: ' + data.message, 'danger');
                // ボタンを再有効化
                updateButton.disabled = false;
                updateButton.textContent = '更新';
            }
        })
        .catch(error => {
            console.error('エラー:', error);
            showMessage('在庫更新に失敗しました。システム管理者に連絡してください。', 'danger');
            // ボタンを再有効化
            updateButton.disabled = false;
            updateButton.textContent = '更新';
        });
    });
});
