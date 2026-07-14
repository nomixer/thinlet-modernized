/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — Tab/Shift+Tab focus traversal: the focus owner has no
 * public getter, so the recorded {@code focusgained}/{@code focuslost} sequence IS
 * the assertion, plus {@code requestFocus} boolean probes (DECISIONS.md D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputFocusTraversalTest {

    private static final String FIXTURE = "/input/focus.xml";

    @Test
    void tabCyclesThroughFocusablesSkippingDisabledAndNonWidgets() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        d.focusGained(); // initial focus lands on the first focusable: f1
        for (int i = 0; i < 5; i++) {
            d.press(KeyEvent.VK_TAB);
        }
        assertThat(h.events)
                .as("Tab skips the disabled button and the label, descends into the selected tab, and wraps")
                .containsExactly(
                        "gained:f1", "lost:f1", "gained:f2", "gained:f4", "gained:ftp", "gained:f5", "gained:f1");
    }

    @Test
    void shiftTabTraversesBackward() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        d.focusGained();
        d.press(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
        d.press(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
        assertThat(h.events)
                .as("Shift+Tab wraps backward into the selected tab's content, then back to the pane")
                .containsExactly("gained:f1", "lost:f1", "gained:f5", "gained:ftp");
    }

    @Test
    void requestFocusProbesFocusability() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        assertThat(d.thinlet().requestFocus(d.find("f3")))
                .as("a disabled button is not focusable")
                .isFalse();
        assertThat(d.thinlet().requestFocus(d.find("f6")))
                .as("content of an unselected tab is not focusable")
                .isFalse();
        assertThat(d.thinlet().requestFocus(d.find("lb")))
                .as("a label is never focusable")
                .isFalse();
        assertThat(d.thinlet().requestFocus(d.find("f1")))
                .as("an enabled textfield is focusable")
                .isTrue();
    }

    @Test
    void selectingTheOtherTabChangesWhatTabReaches() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        d.thinlet().setInteger(d.find("ftp"), "selected", 1);
        d.paint();
        d.focusGained();
        d.thinlet().requestFocus(d.find("ftp"));
        h.events.clear();
        d.press(KeyEvent.VK_TAB);
        assertThat(h.events)
                .as("with tab B selected, Tab from the pane reaches f6, not f5")
                .containsExactly("gained:f6");
    }
}
