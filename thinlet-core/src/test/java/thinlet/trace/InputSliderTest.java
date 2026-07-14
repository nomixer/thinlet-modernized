/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — slider: the integer {@code "value"} getter is exact
 * (re-paint diff skipped); interior mouse positions are font-mapped so only the
 * clamps are exact and interior clicks assert ordering (DECISIONS.md D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputSliderTest {

    private static final String FIXTURE = "/input/slider.xml";
    private static final String FIXTURE2 = "/input/slider2.xml";

    @Test
    void keyboardHomeEndArrowsAndPageKeysAreExact() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object sl = d.find("sl");
        d.focusGained();
        t.requestFocus(sl);

        d.home();
        assertThat(t.getInteger(sl, "value"))
                .as("Home collapses to the minimum")
                .isZero();
        d.arrowRight();
        assertThat(t.getInteger(sl, "value"))
                .as("Right steps by unit (default 5)")
                .isEqualTo(5);
        d.press(KeyEvent.VK_PAGE_DOWN);
        assertThat(t.getInteger(sl, "value"))
                .as("PageDown steps by block (default 25)")
                .isEqualTo(30);
        d.end();
        assertThat(t.getInteger(sl, "value")).as("End drives to the maximum").isEqualTo(100);
        d.arrowLeft();
        assertThat(t.getInteger(sl, "value")).as("Left steps back by unit").isEqualTo(95);
        d.press(KeyEvent.VK_PAGE_UP);
        assertThat(t.getInteger(sl, "value")).as("PageUp steps back by block").isEqualTo(70);
    }

    @Test
    void keyboardAtMaximumIsNoOpAndFiresNoAction() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object sl = d.find("sh");
        d.focusGained();
        t.requestFocus(sl);
        d.end();
        assertThat(t.getInteger(sl, "value")).as("End reaches the maximum").isEqualTo(100);
        assertThat(h.events).as("the End change fires one action").containsExactly("sh");
        d.arrowRight();
        assertThat(t.getInteger(sl, "value")).as("Right at max changes nothing").isEqualTo(100);
        assertThat(h.events).as("and a no-op change fires no action").containsExactly("sh");
    }

    @Test
    void mousePressAtTheEndsClampsExactly() throws IOException {
        InputDriver low = InputDriver.load(FIXTURE2, new RecordingHandler());
        Object shLow = low.find("sh");
        low.clickAt(shLow, 0);
        assertThat(low.thinlet().getInteger(shLow, "value"))
                .as("press at the far left clamps to the minimum")
                .isZero();

        InputDriver high = InputDriver.load(FIXTURE2, new RecordingHandler());
        Object shHigh = high.find("sh");
        high.clickAt(shHigh, high.size(shHigh).width - 1);
        assertThat(high.thinlet().getInteger(shHigh, "value"))
                .as("press at the far right clamps to the maximum")
                .isEqualTo(100);
    }

    @Test
    void mousePressJumpsProportionallyAndMonotonically() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object sl = d.find("sh");
        Dimension dim = d.size(sl);
        d.clickAt(sl, dim.width / 4);
        int quarter = t.getInteger(sl, "value");
        d.paint();
        d.clickAt(sl, dim.width / 2);
        int half = t.getInteger(sl, "value");
        assertThat(quarter)
                .as("a quarter-way press lands strictly inside the range")
                .isGreaterThan(0)
                .isLessThan(half);
        assertThat(half)
                .as("a half-way press lands further right, below the max")
                .isLessThan(100);
        assertThat(h.events).as("each value change fires an action").containsExactly("sh", "sh");
    }

    @Test
    void mouseDragTracksThePointer() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sl = d.find("sh");
        Dimension dim = d.size(sl);
        d.dragInside(sl, dim.width / 2, dim.height / 2, dim.width / 8, dim.height / 2);
        int landed = t.getInteger(sl, "value");
        assertThat(landed)
                .as("dragging left from the half-way press lands well below it")
                .isGreaterThan(0)
                .isLessThan(40);
    }

    @Test
    void verticalSliderMapsTheYAxis() throws IOException {
        InputDriver top = InputDriver.load(FIXTURE2, new RecordingHandler());
        Object svTop = top.find("sv");
        top.clickAt(svTop, top.size(svTop).width / 2, 0);
        assertThat(top.thinlet().getInteger(svTop, "value"))
                .as("press at the very top clamps to the minimum")
                .isZero();

        InputDriver bottom = InputDriver.load(FIXTURE2, new RecordingHandler());
        Object svBottom = bottom.find("sv");
        bottom.clickAt(svBottom, bottom.size(svBottom).width / 2, bottom.size(svBottom).height - 1);
        assertThat(bottom.thinlet().getInteger(svBottom, "value"))
                .as("press at the very bottom clamps to the maximum")
                .isEqualTo(100);
    }

    /** Locked 2005 semantics: a press teleports the knob to the pointer — no track paging. See KNOWN-QUIRKS Q6. */
    @Test
    @Tag("documents-current-behavior")
    void trackPressTeleportsTheKnob_noPageStepSemantics() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object sl = d.find("sh");
        assertThat(t.getInteger(sl, "value")).as("starting value").isEqualTo(40);
        d.clickAt(sl, d.size(sl).width - 1);
        assertThat(t.getInteger(sl, "value"))
                .as("a far-right press jumps straight to 100 — not 40 + block(25) as page semantics would")
                .isEqualTo(100);
    }
}
