/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — scrollbar mouse parts: arrows, tracks, knob (D64). The
 * scroll offset lives in the private {@code :view} rectangle (no public getter), so
 * assertions read it via {@link InputDriver#viewRect} — with one re-paint diff
 * corroboration on the first arrow test since the primary observable is a
 * chain-walk, not a public getter.
 *
 * <p>Timer posture (D64): arrow/track presses arm the 300ms auto-repeat, so every
 * mouse assertion here is positioned to make repeats clamped no-ops: arrows are
 * clicked one 10px step from their clamp, and the fixture's page extent (the
 * {@code :port} height) exceeds the whole scroll range, so a track click lands
 * exactly at the clamp. Knob drags arm no timer. The scrollbar thickness equals
 * the {@code :vertical} rect width (/{@code :horizontal} height), which is how the
 * arrow-adjacent aim points are derived without touching FontMetrics.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputScrollBarTest {

    private static final String FIXTURE = "/input/scroll.xml";
    private static final String HFIXTURE = "/input/arrows.xml";

    private static Rectangle vbar(InputDriver d, Object widget) {
        return (Rectangle) d.property(widget, ":vertical");
    }

    private static int maxY(InputDriver d, Object widget) {
        Rectangle view = d.viewRect(widget);
        Rectangle port = (Rectangle) d.property(widget, ":port");
        return view.height - port.height;
    }

    /** Wheel-scrolls until the view clamps at the bottom (each notch is one exact 10px step). */
    private static void wheelToBottom(InputDriver d, Object widget) {
        for (int i = 0; i < 100; i++) {
            int before = d.viewRect(widget).y;
            d.scroll(widget, 1);
            if (d.viewRect(widget).y == before) {
                return;
            }
        }
        throw new IllegalStateException("view did not clamp within 100 wheel notches");
    }

    @Test
    void downArrowScrollsTenPixels_exactFromTheClampAdjacentPosition() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object lst = d.find("slist");
        wheelToBottom(d, lst);
        int max = maxY(d, lst);
        d.scroll(lst, -1); // one step above the bottom clamp
        assertThat(d.viewRect(lst).y)
                .as("positioned one 10px step above the clamp")
                .isEqualTo(max - 10);

        Trace before = d.paint();
        Rectangle v = vbar(d, lst);
        d.clickAt(lst, v.x + v.width / 2, v.y + v.height - 3); // the down arrow
        assertThat(d.viewRect(lst).y)
                .as("the down arrow scrolls exactly 10px into the clamp; repeats are no-ops")
                .isEqualTo(max);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the arrow scroll repaints visibly")
                .isNotEmpty();
    }

    @Test
    void upArrowScrollsTenPixelsBackToTheTop() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object lst = d.find("slist");
        d.scroll(lst, 1);
        assertThat(d.viewRect(lst).y).as("one wheel notch scrolls 10px").isEqualTo(10);
        Rectangle v = vbar(d, lst);
        d.clickAt(lst, v.x + v.width / 2, v.y + 3); // the up arrow
        assertThat(d.viewRect(lst).y)
                .as("the up arrow scrolls exactly 10px back to the top clamp")
                .isZero();
    }

    @Test
    void arrowPressAtTheExtremeIsASilentNoOp() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object lst = d.find("slist");
        Rectangle v = vbar(d, lst);
        d.clickAt(lst, v.x + v.width / 2, v.y + 3); // up arrow at the top
        assertThat(d.viewRect(lst).y)
                .as("at the top the up arrow changes nothing (processScroll returns false before the timer)")
                .isZero();
    }

    @Test
    void trackClickPagesByThePortExtent_clampedExactlyHere() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object lst = d.find("slist");
        int max = maxY(d, lst);
        Rectangle v = vbar(d, lst);
        int block = v.width; // bar thickness == arrow block size
        d.clickAt(lst, v.x + v.width / 2, v.y + v.height - block - 5); // downtrack, above the down arrow
        assertThat(d.viewRect(lst).y)
                .as("one page exceeds this fixture's whole range, so the downtrack click clamps exactly")
                .isEqualTo(max);

        d.paint();
        Rectangle v2 = vbar(d, lst);
        d.clickAt(lst, v2.x + v2.width / 2, v2.y + block + 5); // uptrack, below the up arrow
        assertThat(d.viewRect(lst).y)
                .as("and the uptrack click pages exactly back to the top")
                .isZero();
    }

    @Test
    void knobDragClampsExactlyAtBothEnds_noTimerInvolved() throws IOException {
        InputDriver down = InputDriver.load(FIXTURE, new InputHandler());
        Object lst = down.find("slist");
        int max = maxY(down, lst);
        Rectangle v = vbar(down, lst);
        int block = v.width;
        down.dragInside(lst, v.x + v.width / 2, v.y + block + 2, v.x + v.width / 2, v.y + v.height);
        assertThat(down.viewRect(lst).y)
                .as("dragging the knob past the track bottom clamps to the max scroll")
                .isEqualTo(max);

        InputDriver up = InputDriver.load(FIXTURE, new InputHandler());
        Object lst2 = up.find("slist");
        wheelToBottom(up, lst2);
        up.paint();
        Rectangle v2 = vbar(up, lst2);
        up.dragInside(lst2, v2.x + v2.width / 2, v2.y + v2.height - block - 2, v2.x + v2.width / 2, v2.y);
        assertThat(up.viewRect(lst2).y)
                .as("dragging the knob past the track top clamps to zero")
                .isZero();
    }

    /**
     * The horizontal mirror, via knob-drag clamps and at-clamp arrow no-ops — all
     * exact and timer-free. The ±10px arrow arithmetic itself is pinned on the
     * vertical bar; the wheel cannot position here (see the wheel-no-op test).
     * Also exercises a non-zero {@code :view.x} (the D61 residual gap).
     */
    @Test
    void horizontalKnobAndArrowsClampExactly() throws IOException {
        InputDriver d = InputDriver.load(HFIXTURE, new InputHandler());
        Object lst = d.find("hlist");
        Rectangle h = (Rectangle) d.property(lst, ":horizontal");
        int block = h.height; // bar thickness == arrow block size
        Rectangle view = d.viewRect(lst);
        Rectangle port = (Rectangle) d.property(lst, ":port");
        int maxX = view.width - port.width;
        d.dragInside(lst, h.x + block + 2, h.y + h.height / 2, h.x + h.width, h.y + h.height / 2);
        assertThat(d.viewRect(lst).x)
                .as("dragging the knob past the track right end clamps to the max scroll")
                .isEqualTo(maxX);

        d.paint();
        Rectangle h2 = (Rectangle) d.property(lst, ":horizontal");
        d.clickAt(lst, h2.x + h2.width - 3, h2.y + h2.height / 2); // right arrow at the right clamp
        assertThat(d.viewRect(lst).x)
                .as("the right arrow at the clamp is a no-op")
                .isEqualTo(maxX);

        d.dragInside(lst, h2.x + h2.width - block - 2, h2.y + h2.height / 2, h2.x, h2.y + h2.height / 2);
        assertThat(d.viewRect(lst).x)
                .as("dragging the knob past the track left end clamps to zero")
                .isZero();

        d.paint();
        Rectangle h3 = (Rectangle) d.property(lst, ":horizontal");
        d.clickAt(lst, h3.x + 3, h3.y + h3.height / 2); // left arrow at the left clamp
        assertThat(d.viewRect(lst).x)
                .as("the left arrow at the clamp is a no-op")
                .isZero();
    }

    /** The mouse wheel drives only the vertical bar: on a horizontal-only list it is a no-op. */
    @Test
    void wheelIsANoOpWithoutAVerticalScrollbar() throws IOException {
        InputDriver d = InputDriver.load(HFIXTURE, new InputHandler());
        Object lst = d.find("hlist");
        d.scroll(lst, 1);
        assertThat(d.viewRect(lst).x)
                .as("the wheel never moves the horizontal view")
                .isZero();
        assertThat(d.viewRect(lst).y)
                .as("and there is no vertical overflow to move")
                .isZero();
    }
}
