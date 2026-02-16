# Inventory Management System - Project Guidelines

**プロジェクト**: Spring Boot 4.0.2 + MySQL 8.0 在庫管理システム  
**開発環境**: Windows 11, JDK 21, Maven

---

## Code Style

### Java
- **ロギング**: 全クラスに `@Slf4j` (Lombok) を使用
  - 例: `AdminProductController.java`, `InventoryService.java`（`src/main/java/com/inventory/inventory_management/`配下）
  - `log.debug()`: パラメータ、検索条件
  - `log.info()`: 重要な処理開始・完了
  - `log.warn()`: バリデーションエラー、404/403
  - `log.error()`: 例外、システムエラー
- **JavaDoc**: 全 public メソッドに記載（`@param`, `@return`, `@throws`）
- **コンストラクタインジェクション**: `@RequiredArgsConstructor` 使用

### JavaScript
- **Vanilla JS**: jQuery不使用、`querySelector`/`querySelectorAll` 使用
- **ファイル分割**: `common.js`（共通機能）, `inventory.js`（画面固有）（`src/main/resources/static/js/`配下）
- **Bootstrap 5.2.3 API**: `bootstrap.Modal` 使用

### HTML/CSS
- **Bootstrap 5.2.3** + **Bootstrap Icons 1.10.0**（CDN）
- **Thymeleaf**: `th:text`, `th:if`, `th:selected` 使用
- **カスタムCSS**: `style.css`（`.navbar-custom`, `.search-panel` など）（`src/main/resources/static/css/`配下）

---

## Architecture

### レイヤー構成
**Controller → Service → Repository** の3層アーキテクチャ

```
Controller (src/.../controller/)
  ↓ @RequestParam 受け取り、Model に結果格納
Service (src/.../service/)
  ↓ @Transactional でビジネスロジック実行
Repository (src/.../repository/)
  ↓ JpaRepository + @Query (JPQL)
Entity (src/.../entity/)
```

**例**: 在庫管理機能  
- `InventoryController.java` → `InventoryService.java` → `ProductRepository.java`  
  （`src/main/java/com/inventory/inventory_management/`配下のcontroller/service/repositoryパッケージ）

### エラーハンドリング
- `GlobalExceptionHandler.java`（`src/main/java/com/inventory/inventory_management/exception/`配下）: `@ControllerAdvice` でグローバルエラー処理
- `IllegalArgumentException`（400）、`AccessDeniedException`（403）、`NoResourceFoundException`（404）、`Exception`（500）

---

## Build and Test

### ビルド・実行
```bash
mvn clean install           # ビルド
mvn spring-boot:run         # 開発サーバー起動（dev プロファイル）
mvn test                     # テスト実行
```

### テスト規約
- **`@SpringBootTest`** 必須（`@DataJpaTest` 禁止）
- **`@ActiveProfiles("test")`**: H2 インメモリDB（MySQL互換モード）使用
- **MockMvc**: `InventoryControllerTest.java`（`src/test/java/com/inventory/inventory_management/controller/`配下）
- **Mockito**: `InventoryServiceTest.java`（`src/test/java/com/inventory/inventory_management/service/`配下）
- **テストデータ**: `schema-test.sql`, `data-test.sql`（`src/test/resources/`配下）

---

## Project Conventions

### 命名規則
- **Controller**: `AdminProductController`, `InventoryController`
- **Service**: `InventoryService`, `UserService`
- **Repository**: `ProductRepository`, `UserRepository`
- **Entity**: `Product`, `User`, `StockTransaction`

### パッケージ構造
```
com.inventory.inventory_management/
├── config/          # Spring Security, CSRF設定
├── controller/      # @Controller
├── entity/          # @Entity（DTOなし、Entity直接使用）
├── exception/       # GlobalExceptionHandler
├── repository/      # JpaRepository
├── security/        # UserDetailsServiceImpl, LoginAttemptService
└── service/         # @Service, @Transactional
```

### Validation
- **Bean Validation**: `@Column(nullable = false)`, `@Column(unique = true)`
- **Optional**: Repository戻り値に使用（例: `Optional<User> findByUsername(String username)`）
- **Service層**: `orElseThrow()` でnullチェック

