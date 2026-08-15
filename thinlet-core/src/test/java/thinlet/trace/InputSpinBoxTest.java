/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — spinbox: the live value is the {@code "text"} string, so
 * assertions are exact getter reads (re-paint diff skipped) and mouse spins start
 * clamp-adjacent to neutralize the 375ms auto-repeat (DECISIONS.md D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputSpinBoxTest {

    private static final String FIXTURE = "/input/spin.xml";
    private static final String ARROWS_FIXTURE = "/input/arrows.xml";
    private static final String VALUE_FIXTURE = "/input/spin-value.xml";

    /** Clicks the widget's up-arrow block: right-edge column, upper half. */
    private static void clickUpArrow(InputDriver d, Object spinbox) {
        Dimension dim = d.size(spinbox);
        d.clickAt(spinbox, dim.width - 2, dim.height / 4);
    }

    /** Clicks the widget's down-arrow block: right-edge column, lower half. */
    private static void clickDownArrow(InputDriver d, Object spinbox) {
        Dimension dim = d.size(spinbox);
        d.clickAt(spinbox, dim.width - 2, dim.height * 3 / 4);
    }

    @Test
    void mouseUpArrowStepsOnceExactlyAtClampAdjacentValue() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object sp = d.find("spup");
        clickUpArrow(d, sp);
        assertThat(d.thinlet().getString(sp, "text"))
                .as("clamp-adjacent up-arrow click steps 99 -> 100; auto-repeat steps clamp to no-ops")
                .isEqualTo("100");
        assertThat(h.events)
                .as("exactly one action for the single successful step")
                .containsExactly("spup");
    }

    @Test
    void mouseDownArrowStepsOnceExactlyAtClampAdjacentValue() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object sp = d.find("spdn");
        clickDownArrow(d, sp);
        assertThat(d.thinlet().getString(sp, "text"))
                .as("clamp-adjacent down-arrow click steps 1 -> 0")
                .isEqualTo("0");
        assertThat(h.events).as("exactly one action").containsExactly("spdn");
    }

    @Test
    void mouseUpArrowAtMaximumIsSilentNoOp() throws IOException {
        InputDriver d = InputDriver.load(ARROWS_FIXTURE, new InputHandler());
        Object sp = d.find("spmax");
        clickUpArrow(d, sp);
        assertThat(d.thinlet().getString(sp, "text"))
                .as("at maximum the up arrow changes nothing (processSpin returns false before any write)")
                .isEqualTo("100");
    }

    @Test
    void keyboardArrowsSpinByStepWithoutAnyTimer() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Thinlet t = d.thinlet();
        Object sp = d.find("spmid");
        d.focusGained();
        d.click(sp); // text-area click focuses the spinbox
        d.arrowUp();
        assertThat(t.getString(sp, "text"))
                .as("Up spins 50 -> 57 by the step attribute")
                .isEqualTo("57");
        d.arrowDown();
        assertThat(t.getString(sp, "text")).as("Down spins back 57 -> 50").isEqualTo("50");
        assertThat(h.events).as("one action per successful step").containsExactly("spmid", "spmid");
    }

    @Test
    void keyboardSpinClampsSilentlyAtMinimum() throws IOException {
        InputDriver d = InputDriver.load(ARROWS_FIXTURE, new InputHandler());
        Object sp = d.find("spmin");
        d.focusGained();
        d.click(sp);
        d.arrowDown();
        assertThat(d.thinlet().getString(sp, "text"))
                .as("at minimum the Down key changes nothing")
                .isEqualTo("0");
    }

    @Test
    void successfulSpinParksCaretSelectionAtTextEnd() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("spmid");
        d.focusGained();
        d.click(sp);
        d.arrowUp();
        assertThat(t.getInteger(sp, "start"))
                .as("processSpin sets the selection anchor to the new text length")
                .isEqualTo(2);
        assertThat(t.getInteger(sp, "end"))
                .as("and collapses the caret to index 0")
                .isZero();
    }

    /**
     * KNOWN-QUIRKS Q4, fixed in 0.2.x (D83): the DTD-registered integer {@code value} was
     * dead storage. It now tracks the spin state, and a declared {@code text} wins over a
     * conflicting declared {@code value} — the fixture declares {@code text="5" value="42"}.
     */
    @Test
    void valueTracksTheSpinState_andADeclaredTextWins() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("spdead");
        assertThat(t.getInteger(sp, "value"))
                .as("text is the display, so it wins the declared conflict: 5, not 42")
                .isEqualTo(5);
        d.focusGained();
        d.click(sp);
        d.arrowUp();
        assertThat(t.getString(sp, "text")).as("spinning moves the text").isEqualTo("6");
        assertThat(t.getInteger(sp, "value"))
                .as("and the value follows it — no longer dead storage")
                .isEqualTo(6);
    }

    /** Typing into the spinbox moves {@code value} too — the same mirror as spinning (D83). */
    @Test
    void typedDigitsMoveTheValue() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("spmid"); // text="50"
        d.focusGained();
        d.click(sp);
        d.type("7"); // appended at the caret the click placed

        assertThat(t.getInteger(sp, "value"))
                .as("value mirrors whatever the field now reads")
                .isEqualTo(Integer.parseInt(t.getString(sp, "text")));
    }

    /** The mirror runs both ways on the public setters (D83). */
    @Test
    void theTwoPropertiesTrackEachOtherThroughTheApi() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("spmid");

        t.setInteger(sp, "value", 33);
        assertThat(t.getString(sp, "text"))
                .as("setInteger(value) drives the display")
                .isEqualTo("33");

        t.setString(sp, "text", "8");
        assertThat(t.getInteger(sp, "value"))
                .as("setString(text) drives the value")
                .isEqualTo(8);
    }

    /**
     * A spinbox declaring only {@code value} gets the matching display; non-numeric text
     * (the 2005 corpus ships {@code <spinbox text="SpinBox">}) leaves {@code value} at its
     * default rather than inventing a number.
     */
    @Test
    void valueOnlySeedsTheDisplay_andNonNumericTextLeavesValueAlone() throws IOException {
        InputDriver d = InputDriver.load(VALUE_FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();

        Object seeded = d.find("spvalue");
        assertThat(t.getString(seeded, "text")).as("value=\"7\" alone shows 7").isEqualTo("7");
        assertThat(t.getInteger(seeded, "value")).isEqualTo(7);

        Object words = d.find("sptext");
        assertThat(t.getInteger(words, "value"))
                .as("unparseable text leaves the DTD default standing")
                .isZero();
    }

    /**
     * 0.2.x behavior (D75): {@code editable="false"} is read-only on every value path.
     * 2005 gated typed digits only, so the arrows and Up/Down still spun (KNOWN-QUIRKS Q5).
     * Each no-op assertion is paired with the same gesture on an editable sibling, so a
     * gesture that silently missed the widget cannot pass this test vacuously.
     */
    @Test
    void nonEditableSpinboxRejectsSpinningAsWellAsTyping() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sp = d.find("spro");
        d.focusGained();
        d.click(sp);
        d.type("7");
        assertThat(t.getString(sp, "text"))
                .as("typed digits are gated by editable=false")
                .isEqualTo("10");
        d.arrowUp();
        assertThat(t.getString(sp, "text")).as("the Up key no longer spins").isEqualTo("10");
        d.paint(); // flush any pending re-layout before pixel-aiming the arrow
        clickUpArrow(d, sp);
        assertThat(t.getString(sp, "text"))
                .as("the mouse arrow no longer spins either")
                .isEqualTo("10");

        // Controls: the identical gestures on editable spinboxes still move the value.
        Object keyControl = d.find("spmid");
        d.click(keyControl);
        d.arrowUp();
        assertThat(t.getString(keyControl, "text"))
                .as("the Up key gesture itself works: 50 + step 7")
                .isEqualTo("57");
        Object mouseControl = d.find("spup"); // clamp-adjacent: auto-repeat steps are no-ops
        d.paint();
        clickUpArrow(d, mouseControl);
        assertThat(t.getString(mouseControl, "text"))
                .as("the arrow-block click geometry itself works: 99 -> 100")
                .isEqualTo("100");
    }
}
