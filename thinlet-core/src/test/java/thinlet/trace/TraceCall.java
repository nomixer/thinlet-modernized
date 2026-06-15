/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.util.List;

/**
 * One recorded drawing call. Per DECISIONS.md D7 the args are split into two
 * buckets: {@code cat} (categorical — compared exactly: colors, fonts, strings,
 * booleans, shape names) and {@code num} (numeric coordinate/size values —
 * compared within a pixel tolerance).
 */
final class TraceCall {

    final String op;
    final List<String> cat;
    final List<Double> num;

    TraceCall(String op, List<String> cat, List<Double> num) {
        this.op = op;
        this.cat = cat;
        this.num = num;
    }
}
