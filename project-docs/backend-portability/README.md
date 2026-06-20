# Backend-portability inventory

**Status: first cut (Phase 2).** Created as stubs in Phase 0 and now populated
from the committed golden traces (the `TracingGraphics2D` / `LayoutTrace`
harness output), curated by the `trace-curator` agent — whose repeatable
procedure is codified at [`.claude/agents/trace-curator.md`](../../.claude/agents/trace-curator.md).
Rationale: `DECISIONS.md` **D34**.

Purpose (documentation only — no behavior change in this project):

- Document how Thinlet renders, lays out, and consumes input *today*.
- Provide the porting surface for a future alternative backend
  (Canvas / WebGPU / WASM) under Enhanced Thinlet.
- Feed HiDPI / resolution-scaling design input for Enhanced Thinlet.

| File | Sourced from | Status |
|------|--------------|--------|
| [`rendering-primitives.md`](rendering-primitives.md) | every `Graphics2D` call in the trace `calls` arrays | populated (first cut) |
| [`layout-algorithms.md`](layout-algorithms.md) | `LayoutTrace` widget bounds + `doLayout` dispatch | populated (first cut) |
| [`input-surface.md`](input-surface.md) | AWT events Thinlet consumes | **deferred** — no input-event capture in the harness yet |

The [`cross-jdk-trace-diff.md`](cross-jdk-trace-diff.md) reference documents the
tooling that captures and compares the per-runtime traces (JDK 8/11/17/21) into a
divergence report — the quantified `FontMetrics` drift the curator will draw on.
