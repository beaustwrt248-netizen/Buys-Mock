# Nova Knowledge Platform Expansion Design

## Goal
Expand Nova from a catalogue-heavy keyword knowledge store into a scalable, evidence-grounded hybrid knowledge platform spanning the Morley ecosystem.

## Current baseline
- `nova_knowledge_items` contains 4,696 active catalogue/reference items.
- The live catalogue contains 1,894 structured device records.
- Nova has 26 active Guardian learning experiences.
- Existing retrieval is PostgreSQL full-text oriented; the Supabase project supports pgvector but the vector extension is not currently installed.
- Existing Nova protected-action and human-approval boundaries remain authoritative.

## Architecture
Use PostgreSQL/Supabase as the system of record and combine lexical retrieval with semantic vector retrieval. Preserve existing knowledge rows and introduce additive metadata/chunk/evidence structures rather than replacing the current store destructively.

Knowledge is represented as domain-scoped chunks with stable source identity, provenance, trust, freshness, confidence, content hash, version and lifecycle state. Search ranks candidates using lexical relevance, semantic similarity, source authority, freshness and confidence. Retrieval must degrade safely to lexical search if embeddings are unavailable.

## Knowledge domains
Initial domains are catalogue, project/development, Guardian, support, release, pricing, UI/UX rules, operational procedures and durable Nova decisions. Business domains such as inventory, sales and valuation are connected when their authoritative datasets contain records; Nova must never fabricate absent business metrics.

## Ingestion
Each adapter emits normalized source documents. The ingestion pipeline chunks documents deterministically, hashes normalized content, deduplicates unchanged chunks, versions changed chunks, records provenance and queues embedding generation. Re-ingestion must be idempotent.

Initial internal adapters cover device catalogue records, Guardian learning/incidents, support records and approved repository documentation/release metadata. External web research remains evidence-first and stores publisher/source URL, retrieval time, excerpt/field, confidence and freshness.

## Retrieval
Hybrid retrieval combines PostgreSQL full-text candidates with vector nearest-neighbor candidates. Ranking applies configurable weights to lexical score, semantic score, authority, freshness and confidence. Filters include domain, status, trust, source type and freshness requirements. Every returned knowledge item retains provenance sufficient for Explain mode.

## Learning
Outcome feedback may tune recommendation ranking and confidence, but cannot silently rewrite protected rules, permissions, approval boundaries, pricing policy or Guardian protected-repair policy. Guardian successful/failed outcomes become technical learning records with root cause, repair, regression coverage and observed outcome.

## Safety and permissions
Database migrations, RLS, production functions and protected data writes are high risk. Implementation is prepared on a Nova branch/PR and must not be merged or applied to production without Beau's explicit approval for the specific ready PR. Existing data is preserved; migrations are additive and reversible where practical. Sensitive identifiers are excluded from general knowledge chunks.

## Observability
Track ingestion runs, documents/chunks discovered, inserted/updated/unchanged counts, embedding status/failures, retrieval latency, lexical/vector contribution, source/domain distribution and stale knowledge. Health reporting must distinguish indexed knowledge from authoritative live operational records.

## Rollout
1. Add schema and contracts for normalized sources, chunks, evidence and embedding state.
2. Add deterministic ingestion/dedupe/versioning with internal adapters.
3. Add hybrid retrieval with lexical fallback.
4. Add embedding worker/provider boundary and backfill controls.
5. Add observability and regression/security coverage.
6. Backfill internal knowledge after the migration is explicitly approved and deployed.

## Definition of done
The expansion is ready when migrations are reviewable and reversible, ingestion is idempotent, retrieval works with and without embeddings, provenance is returned for material answers, protected boundaries remain enforced, tests/security gates pass, and the PR clearly requires human approval before production merge/deployment.