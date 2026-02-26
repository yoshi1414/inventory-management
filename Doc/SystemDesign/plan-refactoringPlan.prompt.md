# リファクタリング計画

---

## 🔴 重要度：高

### [A-1] `createSort()` / `buildSort()` の重複（3ファイル）
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`、`service/AdminProductService.java`
- 問題: `createSort()` がほぼ同一内容で3クラスに存在
- 推奨修正: `util/SortUtils.java` に一元化

### [A-2] 在庫フィルター変換ロジックの重複
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`
- 問題: `"low"/"out"/"sufficient"` → `minStock`/`maxStock` 変換の `switch` 文が重複。`AdminInventoryService` 内でもさらに2箇所重複
- 推奨修正: `StockFilterUtils` に切り出す

### [A-3] `getCurrentUserId()` の完全重複
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`
- 問題: `SecurityContextHolder` からユーザー名取得が2箇所で全く同一
- 推奨修正: `SecurityUtils.getCurrentUserId()` に切り出す

### [A-4] カウント・履歴取得ロジックの重複
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`
- 問題: `getLowStockCount()` / `getOutOfStockCount()` / `getStockTransactions()` が両サービスに存在
- 推奨修正: 共通サービスまたは基底クラスに移動

### [A-5] 削除・復元ロジックの重複
- 対象: `service/AdminProductService.java`、`service/AdminInventoryService.java`
- 問題: 商品の論理削除・復元が両サービスに分散
- 推奨修正: `AdminProductService` に一元化し、委譲パターンへ

### [A-6] `createProductDetail` / `updateProductDetail` のフィールドセット重複
- 対象: `service/AdminProductService.java`（同クラス内）
- 問題: 約15行のフィールドセット処理が2メソッドに重複
- 推奨修正: `applyFormToProduct(Product, ProductDetailForm)` に切り出す

### [A-7] CSP ポリシー文字列の重複
- 対象: `security/SecurityConfig.java`（2箇所）
- 問題: 管理者用・一般ユーザー用フィルターチェーンで同一の CSP 文字列がハードコード
- 推奨修正: `private static final String CSP_POLICY = "..."` として定数化

