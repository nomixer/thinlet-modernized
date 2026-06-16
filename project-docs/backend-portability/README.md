# Backend-portability inventory

**Status: stubs.** These documents are created empty in Phase 0 and populated
from Phase 1's trace output (the `TracingGraphics2D` / `LayoutTrace` harness),
curated by the `trace-curator` agent.

Purpose (documentation only — no behavior change in this project):

- Document how Thinlet renders, lays out, and consumes input *today*.
- Provide the porting surface for a future alternative backend
  (Canvas / WebGPU / WASM) under Enhanced Thinlet.
- Feed HiDPI / resolution-scaling design input for Enhanced Thinlet.

| File | Sourced from |
|------|--------------|
| [`rendering-primitives.md`](rendering-primitives.md) | every `Graphics2D` call site in the trace |
| [`layout-algorithms.md`](layout-algorithms.md) | `LayoutTrace` entry/exit over each layout pass |
| [`input-surface.md`](input-surface.md) | AWT events Thinlet consumes |
