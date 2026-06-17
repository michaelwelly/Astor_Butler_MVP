#!/usr/bin/env python3
"""Compare Duckling and Natasha on Russian guest phrases.

The script is intentionally optional-dependency friendly:
- if Natasha is not installed, it records the status as skipped;
- if Duckling is not running, it records the status as skipped.
"""

from __future__ import annotations

import json
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone


ROOT = pathlib.Path(__file__).resolve().parents[1]
CORPUS = ROOT / "src/main/resources/understanding/guest-input-golden-corpus.jsonl"
OUT_DIR = ROOT / "target/nlu-spike"
OUT_FILE = OUT_DIR / "duckling-natasha-report.json"
DUCKLING_URL = os.environ.get("DUCKLING_URL", "http://localhost:8000/parse")


def read_corpus() -> list[dict]:
    examples: list[dict] = []
    with CORPUS.open("r", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                examples.append(json.loads(line))
    return examples


def call_duckling(text: str) -> dict:
    payload = urllib.parse.urlencode(
        {
            "locale": "ru_RU",
            "tz": "Asia/Yekaterinburg",
            "text": text,
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        DUCKLING_URL,
        data=payload,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=3) as response:
            return {"status": "ok", "items": json.loads(response.read().decode("utf-8"))}
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        return {"status": "skipped", "reason": str(exc)}


def run_natasha(text: str) -> dict:
    try:
        from natasha import Doc, MorphVocab, NewsEmbedding, NewsMorphTagger, NewsNERTagger, Segmenter
    except ImportError as exc:
        return {"status": "skipped", "reason": str(exc)}

    segmenter = Segmenter()
    emb = NewsEmbedding()
    morph_tagger = NewsMorphTagger(emb)
    ner_tagger = NewsNERTagger(emb)
    morph_vocab = MorphVocab()

    doc = Doc(text)
    doc.segment(segmenter)
    doc.tag_morph(morph_tagger)
    doc.tag_ner(ner_tagger)
    for span in doc.spans:
        span.normalize(morph_vocab)

    return {
        "status": "ok",
        "tokens": [
            {
                "text": token.text,
                "pos": token.pos,
                "feats": token.feats,
            }
            for token in doc.tokens
        ],
        "entities": [
            {
                "text": span.text,
                "normal": span.normal,
                "type": span.type,
            }
            for span in doc.spans
        ],
    }


def main() -> int:
    examples = read_corpus()
    samples = examples + [
        {"state": "TABLE_BOOKING_COLLECT_DATE", "text": "На следующую пятницу вечером", "intent": "PROVIDE_DATE"},
        {"state": "TABLE_BOOKING_COLLECT_TIME", "text": "часов к половине девятого", "intent": "PROVIDE_TIME"},
        {"state": "READY_FOR_DIALOG", "text": "А можно как-нибудь тихо, у окна, и еще винную карту?", "intent": "TABLE_BOOKING"},
    ]

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "ducklingUrl": DUCKLING_URL,
        "corpus": str(CORPUS),
        "items": [],
    }
    for sample in samples:
        text = sample["text"]
        report["items"].append(
            {
                "text": text,
                "state": sample.get("state"),
                "expectedIntent": sample.get("intent"),
                "duckling": call_duckling(text),
                "natasha": run_natasha(text),
            }
        )

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    OUT_FILE.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUT_FILE}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
