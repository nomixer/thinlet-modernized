/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.util.List;

/** A full render trace: the ordered drawing calls plus the laid-out widget bounds. */
final class Trace {

    final List<TraceCall> calls;
    final List<LayoutNode> layout;

    Trace(List<TraceCall> calls, List<LayoutNode> layout) {
        this.calls = calls;
        this.layout = layout;
    }
}
