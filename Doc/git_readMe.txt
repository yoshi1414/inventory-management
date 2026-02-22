ーーーーーーーーーーーーーーーーーーーーーーーーーーー
#gitのローカル情報を確認
git config --local --list

#gitのローカル設定
git config --local user.name yoshi1414
git config --local user.email yoshi20251403@gmail.com

#gitのローカル初期化
git init

ーーーーーーーーーーーーーーーーーーーーーーーーーー
#sshキー作成
ssh-keygen -t rsa -f C:\Users\Iyosh\.ssh\id_rsa_new_project

#sshキーをクリップボードにコピー
clip < C:\Users\Iyosh\.ssh\id_rsa_new_project.pub

#以下の内容を C:\Users\Iyosh\.ssh\config に追加してください（ファイルが存在しない場合は新規作成）:
Host github.com
    HostName github.com
    User git
    IdentityFile C:\Users\Iyosh\.ssh\id_rsa_new_project

#リモート接続確認（ssh）
ssh -T git@github.com

ーーーーーーーーーー
#リモートリポジトリの設定（SSH）⇒gitHub画面のコード欄で確認する
 git remote set-url origin git@github.com:yoshi1414/inventory-management.git

#リモートリポジトリの確認
git remote -v

ーーーーーーーーーーーーーーーーーーーーーーーーーー
#リモートリポジトリの初回Push
git pull origin main
 or
#リモートリポジトリの初回clone(Dirも作られる⇒作りたくない場合は、pullする)
git clone git@github.com:yoshi1414/inventory-management.git

ーーーーーーーーーーーーーーーーーーーーーーーーーー
#Gitブランチについて
##要件定義ブランチ
git branch requirements-definition-1

##設計ブランチ（画面）
git branch system-design-view-1

##設計ブランチ（DB）
git branch system-design-db-1

##開発ブランチ
git checkout -b feature/login-screen

##開発ブランチ 在庫
git checkout -b feature/inventory

##そのほかブランチ
git checkout -b feature/other

##開発ブランチ 在庫(管理者)
git checkout -b feature/AdminInventory

##開発ブランチ 商品(管理者)
git checkout -b feature/product

##開発ブランチ 在庫(管理者)
git checkout -b feature/AdminUsers

##テストブランチ 在庫
git checkout -b test/system_test_execution

ーーーーーーーーーーーーー
##作成しながら、ブランチ切り替え
git checkout -b requirements-definition-1
　or
##Gitブランチ切り替え
git checkout requirements-definition-1

##現在のGitブランチ確認
git branch

##Gitブランチをリモートへ追加
git push -u origin requirements-definition-1

--------------------------------------------
#ローカルブランチの削除手順
##現在のブランチを確認
#削除対象のブランチでないことを確認する
#削除対象の場合、mainブランチに切り替える

git checkout main

##ローカルブランチを削除
#マージ済みのブランチを削除する

git branch -d requirements-definition-1
git branch -d requirements-definition-2

##削除後、ローカルブランチ一覧を確認

git branch

#強制削除が必要な場合（マージされていないブランチ）
#データが失われる可能性があるため注意

git branch -D requirements-definition-1
git branch -D requirements-definition-2

----
##リモートブランチを削除
1.リモートブランチ確認
git branch -r

2.リモートブランチの削除 (削除名には、/originは不要)
git push origin --delete branchName

3.ローカルとリモートのブランチを合わせる
※ローカルブランチ削除になるので、注意
git fetch --prune

4.リモートリポジトリの最新状態を取得し、ローカルで追跡している削除済みのリモートブランチを整理します。
git branch -vv
ローカルブランチとリモートブランチの対応状況を表示します。
gone と表示されるブランチは、リモートで削除されているがローカルに残っているものです。

----
##リモートブランチからファイルを強制上書きする（★★コンフリクトのとき）
1.このコマンドは、リモートリポジトリ（origin）から最新の変更を取得します。
ただし、ローカルのブランチには影響を与えません（マージやリベースは行われません）。
git fetch origin

2.このコマンドは、リモートブランチ origin/main にある git_readMe.txt ファイルをローカルに上書きします。
git_readMe.txt がリモートの origin/main ブランチにある内容で上書きされました。
git checkout origin/main -- Doc/git_readMe.txt

ーーーーーーーーーーーーーーーーーーーーーーーーーー
mainへマージあとに、ローカルリポジトリを最新にするには
##mainブランチへ切り替え
git checkout main

##リモートリポジトリの最新情報を取得（originはリモートの最新:git remote -v で確認できる）
git fetch origin

##リモートの main をローカルの main にマージ
##リモート追跡ブランチ（origin/main）の最新状態をローカルの main ブランチにマージします。
git merge origin/main

以上で最新化が可能です。
コンフリクトが起きると、マージできないです。
ローカルのみのファイルは消されないが、一時退避「git stash」やローカルコミット「git add とgit commit」をしておくとよい
　　　　↓　以下がstushありの例
ーーーーーーーーーーーーーーーーーーーーーーーーーー
#ローカルファイルの保持
# 1. 現在の変更を確認
git status

# 2. 現在の変更を一時退避
git stash

# 3. mainブランチへ切り替え
git checkout main

# 4. リモートリポジトリの最新情報を取得
git fetch origin

# 5. リモートの main をローカルの main にマージ
git merge origin/main

# 6. 一時退避した変更を戻す
git stash pop

--------
補足
・git stash list を実行して、スタッシュがまだ存在しているか確認できます。
・git stash drop で手動で削除できます
・git stash clear ですべてのスタッシュを削除ができます

ーーーーーーーーーーーーーーーーーーーーーーーーーー
ブランチ内容がすべて取り込まれているかの確認
1.現在のブランチを確認
git branch
  feature/inventory
* main

2.mainと他ブランチを比較
git log main..feature/inventory
※結果がなにも返ってこなければ、取り込みが完了している

ーーーーーーーーーーーーー
以下でリモートブランチの最新情報をローカルに反映します。
ローカルブランチのソース削除などはされないです。

git fetch --prune
リモートリポジトリの最新状態を取得し、ローカルで追跡している削除済みのリモートブランチを整理します。

git branch -vv
ローカルブランチとリモートブランチの対応状況を表示します。
gone と表示されるブランチは、リモートで削除されているがローカルに残っているものです。

ーーーーーーーーーーーーー
下記はコミットコメント例です。
管理者用在庫管理画面および商品管理関連設計書を追加 #10
- 管理者用在庫管理画面設計書を作成
- 商品管理画面設計書を作成
- 商品詳細画面設計書を作成

コミットコメント例のようにして、添付内容のコミットコメントを考えて
ーーーーーーーーーーーーー

ログアウト機能実装とテスト、エラー画面制御、システムテスト仕様書作成 

- CustomLogoutSuccessHandlerを実装し、ロール情報に基づいたリダイレクト処理を追加
- GlobalExceptionHandlerでHTTPエラー（404/403/400/500）を統一的に処理
- error.htmlエラーテンプレートを作成（ユーザー/管理者兼用）
- ログアウト成功ハンドラーの単体テスト（CustomLogoutSuccessHandlerTest）を実装
- グローバル例外ハンドラーの統合テスト（GlobalExceptionHandlerTest）を実装
- 管理者テンプレートのログアウトモーダル検証テスト（AdminLogoutTemplateTest）を実装
- システムテスト仕様書を作成（一般ユーザー・管理ユーザーシナリオ・エラーハンドリング）
- 管理者用ユーザ管理機能の追加（一覧・登録・更新・削除）
