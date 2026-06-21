/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import thinlet.Thinlet;

/**
 * Minimal event handler for the input fixtures. Thinlet's parser resolves the
 * {@code action="onClick(thinlet)"} binding against this object by reflection (the
 * same mechanism {@link CorpusHandler} relies on); the smoke suite asserts that a
 * real synthesized click routes through {@code processEvent} all the way to this
 * method by reading {@link #clicked}. Selection/scroll scenarios assert via public
 * getters and re-paint trace diffs instead, so they need no handler callbacks.
 */
public class InputHandler {

    /** Set true when the bound button action fires. */
    public boolean clicked;

    public void onClick(Thinlet t) {
        clicked = true;
    }
}
