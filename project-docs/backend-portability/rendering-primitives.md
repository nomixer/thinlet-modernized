# Rendering primitives

> **Stub — populated from Phase 1 trace output.**

Every `Graphics2D` call site in `Thinlet.java`: what it draws, where, and what
backend state it implies. For each primitive, per-DPR (device-pixel-ratio)
scaling notes.

Three purposes:

1. Document how Thinlet renders today.
2. Porting surface for an alternative Canvas / WebGPU / WASM backend.
3. HiDPI design input for Enhanced Thinlet.

<!-- Populated by the trace-curator agent from the semantic-surface trace JSON
     (thinlet-core/src/test/resources/trace/golden.json) once the Phase 1
     tracing harness lands. -->
