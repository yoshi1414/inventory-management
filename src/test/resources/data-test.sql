-- テスト環境用初期データ
-- パスワード: password (BCryptハッシュ化済み)

-- ロールマスタ
INSERT INTO roles (id, role_name, description) VALUES
(1, 'ROLE_USER', '一般ユーザー'),
(2, 'ROLE_ADMIN', '管理者');

-- テスト用一般ユーザー
INSERT INTO users (id, username, password, email, full_name, is_active) VALUES
(1, 'testuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'testuser@example.com', 'テストユーザー', TRUE);

-- テスト用管理者ユーザー
INSERT INTO users (id, username, password, email, full_name, is_active) VALUES
(2, 'adminuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'adminuser@example.com', '管理者ユーザー', TRUE);

-- ロール割当：一般ユーザー → ROLE_USER
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

-- ロール割当：管理者 → ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);

-- システム設定
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES
('stock_warning_threshold', '20', '在庫不足閾値', 'INTEGER'),
('stock_critical_threshold', '0', '在庫切れ閾値', 'INTEGER'),
('session_timeout_minutes', '30', 'セッションタイムアウト（分）', 'INTEGER'),
('max_login_attempts', '5', '最大ログイン試行回数', 'INTEGER');
