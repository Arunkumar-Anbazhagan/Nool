# Architecture Decision Records (ADR)

## ADR-001: Multi-Module Kotlin Multiplatform (KMP) Architecture
- **Context**: Nool needs to support Android and Desktop (Windows, macOS, Linux) with high code reuse while preserving privacy-first, local-first principles.
- **Options Considered**:
  1. Monolithic Android codebase with desktop added as separate repo.
  2. Full KMP with shared UI from day 1.
  3. Shared KMP core (`jvm` + `androidTarget`) with convention plugins (`build-logic`), thin platform wrappers.
- **Decision**: Option 3. Extract core modules (`core-model`, `core-markdown`, `core-graph`, `core-templates`, `core-index`) targeting JVM + Android. Use Compose Multiplatform for shared UI.
- **Consequences**: Easy reuse on Desktop without rewriting business logic, while retaining full native integration on Android.

## ADR-002: SQLDelight with External-Content FTS5 Index
- **Context**: Room lacks native KMP support and had tight Android dependencies. Need fast full-text search with prefix support and query operators.
- **Options Considered**:
  1. Room Multiplatform.
  2. Pure in-memory Lucene / custom index.
  3. SQLDelight with SQLite FTS5 external-content table.
- **Decision**: Option 3. SQLDelight schema with `notes` content table and `note_fts USING fts5(..., content='notes', prefix='2 3', tokenize='unicode61')`, synchronized via triggers.
- **Consequences**: Sub-millisecond full-text queries, deterministic disposable index, seamless cross-platform support via `AndroidSqliteDriver` and `JdbcSqliteDriver`.

## ADR-003: Safe Resumable Migration from Room to SQLDelight
- **Context**: Upgrading existing users without risking vault index inconsistency.
- **Options Considered**:
  1. Immediately delete Room database on app upgrade.
  2. Keep both indefinitely.
  3. Dual-phase resumable indexing with guarded deletion upon 100% completion.
- **Decision**: Option 3. Run initial indexing into SQLDelight; keep progress in DataStore; only delete legacy `nool_index_cache.db` once full indexing is verified complete.
- **Consequences**: Zero risk of half-migrated index or user search breakage.
