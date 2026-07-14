/* Thinlet (modernized) — layout-state sidecar net (test scope). */
package thinlet.trace;

/**
 * A widget's sparse scroll/layout state (:port/:view/:widths/:offset) plus its
 * bounds as a human anchor, captured for the layout-state sidecar goldens
 * (DECISIONS.md D61). A null array/Integer field means the key is absent.
 */
final class LayoutStateNode {

    final String className;
    final int x;
    final int y;
    final int w;
    final int h;
    final int[] port; // viewport [x, y, w, h]
    final int[] view; // [scrollX, scrollY, contentW, contentH]
    final int[] widths; // table column widths
    final Integer offset; // text scroll offset (negative = alignment shift)

    LayoutStateNode(
            String className, int x, int y, int w, int h, int[] port, int[] view, int[] widths, Integer offset) {
        this.className = className;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.port = port;
        this.view = view;
        this.widths = widths;
        this.offset = offset;
    }
}
