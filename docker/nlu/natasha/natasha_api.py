from __future__ import annotations

import re
from functools import lru_cache
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel

try:
    from natasha import Doc, MorphVocab, NewsEmbedding, NewsMorphTagger, NewsNERTagger, Segmenter
except ImportError:  # pragma: no cover - container build installs natasha
    Doc = None
    MorphVocab = None
    NewsEmbedding = None
    NewsMorphTagger = None
    NewsNERTagger = None
    Segmenter = None


app = FastAPI(title="Astor Butler Natasha NLU", version="0.1.0")


class AnalyzeRequest(BaseModel):
    text: str


class AnalyzeResponse(BaseModel):
    source: str = "natasha"
    slots: list[dict[str, Any]]
    entities: list[dict[str, Any]]
    tokens: list[dict[str, Any]]


@lru_cache(maxsize=1)
def _pipeline() -> tuple[Any, Any, Any, Any, Any] | None:
    if Doc is None:
        return None
    segmenter = Segmenter()
    embedding = NewsEmbedding()
    morph_tagger = NewsMorphTagger(embedding)
    ner_tagger = NewsNERTagger(embedding)
    morph_vocab = MorphVocab()
    return segmenter, morph_tagger, ner_tagger, morph_vocab, Doc


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest) -> AnalyzeResponse:
    text = (request.text or "").strip()
    entities: list[dict[str, Any]] = []
    tokens: list[dict[str, Any]] = []
    slots = rule_slots(text)

    pipeline = _pipeline()
    if text and pipeline is not None:
        segmenter, morph_tagger, ner_tagger, morph_vocab, doc_cls = pipeline
        doc = doc_cls(text)
        doc.segment(segmenter)
        doc.tag_morph(morph_tagger)
        doc.tag_ner(ner_tagger)
        for span in doc.spans:
            span.normalize(morph_vocab)

        entities = [
            {
                "text": span.text,
                "normal": span.normal,
                "type": span.type,
            }
            for span in doc.spans
        ]
        tokens = [
            {
                "text": token.text,
                "pos": token.pos,
                "feats": token.feats,
            }
            for token in doc.tokens
        ]

    return AnalyzeResponse(slots=slots, entities=entities, tokens=tokens)


def rule_slots(text: str) -> list[dict[str, Any]]:
    normalized = text.lower().replace("ё", "е")
    slots: list[dict[str, Any]] = []

    party_size = party_size_from_text(normalized)
    if party_size is not None:
        slots.append({"name": "partySize", "value": str(party_size), "confidence": 0.86})

    if any(marker in normalized for marker in ("тих", "у окна", "винн", "диван", "vip", "вип", "не проход")):
        slots.append({"name": "seatingPreference", "value": normalized, "confidence": 0.76})

    table_match = re.fullmatch(r"(?:стол(?:ик)?\s*)?([1-9]|1\d)", normalized.strip())
    if table_match:
        slots.append({"name": "tableNumber", "value": table_match.group(1), "confidence": 0.84})

    return slots


def party_size_from_text(text: str) -> int | None:
    if any(word in text for word in ("двоих", "двоем", "двоём", "двое", "двух", "вдвоем", "вдвоём")):
        return 2
    if any(word in text for word in ("троих", "трое", "трех", "трёх", "втроем", "втроём")):
        return 3
    if any(word in text for word in ("четверых", "четверо", "четырех", "четырёх", "вчетвером")):
        return 4
    compact = re.search(r"(?:^|\s)на\s+(\d{1,2})\s*(?:x|х|-х|-x)?(?:\s|$)", text)
    if compact:
        return int(compact.group(1))
    guests = re.search(r"(?:^|\s)(\d{1,2})\s*(?:гостей|гостя|человек|персон|чел)", text)
    if guests:
        return int(guests.group(1))
    return None
