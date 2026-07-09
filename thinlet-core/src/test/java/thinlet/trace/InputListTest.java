/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — {@code list} selection: mouse click, arrow/Home/End
 * keyboard navigation, and Shift-extended multi-selection. Each scenario asserts
 * the exact selection via public getters (the primary, JDK-invariant signal) and
 * corroborates that the re-paint trace changed (same-JVM, tolerance 0.0).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputListTest {

    private static final String FIXTURE = "/input/list.xml";

    @Test
    void clickSelectsListItem() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object lst = d.find("lst");
        Trace before = d.paint();
        d.click(t.getItem(lst, 2));
        assertThat(t.getSelectedIndex(lst))
                .as("third item is selected by a click")
                .isEqualTo(2);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("selecting a row repaints the list")
                .isNotEmpty();
    }

    @Test
    void arrowDownMovesSelection() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object lst = d.find("lst");
        d.focusGained();
        d.click(t.getItem(lst, 0));
        assertThat(t.getSelectedIndex(lst)).as("first item selected").isEqualTo(0);
        Trace before = d.paint();
        d.arrowDown();
        assertThat(t.getSelectedIndex(lst))
                .as("arrow-down advances selection by one")
                .isEqualTo(1);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("moving the selection repaints the list")
                .isNotEmpty();
    }

    @Test
    void homeAndEndJumpSelection() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object lst = d.find("lst");
        int last = t.getCount(lst) - 1;
        d.focusGained();
        d.click(t.getItem(lst, 0));
        d.end();
        assertThat(t.getSelectedIndex(lst)).as("End selects the last item").isEqualTo(last);
        d.home();
        assertThat(t.getSelectedIndex(lst)).as("Home selects the first item").isEqualTo(0);
    }

    /**
     * Pins the Down-before-repaint race (D48/D50): the {@code :lead} adoption is a
     * <em>paint-time</em> write ({@code ensureLeadForPaint}), so a focused list that has
     * never been painted still has a null lead, and Down selects the <em>first</em> item.
     * Relocating the write to focus-gain time would make this select the second item.
     */
    @Test
    void downOnFocusedNeverPaintedListSelectsFirstItem() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object lst = d.find("lst");
        d.focusGained();
        assertThat(t.requestFocus(lst)).as("list is focusable").isTrue();
        d.arrowDown();
        assertThat(t.getSelectedIndex(lst))
                .as("Down with a null :lead (focused, never painted) selects the first item")
                .isEqualTo(0);
    }

    /**
     * The other side of the D48/D50 race pin: once a <em>focused</em> paint has run, the
     * paint-time write has adopted the first item as {@code :lead} (selecting nothing),
     * so Down moves to the <em>second</em> item. Dropping the paint-time adoption would
     * make this select the first item.
     */
    @Test
    void downAfterFocusedPaintSelectsSecondItem() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object lst = d.find("lst");
        d.focusGained();
        assertThat(t.requestFocus(lst)).as("list is focusable").isTrue();
        d.paint();
        assertThat(t.getSelectedIndex(lst))
                .as("the paint-time :lead adoption selects nothing by itself")
                .isEqualTo(-1);
        d.arrowDown();
        assertThat(t.getSelectedIndex(lst))
                .as("Down after a focused paint moves from the adopted lead to the second item")
                .isEqualTo(1);
    }

    @Test
    void shiftArrowExtendsMultiSelection() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object mlst = d.find("mlst");
        d.focusGained();
        d.click(t.getItem(mlst, 0));
        assertThat(t.getSelectedItems(mlst))
                .as("single item selected after click")
                .hasSize(1);
        Trace before = d.paint();
        d.press(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK);
        assertThat(t.getSelectedItems(mlst))
                .as("Shift+Down extends the selection to two items")
                .hasSize(2);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("extending the selection repaints the list")
                .isNotEmpty();
    }
}
