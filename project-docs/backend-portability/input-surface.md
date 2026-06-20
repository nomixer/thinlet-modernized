# Input surface

> **Deferred — not yet curated.** Unlike `rendering-primitives.md` and
> `layout-algorithms.md`, this inventory has **no trace source today.** The
> golden-trace harness records the **paint stream** (`TracingGraphics2D`) and the
> **resolved layout** (`LayoutTrace`) only; it does **not** capture AWT input
> events. There is nothing in the committed goldens to curate this from, so it is
> intentionally left as a stub rather than written from memory.

What this doc will eventually cover: the events Thinlet consumes from AWT
(mouse, key, focus, mouse-wheel, and the repaint/timer ticks), with
browser-equivalent mappings noted for a future portable backend.

Two ways to populate it when prioritized (a separate slice, not this one):

1. **Extend the harness** to record an input-event trace (replay a scripted
   event sequence over each corpus screen and serialize the dispatched events),
   then curate from that data — keeping this doc's provenance trace-backed and
   cross-JDK-checkable, consistent with the other two.
2. **Source-derived pass** over `Thinlet.java`'s event handling directly — the
   AWT listener surface is enabled in the `Thinlet()` constructor
   (`thinlet-core/src/main/java/thinlet/Thinlet.java:108`) and dispatched through
   the class's `processEvent`/listener methods. This is faster but its provenance
   is the source, not a trace, and it cannot be cross-JDK-validated by the
   existing tooling.

Either path is tracked as remaining Phase-2 work; see `DECISIONS.md` **D34**.
