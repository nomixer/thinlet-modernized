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
