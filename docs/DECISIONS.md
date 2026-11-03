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

## ADR-004: Editor Technology Selection: Native Compose Text Architecture with Modular Interactive Live Elements
- **Context**: Need a responsive, robust editor supporting Android IME, cursor stability, live preview elements (checkboxes, tables, callouts, math), desktop reuse without bloat, and Vim keybindings.
- **Options Considered**:
  1. **CodeMirror 6 in embedded WebView**:
     - *Pros*: Battle-tested Live Preview (decorations), native Vim keybindings (`@replit/codemirror-vim`).
     - *Cons*: WebView bridge on Android has IME desync / keyboard latency; Desktop requires bundling CEF/JCEF/Chromium (+100MB binary footprint); asynchronous bridge complicates undo/redo.
  2. **Native Compose Text Architecture (`BasicTextField` / `TextFieldValue` + Compose Live Renderers)**:
     - *Pros*: Zero binary bloat, 100% native IME and keyboard handles on Android and Desktop JVM, instantaneous keystroke processing, seamless Koin/StateFlow integration, shared Compose UI across mobile and desktop.
     - *Cons*: Advanced modal Vim editing requires specialized key mapping.
- **Decision**: Option 2. Build on Native Compose Text Architecture with an interactive editing toolbar, slash commands (`/` trigger), interactive preview with minimal-diff disk write-backs, offline syntax highlighting, and offline KaTeX math.
- **Consequences**: Lightweight APK and Desktop bundles, flawless Android IME and cursor stability, 100% offline security, and shared UI across Android and Desktop.
