/**
 * 管理者用在庫管理画面JavaScript
 * 在庫編集・履歴表示・削除/復元機能を提供
 */

// モーダル要素
let stockModal = null;
let historyModal = null;
let deleteModal = null;

// 現在編集中の商品ID
let currentProductId = null;
let currentProductName = null;
let currentStock = null;

// CSRF トークン
const csrfToken = document.body.getAttribute('data-csrf-token');
const csrfHeader = document.body.getAttribute('data-csrf-header');

/**
 * DOMContentLoadedイベント
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('管理者用在庫管理画面JS loaded');
    
    // モーダル初期化
    const stockModalElement = document.getElementById('stockModal');
    if (stockModalElement) {
        stockModal = new bootstrap.Modal(stockModalElement);
    }
    
    const historyModalElement = document.getElementById('historyModal');
    if (historyModalElement) {
        historyModal = new bootstrap.Modal(historyModalElement);
    }
    
    const deleteModalElement = document.getElementById('deleteModal');
    if (deleteModalElement) {
        deleteModal = new bootstrap.Modal(deleteModalElement);
    }
    
    // 在庫リンククリックイベント
    document.querySelectorAll('.stock-link').forEach(link => {
        link.addEventListener('click', function() {
            const productId = this.getAttribute('data-product-id');
            const productName = this.getAttribute('data-product-name');
            const currentStockValue = this.getAttribute('data-current-stock');
            openStockModal({ dataset: { productId, productName, currentStock: currentStockValue } });
        });
    });
    
    // 在庫数入力時の予測表示
    const stockInput = document.getElementById('stockInput');
    if (stockInput) {
        stockInput.addEventListener('input', updatePredictedStock);
    }
    
    // 操作選択変更時の予測表示
    document.querySelectorAll('input[name="stockOperation"]').forEach(radio => {
        radio.addEventListener('change', updatePredictedStock);
    });
});

/**
 * 在庫編集モーダルを開く
 * @param {HTMLElement} button クリックされたボタン要素
 */
function openStockModal(button) {
    currentProductId = button.dataset.productId;
    currentProductName = button.dataset.productName;
    currentStock = parseInt(button.dataset.currentStock);
    
    // モーダル表示内容を設定
    document.getElementById('stockModalProductName').textContent = currentProductName;
    document.getElementById('stockModalCurrentStock').textContent = currentStock;
    document.getElementById('stockInput').value = '';
    document.getElementById('stockRemarks').value = '';
    document.getElementById('stockModalPredictedStock').textContent = '-';
    document.getElementById('stockModalMessage').classList.add('d-none');
    
    // ラジオボタンを初期化
    document.getElementById('stockIn').checked = true;
    
    // モーダルを表示
    if (stockModal) {
        stockModal.show();
    }
}

/**
 * 更新後在庫数を予測表示
 */
function updatePredictedStock() {
    const operation = document.querySelector('input[name="stockOperation"]:checked').value;
    const quantity = parseInt(document.getElementById('stockInput').value) || 0;
    
    let predicted = currentStock;
    
    switch (operation) {
        case 'in':
            predicted = currentStock + quantity;
            break;
        case 'out':
            predicted = currentStock - quantity;
            break;
        case 'set':
            predicted = quantity;
            break;
    }
    
    const predictedElement = document.getElementById('stockModalPredictedStock');
    predictedElement.textContent = predicted;
    
    // 色分け
    if (predicted < 0) {
        predictedElement.className = 'h4 mb-0 text-danger';
    } else if (predicted === 0) {
        predictedElement.className = 'h4 mb-0 text-warning';
    } else if (predicted <= 20) {
        predictedElement.className = 'h4 mb-0 text-warning';
    } else {
        predictedElement.className = 'h4 mb-0 text-success';
    }
}

/**
 * 在庫更新を実行
 */
async function submitStockUpdate() {
    const operation = document.querySelector('input[name="stockOperation"]:checked').value;
    const quantity = parseInt(document.getElementById('stockInput').value);
    const remarks = document.getElementById('stockRemarks').value;
    
    // バリデーション
    if (!quantity || quantity < 0) {
        showStockModalMessage('数量を入力してください', 'danger');
        return;
    }
    
    if (operation === 'out' && quantity > currentStock) {
        showStockModalMessage('出庫数が在庫数を超えています', 'danger');
        return;
    }
    
    try {
        const response = await fetch('/admin/api/inventory/update-stock', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                productId: parseInt(currentProductId),
                transactionType: operation,
                quantity: quantity,
                remarks: remarks
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showStockModalMessage(data.message, 'success');
            
            // 2秒後にモーダルを閉じてページをリロード
            setTimeout(() => {
                if (stockModal) {
                    stockModal.hide();
                }
                location.reload();
            }, 2000);
        } else {
            showStockModalMessage(data.message, 'danger');
        }
    } catch (error) {
        console.error('在庫更新エラー:', error);
        showStockModalMessage('在庫更新に失敗しました', 'danger');
    }
}

