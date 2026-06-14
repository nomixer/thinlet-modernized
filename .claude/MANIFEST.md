# Claude-added files — manifest

Files Claude added to this repo for **orientation/reference only** (not product
source). Listed here so they can be removed in one pass later. To erase Claude's
footprint entirely, delete every path in the table below (and the then-empty
`.claude/` directory).

This tracks only Claude's *meta* docs. It does **not** list real deliverables
(e.g. Phase 1 harness code/tests under `thinlet-core/`), which are normal
project source and stay.

| Path | Purpose | Safe to delete? |
| --- | --- | --- |
| `CLAUDE.md` (repo root) | Session orientation, auto-loaded by Claude Code at startup. The only Claude file that must live outside `.claude/` (only the root `CLAUDE.md` is auto-loaded). | Yes — loses auto-orientation only |
| `.claude/paint-pipeline-map.md` | Engineering reference for `Thinlet.java`'s paint/layout pipeline, drawing vocabulary, and widget model (Phase 1 golden-trace harness background). | Yes |
| `.claude/MANIFEST.md` | This file. | Yes |

## Policy (keep the repo tidy)

- Keep files outside `.claude/` to the minimum. **Only `CLAUDE.md`** lives at the
  repo root; everything else Claude adds for reference goes under `.claude/`.
- Whenever a Claude-authored reference/orientation file is added or removed,
  update the table above so this stays a complete cleanup list.
