# NLU Tools Spike

This spike checks whether external Russian-language tools help Astor Butler normalize noisy guest input before FSM routing.

## Scope

- Duckling: date/time/number extraction for Russian guest phrases.
- Natasha: Russian tokenization, morphology and named entities.
- Spring AI + pgvector: semantic example retrieval from approved intent examples.

## Run

```bash
python3 scripts/spike_russian_nlu_tools.py
```

The report is written to:

```text
target/nlu-spike/duckling-natasha-report.json
```

Optional tools:

```bash
python3 -m pip install natasha
DUCKLING_URL=http://localhost:8000/parse python3 scripts/spike_russian_nlu_tools.py
```

If Duckling is not running or Natasha is not installed, the script marks that part as `skipped`.

## Decision Rule

Use Duckling only if it improves Russian date/time extraction over our golden corpus. Use Natasha only if it gives stable value for names, places, staff references, or noisy STT text. Neither tool replaces FSM; they only produce machine-readable slots before FSM transition checks.

## Runtime Plan

1. Approved phrases live in `intent_examples`.
2. Optional embeddings live in `intent_example_embeddings`.
3. Missed/low-confidence phrases go to `intent_understanding_misses`.
4. Codex expands the golden corpus and re-runs tests before manual Telegram checks.