### [D-1] 在庫閾値マジックナンバー `20`
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`、`entity/Product.java`
- 問題: 在庫不足しきい値 `20` が3箇所にハードコード
- 推奨修正: `StockConstants.LOW_STOCK_THRESHOLD = 20` として定数クラスに切り出す

### [D-2] `@Scheduled` のマジックナンバー
- 対象: `security/LoginAttemptService.java`
- 問題: `@Scheduled(fixedRate = 3600000)` の値がハードコード
- 推奨修正: `private static final long CLEANUP_INTERVAL_MS = 3600_000L` として定数化

### [E-1] `findByProductCode` が `null` 返却（Optional未使用）
- 対象: `repository/ProductRepository.java`、`service/AdminProductService.java`
- 問題: `== null` チェックが必要になっており、プロジェクト規約違反
- 推奨修正: `Optional<Product> findByProductCode(String)` に変更

### [H-1] `updateStock` の二重バリデーション
- 対象: `controller/InventoryController.java`
- 問題: Controller と Service の両方に同じ入力チェックが存在
- 推奨修正: `UpdateStockRequest` DTO に Bean Validation を付与し、Controller は `@Valid` のみに統一

### [H-2] `PasswordChangeController` の手動バリデーション
- 対象: `controller/PasswordChangeController.java`
- 問題: null・空文字・パスワード一致チェックが Controller 内に約35行あり、Service にも重複
- 推奨修正: パスワード用 DTO + Bean Validation に変更

---

## 🟡 重要度：中

### [B-1] `InventoryController` に MVC と REST API が混在
- 対象: `controller/InventoryController.java`
- 推奨修正: `InventoryApiController.java` を分離する（`AdminInventoryApiController.java` のパターンを踏襲）

### [B-2] `AdminInventoryService` の責務過多（451行・6責務）
- 対象: `service/AdminInventoryService.java`
- 推奨修正: `StockTransactionService`（履歴管理）を分離し、削除/復元は `AdminProductService` に統合

### [A-8] `SearchCriteriaDto` と `ProductSearchCriteriaDto` が6フィールド重複
- 対象: `dto/request/SearchCriteriaDto.java`、`dto/request/ProductSearchCriteriaDto.java`
- 推奨修正: `SearchCriteriaDto` を基底クラスにし、各画面固有フィールドをサブクラスで追加

### [F-1] `AdminUserService` の `@Transactional` が他クラスと不統一
- 対象: `service/AdminUserService.java`
- 推奨修正: クラスレベルに `@Transactional(readOnly = true)` を付与し、更新系メソッドのみに `@Transactional` を付ける

### [J-1] `@Slf4j` 未使用（規約違反）
- 対象: `service/UserService.java`、`security/SecurityConfig.java`、`security/UserDetailsServiceImpl.java`
- 推奨修正: `private static final Logger logger = ...` を `@Slf4j` + `log.*` に統一

### [J-2] `UserService` の意味のない `try-catch`（再スローのみ）
- 対象: `service/UserService.java`
- 推奨修正: try-catch を削除、`@Transactional(readOnly = true)` のみで十分

### [K-1] `RuntimeException` ラップが `GlobalExceptionHandler` を素通り
- 対象: `service/InventoryService.java`、`service/AdminInventoryService.java`
- 推奨修正: カスタム例外クラス（例: `InventorySearchException`）を作成し、`GlobalExceptionHandler` に対応ハンドラーを追加

### [K-2] DB例外を `return 0` で握りつぶし（障害検知不能）
- 対象: `service/InventoryService.java`（`getLowStockCount()`・`getOutOfStockCount()`）
- 推奨修正: 例外はそのまま上位へ伝播させる

### [H-3] Controller に遷移元判定ロジック混在
- 対象: `controller/AdminInventoryController.java`
- 推奨修正: 専用 DTO に詰めてサービス層で組み立てる

### [L-1] `SecurityConfig` が `@Autowired` フィールドインジェクション（規約違反）
- 対象: `security/SecurityConfig.java`
- 推奨修正: フィールドを `private final` + `@RequiredArgsConstructor` に変更

---

## 🟢 重要度：低

### [L-2] `PasswordEncoderUtil.java` が空ファイル
- 対象: `util/PasswordEncoderUtil.java`
- 推奨修正: 削除するか、`SecurityConfig` から `PasswordEncoder` の定義をこちらに移動して整理

### [L-3] `dto/response/` フォルダが空・REST エンドポイントが `Map<String, Object>` 返却
- 対象: `dto/response/`
- 推奨修正: `UpdateStockResponse.java` 等を作成し、レスポンス構造を型で表現

### [L-4] `form/` パッケージが規約の `dto/request/` と不一致
- 対象: `form/`
- 推奨修正: `form/` 配下のクラスを `dto/request/` に移動し `form/` パッケージを廃止

### [E-2] `@PathVariable` に到達不能な `null` チェック
- 対象: `controller/AdminInventoryController.java`、`controller/InventoryController.java`
- 推奨修正: `id == null` の条件を削除し、`id <= 0` チェックのみ残す

---

## 優先対応の順番（推奨）

1. **[A-3]** `SecurityUtils.getCurrentUserId()` 切り出し — 影響範囲が小さく即実施できる
2. **[A-7]** CSP 定数化 — SecurityConfig 内のみで完結
3. **[D-1]** 在庫閾値の定数化 — バグ防止効果が高い
4. **[A-1]** `SortUtils` 共通化 — 3サービス横断だが独立しており影響範囲が限定的
5. **[J-1]** `@Slf4j` に統一 — 規約統一のみでリスクゼロ
