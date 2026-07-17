/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — dialog header drag + edge resize: bounds are chain-walked
 * and COPIED before each gesture (the 2005 code mutates the Rectangle in place);
 * placement is font-derived, so assertions are relative deltas and clamps (D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputDialogTest {

    private static final String FIXTURE = "/input/dialog.xml";

    private static Rectangle boundsCopy(InputDriver d, Object widget) {
        Rectangle live = (Rectangle) d.property(widget, "bounds");
        return new Rectangle(live);
    }

    @Test
    void headerDragMovesTheDialogByTheExactDelta() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Object dlg = d.find("dlg");
        Rectangle b0 = boundsCopy(d, dlg);
        Trace before = d.paint();
        d.dragInside(dlg, b0.width / 2, 8, b0.width / 2 + 40, 8 + 25); // y=8 is inside the header strip
        Rectangle b1 = boundsCopy(d, dlg);
        assertThat(b1.x).as("the drag moves the dialog by the exact x delta").isEqualTo(b0.x + 40);
        assertThat(b1.y).as("and the exact y delta").isEqualTo(b0.y + 25);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the move repaints visibly")
                .isNotEmpty();
    }

    @Test
    void headerDragClampsInsideTheDesktop() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Object dlg = d.find("dlg");
        Rectangle b0 = boundsCopy(d, dlg);
        d.dragInside(dlg, b0.width / 2, 8, b0.width / 2 - 2000, 8 - 2000);
        Rectangle atMin = boundsCopy(d, dlg);
        assertThat(atMin.x).as("a far negative drag clamps to the left edge").isZero();
        assertThat(atMin.y).as("and the top edge").isZero();

        d.paint();
        d.dragInside(dlg, atMin.width / 2, 8, atMin.width / 2 + 4000, 8 + 4000);
        Rectangle atMax = boundsCopy(d, dlg);
        assertThat(atMax.x)
                .as("a far positive drag clamps to the right edge")
                .isEqualTo(InputDriver.WIDTH - atMax.width);
        assertThat(atMax.y).as("and the bottom edge").isEqualTo(InputDriver.HEIGHT - atMax.height);
    }

    @Test
    void edgeDragsResizeWhenResizable() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Object dlg = d.find("dlg");
        d.thinlet().setBoolean(dlg, "resizable", true);
        Rectangle b0 = boundsCopy(d, dlg);
        d.dragInside(dlg, b0.width - 2, b0.height / 2, b0.width - 2 + 40, b0.height / 2); // :e edge
        Rectangle b1 = boundsCopy(d, dlg);
        assertThat(b1.width).as("the east edge drag widens by the exact delta").isEqualTo(b0.width + 40);
        assertThat(b1.height).as("without touching the height").isEqualTo(b0.height);

        d.paint();
        d.dragInside(dlg, b1.width - 2, b1.height - 2, b1.width - 2 + 30, b1.height - 2 + 25); // :se corner
        Rectangle b2 = boundsCopy(d, dlg);
        assertThat(b2.width).as("the corner drag widens").isEqualTo(b1.width + 30);
        assertThat(b2.height).as("and heightens by the exact deltas").isEqualTo(b1.height + 25);
    }

    @Test
    void edgesAreInertWhenNotResizable() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Object dlg = d.find("dlg");
        Rectangle b0 = boundsCopy(d, dlg);
        d.dragInside(dlg, b0.width - 2, b0.height / 2, b0.width - 2 + 40, b0.height / 2);
        assertThat(boundsCopy(d, dlg))
                .as("without resizable=true the edge strip is plain dialog body")
                .isEqualTo(b0);
    }

    /** The fixed Q7 contract (0.2.x, D73): close is live; maximize/iconify glyphs are undrawn. */
    @Test
    void closeGlyphClosesTheDialogAndTheOtherGlyphsAreUndrawn() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object dlg = d.find("dlg");
        Rectangle b0 = boundsCopy(d, dlg);
        int titleheight = ((Integer) d.property(dlg, ":titleheight")).intValue();

        // Press on the X but release away from it: a cancel, not a close — and the
        // glyph is no longer part of the drag handle, so the dialog must not move.
        d.dragInside(dlg, b0.width - 10, 8, b0.width - 10 - 3 * titleheight, 8);
        assertThat(d.find("dlg"))
                .as("release away from the glyph cancels the close")
                .isNotNull();
        assertThat(boundsCopy(d, dlg)).as("the glyph is not a drag handle").isEqualTo(b0);

        // The undrawn maximize spot (one glyph-width left of close) is plain header
        // again: dragging there moves the dialog.
        d.paint();
        d.dragInside(dlg, b0.width - 10 - titleheight, 8, b0.width - 10 - titleheight + 20, 8);
        assertThat(boundsCopy(d, dlg).x)
                .as("the old maximize spot drags the dialog — nothing is drawn or wired there")
                .isEqualTo(b0.x + 20);

        // A clean click on the X closes (removes) the dialog.
        d.paint();
        Rectangle b1 = boundsCopy(d, dlg);
        d.clickAt(dlg, b1.width - 10, 8);
        assertThat(d.find("dlg"))
                .as("clicking the close glyph closes the dialog")
                .isNull();
        assertThat(h.events).as("closing fires no bound handler — remove only").isEmpty();
    }
}
