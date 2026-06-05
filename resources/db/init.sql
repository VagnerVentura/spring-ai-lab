-- ============================================================
-- init.sql — executado uma vez ao criar o container
-- ============================================================
-- Habilita a extensão pgvector no banco ragdb
-- Isso adiciona o tipo VECTOR e os operadores de similaridade
-- ============================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- O Spring AI cria a tabela vector_store automaticamente
-- via spring.ai.vectorstore.pgvector.initialize-schema=true
-- Mas você pode ver como ela fica com:
--   SELECT * FROM vector_store LIMIT 5;
--   \d vector_store
