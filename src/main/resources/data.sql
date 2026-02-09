-- テストユーザーデータ
-- パスワード: password (BCryptハッシュ化済み)

INSERT IGNORE INTO users (username, password, email, full_name, is_active) VALUES
('testuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'testuser@example.com', 'テストユーザー', TRUE);

-- テストユーザーに一般ユーザーロールを付与
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'testuser'), (SELECT id FROM roles WHERE role_name = 'ROLE_USER'));