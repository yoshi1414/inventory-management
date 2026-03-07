# Herokuへアプリをデプロイする準備（まとめ）

## 1. 本番環境用設定ファイルの作成
開発環境では `application.properties` や `application-test.properties` にデータベース接続情報やメールサーバー情報
などの**機密情報（パスワードなど）**を記載している。
しかし、機密情報を記載したファイルを本番環境に置くのはセキュリティ上厳禁であるため、本番環境では別の設定方法を利用する。

### Herokuの環境変数
Herokuでは以下の仕組みが利用できる。
- Herokuには機密情報を環境変数として設定できる機能がある

そのため、本番環境用に以下のファイルを作成する。

#### `application-production.properties`
例：
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.mail.host=${MAILGUN_SMTP_SERVER}
spring.mail.port=${MAILGUN_SMTP_PORT}

stripe.api-key=${STRIPE_API_KEY}
```

`${}` の形式で Herokuの環境変数を参照するように設定する。

---

## 2. 開発用設定ファイルをGit管理対象外にする

開発環境用の設定ファイルには機密情報が含まれるため、GitHubへアップロードしないように設定する。

### `.gitignore`の役割
`.gitignore`ファイル内に記述したファイルはGitの管理対象外になる（本番環境にアップロードされない）

`.gitignore` に以下を追加する。
```
/src/main/resources/application.properties
/src/main/resources/application-test.properties
```
これにより、
- DBパスワード
- メール認証情報

などの機密情報がGitHubに公開されることを防ぐ。

---

## 3. Procfileの作成

Herokuでは、アプリ起動時のコマンドを指定する仕組みがある。

Herokuでは、`Procfile`という名前のファイルをプロジェクトのルートディレクトリに作成し、その中にコマンドを記述することで、アプリの起動時にそのコマンドが実行される

#### `Procfile`
```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar -Dspring.profiles.active=production target/*.jar
productionは、適宜変更すること

```
この設定により
- `spring.profiles.active=production`
が有効になり、
- `application-production.properties`
が本番環境で使用される。

---

## 4. Javaバージョンの指定

ローカル環境と本番環境でJavaバージョンが違うと、アプリが正常に動作しない可能性がある。

そのため、Heroku側のJavaバージョンを指定する。

Herokuでは、`system.properties`という名前のファイルをプロジェクトのルートディレクトリに作成することで、Javaのバージョンを指定できる

#### `system.properties`
```
java.runtime.version=21
```

---

## 5. デプロイ前の動作確認

Herokuにデプロイする前に、以下を確認する。
- アプリの動作確認
- テストコードの実行

すべて正常に動作することを確認してからデプロイを行う。

---

## 6. Heroku作業

### ログイン

Herokuにログイン

```bash
heroku login -i
```

※パスワードは、APIキーをWEB画面のHerokuから取得する

### アプリ

アプリ名設定

```bash
heroku apps:create inventory
```

### DB設定について

DBアドオンを設定

```bash
heroku addons:create jawsdb-maria:kitefin
```

herokuのDB情報（コマンド：`heroku config:get JAWSDB_MARIA_URL`）

```
mysql://XXXXXXXXX:AAAAAAAA@er7lx9km02rjyf3n.cbetxkdyhwsb.us-east-1.rds.amazonaws.com:3306/mrgwzv3mt1zoj6f9
↓
mysql://ユーザー名:パスワード@ホスト名:3306/データベース名

ユーザー名: XXXXXXXXX
パスワード: AAAAAAAA
ホスト名: er7lx9km02rjyf3n.cbetxkdyhwsb.us-east-1.rds.amazonaws.com:3306/mrgwzv3mt1zoj6f9
```

### Heroku上に環境変数を設定する（heroku CLI で設定）

DB_URL を設定

```bash
heroku config:set DB_URL=jdbc:mysql://er7lx9km02rjyf3n.cbetxkdyhwsb.us-east-1.rds.amazonaws.com:3306/mrgwzv3mt1zoj6f9
```

DB_USERNAME を設定

```bash
heroku config:set DB_USERNAME=XXXXXXXXX
```

DB_PASSWORD を設定

```bash
heroku config:set DB_PASSWORD=AAAAAAAA
```

サーバ停止
```bash
heroku ps:scale web=0 -a inventory
```

現在の Dyno の状態を確認
```bash
heroku ps -a
```

アプリを再開（Dyno を 1 にスケール）
```bash
heroku ps:scale web=1 -a inventory
```

ログ出力
```bash
 heroku logs --tail
```

## まとめ

Herokuへデプロイする前には以下の準備が必要である。
- 本番環境用の `application-production.properties` を作成
- 機密情報は Herokuの環境変数で管理
- `.gitignore` により開発用設定ファイルをGit管理対象外にする
- `Procfile` を作成し、アプリ起動コマンドを定義
- `system.properties` を作成してJavaバージョンを指定
- HerokuにCLIでログインして、諸情報を設定

