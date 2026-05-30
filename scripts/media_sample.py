#!/usr/bin/env python3
"""Create a small local media sample from a full JSONL manifest."""

from __future__ import annotations

import argparse
import json
import shutil
from collections import OrderedDict
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Build a representative sample manifest and optional file copy.")
    parser.add_argument("manifest", help="Full media manifest JSONL.")
    parser.add_argument("--out", default="/private/tmp/astor_media_sample_manifest.jsonl")
    parser.add_argument("--copy-dir", default="")
    parser.add_argument("--limit", type=int, default=10)
    parser.add_argument("--max-size-mb", type=int, default=180)
    args = parser.parse_args()

    rows = read_rows(Path(args.manifest))
    selected = select_rows(rows, args.limit, args.max_size_mb * 1024 * 1024)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as file:
        for row in selected:
            file.write(json.dumps(row, ensure_ascii=False) + "\n")

    if args.copy_dir:
        copy_root = Path(args.copy_dir)
        copy_root.mkdir(parents=True, exist_ok=True)
        for row in selected:
            destination = copy_root / row["relativePath"]
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(row["sourcePath"], destination)

    print(f"manifest={out}")
    print(f"copyDir={args.copy_dir or '-'}")
    print(f"files={len(selected)}")
    print(json.dumps(summary(selected), ensure_ascii=False, indent=2))


def read_rows(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8") as file:
        return [json.loads(line) for line in file if line.strip()]


def select_rows(rows: list[dict], limit: int, max_size_bytes: int) -> list[dict]:
    candidates = [row for row in rows if row["mediaType"] == "video" and row["sizeBytes"] <= max_size_bytes]
    candidates.sort(key=lambda row: (group_key(row), row["sizeBytes"], row["relativePath"]))

    grouped: OrderedDict[str, list[dict]] = OrderedDict()
    for row in candidates:
        grouped.setdefault(group_key(row), []).append(row)

    selected: list[dict] = []
    for group_rows in grouped.values():
        if len(selected) >= limit:
            break
        selected.append(group_rows[0])

    if len(selected) < limit:
        selected_ids = {row["_id"] for row in selected}
        for row in candidates:
            if row["_id"] not in selected_ids:
                selected.append(row)
                selected_ids.add(row["_id"])
            if len(selected) >= limit:
                break

    return selected


def group_key(row: dict) -> str:
    parts = Path(row["relativePath"]).parts
    if len(parts) >= 2:
        return "/".join(parts[:2])
    return parts[0] if parts else "root"


def summary(rows: list[dict]) -> dict:
    return {
        "totalBytes": sum(row["sizeBytes"] for row in rows),
        "items": [
            {
                "relativePath": row["relativePath"],
                "sizeBytes": row["sizeBytes"],
                "objectKey": row["objectKey"],
            }
            for row in rows
        ],
    }


if __name__ == "__main__":
    main()
