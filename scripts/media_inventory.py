#!/usr/bin/env python3
"""Scan local media/content folders and build an Astor Butler media manifest."""

from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
from datetime import datetime, timezone
from pathlib import Path


VIDEO_EXTENSIONS = {".mp4", ".webm", ".mov", ".m4v", ".avi", ".mkv"}
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".tiff"}
DOCUMENT_EXTENSIONS = {".pdf", ".docx", ".doc", ".pptx", ".ppt", ".xlsx", ".xls", ".txt", ".md"}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg"}


def main() -> None:
    parser = argparse.ArgumentParser(description="Build media manifest JSONL from a local source directory.")
    parser.add_argument("source_dir", help="Local folder with media/content files, for example a synced Yandex Disk folder.")
    parser.add_argument("--out", default="/private/tmp/astor_media_manifest.jsonl")
    parser.add_argument("--bucket", default="astor-media")
    args = parser.parse_args()

    source_dir = Path(args.source_dir).expanduser().resolve()
    if not source_dir.exists() or not source_dir.is_dir():
        raise SystemExit(f"Source directory does not exist or is not a directory: {source_dir}")

    rows = []
    for path in sorted(source_dir.rglob("*")):
        if not path.is_file() or is_hidden(path, source_dir):
            continue
        relative_path = path.relative_to(source_dir)
        media_type = classify(path)
        if media_type == "unknown":
            continue
        stat = path.stat()
        object_key = object_key_for(media_type, relative_path)
        rows.append({
            "_id": stable_id(str(source_dir), str(relative_path)),
            "sourceRoot": str(source_dir),
            "sourcePath": str(path),
            "relativePath": str(relative_path),
            "bucket": args.bucket,
            "objectKey": object_key,
            "mediaType": media_type,
            "extension": path.suffix.lower(),
            "contentType": mimetypes.guess_type(path.name)[0] or "application/octet-stream",
            "sizeBytes": stat.st_size,
            "sha256": sha256(path),
            "modifiedAt": datetime.fromtimestamp(stat.st_mtime, timezone.utc).isoformat(),
            "scannedAt": datetime.now(timezone.utc).isoformat(),
            "status": "DISCOVERED",
            "tags": tags_for(relative_path, media_type),
        })

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as file:
        for row in rows:
            file.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(f"source={source_dir}")
    print(f"manifest={out}")
    print(f"files={len(rows)}")
    print(json.dumps(summary(rows), ensure_ascii=False, indent=2))


def is_hidden(path: Path, root: Path) -> bool:
    return any(part.startswith(".") for part in path.relative_to(root).parts)


def classify(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in VIDEO_EXTENSIONS:
        return "video"
    if suffix in IMAGE_EXTENSIONS:
        return "image"
    if suffix in DOCUMENT_EXTENSIONS:
        return "document"
    if suffix in AUDIO_EXTENSIONS:
        return "audio"
    return "unknown"


def object_key_for(media_type: str, relative_path: Path) -> str:
    return "/".join(["raw", *relative_path.parts])


def tags_for(relative_path: Path, media_type: str) -> list[str]:
    tags = [media_type]
    tags.extend(part.lower().replace(" ", "_") for part in relative_path.parts[:-1])
    return sorted(set(tag for tag in tags if tag))


def stable_id(source_root: str, relative_path: str) -> str:
    return hashlib.sha256(f"{source_root}:{relative_path}".encode("utf-8")).hexdigest()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def summary(rows: list[dict]) -> dict[str, int]:
    result: dict[str, int] = {}
    for row in rows:
        result[row["mediaType"]] = result.get(row["mediaType"], 0) + 1
    return result


if __name__ == "__main__":
    main()
