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
# 1. 現在の変更を一時退避
git stash

# 2. mainブランチへ切り替え
git checkout main

# 3. リモートリポジトリの最新情報を取得
git fetch origin

# 4. リモートの main をローカルの main にマージ
git merge origin/main

# 5. 一時退避した変更を戻す
git stash pop

--------
補足
・git stash list を実行して、スタッシュがまだ存在しているか確認できます。
・git stash drop で手動で削除できます
・git stash clear ですべてのスタッシュを削除ができます

ーーーーーーーーーーーーーーーーーーーーーーーーーー
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

ーーーーーーーーーーーーーーーーーーーーーーーーーー
ローカル環境にマージするには
1.取り込み
git fetch

2.main ブランチに切り替え
git checkout main

3.リモートの main をローカルの main にマージ
git merge origin/main

ーーーーーーーーーーーーー
コミットコメント例
管理者用在庫管理画面および商品管理関連設計書を追加 #10
- 管理者用在庫管理画面設計書を作成
- 商品管理画面設計書を作成
- 商品詳細画面設計書を作成

コミットコメント例を参考にして、添付内容のコミットコメントを考えて
ーーーーーーーーーーーーー



