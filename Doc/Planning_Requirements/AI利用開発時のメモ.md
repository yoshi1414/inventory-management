
#ER図の設計について
 GitHub Copilotで以下コマンドで作成する。
 ーーー
 draw.io (diagrams.net) に直接インポートできる形式として、
 Mermaid（マーメイド） 記法でコードを作成してください。

 そして、コピー内容をdraw.ioへペースト
 draw.io画面上でイメージ図の出力で出力OK

#データベース設計書について
 ClaudeのWebにて、以下プロントと設計書.mdを添付して作成してもらいます
 エクセル形式でDB設計書を作成してください

ーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーー
＜＜＜プロンプト＞＞＞

#依頼
spring securityを利用したログイン処理を作成してください。
html作成はしないでください。
管理者ログイン処理の作成はしないでください。

＃作成したいログイン処理
controller
エンティテイ
サービス
リポジトリ
UserDetails

＃参考情報
参考処理として、以下を保存しておりますが、こちらをもとに修正してください。
/src/main/java/com/inventory/inventory_management/security/UserDetailsImpl.java)
/src/main/java/com/inventory/inventory_management/security/UserDetailsServiceImpl.java

それでは、作成してください
ーーーーーーーーーーー
＃管理者用のホーム画面へ表示したい内容
・総会員数
・無料会員数
・有料会員数
・店舗数
・総予約数
・ユーザからの月間売り上げ

＃ログアノテーションを利用
@Slf4j

＃エラーハンドラーは
GlobalExceptionHandlerを利用
