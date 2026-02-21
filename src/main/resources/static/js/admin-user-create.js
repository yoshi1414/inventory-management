/**
 * ユーザー新規登録画面 JavaScript
 * パスワード表示/非表示切替、パスワード一致チェックを担当する
 */
document.addEventListener('DOMContentLoaded', function () {
    setupPasswordToggle('password', 'togglePassword', 'togglePasswordIcon');
    setupPasswordToggle('confirmPassword', 'toggleConfirmPassword', 'toggleConfirmPasswordIcon');
    setupPasswordMatchValidation();
});

/**
 * パスワード表示/非表示トグルを設定する
 * @param {string} inputId       対象 input の ID
 * @param {string} buttonId      トグルボタンの ID
 * @param {string} iconId        アイコン要素の ID
 */
function setupPasswordToggle(inputId, buttonId, iconId) {
    const input  = document.getElementById(inputId);
    const button = document.getElementById(buttonId);
    const icon   = document.getElementById(iconId);

    if (!input || !button || !icon) return;

    button.addEventListener('click', function () {
        const isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';
        icon.classList.toggle('bi-eye',      !isPassword);
        icon.classList.toggle('bi-eye-slash', isPassword);
    });
}

/**
 * パスワード一致チェックをリアルタイムで行う
 */
function setupPasswordMatchValidation() {
    const password        = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    if (!password || !confirmPassword) return;

    function checkMatch() {
        if (confirmPassword.value === '') {
            confirmPassword.setCustomValidity('');
            return;
        }
        if (password.value !== confirmPassword.value) {
            confirmPassword.setCustomValidity('パスワードが一致しません');
        } else {
            confirmPassword.setCustomValidity('');
        }
    }

    password.addEventListener('input', checkMatch);
    confirmPassword.addEventListener('input', checkMatch);
}
