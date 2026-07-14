/* Thinlet (modernized) — live-Drafts playthrough (test scope). */
package thinlet.drafts;

import java.awt.AWTEvent;

/**
 * Test host for the live-Drafts playthrough: exposes the protected
 * {@code processEvent} funnel to {@code InputDriver.attach} (DECISIONS.md D65).
 * Lives in {@code thinlet.drafts} because {@code Thinlet.parse(String, …)}
 * resolves classpath-relative to the RUNTIME class's package — a subclass in any
 * other package would break Drafts' own {@code parse("drafts.xml")} and every
 * page's {@code parse(ui, …)}.
 */
public class DraftsHost extends Drafts {

    public void funnel(AWTEvent e) {
        processEvent(e);
    }
}
