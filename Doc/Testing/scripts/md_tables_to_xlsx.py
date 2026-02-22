#!/usr/bin/env python3
"""
Markdown 内のテーブル（| 区切りの Markdown テーブル）を抽出して
Excel ファイル（複数シート）へ出力するスクリプト。

使い方:
  python md_tables_to_xlsx.py input.md output.xlsx

 - 各テーブルは直近の見出し（#, ##, ...）をシート名の元にする
 - 見出しが無い場合は Sheet1, Sheet2... とする
"""
import argparse
import re
from pathlib import Path
from collections import defaultdict
import pandas as pd


def sanitize_sheet_name(name: str) -> str:
    # Excel シート名は 31 文字以内、禁止文字: : \/?*[]
    name = re.sub(r'[:\\/?*\[\]]', '_', name)
    name = name.strip()
    if len(name) == 0:
        return 'Sheet'
    return name[:31]


def parse_markdown_tables(lines):
    tables = []  # list of (heading, header_cells, list[row_cells])
    last_heading = None
    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.match(r'^(#{1,6})\s*(.*)$', line)
        if m:
            last_heading = m.group(2).strip()
            i += 1
            continue

        # detect table header (a line containing |) and following separator like |---|
        if '|' in line:
            # lookahead for separator line
            if i+1 < len(lines) and re.search(r'^[\s\|:-]+$', lines[i+1].replace(' ', '')):
                header_line = line
                sep_line = lines[i+1]
                rows = []
                i += 2
                # collect rows
                while i < len(lines) and '|' in lines[i]:
                    # stop if next is a heading or empty line without pipe
                    if re.match(r'^\s*$', lines[i]):
                        break
                    rows.append(lines[i])
                    i += 1

                # parse cells
                def split_row(r):
                    # strip leading/trailing | then split
                    r = r.strip()
                    if r.startswith('|') and r.endswith('|'):
                        r = r[1:-1]
                    parts = [c.strip() for c in r.split('|')]
                    return parts

                header_cells = split_row(header_line)
                data = [split_row(r) for r in rows if r.strip()]
                tables.append((last_heading or 'Table', header_cells, data))
                continue

        i += 1

    return tables


def tables_to_excel(tables, out_path: Path):
    # group tables by heading; create sheet per table (append index if multiple)
    sheets = defaultdict(list)
    for heading, header, rows in tables:
        sheets[heading].append((header, rows))

    with pd.ExcelWriter(out_path, engine='openpyxl') as writer:
        for heading, table_list in sheets.items():
            for idx, (header, rows) in enumerate(table_list, start=1):
                sheet_name = heading if len(table_list) == 1 else f"{heading}_{idx}"
                sheet_name = sanitize_sheet_name(sheet_name)
                # ensure all rows have same length as header
                max_cols = max(len(header), *(len(r) for r in rows) if rows else [0])
                def pad(row):
                    return list(row) + [''] * (max_cols - len(row))

                df_rows = [pad(header)] + [pad(r) for r in rows]
                df = pd.DataFrame(df_rows)
                # set first row as header
                df.columns = df.iloc[0]
                df = df[1:]
                # write
                df.to_excel(writer, sheet_name=sheet_name, index=False)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='input markdown file')
    parser.add_argument('output', help='output xlsx file')
    args = parser.parse_args()

    inp = Path(args.input)
    out = Path(args.output)
    if not inp.exists():
        print(f"Input file not found: {inp}")
        raise SystemExit(1)

    text = inp.read_text(encoding='utf-8')
    lines = text.splitlines()
    tables = parse_markdown_tables(lines)
    if not tables:
        print('No markdown tables found.')
        # create an empty workbook with a single sheet containing the whole text
        df = pd.DataFrame({'content': lines})
        with pd.ExcelWriter(out, engine='openpyxl') as writer:
            df.to_excel(writer, sheet_name='Content', index=False)
        print(f'Wrote full content to {out}')
        return

    tables_to_excel(tables, out)
    print(f'Wrote {len(tables)} tables to {out}')


if __name__ == '__main__':
    main()
