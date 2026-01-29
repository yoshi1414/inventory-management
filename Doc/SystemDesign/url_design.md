# URL設計メモ

## 概要
商品在庫管理システムの画面遷移図に基づいたURL設計です。RESTfulな設計原則に従い、直感的でわかりやすいURL構造を採用しています。

---

## 一般ユーザー画面遷移

### 1. ログイン画面
- **URL**: /login
- **HTTPメソッド**: GET（画面表示）, POST（ログイン処理）
- **説明**: ユーザー認証を行う画面

### 2. メニュー画面
- **URL**: /menu
- **HTTPメソッド**: GET
- **説明**: ログイン後のトップ画面。各機能へのナビゲーション

### 3. 商品・在庫一覧画面
- **URL**: /products
- **HTTPメソッド**: GET
- **説明**: 商品と在庫状況の一覧を表示
- **クエリパラメータ**:
  - category: カテゴリでフィルタリング
  - status: ステータスでフィルタリング
  - page: ページネーション

### 4. 商品詳細画面
- **URL**: /products/{productId}
- **HTTPメソッド**: GET
- **説明**: 特定商品の詳細情報を表示
- **パラメータ**: {productId} - 商品ID（動的パラメータ）

### 5. 入出庫履歴画面
- **URL**: /products/{productId}/transactions
- **HTTPメソッド**: GET
- **説明**: 特定商品の入出庫履歴を時系列で表示
- **パラメータ**: {productId} - 商品ID（動的パラメータ）
- **クエリパラメータ**:
  - type: 入庫/出庫でフィルタリング
  - startDate: 開始日
  - endDate: 終了日

### 6. パスワード変更画面
- **URL**: /users/password
- **HTTPメソッド**: GET（画面表示）, POST（変更処理）
- **説明**: ログインユーザーのパスワード変更

### 7. ログアウト
- **URL**: /logout
- **HTTPメソッド**: POST
- **説明**: ログアウト処理を実行

---

## 管理ユーザー画面遷移

### 1. ログイン画面
- **URL**: /admin/login
- **HTTPメソッド**: GET（画面表示）, POST（ログイン処理）
- **説明**: 管理者認証を行う画面（一般ユーザーと共通）

### 2. メニュー画面
- **URL**: /admin/menu
- **HTTPメソッド**: GET
- **説明**: 管理者用メニュー（権限に応じて表示内容が変わる）

### 3. ユーザー管理画面
#### ユーザー一覧
- **URL**: /admin/users
- **HTTPメソッド**: GET
- **説明**: 登録済みユーザーの一覧を表示

#### ユーザー新規登録
- **URL**: /admin/users/create
- **HTTPメソッド**: GET（画面表示）, POST（登録処理）
- **説明**: 新規ユーザーを登録

#### ユーザー編集
- **URL**: /admin/users/{userId}/edit
- **HTTPメソッド**: GET（画面表示）, PUT（更新処理）
- **説明**: ユーザー情報を編集
- **パラメータ**: {userId} - ユーザーID（動的パラメータ）

#### ユーザー削除
- **URL**: /admin/users/{userId}/delete
- **HTTPメソッド**: POST, DELETE
- **説明**: ユーザーを論理削除
- **パラメータ**: {userId} - ユーザーID（動的パラメータ）

### 4. 商品登録/変更画面
#### 商品一覧（管理者用）
- **URL**: /admin/products
- **HTTPメソッド**: GET
- **説明**: 商品一覧を表示（編集・削除ボタン付き）

#### 商品新規登録
- **URL**: /admin/products/create
- **HTTPメソッド**: GET（画面表示）, POST（登録処理）
- **説明**: 新規商品を登録

#### 商品編集
- **URL**: /admin/products/{productId}/edit
- **HTTPメソッド**: GET（画面表示）, PUT（更新処理）
- **説明**: 商品情報を編集
- **パラメータ**: {productId} - 商品ID（動的パラメータ）

#### 商品削除
- **URL**: /admin/products/{productId}/delete
- **HTTPメソッド**: POST, DELETE
- **説明**: 商品を論理削除
- **パラメータ**: {productId} - 商品ID（動的パラメータ）

### 5. 在庫管理画面
#### 在庫管理トップ
- **URL**: /admin/inventory
- **HTTPメソッド**: GET
- **説明**: 在庫管理のメイン画面

#### 入庫処理
- **URL**: /admin/inventory/in
- **HTTPメソッド**: GET（画面表示）, POST（入庫処理）
- **説明**: 商品の入庫を登録

#### 出庫処理
- **URL**: /admin/inventory/out
- **HTTPメソッド**: GET（画面表示）, POST（出庫処理）
- **説明**: 商品の出庫を登録

#### 在庫履歴一覧
- **URL**: /admin/inventory/transactions
- **HTTPメソッド**: GET
- **説明**: すべての入出庫履歴を表示

### 6. ログアウト
- **URL**: /admin/logout
- **HTTPメソッド**: POST
- **説明**: ログアウト処理を実行（一般ユーザーと共通）

---

## URL設計の原則

### RESTful設計
- リソース（商品、ユーザー、在庫）をURLで表現
- HTTPメソッドで操作を表現（GET: 取得, POST: 作成, PUT: 更新, DELETE: 削除）
- 階層構造を適切に使用（例: /products/{productId}/transactions）

### 命名規則
- 小文字とハイフンを使用（例: /users/password）
- 複数形でリソースを表現（例: /products, /users）
- 動詞は最小限に（RESTful原則に従う）

### セキュリティ考慮事項
- 認証が必要な画面は Spring Security で保護
- 管理者専用画面（/admin/**）は権限チェックを実施
- CSRF トークンによる保護

---

## 補足: 追加機能URL（第2フェーズ）

### CSVインポート/エクスポート
- **インポート**: /products/import（POST）
- **エクスポート**: /products/export（GET）

### レポート・分析機能
- **在庫回転率**: /reports/inventory-turnover（GET）
- **売上分析**: /reports/sales-analysis（GET）

### API エンドポイント（将来的な拡張）
- **商品API**: /api/v1/products
- **在庫API**: /api/v1/inventory
- **ユーザーAPI**: /api/v1/users
