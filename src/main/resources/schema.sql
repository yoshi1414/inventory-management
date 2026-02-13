-- 在庫管理システム データベーススキーマ
-- データベース: inventory_management_db
-- 作成日: 2026年2月7日

-- ロールマスタテーブル
CREATE TABLE IF NOT EXISTS roles (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ユーザーマスタテーブル
CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ユーザーロール関連テーブル
CREATE TABLE IF NOT EXISTS user_roles (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_roles (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 商品マスタテーブル
CREATE TABLE IF NOT EXISTS products (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(8) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sku VARCHAR(50) NULL UNIQUE,
    price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    stock INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    description TEXT NULL,
    rating DECIMAL(2,1) NULL,
    warranty_months INTEGER NULL,
    dimensions VARCHAR(100) NULL,
    variations VARCHAR(200) NULL,
    manufacturing_date DATE NULL,
    expiration_date DATE NULL,
    tags VARCHAR(200) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CHECK (price >= 0),
    CHECK (stock >= 0),
    CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5)),
    CHECK (warranty_months IS NULL OR warranty_months >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 在庫履歴テーブル
CREATE TABLE IF NOT EXISTS stock_transactions (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    before_stock INTEGER NOT NULL,
    after_stock INTEGER NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(255) NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CHECK (transaction_type IN ('in', 'out')),
    CHECK (quantity > 0),
    CHECK (before_stock >= 0),
    CHECK (after_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- システム設定テーブル
CREATE TABLE IF NOT EXISTS system_settings (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(200) NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (data_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'DECIMAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 監査ログテーブル
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    record_id VARCHAR(50) NOT NULL,
    user_id INT NULL,
    username VARCHAR(50) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- インデックス作成
-- products テーブル
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_deleted ON products(deleted_at);

-- stock_transactions テーブル
CREATE INDEX IF NOT EXISTS idx_stock_trans_date ON stock_transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_stock_trans_type ON stock_transactions(transaction_type);

-- users テーブル
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);

-- user_roles テーブル
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);

-- audit_logs テーブル
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_table ON audit_logs(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_record ON audit_logs(record_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_date ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_composite ON audit_logs(table_name, record_id, created_at);

-- 初期データ投入
-- ロールマスタ
INSERT INTO roles (id, role_name, description) VALUES
(1, 'ROLE_USER', '一般ユーザー'),
(2, 'ROLE_ADMIN', '管理者')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- システム設定
INSERT INTO system_settings (setting_key, setting_value, description, data_type) VALUES
('stock_warning_threshold', '20', '在庫不足閾値', 'INTEGER'),
('stock_critical_threshold', '0', '在庫切れ閾値', 'INTEGER'),
('session_timeout_minutes', '30', 'セッションタイムアウト（分）', 'INTEGER'),
('max_login_attempts', '5', '最大ログイン試行回数', 'INTEGER')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
