/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Smoke layer of the input-capture regression net (the graduated D36 feasibility
 * probe): scripted AWT input through Thinlet's real {@code processEvent} funnel,
 * headless on Xvfb {@code :99}, asserted black-box. Establishes the load-bearing
 * driver properties the widget suites build on — mouse hit-testing, action
 * dispatch, the re-paint trace signal, run-to-run determinism, and keyboard typing.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputSmokeTest {

    private static final String FIXTURE = "/input/smoke.xml";

    @Test
    void mouseClickTogglesCheckbox_blackBoxGetter() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object c1 = d.find("c1");
        assertThat(c1).as("checkbox c1 resolved by name").isNotNull();
        assertThat(t.getBoolean(c1, "selected")).as("checkbox starts unchecked").isFalse();
        d.click(c1);
        assertThat(t.getBoolean(c1, "selected"))
                .as("a real synthesized click, routed through processEvent, toggled the checkbox")
                .isTrue();
    }

    @Test
    void mouseClickFiresButtonAction_handlerDispatch() throws IOException {
        InputHandler handler = new InputHandler();
        InputDriver d = InputDriver.load(FIXTURE, handler);
        d.click(d.find("b1"));
        assertThat(handler.clicked)
                .as("the button's action binding fired all the way to the handler")
                .isTrue();
    }

    @Test
    void rePaintTraceChangesAfterClick_andIsDeterministic() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Trace before = d.paint();
        d.click(d.find("c1"));
        Trace after = d.paint();
        assertThat(TraceComparator.compare(before, after, 0.0))
                .as("re-painting after the toggle yields a visibly different trace")
                .isNotEmpty();

        // Determinism: an independent identical run reaches the same post-click trace.
        InputDriver d2 = InputDriver.load(FIXTURE, new InputHandler());
        d2.click(d2.find("c1"));
        Trace after2 = d2.paint();
        assertThat(TraceComparator.compare(after, after2, 0.0))
                .as("post-click trace is identical across two runs (determinism)")
                .isEmpty();
    }

    @Test
    void keyboardTypingUpdatesTextfield_afterSyntheticFocus() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object t1 = d.find("t1");
        d.focusGained(); // headless requestFocus() delivers no FOCUS_GAINED; synthesize it
        d.click(t1); // move focus to the textfield
        d.type("hi");
        assertThat(t.getString(t1, "text"))
                .as("typed characters landed in the focused textfield")
                .isEqualTo("hi");
    }
}
