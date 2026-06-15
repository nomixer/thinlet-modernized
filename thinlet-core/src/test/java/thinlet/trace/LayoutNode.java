/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

/** A widget's computed bounds, captured for the layout half of a trace. */
final class LayoutNode {

    final String className;
    final int x;
    final int y;
    final int w;
    final int h;

    LayoutNode(String className, int x, int y, int w, int h) {
        this.className = className;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
