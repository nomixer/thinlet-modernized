/* Thinlet (modernized) — live-Drafts playthrough (test scope). */
package thinlet.drafts;

import java.awt.AWTEvent;

/**
 * Test host exposing the protected {@code processEvent} funnel to
 * {@code InputDriver.attach}. Must live in {@code thinlet.drafts}: parse resolves
 * classpath-relative to the RUNTIME class's package (DECISIONS.md D65).
 */
public class DraftsHost extends Drafts {

    public void funnel(AWTEvent e) {
        processEvent(e);
    }
}
