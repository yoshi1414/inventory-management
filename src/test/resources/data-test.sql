-- テスト環境用初期データ
-- パスワード: password (BCryptハッシュ化済み)

-- テーブルクリア（外部キー制約の順序を考慮）
DELETE FROM audit_logs;
DELETE FROM stock_transactions;
DELETE FROM user_roles;
DELETE FROM products;
DELETE FROM users;
DELETE FROM roles;
DELETE FROM system_settings;

-- ロールマスタ
INSERT INTO roles (id, role_name, description, created_at) VALUES (1, 'ROLE_USER', '一般ユーザー', CURRENT_TIMESTAMP);
INSERT INTO roles (id, role_name, description, created_at) VALUES (2, 'ROLE_ADMIN', '管理者', CURRENT_TIMESTAMP);

-- テスト用一般ユーザー
INSERT INTO users (id, username, password, email, full_name, is_active, created_at, updated_at) VALUES (1, 'testuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'testuser@example.com', 'テストユーザー', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- テスト用管理者ユーザー
INSERT INTO users (id, username, password, email, full_name, is_active, created_at, updated_at) VALUES (2, 'adminuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'adminuser@example.com', '管理者ユーザー', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ロール割当：一般ユーザー → ROLE_USER
INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (1, 1, CURRENT_TIMESTAMP);

-- ロール割当：管理者 → ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (2, 2, CURRENT_TIMESTAMP);

-- システム設定
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES ('stock_warning_threshold', '20', '在庫不足閾値', 'INTEGER');
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES ('stock_critical_threshold', '0', '在庫切れ閾値', 'INTEGER');
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES ('session_timeout_minutes', '30', 'セッションタイムアウト（分）', 'INTEGER');
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES ('max_login_attempts', '5', '最大ログイン試行回数', 'INTEGER');

-- テスト用商品データ
INSERT INTO products (id, product_code, product_name, category, sku, price, stock, status, description, created_at, updated_at) VALUES (1, 'P0000001', 'テスト商品A', 'Electronics', 'SKU-TEST-001', 1000.00, 50, 'active', 'テスト用の商品A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO products (id, product_code, product_name, category, sku, price, stock, status, description, created_at, updated_at) VALUES (2, 'P0000002', 'テスト商品B', 'Clothing', 'SKU-TEST-002', 2000.00, 30, 'active', 'テスト用の商品B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO products (id, product_code, product_name, category, sku, price, stock, status, description, created_at, updated_at) VALUES (3, 'P0000003', 'テスト商品C', 'Food', 'SKU-TEST-003', 500.00, 10, 'active', 'テスト用の商品C', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO products (id, product_code, product_name, category, sku, price, stock, status, description, created_at, updated_at) VALUES (4, 'P0000004', 'テスト商品D', 'Books', 'SKU-TEST-004', 1500.00, 0, 'active', 'テスト用の商品D（在庫切れ）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- テスト用在庫履歴データ（remarksカラムを含む）
INSERT INTO stock_transactions (product_id, transaction_type, quantity, before_stock, after_stock, user_id, transaction_date, remarks) VALUES (1, 'in', 50, 0, 50, 'testuser', CURRENT_TIMESTAMP, '初期在庫入庫');
INSERT INTO stock_transactions (product_id, transaction_type, quantity, before_stock, after_stock, user_id, transaction_date, remarks) VALUES (2, 'in', 30, 0, 30, 'testuser', CURRENT_TIMESTAMP, '初期在庫入庫');
INSERT INTO stock_transactions (product_id, transaction_type, quantity, before_stock, after_stock, user_id, transaction_date, remarks) VALUES (3, 'in', 20, 0, 20, 'adminuser', CURRENT_TIMESTAMP, '初期在庫入庫');
INSERT INTO stock_transactions (product_id, transaction_type, quantity, before_stock, after_stock, user_id, transaction_date, remarks) VALUES (3, 'out', 10, 20, 10, 'adminuser', CURRENT_TIMESTAMP, 'テスト出庫処理');
