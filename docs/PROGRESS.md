# Nool Project Progress

Last Updated: 2026-11-03

## 1. Phase Status Checklist

### Phase 0: Multi-Module KMP Refactoring & SQLDelight Index Migration
- [x] Multi-module KMP extraction (`core-model`, `core-markdown`, `core-graph`, `core-templates`, `core-index`)
- [x] Convention plugins in `build-logic` (`nool.kmp.library`)
- [x] SQLDelight schema with external-content FTS5 (`content='notes'`, `prefix='2 3'`), sync triggers
- [x] Resumable migration & Room index cleanup (`nool_index_cache.db` only deleted upon 100% reindex)
- [x] Renamed `:app` module to `:androidApp` (tag: `phase-0-done`)

---

### Phase 0.5: Hardening of the Existing Foundation
- [x] Confirm which SQLite driver Android uses (`AndroidSqliteDriver` uses platform SQLite with native FTS5 support on API 26+). Verified FTS5 prefix queries (`qui*`, `knowledge*`).
- [x] Verify FTS5 external-content triggers use the special 'delete' command (`notes_ad` and `notes_au`). Verified with mutation test suite and FTS5 integrity-check (`testFtsIntegrityCheckAfterMutations`).
- [x] Removed Room dependencies and eliminated backward-compatibility delegation shims in `:androidApp`.
- [x] Benchmark a generated 10,000-note vault (full reindex: 9.75ms/note, incremental update: 22.35ms, search p50: 15.43ms, p95: 27.66ms). Written to `docs/benchmarks.md`.
- [x] Wrote `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/SECURITY.md`, and `docs/benchmarks.md`.

---

### Phase 1: Editor Upgrade (DONE - Tag: phase-1-done)
- [x] Timeboxed spike: CodeMirror 6 in WebView versus native Compose text editing. Recorded ADR-004 in `docs/DECISIONS.md`.
- [x] Interactive task checkboxes in preview (writes back to the file with minimal diffs).
- [x] Slash commands (tables, callouts, templates, headings, embeds via `SlashCommandCatalog` and `SlashCommandPopup`).
- [x] Live preview (WYSIWYG-style) editing mode.
- [x] Offline KaTeX math (inline `$...$`, block `$$...$$` via `MathRenderer` & `MathBlockCard`).
- [x] Code syntax highlighting (bundled, offline via `SyntaxHighlighter`).
- [x] Footnotes and definition lists (CommonMark inline footnote annotation and definition list block styling).
- [x] Visual table editor (`VisualTableEditorDialog`).
- [x] Paste handling (images to an attachments folder, HTML to Markdown via `PasteHandler`).
- [x] Focus/typewriter mode, word count, reading time.
- [x] Preserve undo/redo, smart lists, toolbar, wikilink autocomplete.

---

### Phase 2: Search, Linking, Knowledge (DONE - Tag: phase-2-done)
- [x] FTS5 search with ranking (`bm25`) and prefix matching wired into the query parser (tags, title, path, folder, content).
- [x] Block references (`[[Note#^id]]`) and heading links (`[[Note#Heading]]`), including embeds of a block (`NoteEmbedHelper.extractBlock`) or section.
- [x] Unlinked mentions with one-tap link conversion in Backlinks and right panel.
- [x] Aliases (frontmatter aliases) and folder notes (`folder/folder.md`, `folder/_index.md`, `folder/index.md`) in `LinkResolver`.
- [x] Tag hierarchy (`#a/b/c`) with `TagHierarchyBuilder`, `TagTreeNode`, and interactive nested tag browser in Left Sidebar.
- [x] Local graph (neighbors, depth control, filters by tag/folder/property) alongside the global graph.
- [x] Saved searches / smart folders with operator evaluation (`tag:`, `path:`, `folder:`, `title:`, `-exclude`, exact phrases).
- [x] Daily/weekly notes (`TemplateManager.getOrCreateWeeklyNote`), month calendar view dialog, and "on this day" historical recall.
- [x] Tasks aggregation with due dates (`📅 YYYY-MM-DD`), interactive task toggle dashboard, and WorkManager reminders on Android (`TaskReminderWorker`).
- [x] Spaced repetition (`Q:: A` and cloze deletions, SM-2 scheduling stored in `.nool/reviews.json`, diff-friendly) with interactive review session dialog.
- [x] Incremental indexing: `AndroidFileObserverVaultWatcher` (Android) and `JvmWatchServiceVaultWatcher` (desktop) behind `VaultWatcher`.

---

### Phase 3: Property Database Views
- [ ] Property-query engine over frontmatter with a small documented syntax and type inference (text, number, date, list, checkbox).
- [ ] Table, Kanban, Calendar, Gallery views.
- [ ] Inline property editing with deterministic YAML write-back (stable key order, minimal diffs) and a diff-stability test suite.

---

