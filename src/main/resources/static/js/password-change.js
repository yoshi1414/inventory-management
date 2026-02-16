/**
 * パスワード変更画面用JavaScript
 * 
 * パスワード強度チェック、要件検証、パスワード一致確認などの
 * クライアント側バリデーションを提供します。
 */

document.addEventListener('DOMContentLoaded', function() {
    'use strict';

    // 要素の取得
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const toggleNewPasswordBtn = document.getElementById('toggleNewPassword');
    const toggleNewPasswordIcon = document.getElementById('toggleNewPasswordIcon');
    const passwordStrengthBar = document.getElementById('passwordStrengthBar');
    const passwordStrengthText = document.getElementById('passwordStrengthText');
    const submitButton = document.getElementById('submitButton');
    const form = document.getElementById('passwordChangeForm');

    // パスワード要件の要素
    const requirements = {
        length: document.getElementById('req-length'),
        uppercase: document.getElementById('req-uppercase'),
        lowercase: document.getElementById('req-lowercase'),
        number: document.getElementById('req-number'),
        special: document.getElementById('req-special')
    };

    // 初期化
    initializeEventListeners();

    /**
     * イベントリスナーを初期化
     */
    function initializeEventListeners() {
        // パスワード表示/非表示切り替え
        toggleNewPasswordBtn.addEventListener('click', handleTogglePasswordVisibility);

        // パスワード強度チェック
        newPasswordInput.addEventListener('input', handlePasswordInput);

        // パスワード確認チェック
        confirmPasswordInput.addEventListener('input', handleConfirmPasswordInput);
        newPasswordInput.addEventListener('blur', handleConfirmPasswordInput);

        // フォーム送信
        form.addEventListener('submit', handleFormSubmit);
    }

    /**
     * パスワード表示/非表示切り替え処理
     */
    function handleTogglePasswordVisibility() {
        const type = newPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        newPasswordInput.setAttribute('type', type);
        
        if (type === 'text') {
            toggleNewPasswordIcon.classList.remove('bi-eye');
            toggleNewPasswordIcon.classList.add('bi-eye-slash');
        } else {
            toggleNewPasswordIcon.classList.remove('bi-eye-slash');
            toggleNewPasswordIcon.classList.add('bi-eye');
        }
    }

    /**
     * パスワード入力時の処理
     */
    function handlePasswordInput() {
        const password = this.value;
        checkPasswordStrength(password);
        checkPasswordRequirements(password);
    }

    /**
     * パスワード確認入力時の処理
     */
    function handleConfirmPasswordInput() {
        checkPasswordMatch();
    }

    /**
     * パスワード強度判定
     * @param {string} password - 判定対象のパスワード
     */
    function checkPasswordStrength(password) {
        if (password.length === 0) {
            passwordStrengthBar.className = 'password-strength-bar';
            passwordStrengthText.textContent = '';
            return;
        }

        let strength = 0;
        
        // 長さチェック
        if (password.length >= 8) strength++;
        if (password.length >= 12) strength++;
        
        // 文字種チェック
        if (/[a-z]/.test(password)) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++;

        // 強度表示の更新
        updatePasswordStrengthDisplay(strength);
    }

    /**
     * パスワード強度表示を更新
     * @param {number} strength - 強度スコア（0-6）
     */
    function updatePasswordStrengthDisplay(strength) {
        passwordStrengthBar.className = 'password-strength-bar';
        
        if (strength <= 2) {
            passwordStrengthBar.classList.add('weak');
            passwordStrengthText.textContent = '強度: 弱い';
            passwordStrengthText.style.color = '#dc3545';
        } else if (strength <= 4) {
            passwordStrengthBar.classList.add('medium');
            passwordStrengthText.textContent = '強度: 普通';
            passwordStrengthText.style.color = '#ffc107';
        } else {
            passwordStrengthBar.classList.add('strong');
            passwordStrengthText.textContent = '強度: 強い';
            passwordStrengthText.style.color = '#198754';
        }
    }

    /**
     * パスワード要件チェック
     * @param {string} password - チェック対象のパスワード
     */
    function checkPasswordRequirements(password) {
        // 8文字以上
        updateRequirement(requirements.length, password.length >= 8);
        
        // 英大文字
        updateRequirement(requirements.uppercase, /[A-Z]/.test(password));
        
        // 英小文字
        updateRequirement(requirements.lowercase, /[a-z]/.test(password));
        
        // 数字
        updateRequirement(requirements.number, /[0-9]/.test(password));
        
        // 特殊文字
        updateRequirement(requirements.special, /[!@#$%^&*(),.?":{}|<>]/.test(password));
    }

    /**
     * 要件表示を更新
     * @param {HTMLElement} element - 要件要素
     * @param {boolean} isValid - 要件が満たされているか
     */
    function updateRequirement(element, isValid) {
        const icon = element.querySelector('i');
        
        if (isValid) {
            element.classList.remove('invalid');
            element.classList.add('valid');
            icon.classList.remove('bi-circle');
            icon.classList.add('bi-check-circle-fill');
        } else {
            element.classList.remove('valid');
            element.classList.add('invalid');
            icon.classList.remove('bi-check-circle-fill');
            icon.classList.add('bi-circle');
        }
    }

    /**
     * パスワード一致確認
     */
    function checkPasswordMatch() {
        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        const errorDiv = document.getElementById('confirmPasswordError');

        if (confirmPassword.length === 0) {
            confirmPasswordInput.classList.remove('is-invalid', 'is-valid');
            errorDiv.textContent = '';
            return;
        }

        if (newPassword === confirmPassword) {
            confirmPasswordInput.classList.remove('is-invalid');
            confirmPasswordInput.classList.add('is-valid');
            errorDiv.textContent = '';
        } else {
            confirmPasswordInput.classList.remove('is-valid');
            confirmPasswordInput.classList.add('is-invalid');
            errorDiv.textContent = 'パスワードが一致しません';
        }
    }

    /**
     * フォーム送信時のバリデーション
     * @param {Event} e - 送信イベント
     */
    function handleFormSubmit(e) {
        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // パスワード検証
        if (!validatePassword(newPassword)) {
            e.preventDefault();
            return false;
        }

        // パスワード一致確認
        if (newPassword !== confirmPassword) {
            e.preventDefault();
            confirmPasswordInput.classList.add('is-invalid');
            document.getElementById('confirmPasswordError').textContent = 'パスワードが一致しません';
            return false;
        }

        // 送信中はボタンを無効化
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>変更中...';
    }

    /**
     * パスワード検証
     * @param {string} password - 検証対象のパスワード
     * @returns {boolean} パスワードが要件を満たしおているか
     */
    function validatePassword(password) {
        if (password.length < 8) {
            alert('パスワードは8文字以上で入力してください。');
            return false;
        }
        if (!/[A-Z]/.test(password)) {
            alert('パスワードには英大文字を含めてください。');
            return false;
        }
        if (!/[a-z]/.test(password)) {
            alert('パスワードには英小文字を含めてください。');
            return false;
        }
        if (!/[0-9]/.test(password)) {
            alert('パスワードには数字を含めてください。');
            return false;
        }
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
            alert('パスワードには特殊文字を含めてください。');
            return false;
        }
        return true;
    }
});
