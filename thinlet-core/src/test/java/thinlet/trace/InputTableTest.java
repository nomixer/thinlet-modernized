/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — {@code table}: selection by mouse and keyboard, the
 * shift/control paths, {@code perform}, and the inert column header (DECISIONS.md D78).
 * Rows are clicked by their widget object ({@code d.click(getItem(...))}); the driver's
 * {@code origin} accounts for the header/scroll offset since D80. The header band is
 * measured from the live {@code :port} rect, never a font-derived constant, so the
 * cross-JDK rows are safe by construction.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputTableTest {

    private static final String FIXTURE = "/input/table.xml";
    private static final String FIXTURE2 = "/input/table2.xml";

    /** The header band's height: {@code :port}.y is the top inset layoutScroll reserved for it. */
    private static int headerHeight(InputDriver d, Object table) {
        return ((Rectangle) d.property(table, ":port")).y;
    }

    /** Row indices currently marked selected, in model order — "" when none are. */
    private static String selectedRows(Thinlet t, Object table) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.getCount(table); i++) {
            if (t.getBoolean(t.getItem(table, i), "selected")) {
                sb.append(sb.length() == 0 ? "" : ",").append(i);
            }
        }
        return sb.toString();
    }

    @Test
    void clickSelectsTheRowUnderThePointer() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("tbl");
        Trace before = d.paint();
        d.click(t.getItem(tbl, 1));
        assertThat(t.getSelectedIndex(tbl)).as("the clicked row is selected").isEqualTo(1);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("selecting a row repaints the table")
                .isNotEmpty();
    }

    @Test
    void clickingAnotherRowMovesTheSelectionInSingleMode() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("tbl");
        d.click(t.getItem(tbl, 1));
        d.click(t.getItem(tbl, 2));
        assertThat(selectedRows(t, tbl))
                .as("selection=single holds exactly one row: the newest click")
                .isEqualTo("2");
    }

    @Test
    void arrowKeysMoveTheSelectionAndHomeEndJump() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("tbl");
        d.focusGained();
        d.click(t.getItem(tbl, 0));
        d.arrowDown();
        assertThat(t.getSelectedIndex(tbl)).as("arrow-down advances one row").isEqualTo(1);
        d.arrowUp();
        assertThat(t.getSelectedIndex(tbl)).as("arrow-up goes back").isEqualTo(0);
        d.end();
        assertThat(t.getSelectedIndex(tbl)).as("End jumps to the last row").isEqualTo(2);
        d.home();
        assertThat(t.getSelectedIndex(tbl)).as("Home jumps to the first row").isEqualTo(0);
    }

    @Test
    void doubleClickFiresPerformAfterTheSelectionActions() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        d.click(t.getItem(tbl, 0));
        assertThat(h.events).as("a single click fires action only").containsExactly("multi");
        h.events.clear();
        d.doubleClick(t.getItem(tbl, 1));
        assertThat(h.events)
                .as("action fires once for the selection change, then perform on the clickCount-2 press "
                        + "— the second press re-selects an already-selected row, which fires nothing")
                .containsExactly("multi", "perform");
    }

    @Test
    void shiftClickExtendsTheSelectionFromTheLead() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        d.click(t.getItem(tbl, 1));
        d.clickWithModifiers(t.getItem(tbl, 3), InputEvent.SHIFT_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("shift-click selects the whole run from the lead to the clicked row")
                .isEqualTo("1,2,3");
    }

    @Test
    void controlClickTogglesOneRowInMultipleMode() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        d.click(t.getItem(tbl, 0));
        d.clickWithModifiers(t.getItem(tbl, 2), InputEvent.CTRL_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("control-click adds a disjoint row, keeping the first")
                .isEqualTo("0,2");
        d.clickWithModifiers(t.getItem(tbl, 2), InputEvent.CTRL_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("control-clicking it again removes it")
                .isEqualTo("0");
    }

    @Test
    void intervalModeExtendsWithShift() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("interval");
        d.click(t.getItem(tbl, 0));
        d.clickWithModifiers(t.getItem(tbl, 2), InputEvent.SHIFT_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("selection=interval extends a contiguous run, as multiple does")
                .isEqualTo("0,1,2");
    }

    @Test
    void intervalModeRefusesTheDisjointControlClick() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("interval");
        d.click(t.getItem(tbl, 0));
        d.clickWithModifiers(t.getItem(tbl, 1), InputEvent.SHIFT_DOWN_MASK);
        d.clickWithModifiers(t.getItem(tbl, 3), InputEvent.CTRL_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("control-click cannot add a disjoint row here: only multiple toggles, "
                        + "so interval collapses to the clicked row")
                .isEqualTo("3");
    }

    @Test
    void shiftArrowExtendsTheSelectionFromTheMouseLead() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        d.focusGained();
        d.click(t.getItem(tbl, 1));
        d.press(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK);
        assertThat(selectedRows(t, tbl))
                .as("shift-arrow extends from the row the mouse left as lead")
                .isEqualTo("1,2");
    }

    /**
     * The column header is inert: {@code findComponent}'s table branch hit-tests the
     * scrollbars and stops at an empty {@code if} body where header handling would go,
     * so no click ever reaches a column (KNOWN-QUIRKS Q14).
     */
    @Test
    @Tag("documents-current-behavior")
    void clickingTheColumnHeaderDoesNothingAtAll() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        int header = headerHeight(d, tbl);
        assertThat(header)
                .as("the fixture's table really does reserve a header band")
                .isGreaterThan(0);
        d.click(t.getItem(tbl, 2)); // a live selection to prove the click below would have shown
        h.events.clear();
        d.clickAt(tbl, 10, header / 2);
        assertThat(t.getSelectedIndex(tbl))
                .as("the header swallows the click: the selection is untouched")
                .isEqualTo(2);
        assertThat(h.events).as("and nothing fires").isEmpty();
    }

    /** The user-visible consequence of Q14: the sort glyph is app-driven only. */
    @Test
    @Tag("documents-current-behavior")
    void clickingTheHeaderOfASortedColumnLeavesTheSortUnchanged() throws IOException {
        InputDriver d = InputDriver.load("/input/sortasc.xml", new InputHandler());
        Thinlet t = d.thinlet();
        Object tbl = d.find("tbl");
        Object column = t.getItem(t.getWidget(tbl, "header"), 0);
        d.clickAt(tbl, 10, headerHeight(d, tbl) / 2);
        assertThat(t.getChoice(column, "sort"))
                .as("no user gesture drives the sort glyph: only the app can set it")
                .isEqualTo("ascent");
    }

    @Test
    void clickingBelowTheLastRowLeavesTheSelectionAlone() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object tbl = d.find("multi");
        d.click(t.getItem(tbl, 1));
        h.events.clear();
        d.clickAt(tbl, 10, d.size(tbl).height - 2);
        assertThat(t.getSelectedIndex(tbl))
                .as("the empty strip below the rows selects nothing new")
                .isEqualTo(1);
        assertThat(h.events).as("and fires nothing").isEmpty();
    }

    @Test
    void aTableWithNoRowsAbsorbsClicksWithoutFiring() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE2, h);
        Thinlet t = d.thinlet();
        Object tbl = d.find("empty");
        d.click(tbl);
        assertThat(t.getSelectedIndex(tbl))
                .as("an empty table has nothing to select")
                .isEqualTo(-1);
        assertThat(h.events).as("and fires nothing").isEmpty();
    }

    /** The control for the header tests: same widget, no {@code <header>} child. */
    @Test
    void aHeaderlessTableReservesOnlyItsBorderAboveTheRows() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE2, new RecordingHandler());
        Thinlet t = d.thinlet();
        Object bare = d.find("bare");
        assertThat(headerHeight(d, bare))
                .as("with no header the top inset is just the 1 px border")
                .isEqualTo(1);
        assertThat(headerHeight(d, d.find("multi")))
                .as("whereas a header reserves a real band")
                .isGreaterThan(headerHeight(d, bare));
        d.click(t.getItem(bare, 1));
        assertThat(t.getSelectedIndex(bare))
                .as("and the rows are hit-tested from that top edge")
                .isEqualTo(1);
    }
}