/**
 * 在庫モーダルにメッセージを表示
 * @param {string} message メッセージ
 * @param {string} type メッセージタイプ（success/danger/warning）
 */
function showStockModalMessage(message, type) {
    const messageElement = document.getElementById('stockModalMessage');
    messageElement.textContent = message;
    messageElement.className = `alert alert-${type}`;
    messageElement.classList.remove('d-none');
}

/**
 * 在庫履歴モーダルを開く
 * @param {HTMLElement} button クリックされたボタン要素
 */
async function openHistoryModal(button) {
    const productId = button.dataset.productId;
    const productName = button.dataset.productName;
    
    // モーダル表示内容を設定
    document.getElementById('historyModalProductName').textContent = productName;
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">読み込み中...</td></tr>';
    
    // モーダルを表示
    if (historyModal) {
        historyModal.show();
    }
    
    // 履歴データを取得
    try {
        const response = await fetch(`/admin/api/inventory/products/${productId}/history`);
        const data = await response.json();
        
        if (data.success && data.transactions && data.transactions.length > 0) {
            // 履歴を表示
            tbody.innerHTML = '';
            data.transactions.forEach(transaction => {
                const row = document.createElement('tr');
                
                // 日時
                const tdDate = document.createElement('td');
                tdDate.textContent = formatDateTime(transaction.transactionDate);
                row.appendChild(tdDate);
                
                // 種別
                const tdType = document.createElement('td');
                const typeText = transaction.transactionType === 'in' ? '入庫' : 
                                transaction.transactionType === 'out' ? '出庫' : '設定';
                tdType.innerHTML = `<span class="badge ${transaction.transactionType === 'in' ? 'bg-success' : 'bg-danger'}">${typeText}</span>`;
                row.appendChild(tdType);
                
                // 数量
                const tdQuantity = document.createElement('td');
                tdQuantity.textContent = transaction.quantity;
                row.appendChild(tdQuantity);
                
                // 変更前
                const tdBefore = document.createElement('td');
                tdBefore.textContent = transaction.beforeStock;
                row.appendChild(tdBefore);
                
                // 変更後
                const tdAfter = document.createElement('td');
                tdAfter.textContent = transaction.afterStock;
                row.appendChild(tdAfter);
                
                // 実行者
                const tdUser = document.createElement('td');
                tdUser.textContent = transaction.userId;
                row.appendChild(tdUser);
                
                // 備考
                const tdRemarks = document.createElement('td');
                tdRemarks.textContent = transaction.remarks || '-';
                row.appendChild(tdRemarks);
                
                tbody.appendChild(row);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">履歴がありません</td></tr>';
        }
    } catch (error) {
        console.error('履歴取得エラー:', error);
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">履歴の取得に失敗しました</td></tr>';
    }
}

/**
 * 削除確認モーダルを開く
 * @param {HTMLElement} button クリックされたボタン要素
 */
function openDeleteModal(button) {
    const productId = button.dataset.productId;
    const productName = button.dataset.productName;
    
    document.getElementById('deleteProductId').value = productId;
    document.getElementById('deleteProductName').textContent = productName;
    
    if (deleteModal) {
        deleteModal.show();
    }
}

/**
 * 削除を実行
 */
async function confirmDelete() {
    const productId = document.getElementById('deleteProductId').value;
    
    try {
        const response = await fetch(`/admin/api/inventory/products/${productId}/delete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            // 成功メッセージを表示してリロード
            if (deleteModal) {
                deleteModal.hide();
            }
            alert(data.message);
            location.reload();
        } else {
            alert(data.message);
        }
    } catch (error) {
        console.error('削除エラー:', error);
        alert('商品削除に失敗しました');
    }
}

/**
 * 商品を復元
 * @param {HTMLElement} button クリックされたボタン要素
 */
async function restoreProduct(button) {
    const productId = button.dataset.productId;
    const productName = button.dataset.productName;
    
    if (!confirm(`商品「${productName}」を復元しますか?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/api/inventory/products/${productId}/restore`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert(data.message);
            location.reload();
        } else {
            alert(data.message);
        }
    } catch (error) {
        console.error('復元エラー:', error);
        alert('商品復元に失敗しました');
    }
}

/**
 * 日時をフォーマット
 * @param {string} dateTimeString 日時文字列
 * @return {string} フォーマット済み日時
 */
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '-';
    
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}
