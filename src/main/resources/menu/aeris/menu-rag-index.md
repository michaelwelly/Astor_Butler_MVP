# AERIS Menu RAG Index

This file is the lightweight MVP index for menu-aware answers.

Runtime PDF assets:

- `MENU AERIS A4 2026 DIGITAL.pdf` - kitchen / main menu.
- `BAR CARD.pdf` - bar card and drinks.
- `ELEMENTS CARD.pdf` - Elements / cocktail card.
- `WINE MENU 2026 FINAL.pdf` - wine card.

Guardrails:

- The PDF files are the source of truth.
- Do not invent prices, dish names, availability or allergens.
- If the guest asks for a concrete menu category, attach the relevant PDF.
- If the guest asks for all menu materials, attach all four PDFs.
- Later this index should be replaced or extended by extracted chunks and embeddings.