---

## Integration Points

### データベース
- **MySQL 8.0**: `application.properties`（`src/main/resources/`配下）
  ```properties
  spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/springboot_inventory
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.open-in-view=false
  ```
- **プロファイル**:
  - `dev`: `application-dev.properties`（開発用）
  - `prod`: `application-prod.properties`（本番用）
  - `test`: H2 インメモリDB

### 初期データ
- `schema.sql`: DDL定義
- `data.sql`: サンプルデータ（10253行）  
  （いずれも`src/main/resources/`配下）

---

## Security

### Spring Security設定
**ファイル**: `SecurityConfig.java`（`src/main/java/com/inventory/inventory_management/security/`配下）

- **認可**: `/admin/**` は `ROLE_ADMIN` 必須、それ以外は認証必須
- **ログイン**: Form認証（`/login`, `/admin/login`）
- **CSRF**: `CsrfConfig.java`（`src/main/java/com/inventory/inventory_management/config/`配下）でカスタム設定
- **XSS対策**: Content Security Policy 設定済み
- **パスワード**: BCrypt強度12（`BCryptPasswordEncoder(12)`）
- **ブルートフォース対策**: `LoginAttemptService.java`（`src/main/java/com/inventory/inventory_management/security/`配下）（最大5回、24時間リセット）
- **Remember-Me**: 24時間有効（prod: secure cookie、dev: 通常cookie）

### 本番環境チェック
- `ProdRequiredPropertiesValidator.java`（`src/main/java/com/inventory/inventory_management/config/`配下）: 必須プロパティ未設定時に起動停止

---

## PowerShell コマンドテンプレート

### 1) READ (UTF‑8 no BOM, line‑numbered)
```bash
bash -lc 'powershell -NoLogo -Command "
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false);
Set-Location -LiteralPath (Convert-Path .);
function Get-Lines { param([string]$Path,[int]$Skip=0,[int]$First=40)
  $enc=[Text.UTF8Encoding]::new($false)
  $text=[IO.File]::ReadAllText($Path,$enc)
  if($text.Length -gt 0 -and $text[0] -eq [char]0xFEFF){ $text=$text.Substring(1) }
  $ls=$text -split \"`r?`n\"
  for($i=$Skip; $i -lt [Math]::Min($Skip+$First,$ls.Length); $i++){ \"{0:D4}: {1}\" -f ($i+1), $ls[$i] }
}
Get-Lines -Path \"path/to/file.ext\" -First 120 -Skip 0
"'
```

### 2) WRITE (UTF‑8 no BOM, atomic replace, backup)
```bash
bash -lc 'powershell -NoLogo -Command "
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false);
Set-Location -LiteralPath (Convert-Path .);
function Write-Utf8NoBom { param([string]$Path,[string]$Content)
  $dir = Split-Path -Parent $Path
  if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
  }
  $tmp = [IO.Path]::GetTempFileName()
  try {
    $enc = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllText($tmp,$Content,$enc)
    Move-Item $tmp $Path -Force
  }
  finally {
    if (Test-Path $tmp) {
      Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    }
  }
}
$file = "path/to/your_file.ext"
$enc  = [Text.UTF8Encoding]::new($false)
$old  = (Test-Path $file) ? ([IO.File]::ReadAllText($file,$enc)) : ''
Write-Utf8NoBom -Path $file -Content ($old+"`nYOUR_TEXT_HERE`n")
"'
```

---

## コーディングルール

- **すべて日本語で回答する**
- **UTF-8 (BOMなし)** エンコーディング必須
- **変数を定義してから使用**（`$` のエスケープ禁止）
- **JavaDocコメント**: 全publicメソッドに必須（`@param`, `@return`, `@throws`）
- **デバッグログ**: `@Slf4j` でloggerを使用。`log.debug()`, `log.info()`, `log.warn()`, `log.error()` を適切に使分け
- **セキュリティチェック**: 入力値検証、SQLインジェクション対策、XSS/CSRF対策を実施
- **戻り値**: `Optional<T>` を使用して null 安全性を確保。`orElseThrow()` でnullチェック
- **エラーハンドラー**: `GlobalExceptionHandler.java` で例外を一括処理
- **HTMLにJSを直接記載しない**: `src/main/resources/static/js/` ディレクトリに集約
- **HTMLにCSSを記載しない**: `src/main/resources/static/css/` ディレクトリに集約
- **Bootstrap優先**: カスタムCSSは必要な場合のみ
- **責務分離**: Controller（リクエスト処理）→ Service（ビジネスロジック）→ Repository（データアクセス）に明確に分離

