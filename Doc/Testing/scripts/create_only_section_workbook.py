#!/usr/bin/env python3
from pathlib import Path
import re
from openpyxl import Workbook

TARGET = {
    '4': '4. 一般ユーザーシナリオ',
    '5': '5. 管理ユーザーシナリオ',
    '6': '6. エラーハンドリングシナリオ',
}


def parse_tables_by_section(lines):
    tables_by_section = {k: [] for k in TARGET.keys()}
    current_section = None
    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.match(r'^(#{1,6})\s*(.*)$', line)
        if m:
            heading = m.group(2).strip()
            for key, title in TARGET.items():
                if heading.startswith(title):
                    current_section = key
                    break
            else:
                # if another heading encountered at same level, leave current_section unchanged
                pass
            i += 1
            continue

        if '|' in line:
            if i+1 < len(lines) and re.search(r'^[\s\|:\-]+$', lines[i+1].replace(' ', '')):
                header_line = line
                i += 2
                rows = []
                while i < len(lines) and '|' in lines[i]:
                    if re.match(r'^\s*$', lines[i]):
                        break
                    rows.append(lines[i])
                    i += 1

                def split_row(r):
                    r = r.strip()
                    if r.startswith('|') and r.endswith('|'):
                        r = r[1:-1]
                    parts = [c.strip() for c in r.split('|')]
                    return parts

                header_cells = split_row(header_line)
                data = [split_row(r) for r in rows if r.strip()]
                if current_section in tables_by_section:
                    tables_by_section[current_section].append((header_cells, data))
                continue
        i += 1
    return tables_by_section


def create_workbook(md_path: Path, out_path: Path):
    text = md_path.read_text(encoding='utf-8')
    lines = text.splitlines()
    tables_by_section = parse_tables_by_section(lines)

    wb = Workbook()
    # remove default sheet
    default = wb.active
    wb.remove(default)

    for key, title in TARGET.items():
        ws = wb.create_sheet(title=title[:31])
        ws.append([title])
        ws.append([''])
        tables = tables_by_section.get(key, [])
        if not tables:
            ws.append(['(No tables found in this section)'])
            continue
        for idx, (header, rows) in enumerate(tables, start=1):
            ws.append([f'Table {idx}'])
            ws.append(header)
            for r in rows:
                ws.append(r)
            ws.append([''])

    wb.save(out_path)
    print(f'Created workbook with sheets: {list(TARGET.values())} -> {out_path}')


if __name__ == '__main__':
    import sys
    if len(sys.argv) != 3:
        print('Usage: create_only_section_workbook.py input.md output.xlsx')
        raise SystemExit(1)
    md = Path(sys.argv[1])
    out = Path(sys.argv[2])
    if not md.exists():
        print('Input md not found:', md)
        raise SystemExit(1)
    create_workbook(md, out)
