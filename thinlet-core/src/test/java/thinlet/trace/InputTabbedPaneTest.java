/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — {@code tabbedpane} (D64). Selection is the public
 * {@code getSelectedIndex}; focus movement — which has no public getter — is
 * observed through recorded {@code focusgained} handler invocations
 * ({@link RecordingHandler}). Exact getters/recordings, so the re-paint diff
 * corroboration is skipped (InputSplitPaneTest precedent).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputTabbedPaneTest {

    private static final String FIXTURE = "/input/tabs.xml";
    private static final String FIXTURE2 = "/input/tabs2.xml";

    @Test
    void mouseClickSelectsTheClickedTab() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object tp = d.find("tp");
        d.click(d.find("t2")); // a tab's bounds are its header rect
        assertThat(d.thinlet().getSelectedIndex(tp))
                .as("clicking the second tab header selects it")
                .isEqualTo(1);
    }

    @Test
    void keyboardArrowsSwitchTabsWithoutWrapping() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tp = d.find("tp");
        d.focusGained();
        d.click(d.find("t1")); // clicking the selected tab focuses the pane itself
        d.arrowRight();
        d.paint();
        assertThat(t.getSelectedIndex(tp)).as("Right moves to the next tab").isEqualTo(1);
        d.arrowRight();
        d.paint();
        assertThat(t.getSelectedIndex(tp))
                .as("Right again reaches the last tab")
                .isEqualTo(2);
        d.arrowRight();
        d.paint();
        assertThat(t.getSelectedIndex(tp))
                .as("Right at the last tab does not wrap")
                .isEqualTo(2);
        d.arrowLeft();
        d.paint();
        assertThat(t.getSelectedIndex(tp)).as("Left moves back").isEqualTo(1);
    }

    @Test
    void keyboardSwitchSkipsDisabledTabsAndFiresAction() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object tp = d.find("tp2");
        d.focusGained();
        d.click(d.find("t1"));
        d.arrowRight();
        d.paint();
        assertThat(t.getSelectedIndex(tp))
                .as("Right skips the disabled second tab and lands on the third")
                .isEqualTo(2);
        assertThat(h.events).as("the keyboard switch fires the pane's action").contains("tp2");
    }

    @Test
    void mouseClickOnADisabledTabIsANoOp() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Object tp = d.find("tp2");
        d.click(d.find("t2"));
        assertThat(d.thinlet().getSelectedIndex(tp))
                .as("a disabled tab header ignores the click")
                .isZero();
    }

    /**
     * The mouse/keyboard focus asymmetry: a mouse tab-switch walks focus into the
     * newly selected tab's first focusable child ({@code setNextFocusable}); a
     * keyboard switch leaves focus on the pane. Both paths fire the pane's
     * {@code action} after the switch. The initial {@code gained:tp2} comes from
     * {@link InputDriver#focusGained()} — the pane is the tree's first focusable.
     */
    @Test
    void mouseSelectionMovesFocusIntoTheTab_keyboardSelectionKeepsIt() throws IOException {
        RecordingHandler mouse = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, mouse);
        d.focusGained();
        d.click(d.find("t3"));
        assertThat(mouse.events)
                .as("mouse switch: the tab's button gains focus, then the pane's action fires")
                .containsExactly("gained:tp2", "gained:b3", "tp2");

        RecordingHandler keys = new RecordingHandler();
        InputDriver d2 = InputDriver.load(FIXTURE2, keys);
        d2.focusGained();
        d2.arrowRight();
        d2.paint();
        assertThat(d2.thinlet().getSelectedIndex(d2.find("tp2")))
                .as("keyboard switch selected the third tab (disabled skipped)")
                .isEqualTo(2);
        assertThat(keys.events)
                .as("keyboard switch: the action fires but focus never moves off the pane")
                .containsExactly("gained:tp2", "tp2");
    }

    /** Quirk candidate raised in the PR: selecting a tab with no focusable content throws focus PAST the pane. */
    @Test
    @Tag("documents-current-behavior")
    void mouseSelectingATabWithNoFocusableContentThrowsFocusOutOfThePane() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        d.focusGained();
        d.click(d.find("t4"));
        assertThat(h.events)
                .as("focus walks past the empty tab to the next focusable outside the pane")
                .containsExactly("gained:tp2", "gained:bout", "tp2");
    }

    @Test
    void clickingTheCurrentTabRefocusesThePane() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        d.focusGained(); // initial focus lands on the pane (first focusable)
        d.click(d.find("tf1")); // focus the selected tab's textfield
        d.click(d.find("t1")); // click the already-selected tab header
        assertThat(h.events)
                .as("the field gained on click, then the header click refocuses the pane; no action fires")
                .containsExactly("gained:tp2", "gained:tf1", "gained:tp2");
    }
}