### Phase 4: Git and Sync
- [ ] Extract `core-git` and `core-sync` with platform interfaces (secure storage, network observer); keep services/WorkManager in `androidApp`.
- [ ] Shallow clones (`--depth 1`) as an option, with a "fetch full history" action.
- [ ] Multi-vault support (vault registry, switcher, per-vault index and settings).
- [ ] Debounced auto-commit and push (default off; 2+ minutes of inactivity; lock guards against racing with typing).
- [ ] Per-note history, diff view, restore version.
- [ ] Commit message templates.
- [ ] Selective sync and Git LFS support for attachments.
- [ ] OAuth sign-in for GitHub and GitLab (device flow or browser flow; tokens stored in secure storage).
- [ ] Semantic merge: line-level auto-merge for append-only notes such as daily notes, extending `FrontmatterMerger`.
- [ ] Sync abstraction + WebDAV/Nextcloud provider (opt-in).
- [ ] E2EE sync option: write threat model and protocol in `docs/SECURITY.md`, implement against reference self-hostable relay, ship disabled by default.

---

### Phase 5: Desktop Client
- [ ] Extract `ui-shared` (Compose Multiplatform): replace Android-specific APIs (`LocalContext`, Intents, WebView hosts) with expect/actual or interfaces.
- [ ] `desktopApp` shell: vault open/clone, editor, preview, graph, quick switcher, search, settings.
- [ ] Multi-pane layout, tabs, pop-out windows, resizable sidebars.
- [ ] Command palette with user-assignable keybindings; optional Vim/Emacs modes.
- [ ] System tray and global hotkey quick capture.
- [ ] WatchService-based file watching for external edits.
- [ ] OS credential store adapter (Windows Credential Manager / macOS Keychain / libsecret) behind `SecureStorage` interface.
- [ ] Packaging via Compose Multiplatform (MSI, DMG, DEB) and CI job.
- [ ] Reach parity with Android core features before adding desktop-only extras.

---

### Phase 6: Canvas
- [ ] `core-canvas`: JSON Canvas (`.canvas`) model, parser, deterministic serializer, round-trip tests against public spec.
- [ ] Canvas editor: pan/zoom, text/note/image/link cards, edges, groups, note embeds, viewport culling.
- [ ] Full editing on desktop, view plus light edit on Android.

---

### Phase 7: Interop and Publishing
- [ ] Importers with dry-run preview and report: Obsidian, Logseq, Notion export, Evernote `.enex`, Bear, Apple Notes export.
- [ ] Static-site publishing (backlinks, graph, tags) deployable to GitHub Pages; read-only share.
- [ ] Pandoc export presets on desktop (DOCX, LaTeX, EPUB), detecting installed Pandoc and failing gracefully.

---

### Phase 8: Mobile Extras (Android)
- [ ] Share-to-Nool target (text, links, images).
- [ ] Offline web clipper (URL to Markdown) with explicit user-initiated fetch.
- [ ] Additional widgets (pinned note, daily note, recents).
- [ ] Quick Settings tile and voice-note launcher shortcut.
- [ ] On-device OCR for document scanning and stylus input.

---

### Phase 9: On-device AI (Opt-in, Default Off)
- [ ] `core-ai` interfaces (embedder, summarizer, tagger, transcriber).
- [ ] On-device embeddings for semantic search and related notes; vectors stored in rebuildable index; models downloaded only after explicit opt-in.
- [ ] Optional local summarization and auto-tagging.
- [ ] Optional bring-your-own-key remote model behind explicit consent screen with visible outgoing log.
- [ ] Audio transcription: Android recognizer; whisper.cpp on desktop.

---

### Phase 10: Extensibility and Automation (Desktop)
- [ ] Versioned, permissioned plugin API; sandboxed runtime (QuickJS / GraalJS / WASM); sample plugin; disabled by default; permission prompts.
- [ ] Localhost CLI/REST/IPC interface (`nool new "Title"`, search, open), bound to loopback only with token auth, disabled by default.
- [ ] Browser-extension web clipper talking to local interface.
- [ ] Threat model and review notes in `docs/SECURITY.md`.

---

## 2. Current Step
- **Active Phase**: Phase 1: Editor Upgrade
- **Active Item**: Timeboxed spike: CodeMirror 6 in WebView versus native Compose text editing. Judge on IME behavior, cursor stability, large files, live preview feasibility, Vim/Emacs modes, desktop reuse. Record the decision in `docs/DECISIONS.md`.

## 3. Blockers
- None.

## 4. Needs Manual Verification
- Physical Android device / emulator verification of `AndroidFileObserverVaultWatcher` across deep subdirectories.

## 5. Benchmark Numbers
- 10,000-note full reindex: 97.47s (9.75ms/note, >100 notes/sec)
- Single-note incremental update: 22.35ms (non-blocking)
- Search over 10,000 notes: p50 = 15.43ms, p95 = 27.66ms
