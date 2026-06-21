/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import thinlet.Thinlet;

/**
 * Walks Thinlet's {@code Object[]} widget tree and records each widget's computed
 * bounds. Widgets are {@code Object[]} chains {@code [key, value, next]} with
 * interned-string keys; we traverse them directly (matching keys with {@code
 * equals}) rather than touch Thinlet's private accessors. See
 * {@code .claude/PAINT-PIPELINE-MAP.md}.
 */
final class LayoutTrace {

    private LayoutTrace() {}

    static List<LayoutNode> walk(Object root) {
        List<LayoutNode> out = new ArrayList<>();
        visit(root, out);
        return out;
    }

    private static void visit(Object widget, List<LayoutNode> out) {
        if (widget == null) {
            return;
        }
        Rectangle bounds = (Rectangle) attr(widget, "bounds");
        // Per design decision: widgets without computed bounds are skipped (they
        // contribute no layout signal); we still recurse into their children.
        if (bounds != null) {
            out.add(new LayoutNode(Thinlet.getClass(widget), bounds.x, bounds.y, bounds.width, bounds.height));
        }
        for (Object child = attr(widget, ":comp"); child != null; child = attr(child, ":next")) {
            visit(child, out);
        }
    }

    private static Object attr(Object widget, String key) {
        Object[] entry = (Object[]) widget;
        while (entry != null) {
            if (key.equals(entry[0])) {
                return entry[1];
            }
            entry = (Object[]) entry[2];
        }
        return null;
    }
}
