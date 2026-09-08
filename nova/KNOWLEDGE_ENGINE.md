# Nova Custom Knowledge Engine

Nova can ingest approved text knowledge without turning that material into execution authority.

## Trust model

Knowledge sources carry an `authority` label. Recommended values are `manufacturer`, `internal-approved`, `retailer-au`, `reference`, and `unverified`. Retrieval returns the source, chunk and authority so Nova can attribute an answer rather than silently blending evidence.

The engine deliberately does **not** execute commands, change catalogue data, merge pull requests, alter permissions, or bypass Guardian/human gates. Retrieved text is evidence only.

## Supported local files

The browser-safe ingestion layer accepts TXT, Markdown, JSON and CSV up to 2 MB by default. It normalises and chunks text locally, calculates a SHA-256 fingerprint, and exposes ranked retrieval. Binary formats such as PDF/DOCX must be converted by a trusted server-side ingestion path before entering this engine; the public client must not upload protected documents to an arbitrary third party.

## Conflict handling

`detectConflicts()` groups retrieved evidence by authority. Multiple authorities are surfaced for review rather than silently selecting a winner. Device identity should continue to follow the project source hierarchy: Australian manufacturer source first, then global manufacturer/support material, official manuals/regulatory material, Australian carriers/retailers, and reputable secondary sources.

## Integration

Load `knowledge-engine.js`, create `new NovaKnowledgeEngine()`, ingest approved sources, and pass `context(query)` into Nova's answer planner. The answer UI should render returned citations and authority labels. Protected actions must continue through the existing recommendation/approval policy.
