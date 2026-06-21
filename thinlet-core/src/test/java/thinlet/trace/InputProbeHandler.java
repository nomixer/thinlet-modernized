/* Thinlet (modernized) — Phase 2.x input-capture feasibility probe (test scope). */
package thinlet.trace;

import thinlet.Thinlet;

/**
 * Minimal event handler for the input probe fixture. Thinlet's parser resolves
 * the {@code action="onClick(thinlet)"} binding against this object by
 * reflection (same mechanism {@link CorpusHandler} relies on); the probe asserts
 * that a real synthesized click routes through {@code processEvent} all the way to
 * this method by reading {@link #clicked}.
 */
public class InputProbeHandler {

    /** Set true when the bound button action fires. */
    public boolean clicked;

    public void onClick(Thinlet t) {
        clicked = true;
    }
}
