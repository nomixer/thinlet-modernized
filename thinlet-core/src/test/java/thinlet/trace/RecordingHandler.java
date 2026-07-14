/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import java.util.ArrayList;
import java.util.List;

/**
 * Event handler that records invocations in order, for asserting getter-less
 * behavior (menu/action firing, focus traversal) via Thinlet's
 * {@code method(this.name)} String-argument binding (DECISIONS.md D64).
 */
public class RecordingHandler extends InputHandler {

    /** Invocation log: "name" for actions/menushown, "gained:name"/"lost:name" for focus. */
    public final List<String> events = new ArrayList<>();

    public void record(String name) {
        events.add(name);
    }

    public void focusGained(String name) {
        events.add("gained:" + name);
    }

    public void focusLost(String name) {
        events.add("lost:" + name);
    }
}
