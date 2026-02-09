# AGENTS.md

**Rule:** In each command, **define → use**. Do **not** escape `$`. Use generic `'path/to/file.ext'`.

---

## 1) READ (UTF‑8 no BOM, line‑numbered)

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

---

## 2) WRITE (UTF‑8 no BOM, atomic replace, backup)

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

## このドキュメントの目的
- 共同作業するエージェントが基本ルールを素早く参照できるよう整理する
- コマンド実行時の前提と注意点を一箇所に集約する

## クイックリファレンス
|項目|内容|
|---|---|
|共通エンコーディング|UTF-8 (BOMなし)|
|基本手順|変数を定義してから使用する|
|ファイルパス記法|常に 'path/to/file.ext' を使う|
|注意事項|$ のエスケープは禁止、cd の多用は避ける|
|応答言語|ユーザー指示に従い、原則すべて日本語で回答する|

## ワークフローヒント
- まず対象ファイルを読み、前提を把握してから作業計画を立てる
- 編集前後で差分を確認し、変更点を簡潔に説明できるようにする
- 実行したコマンドは再利用できる形で記録する
- 回答は既定で日本語とし、他言語が必要な場合は事前に確認する
- 作業中のWorkingメッセージも日本語で表示し、進捗を明確に伝える
- テストや動作確認が行えない場合は理由と推奨手順を明示する
- 回答の最後に、対応した差分を必ず記載する（ファイルと変更内容を簡潔に示す）
- 差分説明では、変更ソースファイル名と変更行をセットで記載する（例: src/example/File.java:42）

## ハマりポイント
- PowerShell の文字列では `  ` と ' の組み合わせに注意、事前に構造を整理する
- cd ではなく Set-Location -LiteralPath (Convert-Path .) を使う
- Windows 環境ではパスに全角文字が含まれるため、常にエンコーディング指定を行う
- サンドボックス設定が「never」でも破壊的操作は必ずユーザー指示を確認する

## 検証チェックリスト
- 必要なファイルだけ変更されているか git diff で確認したか
- 実行したコマンドと結果を報告に反映できるか
- テストや画面確認の有無を回答に明記したか
- エラーメッセージの文言と表示場所を確認したか、できない場合は代替策を提示したか

## 変更履歴
- 2025-09-30: ドキュメント構成の拡充を追加（Codex）
- 2025-09-30: 回答末尾の差分説明必須ルールを追記（Codex）
- 2025-09-30: 既定の応答言語を日本語とするルールを追記（Codex）
- 2025-09-30: 差分説明にファイル名と行番号の記載ルールを追記（Codex）
- 2025-09-30: Workingメッセージの日本語表示ルールを追記（Codex）