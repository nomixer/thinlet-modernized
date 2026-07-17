/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Quirk pins Q9 (click-dead combobox icon) and Q10 (sort-glyph direction), plus the
 * {@code checkLocation} mousex-for-y canary. DECISIONS.md D68.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputQuirkPinsTest {

    private static final String COMBO_FIXTURE = "/input/comboicon.xml";

    @Test
    @Tag("documents-current-behavior")
    void clickingTheComboboxIconGlyphDoesNothing() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(COMBO_FIXTURE, h);
        Thinlet t = d.thinlet();
        Object cb = d.find("cb");
        int cy = d.size(cb).height / 2;
        // Discriminator baseline: a click in the text area places the caret at the
        // clicked offset (proves the geometry below actually lands on the widget).
        d.clickAt(cb, 60, cy);
        int caretAfterTextClick = t.getInteger(cb, "start");
        assertThat(caretAfterTextClick)
                .as("the text-area click landed: caret parks past the end of \"Hello\"")
                .isEqualTo(5);
        // The caret write defers a re-layout via the 2005 negative-width dirty
        // flag; hit-testing is blind until a paint resolves it (layoutIfDirty),
        // so paint here as a real app would between gestures.
        d.paint();
        // The editable combobox hit-tests x <= 2 + iconWidth as the "icon" part
        // (/icon/copy.gif is 16 px wide, so x=10 lands squarely on the glyph),
        // and handleMouseEvent's click branch excludes "icon" — a dead zone.
        d.clickAt(cb, 10, cy);
        assertThat(d.property(cb, ":combolist"))
                .as("clicking the icon glyph does not open the drop-down")
                .isNull();
        assertThat(t.getInteger(cb, "start"))
                .as("clicking the icon glyph does not move the caret either")
                .isEqualTo(caretAfterTextClick);
        assertThat(h.events).as("clicking the icon glyph fires nothing").isEmpty();
        // Contrast: the block-wide right strip is the live drop button.
        d.clickAt(cb, d.size(cb).width - 4, cy);
        assertThat(d.property(cb, ":combolist"))
                .as("the arrow strip does open the drop-down")
                .isNotNull();
    }

    @Test
    @Tag("documents-current-behavior")
    void ascendingSortDrawsADownwardTriangleGlyph() throws IOException {
        InputDriver d = InputDriver.load("/input/sortasc.xml", new InputHandler());
        assertThat(sortGlyphDirection(d.paint()))
                .as("sort=\"ascent\" draws the south-pointing glyph")
                .isEqualTo('S');
    }

    @Test
    @Tag("documents-current-behavior")
    void descendingSortDrawsAnUpwardTriangleGlyph() throws IOException {
        InputDriver d = InputDriver.load("/input/sortdesc.xml", new InputHandler());
        assertThat(sortGlyphDirection(d.paint()))
                .as("sort=\"descent\" draws the north-pointing glyph")
                .isEqualTo('N');
    }

    @Test
    void closingTheDropDownUnderTheCursorCommitsAndStaysConsistent() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(COMBO_FIXTURE, h);
        Thinlet t = d.thinlet();
        Object cb = d.find("cb");
        d.clickAt(cb, d.size(cb).width - 4, d.size(cb).height / 2); // open via the arrow strip
        Object combolist = d.property(cb, ":combolist");
        assertThat(combolist).as("the drop-down is open").isNotNull();
        // Click the second choice inside the popup. The release commits it and tears
        // the list down; closeCombo then re-derives hover state via checkLocation —
        // the 2005 call that passes mousex for the y argument (D68: unconsumed on
        // the MOUSE_ENTERED path today). The click point's desktop x and y differ,
        // so a future consumer of the corrupted y would diverge here.
        Rectangle cr = (Rectangle) d.property(t.getItem(cb, 1), "bounds");
        d.clickAt(combolist, cr.x + cr.width / 2, cr.y + cr.height / 2);
        assertThat(d.property(cb, ":combolist"))
                .as("the release closes the drop-down")
                .isNull();
        assertThat(t.getSelectedIndex(cb)).as("the clicked choice is committed").isEqualTo(1);
        assertThat(t.getString(cb, "text"))
                .as("the editable field takes the choice text")
                .isEqualTo("Choice-B");
        assertThat(h.events).as("the commit fires the combobox action").containsExactly("cb");
        assertThat(TraceComparator.compare(d.paint(), d.paint(), 0.0))
                .as("the post-close paint is stable")
                .isEmpty();
    }

    /**
     * Finds the bare 4-scanline arrow glyph in a trace: four consecutive horizontal
     * drawLine calls with widths exactly 0,2,4,6 and single-step y values — y
     * descending is the south/down glyph ('S'), ascending the north/up glyph ('N').
     */
    private static char sortGlyphDirection(Trace trace) {
        List<TraceCall> calls = trace.calls;
        for (int i = 0; i + 3 < calls.size(); i++) {
            Character dir = glyphAt(calls, i);
            if (dir != null) {
                return dir;
            }
        }
        return fail("no 4-scanline arrow glyph found in the trace");
    }

    private static Character glyphAt(List<TraceCall> calls, int start) {
        int previousY = Integer.MIN_VALUE;
        int step = 0;
        for (int k = 0; k < 4; k++) {
            TraceCall c = calls.get(start + k);
            if (!"drawLine".equals(c.op)) {
                return null;
            }
            int x1 = c.num.get(0).intValue();
            int y1 = c.num.get(1).intValue();
            int x2 = c.num.get(2).intValue();
            int y2 = c.num.get(3).intValue();
            if (y1 != y2 || Math.abs(x2 - x1) != 2 * k) {
                return null; // not a horizontal scanline of width 0,2,4,6
            }
            if (k == 1) {
                step = y1 - previousY;
                if (Math.abs(step) != 1) {
                    return null;
                }
            } else if (k > 1 && y1 - previousY != step) {
                return null;
            }
            previousY = y1;
        }
        return (step < 0) ? Character.valueOf('S') : Character.valueOf('N');
    }
}
