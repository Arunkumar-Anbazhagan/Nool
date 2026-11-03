# Nool Architecture Documentation

## 1. High-Level Overview
Nool is a privacy-first, local-first Markdown knowledge base with Git sync, bidirectional linking, graph visualization, and flexible querying. Notes on the filesystem are the absolute source of truth. Indexes and databases are fully disposable and can be rebuilt on-demand from the vault directory.

## 2. Multi-Module KMP Structure

```
Nool/
├── build-logic/                # Gradle convention plugins (nool.kmp.library)
├── shared/
│   ├── core-model/             # Plain domain models (Note, Link, Tag, Property, Vault) & mappers
│   ├── core-markdown/          # CommonMark + GFM tables, strikethrough, WikiLinks, callouts, embeds
│   ├── core-graph/             # Graph data models, Barnes-Hut quadtree, Hooke spring force physics
│   ├── core-templates/         # Template interpolation with kotlinx-datetime
│   └── core-index/             # SQLDelight database, external-content FTS5, prefix queries, sync triggers, VaultWatcher
├── androidApp/                 # Android client (API 26+, Jetpack Compose, Material 3, Foreground Service, WorkManager)
└── desktopApp/                 # Desktop client (Compose Multiplatform for JVM - Windows, macOS, Linux) [Coming in Phase 5]
```

## 3. Technology Stack & Key Libraries
- **Language & Platform**: Kotlin 2.0+, JVM 17, Android minSdk 26, compileSdk 35
- **UI**: Jetpack Compose + Compose Multiplatform, Material 3 with Dynamic Color
- **Dependency Injection**: Koin DI
- **Concurrency**: Kotlin Coroutines + StateFlow / SharedFlow
- **Persistence & Search**: SQLDelight with external-content FTS5 table (`note_fts`) and automatic trigger synchronization; DataStore for preferences
- **Git Sync**: Eclipse JGit 6.8+, MWiede JSch, Android Foreground Service for resilient clone/sync
- **Diagrams & Math**: Offline bundled WebView runner (`mermaid.min.js`), KaTeX math rendering

## 4. Source of Truth & Data Flow
1. **Vault on Filesystem**: Plain Markdown files (`.md`) and standard open formats (`.canvas`).
2. **VaultWatcher**: Emits filesystem mutations (`FileObserver` on Android, `WatchService` on Desktop).
3. **Incremental Indexer**: Parses frontmatter, wikilinks, tags, and full text, updating SQLDelight tables.
4. **FTS5 Triggers**: Keep the external-content search index in continuous lockstep with zero re-indexing overhead.
5. **Disposable Cache**: Deleting the database does not lose any user data. The app can rebuild the index at any time.
