-- V2: AI / RAG tables (pgvector, knowledge documents, chat history)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Layout follows Spring AI's default PgVectorStore table.
-- Dimension 768 matches nomic-embed-text. Changing the embedding model means re-indexing.
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(768)
);
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);

CREATE TABLE knowledge_document (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    source_type VARCHAR(20)  NOT NULL CHECK (source_type IN ('PDF', 'MARKDOWN', 'TEXT')),
    file_name   VARCHAR(255) NOT NULL,
    checksum    VARCHAR(64)  NOT NULL,
    audience    VARCHAR(20)  NOT NULL DEFAULT 'MEMBER' CHECK (audience IN ('PUBLIC', 'MEMBER', 'STAFF')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED' CHECK (status IN ('UPLOADED', 'INDEXING', 'INDEXED', 'FAILED')),
    version     INT          NOT NULL DEFAULT 1,
    uploaded_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    indexed_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (checksum)
);

CREATE TABLE chat_conversation (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES chat_conversation (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content         TEXT        NOT NULL,
    sources         JSONB,      -- chunks / documents cited in the answer
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_message_conversation ON chat_message (conversation_id, created_at);
