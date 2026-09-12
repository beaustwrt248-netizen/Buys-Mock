#!/usr/bin/env python3
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MIGRATION = ROOT / "supabase" / "migrations" / "20260912010000_nova_hybrid_knowledge_foundation.sql"
FUNCTION = ROOT / "supabase" / "functions" / "nova-knowledge" / "index.ts"


class NovaHybridKnowledgeContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.migration = MIGRATION.read_text(encoding="utf-8")
        cls.function = FUNCTION.read_text(encoding="utf-8")
        cls.migration_lower = cls.migration.lower()
        cls.function_lower = cls.function.lower()

    def test_vector_chunk_schema_and_indexes_exist(self):
        required = (
            "create extension if not exists vector",
            "create table if not exists public.nova_knowledge_chunks",
            "embedding extensions.vector(1536)",
            "using gin (search_vector)",
            "using hnsw (embedding extensions.vector_cosine_ops)",
            "source_uri",
            "external_key",
            "fresh_until",
            "authority_weight",
            "confidence",
        )
        for needle in required:
            self.assertIn(needle, self.migration_lower)

    def test_chunk_table_is_service_role_only_and_rls_protected(self):
        required = (
            "alter table public.nova_knowledge_chunks enable row level security",
            "revoke all on table public.nova_knowledge_chunks from anon, authenticated",
            "grant all on table public.nova_knowledge_chunks to service_role",
        )
        for needle in required:
            self.assertIn(needle, self.migration_lower)

    def test_hybrid_rpc_is_invoker_only_with_rrf_and_lexical_fallback(self):
        required = (
            "nova_match_knowledge_hybrid",
            "security invoker",
            "websearch_to_tsquery",
            "rrf_k",
            "query_embedding is not null",
            "revoke all on function public.nova_match_knowledge_hybrid",
            "grant execute on function public.nova_match_knowledge_hybrid",
        )
        for needle in required:
            self.assertIn(needle, self.migration_lower)

    def test_existing_knowledge_is_non_destructively_backfilled(self):
        self.assertIn("insert into public.nova_knowledge_chunks", self.migration_lower)
        self.assertIn("from public.nova_knowledge_items", self.migration_lower)
        self.assertNotIn("delete from public.nova_knowledge_items", self.migration_lower)
        self.assertNotIn("truncate table public.nova_knowledge_items", self.migration_lower)

    def test_categories_expand_beyond_catalogue_only_domains(self):
        for category in ("development", "operations", "research", "policy", "product"):
            self.assertIn(f"'{category}'", self.migration_lower)
            self.assertIn(f"'{category}'", self.function_lower)

    def test_edge_function_uses_existing_openrouter_for_embeddings(self):
        required = (
            "nova_embedding_model",
            "openai/text-embedding-3-small",
            "https://openrouter.ai/api/v1/embeddings",
            "dimensions: embedding_dimensions",
        )
        for needle in required:
            self.assertIn(needle, self.function_lower)

    def test_edge_function_has_chunk_lifecycle_hybrid_search_and_ingestion(self):
        required = (
            "function buildchunks",
            "async function syncchunks",
            "hybrid-search",
            "nova_match_knowledge_hybrid",
            "external_key",
            "action === 'ingest'",
        )
        for needle in required:
            self.assertIn(needle, self.function_lower)

    def test_existing_admin_boundary_and_permanent_delete_guard_remain(self):
        self.assertIn("profile.role !== 'admin'", self.function)
        self.assertIn("Permanent knowledge deletion is disabled through Nova. Archive instead.", self.function)


if __name__ == "__main__":
    unittest.main()
