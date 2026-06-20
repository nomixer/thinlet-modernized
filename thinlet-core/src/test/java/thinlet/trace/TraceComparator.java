/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares two traces per the DECISIONS.md D7 tolerance model: method name and
 * arg arity are structural-exact, categorical args are exact, and numeric
 * coordinate/size args must agree within a pixel tolerance (default ±2 px). The
 * same tolerance applies to layout bounds. Returns a list of human-readable
 * diffs; empty means a match.
 */
final class TraceComparator {

    private TraceComparator() {}

    static double loadDefaultPx(String json) {
        Object parsed = TraceJson.parseValue(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object value = root.get("defaultPx");
        return (value instanceof Number) ? ((Number) value).doubleValue() : 2.0;
    }

    static List<String> compare(Trace expected, Trace actual, double tol) {
        List<String> diffs = new ArrayList<>();
        compareCalls(expected.calls, actual.calls, tol, diffs);
        compareLayout(expected.layout, actual.layout, tol, diffs);
        return diffs;
    }

    /**
     * One numeric coordinate/size difference between two structurally-aligned
     * traces, reported regardless of tolerance — the raw FontMetrics drift signal
     * the cross-JDK diff needs. {@code where} is a stable position key (e.g.
     * {@code "call[12] drawString num[0]"} or {@code "layout[3] button.x"}).
     */
    static final class Delta {

        final String where;
        final String op;
        final double baseline;
        final double other;

        Delta(String where, String op, double baseline, double other) {
            this.where = where;
            this.op = op;
            this.baseline = baseline;
            this.other = other;
        }

        double absDelta() {
            return Math.abs(other - baseline);
        }
    }

    /** The full divergence between two traces: every numeric delta plus any structural/categorical mismatch. */
    static final class TraceDelta {

        final List<Delta> numeric = new ArrayList<>();
        final List<String> structural = new ArrayList<>();
    }

    /**
     * Report-only counterpart to {@link #compare}: enumerates <em>every</em>
     * numeric difference (including sub-tolerance) for the cross-JDK report, and
     * collects any structural/categorical mismatch separately (those must stay
     * exact across JDKs per D7). Does not apply, or depend on, a tolerance — it
     * never gates a build, so the regression gate {@link #compare} is left intact.
     */
    static TraceDelta deltas(Trace baseline, Trace other) {
        TraceDelta out = new TraceDelta();
        callDeltas(baseline.calls, other.calls, out);
        layoutDeltas(baseline.layout, other.layout, out);
        return out;
    }

    private static void callDeltas(List<TraceCall> base, List<TraceCall> other, TraceDelta out) {
        if (base.size() != other.size()) {
            out.structural.add("call count: baseline " + base.size() + " but other " + other.size());
        }
        int n = Math.min(base.size(), other.size());
        for (int i = 0; i < n; i++) {
            TraceCall b = base.get(i);
            TraceCall o = other.get(i);
            if (!b.op.equals(o.op)) {
                out.structural.add("call[" + i + "].op: baseline " + b.op + " but other " + o.op);
                continue;
            }
            if (!b.cat.equals(o.cat)) {
                out.structural.add("call[" + i + "] " + b.op + " cat: baseline " + b.cat + " but other " + o.cat);
            }
            if (b.num.size() != o.num.size()) {
                out.structural.add("call[" + i + "] " + b.op + " num arity: baseline " + b.num.size() + " but other "
                        + o.num.size());
                continue;
            }
            for (int j = 0; j < b.num.size(); j++) {
                double bv = b.num.get(j);
                double ov = o.num.get(j);
                if (bv != ov) {
                    out.numeric.add(new Delta("call[" + i + "] " + b.op + " num[" + j + "]", b.op, bv, ov));
                }
            }
        }
    }

    private static void layoutDeltas(List<LayoutNode> base, List<LayoutNode> other, TraceDelta out) {
        if (base.size() != other.size()) {
            out.structural.add("layout node count: baseline " + base.size() + " but other " + other.size());
        }
        int n = Math.min(base.size(), other.size());
        for (int i = 0; i < n; i++) {
            LayoutNode b = base.get(i);
            LayoutNode o = other.get(i);
            if (!b.className.equals(o.className)) {
                out.structural.add("layout[" + i + "].class: baseline " + b.className + " but other " + o.className);
                continue;
            }
            fieldDelta(out, i, b.className, "x", b.x, o.x);
            fieldDelta(out, i, b.className, "y", b.y, o.y);
            fieldDelta(out, i, b.className, "w", b.w, o.w);
            fieldDelta(out, i, b.className, "h", b.h, o.h);
        }
    }

    private static void fieldDelta(TraceDelta out, int i, String cls, String field, int base, int other) {
        if (base != other) {
            out.numeric.add(new Delta("layout[" + i + "] " + cls + "." + field, cls + "." + field, base, other));
        }
    }

    private static void compareCalls(List<TraceCall> exp, List<TraceCall> act, double tol, List<String> diffs) {
        if (exp.size() != act.size()) {
            diffs.add("call count: expected " + exp.size() + " but was " + act.size());
        }
        int n = Math.min(exp.size(), act.size());
        for (int i = 0; i < n; i++) {
            TraceCall e = exp.get(i);
            TraceCall a = act.get(i);
            if (!e.op.equals(a.op)) {
                diffs.add("call[" + i + "].op: expected " + e.op + " but was " + a.op);
                continue;
            }
            if (!e.cat.equals(a.cat)) {
                diffs.add("call[" + i + "] " + e.op + " cat: expected " + e.cat + " but was " + a.cat);
            }
            if (e.num.size() != a.num.size()) {
                diffs.add("call[" + i + "] " + e.op + " num arity: expected " + e.num.size() + " but was "
                        + a.num.size());
                continue;
            }
            for (int j = 0; j < e.num.size(); j++) {
                if (Math.abs(e.num.get(j) - a.num.get(j)) > tol) {
                    diffs.add("call[" + i + "] " + e.op + " num[" + j + "]: expected " + e.num.get(j) + " but was "
                            + a.num.get(j) + " (tol " + tol + ")");
                }
            }
        }
    }

    private static void compareLayout(List<LayoutNode> exp, List<LayoutNode> act, double tol, List<String> diffs) {
        if (exp.size() != act.size()) {
            diffs.add("layout node count: expected " + exp.size() + " but was " + act.size());
        }
        int n = Math.min(exp.size(), act.size());
        for (int i = 0; i < n; i++) {
            LayoutNode e = exp.get(i);
            LayoutNode a = act.get(i);
            if (!e.className.equals(a.className)) {
                diffs.add("layout[" + i + "].class: expected " + e.className + " but was " + a.className);
                continue;
            }
            checkPx(diffs, i, e.className, "x", e.x, a.x, tol);
            checkPx(diffs, i, e.className, "y", e.y, a.y, tol);
            checkPx(diffs, i, e.className, "w", e.w, a.w, tol);
            checkPx(diffs, i, e.className, "h", e.h, a.h, tol);
        }
    }

    private static void checkPx(List<String> diffs, int i, String cls, String field, int e, int a, double tol) {
        if (Math.abs(e - a) > tol) {
            diffs.add("layout[" + i + "] " + cls + "." + field + ": expected " + e + " but was " + a + " (tol " + tol
                    + ")");
        }
    }
}
