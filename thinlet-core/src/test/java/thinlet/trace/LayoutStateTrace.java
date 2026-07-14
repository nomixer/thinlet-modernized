/* Thinlet (modernized) — layout-state sidecar net (test scope). */
package thinlet.trace;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import thinlet.Thinlet;

/**
 * Walks the widget tree like {@link LayoutTrace} but records the sparse
 * scroll/layout-state keys (:port/:view/:widths/:offset) for the sidecar
 * goldens (DECISIONS.md D61 — including the :combolist/:popup edge rationale).
 */
final class LayoutStateTrace {

    private LayoutStateTrace() {}

    static List<LayoutStateNode> walk(Object root) {
        List<LayoutStateNode> out = new ArrayList<>();
        visit(root, out);
        return out;
    }

    private static void visit(Object widget, List<LayoutStateNode> out) {
        if (widget == null) {
            return;
        }
        Rectangle bounds = (Rectangle) LayoutTrace.attr(widget, "bounds");
        // A node is emitted if and only if the widget is laid out (bounds set)
        // AND carries at least one of the four state keys; children and popups
        // are recursed into either way (mirrors LayoutTrace's null-bounds rule).
        // Popups hang off :combolist/:popup, never :comp/:next, so both edges
        // are followed; the fixed visit order keeps the document deterministic.
        if (bounds != null) {
            Rectangle port = (Rectangle) LayoutTrace.attr(widget, ":port");
            Rectangle view = (Rectangle) LayoutTrace.attr(widget, ":view");
            int[] widths = (int[]) LayoutTrace.attr(widget, ":widths");
            Integer offset = (Integer) LayoutTrace.attr(widget, ":offset");
            if (port != null || view != null || widths != null || offset != null) {
                out.add(new LayoutStateNode(
                        Thinlet.getClass(widget),
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        toArray(port),
                        toArray(view),
                        widths,
                        offset));
            }
        }
        visit(LayoutTrace.attr(widget, ":combolist"), out);
        visit(LayoutTrace.attr(widget, ":popup"), out);
        for (Object child = LayoutTrace.attr(widget, ":comp");
                child != null;
                child = LayoutTrace.attr(child, ":next")) {
            visit(child, out);
        }
    }

    private static int[] toArray(Rectangle r) {
        return (r == null) ? null : new int[] {r.x, r.y, r.width, r.height};
    }
}
