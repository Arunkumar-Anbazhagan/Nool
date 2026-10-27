# Nool

A lightweight, offline-first, Git-synced, linked-knowledge note-taking app for Android built with Kotlin and Jetpack Compose (Material 3).

## Core Principles
1. **Plain Files are the Source of Truth**: All notes (`.md`), schemas (`.app/types/`), and saved views (`.app/views/`) are stored as plain files in standard directories. The exact same repository can be opened in Obsidian, VS Code, or Foam on desktop without translation.
2. **Derived Cache Architecture**: All Room / SQLite databases (FTS full-text search, backlinks, link graph index) are purely derived caches. They can be wiped and fully regenerated incrementally from the disk at any point.
3. **No Extension / Plugin Bloat**: Focused, high-performance native mobile experience.

---

## Link Resolution Rules

Nool implements standard Wiki-link resolution compatible with Obsidian. When a link such as `[[Target]]` or `[[Target#Heading|Custom Alias]]` is encountered:

### 1. Link Syntax
- Standard link: `[[Note title]]`
- Link with custom alias: `[[Note title|Display alias]]`
- Link with heading: `[[Note title#Heading]]`
- Link with heading and alias: `[[Note title#Heading|Display alias]]`
- Embed note: `![[Note title]]` (renders an interactive note preview block)
- Embed image or attachment: `![[attachments/photo.png]]` (renders the inline image)

### 2. Resolution Precedence
Given a link `[[Target]]` authored in note `SourcePath` (e.g. `Projects/Mobile/Architecture.md`):

1. **Explicit Relative Path Match**:
   - If `Target` contains a `/` (e.g. `[[Guides/Setup]]`), Nool matches against `Guides/Setup.md` relative to the vault root or current directory.
2. **Title Match (Case-Insensitive)**:
   - Matches against note file names without `.md` extension.
   - Example: `[[jetpack compose]]` matches `Tech/Jetpack Compose.md`.
3. **Disambiguation for Duplicate Titles**:
   - When multiple notes share the same title across different folders:
     1. **Sibling in Same Folder**: A note residing in the same directory as the source note is given highest priority.
     2. **Root Folder**: A note in the root vault directory is given second priority.
     3. **Shortest Path**: The note with the shallowest directory depth is selected.
4. **Frontmatter Alias Match (Case-Insensitive)**:
   - If no note file matches by title, Nool matches against any alias declared in frontmatter:
     ```yaml
     ---
     aliases: [AI, Machine Learning, Deep Learning]
     ---
     ```
     Querying `[[Machine Learning]]` will resolve directly to this note.
5. **Broken Links**:
   - If no note or alias matches, the link is flagged as unresolved / broken.
   - Tapping an unresolved link in preview mode prompts the user to create the missing note immediately in the current folder.

### 3. Atomic Link Updating on Rename or Move
- Renaming or moving a note automatically scans all referencing Markdown files and updates their `[[Old Title]]` links to `[[New Title]]` in place, preserving any section anchors (`#Heading`) and aliases (`|alias`).

---

## Project Structure & Architecture
- **`com.nool.app.data.files`**: Local vault storage operations (`FileManager`, `VaultRepositoryImpl`).
- **`com.nool.app.data.index`**: Derived SQLite / Room cache, FTS index, `WikiLinkParser`, `LinkResolver`, `FrontmatterAndTagParser`, `IndexManager`.
- **`com.nool.app.domain`**: Repository interfaces, domain models (`VaultItem`, `NoteMetadata`).
- **`com.nool.app.ui`**: Jetpack Compose screens, MVVM ViewModels, Navigation, CommonMark Markdown editor and preview components.