## テストルール

### テスト配置
- **Unit Test** (単一メソッド・ビジネスロジックのテスト): 
  - 配置: `src/test/java/com/inventory/inventory_management/service/`、`repository/` など
  - 例: `InventoryServiceTest.java`（Mockito 使用）
  
- **Integration Test** (Controller → DB のエンドツーエンドテスト): 
  - **必須配置**: `src/test/java/com/inventory/inventory_management/integration/` に配置
  - 例: `InventoryIntegrationTest.java`, `PasswordChangeControllerIntegrationTest.java`
  - 特徴：
    - `@SpringBootTest` で実際のアプリケーションコンテキストを起動
    - `@Transactional` でテスト終了後に自動ロールバック
    - MockMvc で HTTP リクエスト・レスポンスを検証
    - Repository で DB の実際の状態も検証（DB assertion）

### アノテーション
- **`@SpringBootTest`** 必須（`@DataJpaTest` 禁止）
- **`@ActiveProfiles("test")`**: H2 インメモリDB を使用
- **テスト網羅性**: 機能ごとにユニットテスト・統合テストを作成し、テスト網羅率を確認
- **テスト網羅率**: 80％以上を目標とする（Statement Coverage）
- **テスト作成時の報告**: テスト作成時には、テスト网羅率を連絡すること（テスト前・テスト後の网羅率、改善度を含む）
  - **報告項目**:
    - テスト前の网羅率（パーセンテージと合計テスト数）
    - テスト後の网羅率（パーセンテージと合計テスト数）
    - 改善度（増加テスト数とパーセント改善値）
    - 追加されたテストメソッド一覧
    - テスト対象のコンポーネント・機能
- **問題解決時**: テスト失敗時は問題点・解決内容・修正内容を詳細に記録・報告
- **MockMvc/Mockito**: ControllerはMockMvc、Serviceはトークンを使用してテスト

### テスト网羅率80%に満たない場合の例外

テスト网羅率が80%以下でも許容される場合は、以下の理由を文書化すること：

1. **テストデータの複雑性**: 統合テストの事前準備（データベース初期化、テストユーザー作成）に多くの手作業が必要な場合
2. **外部依存の多さ**: API呼び出し、メール送信、ファイルシステムアクセスなど、環境に依存するコンポーネント
3. **UI/フロントエンド関連**: JavaScriptのDOM操作やブラウザの自動テストが実装困難な場合
4. **セキュリティレイヤー**: CSRF対策、セッション管理など、テストの準備コストが異常に高い場合
5. **既知の制限事項**: 実装言語・フレームワークの制約により、特定の機能がテスト不可能な場合

**文書化の要件**:
- テスト失敗の詳細（エラーメッセージ、失敗箇所）
- 原因分析（上記の5つのカテゴリーのいずれに該当するか）
- 改善計画（将来のリファクタリングで解決可能か、または恒久的に制限されるか）
- 実施日と担当者

### テスト項目削除時の対応

テスト失敗時にテスト項目を削除する場合は、以下を必ず連絡・文書化する：

**削除時の報告要件**:
- **削除理由**: テスト項目が削除される正当な理由（例: 機能廃止、実装不可、テスト環境の制約）
- **削除箇所**: 削除されるテストクラス・テストメソッド名
- **影響範囲**: 削除によって網羅されなくなる機能・パス
- **代替案**: 削除後の代替テスト方法（存在する場合）
- **実施日と担当者**: いつ、誰が削除したか
- **承認状況**: テスト削除を承認したプロジェクトリードの名前


