/* Thinlet (modernized) — Phase 2.x input-capture feasibility probe (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Phase 2.x feasibility probe: can scripted AWT input be driven through Thinlet's
 * real {@code processEvent} funnel, headless on Xvfb {@code :99}, with
 * deterministic, assertable outcomes? Findings feed the acceptance gate documented
 * in {@code project-docs/backend-portability/input-harness-probe.md}.
 *
 * <p>The mouse + determinism cases are hard assertions (the confident seams); the
 * keyboard/focus case is the empirical unknown the probe exists to settle.
 */
@Tag("input-probe")
@ExtendWith(XvfbDisplayExtension.class)
class InputProbeTest {

    private static final String FIXTURE = "/input/probe.xml";

    @Test
    void mouseClickTogglesCheckbox_blackBoxGetter() throws IOException {
        InputProbeDriver d = InputProbeDriver.load(FIXTURE, new InputProbeHandler());
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
        InputProbeHandler handler = new InputProbeHandler();
        InputProbeDriver d = InputProbeDriver.load(FIXTURE, handler);
        d.click(d.find("b1"));
        assertThat(handler.clicked)
                .as("the button's action binding fired all the way to the handler")
                .isTrue();
    }

    @Test
    void rePaintTraceChangesAfterClick_andIsDeterministic() throws IOException {
        InputProbeDriver d = InputProbeDriver.load(FIXTURE, new InputProbeHandler());
        Trace before = d.paint();
        d.click(d.find("c1"));
        Trace after = d.paint();
        assertThat(TraceComparator.compare(before, after, 0.0))
                .as("re-painting after the toggle yields a visibly different trace")
                .isNotEmpty();

        // Determinism: an independent identical run reaches the same post-click trace.
        InputProbeDriver d2 = InputProbeDriver.load(FIXTURE, new InputProbeHandler());
        d2.click(d2.find("c1"));
        Trace after2 = d2.paint();
        assertThat(TraceComparator.compare(after, after2, 0.0))
                .as("post-click trace is identical across two runs (determinism)")
                .isEmpty();
    }

    @Test
    void keyboardTypingUpdatesTextfield_afterSyntheticFocus() throws IOException {
        InputProbeDriver d = InputProbeDriver.load(FIXTURE, new InputProbeHandler());
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
