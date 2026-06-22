/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import thinlet.Thinlet;

/**
 * Input regression net — {@code splitpane} divider, by keyboard and by drag, plus a
 * font-scaling pass. The divider position is the integer {@code "divider"} property
 * (read via {@link Thinlet#getInteger(Object, String)}), so every outcome is exact
 * and JDK-/hardware-invariant — no FontMetrics in the assertions.
 *
 * <p>Harness note: Thinlet defers re-layout by flagging a component dirty in {@code
 * validate()} (it negates {@code bounds.width}); the next {@code paint()} re-validates
 * it. So between keyboard steps whose handler reads {@code bounds} (END/RIGHT compute
 * {@code bounds.width − 5}) the test re-paints, exactly as the EDT would repaint
 * between keystrokes.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputSplitPaneTest {

    private static final String FIXTURE = "/input/splitpane.xml";
    private static final int MAX = InputDriver.WIDTH - 5; // splitpane fills the window; divider range 0..MAX

    /** F8 transfers focus to the enclosing splitpane; then arrows/Home/End move the divider. */
    @Test
    void keyboardMovesDivider() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("sp");
        d.focusGained();
        d.click(d.find("bL")); // focus a child, then F8 walks up to the splitpane
        d.press(KeyEvent.VK_F8);

        d.press(KeyEvent.VK_HOME);
        d.paint();
        assertThat(t.getInteger(sp, "divider"))
                .as("Home collapses the divider to 0")
                .isZero();

        d.press(KeyEvent.VK_END);
        d.paint();
        assertThat(t.getInteger(sp, "divider"))
                .as("End drives the divider to the max")
                .isEqualTo(MAX);

        d.press(KeyEvent.VK_LEFT);
        d.paint();
        assertThat(t.getInteger(sp, "divider"))
                .as("Left steps the divider back by 10")
                .isEqualTo(MAX - 10);

        d.press(KeyEvent.VK_RIGHT);
        d.paint();
        assertThat(t.getInteger(sp, "divider"))
                .as("Right steps forward by 10, capped at max")
                .isEqualTo(MAX);
    }

    /**
     * Dragging the divider strip sets {@code divider = cursorX − 2}: the 2px is the
     * grab reference centering the cursor on the 5px handle (correct, not a quirk).
     * Parameterized at 1× and 2× font to show the drag outcome is scale-invariant —
     * the font changes the *initial* divider, not where the drag lands.
     */
    @ParameterizedTest
    @ValueSource(doubles = {1.0, 2.0})
    void dragMovesDividerToCursorMinusGrabOffset(double fontScale) throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler(), fontScale);
        Thinlet t = d.thinlet();
        Object sp = d.find("sp");
        int strip = t.getInteger(sp, "divider") + 2; // press inside the current 5px handle
        d.dragInside(sp, strip, 50, 300, 50);
        assertThat(t.getInteger(sp, "divider"))
                .as("divider follows the cursor (300) minus the 2px handle-grab offset, at any font scale")
                .isEqualTo(298);
    }

    /** Larger font → wider first pane → larger content-derived default divider. */
    @Test
    void autoDividerScalesWithFont() throws IOException {
        InputDriver normal = InputDriver.load(FIXTURE, new InputHandler(), 1.0);
        InputDriver large = InputDriver.load(FIXTURE, new InputHandler(), 2.0);
        int divNormal = normal.thinlet().getInteger(normal.find("sp"), "divider");
        int divLarge = large.thinlet().getInteger(large.find("sp"), "divider");
        assertThat(divLarge)
                .as("the auto divider tracks the first pane's content size, so it grows with the font")
                .isGreaterThan(divNormal);
    }

    /**
     * Locked 2005 quirk: the divider is stored in absolute pixels and only ever
     * *clamped* on resize — never rescaled. So growing the splitpane does not keep the
     * split ratio, and shrinking past the divider clamps it down **permanently** (the
     * position is lost, not restored when the pane grows back). See KNOWN-QUIRKS Q2.
     */
    @Test
    @Tag("documents-current-behavior")
    void dividerIsAbsolutePixels_nonProportionalAndDestructiveClampOnResize() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("sp");
        t.setInteger(sp, "divider", 200);

        d.resize(400, 768);
        assertThat(t.getInteger(sp, "divider"))
                .as("50% split survives a shrink that still fits it")
                .isEqualTo(200);
        d.resize(800, 768);
        assertThat(t.getInteger(sp, "divider"))
                .as("grown back: divider is NOT rescaled — stays 200 (now 25%), not proportional")
                .isEqualTo(200);

        d.resize(150, 768);
        assertThat(t.getInteger(sp, "divider"))
                .as("shrink past the divider clamps it to width-5")
                .isEqualTo(145);
        d.resize(800, 768);
        assertThat(t.getInteger(sp, "divider"))
                .as("clamp is destructive: the 200 position is lost permanently, not restored on grow")
                .isEqualTo(145);
    }
}
