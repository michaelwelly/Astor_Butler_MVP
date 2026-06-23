# Astor Butler Document Store

## Purpose

MongoDB is used as the document-oriented store for project materials, internal documents, extracted text and future AI Adapter lookup.

It does not replace PostgreSQL. PostgreSQL remains the primary relational database for users, roles, bookings, statuses, timelines, posts and links between business entities.

## Database

Local test database:

```text
aether
```

Local MongoDB requires authentication:

```text
MONGO_USER=oracle
MONGO_PASSWORD=astor_unlock
MONGODB_URI=mongodb://oracle:astor_unlock@localhost:27017/aether?authSource=admin
```

MongoDB stores document metadata, extracted text and chunks for AI/search workflows. Original files stay on local disk, Yandex Disk or S3-compatible object storage. Heavy media and binary source files should not be duplicated into MongoDB as raw blobs.

## Collections

### `project_documents`

One document per source file.

Key fields:

- `_id` - stable SHA-256 id generated from source path.
- `runId` - ingest run id.
- `title` - human-readable title.
- `category` - `diploma`, `presentation`, `one-pager`, `speech`, `paper`, `architecture`, `memory`.
- `tags` - search/filter tags.
- `sourcePath` - local source path.
- `extension` - original file extension.
- `mimeType` - detected MIME type.
- `sha256` - file content hash.
- `sizeBytes` - file size.
- `modifiedAt` - original file modification timestamp.
- `ingestedAt` - ingest timestamp.
- `textLength` - extracted text length.
- `wordCount` - extracted word count.
- `preview` - first normalized text fragment.
- `text` - full extracted text.

Indexes:

- unique `sourcePath`;
- `category + tags`;
- text index over `title`, `text`, `preview`.

### `document_chunks`

Chunked text for future AI Adapter retrieval.

Key fields:

- `_id` - `<documentId>:<chunkIndex>`.
- `documentId` - link to `project_documents._id`.
- `runId` - ingest run id.
- `chunkIndex` - chunk number inside the document.
- `sourcePath` - original file path.
- `category` - copied category.
- `tags` - copied tags.
- `text` - normalized chunk text.
- `textLength` - chunk length.

Indexes:

- unique `documentId + chunkIndex`;
- text index over `text`.

### `document_ingest_runs`

One row per ingest run.

Key fields:

- `_id` - run id.
- `startedAt` - ingest timestamp.
- `documentCount` - imported document count.
- `chunkCount` - imported chunk count.
- `missing` - missing paths.

## Current Import

Imported on 2026-05-29:

- `19` documents.
- `114` chunks.
- `0` missing files.

Included sources:

- diploma DOCX files from `Downloads`;
- pitch deck PDFs;
- one-pager DOCX/PDF;
- speech TXT;
- conference paper DOCX files;
- repository architecture docs;
- Obsidian project memory notes.

## Script

Ingest script:

```text
scripts/ingest_project_documents.py
```

The script extracts DOCX, PDF, TXT and Markdown text into JSONL files ready for Mongo import.

Generated files:

```text
/private/tmp/astor_document_ingest/project_documents.jsonl
/private/tmp/astor_document_ingest/document_chunks.jsonl
/private/tmp/astor_document_ingest/document_ingest_runs.jsonl
/private/tmp/astor_document_ingest/summary.md
```

## Future Backend Integration

The future `domain.document` module should provide:

- document registry API;
- document search API;
- chunk lookup for AI Adapter;
- source metadata validation;
- re-ingest command;
- links from documents to posts, media, bookings and timeline events.

AI Adapter should read through an application service, not directly from MongoDB.
