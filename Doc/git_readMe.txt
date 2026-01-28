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
git branch basic-design-view-1

##設計ブランチ（DB）
git branch basic-design-db-1

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



