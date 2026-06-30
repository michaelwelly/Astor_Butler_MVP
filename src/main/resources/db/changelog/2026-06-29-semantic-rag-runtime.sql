CREATE EXTENSION IF NOT EXISTS vector;

DROP INDEX IF EXISTS idx_semantic_embeddings_vector;

ALTER TABLE semantic_embeddings
    ALTER COLUMN embedding DROP DEFAULT;

ALTER TABLE semantic_embeddings
    ALTER COLUMN embedding TYPE vector;

ALTER TABLE semantic_embeddings
    ALTER COLUMN embedding_dimension DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_semantic_embeddings_dimension
    ON semantic_embeddings(embedding_dimension, embedding_model);

CREATE INDEX IF NOT EXISTS idx_semantic_chunks_language
    ON semantic_chunks(language_code, title);
