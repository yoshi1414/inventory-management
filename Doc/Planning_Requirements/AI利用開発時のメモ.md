
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

ーーーーーーーーーーーー
#依頼
商品管理画面の作成してください。
products.htmlの静的な記載は、動的な処理へ修正する

＃作成したい処理
controller
エンティテイ
サービス
リポジトリ

＃参考情報
画面イメージとして、products.htmlをもとに作成してください。
controllerの参考として、AdminInventoryControllerを利用

ーーーーーーーーーーーー
#依頼
在庫詳細画面の作成してください。
html作成はしないでください。
管理者の商品詳細画面とは別での作成となります。

＃作成したい処理
controller
エンティテイ
サービス
リポジトリ

＃参考情報
画面イメージとして、product-detail.htmlをもとに作成してください。

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

ーーーーーーーーーーー
#エラーチェックについて
additional-spring-configuration-metadata.json に追記して、VS Code の問題点に表示されないようにした。

#カバレッジテストは、以下で達成バーをだしています
VS Code の Java のカバレッジ表示（Test Runner for Java 系）です。










完全なテスト構成（Web開発 フルセット）
┌─────────────────────────────────────────────────────┐
│ ① 単体テスト（Unit Test）                           
│   ・JUnit ／ Mockito（Java: Service, Repository）   
│   ・Jest（JavaScript: バリデーション, 画面ロジック）  
├─────────────────────────────────────────────────────┤
│ ② APIテスト                                         
│   ・MockMvc（Controller層のHTTPテスト）             
│   ・Postman / REST Assured（APIの入出力検証）       
├─────────────────────────────────────────────────────┤
│ ③ 結合テスト（Integration Test）                    
│   ・SpringBootTest（Controller〜DB全層通し）        
│   ・DBテスト（DBUnit / Testcontainers）             
├─────────────────────────────────────────────────────┤
│ ④ E2Eテスト / UIテスト                              
│   ・Selenium / Playwright / Cypress                 
│   ・主要シナリオのみ自動化                          
├─────────────────────────────────────────────────────┤
│ ⑤ システムテスト                                    
│   ・手動シナリオテスト（業務フロー全体）            
│   ・テスト仕様書・エビデンス取得                    
├─────────────────────────────────────────────────────┤
│ ⑥ 非機能テスト                                      
│   ・セキュリティテスト（OWASP基準）                 
│   ・性能テスト（JMeter / Gatling）                  
│   ・負荷テスト（同時アクセス確認）                  
├─────────────────────────────────────────────────────┤
│ ⑦ 受け入れテスト（UAT）                             
│   ・ユーザー・発注者による最終確認                  │
│   ・本番環境に近い環境で実施                        
└─────────────────────────────────────────────────────┘
