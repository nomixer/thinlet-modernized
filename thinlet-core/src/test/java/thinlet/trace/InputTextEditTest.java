/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
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
 * Input regression net — text editing in {@code textfield} / {@code passwordfield} /
 * {@code textarea} (Thinlet's {@code processField}). State is read via public getters:
 * {@code getString(w,"text")} for content and {@code getInteger(w,"start"/"end")} for the
 * selection (start = anchor, end = caret; equal when there is no selection). Because the
 * edits and caret navigation are index-based, they are inherently JDK-/font-invariant —
 * no scaling dimension is needed here (splitpane carries that). The one font-dependent
 * path, mouse click → caret index ({@code getCaretLocation}), is covered too: a manual
 * probe on a real desktop confirmed a click lands the caret on the nearest character
 * boundary (D41), and {@code processField}'s press branch self-primes its reference
 * ({@code setReference} with {@code :offset}=0), so {@link InputDriver#clickAt} only has
 * to aim the press. The exact landing index is FontMetrics-dependent, so those tests
 * assert the JDK-tolerant invariants — left/right clamps, left-to-right monotonicity, and
 * that some interior click lands strictly inside the text — not a pixel-exact index (D7).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputTextEditTest {

    private static final String FIXTURE = "/input/textedit.xml";

    private static InputDriver focused(String name) throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        d.focusGained();
        d.click(d.find(name));
        return d;
    }

    @Test
    void typingInsertsAtCaret() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello");
        assertThat(t.getString(tf, "text")).isEqualTo("hello");
        assertThat(t.getInteger(tf, "end"))
                .as("caret sits after the typed text")
                .isEqualTo(5);
    }

    @Test
    void backspaceDeletesCharBeforeCaret() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello");
        d.press(KeyEvent.VK_BACK_SPACE);
        assertThat(t.getString(tf, "text")).isEqualTo("hell");
        assertThat(t.getInteger(tf, "end")).isEqualTo(4);
    }

    @Test
    void deleteRemovesCharAfterCaret() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("abc");
        d.home();
        d.press(KeyEvent.VK_DELETE);
        assertThat(t.getString(tf, "text")).isEqualTo("bc");
        assertThat(t.getInteger(tf, "end")).isZero();
    }

    @Test
    void homeEndArrowsMoveCaret() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("abc");
        d.home();
        assertThat(t.getInteger(tf, "end")).as("Home to start").isZero();
        d.arrowRight();
        assertThat(t.getInteger(tf, "end")).as("Right advances one").isEqualTo(1);
        d.end();
        assertThat(t.getInteger(tf, "end")).as("End to text length").isEqualTo(3);
        d.arrowLeft();
        assertThat(t.getInteger(tf, "end")).as("Left retreats one").isEqualTo(2);
    }

    @Test
    void shiftArrowSelectsThenTypeReplaces() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello");
        d.home();
        d.arrowRight(); // caret at 1
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        assertThat(t.getInteger(tf, "start")).as("selection anchor").isEqualTo(1);
        assertThat(t.getInteger(tf, "end")).as("selection caret").isEqualTo(3);
        d.type("X");
        assertThat(t.getString(tf, "text")).as("typing replaces the selection").isEqualTo("hXlo");
        assertThat(t.getInteger(tf, "end")).isEqualTo(2);
    }

    @Test
    void ctrlASelectsAllThenTypeReplacesEverything() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello");
        d.press(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
        d.type("Z");
        assertThat(t.getString(tf, "text")).isEqualTo("Z");
    }

    @Test
    void backspaceDeletesTheSelection() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello");
        d.home();
        d.press(KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK); // select all
        d.press(KeyEvent.VK_BACK_SPACE);
        assertThat(t.getString(tf, "text")).isEmpty();
        assertThat(t.getInteger(tf, "end")).isZero();
    }

    @Test
    void editsAtBoundariesAreClampedNoOps() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("ab");
        d.home();
        d.press(KeyEvent.VK_BACK_SPACE); // backspace at position 0
        assertThat(t.getString(tf, "text")).as("backspace at start is a no-op").isEqualTo("ab");
        d.end();
        d.arrowRight(); // right at end
        assertThat(t.getInteger(tf, "end")).as("right at end is a no-op").isEqualTo(2);
        d.press(KeyEvent.VK_DELETE); // delete at end
        assertThat(t.getString(tf, "text")).as("delete at end is a no-op").isEqualTo("ab");
    }

    @Test
    void passwordFieldStoresRealText() throws IOException {
        InputDriver d = focused("pf");
        Thinlet t = d.thinlet();
        Object pf = d.find("pf");
        d.type("secret");
        assertThat(t.getString(pf, "text"))
                .as("the real text is stored; masking is a paint-only concern")
                .isEqualTo("secret");
    }

    @Test
    void mouseClickRepositionsCaretToClickedOffset() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello world"); // 11 chars; caret at 11, text shorter than the 24-col field
        int len = 11;
        // Re-validate before measuring/clicking: an edit (and every caret-setting click)
        // calls Thinlet.validate(), which negates bounds.width as a dirty flag until the
        // next paint re-lays-out. Stale (negative) bounds would make the hit-test miss the
        // field, so paint() before each measurement and each click — the same harness
        // artifact the splitpane keyboard tests handle.
        d.paint();
        int w = d.size(tf).width;

        // Click at the far left: the caret collapses to the start of the text.
        d.clickAt(tf, 2);
        assertThat(t.getInteger(tf, "end"))
                .as("click at the left edge -> caret 0")
                .isZero();
        assertThat(t.getInteger(tf, "start"))
                .as("a click collapses any selection")
                .isZero();

        // Click well past the (short) text: the caret clamps to the text length.
        d.paint();
        d.clickAt(tf, w - 2);
        assertThat(t.getInteger(tf, "end"))
                .as("click past the text -> caret clamps to length")
                .isEqualTo(len);
        assertThat(t.getInteger(tf, "start")).isEqualTo(len);

        // Sweep left-to-right: the caret is monotonic non-decreasing, every single click
        // collapses the selection (start==end), and at least one interior click lands
        // strictly between 0 and length — proving real positioning, not just the two
        // clamps. Exact indices are FontMetrics-dependent, so only order/clamps/interior
        // existence are asserted; they hold on any JDK (D7).
        int prev = -1;
        boolean interior = false;
        for (int x = 2; x <= w - 2; x += Math.max(1, w / 20)) {
            d.paint(); // re-validate before this click's hit-test (the prior click dirtied width)
            d.clickAt(tf, x);
            int caret = t.getInteger(tf, "end");
            assertThat(caret)
                    .as("caret never decreases as the click moves right")
                    .isGreaterThanOrEqualTo(prev);
            assertThat(t.getInteger(tf, "start"))
                    .as("each single click collapses the selection")
                    .isEqualTo(caret);
            interior |= (caret > 0) && (caret < len);
            prev = caret;
        }
        assertThat(interior)
                .as("some interior click lands strictly inside the text")
                .isTrue();
    }

    @Test
    void mouseDragSelectsFromPressToReleaseOffset() throws IOException {
        InputDriver d = focused("tf");
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.type("hello world");
        int len = 11;
        d.paint(); // re-validate after the edit before measuring/pressing (see click test)
        int w = d.size(tf).width;
        int midY = d.size(tf).height / 2;

        // Press at the far left, drag past the text: anchor stays at the press offset (0),
        // the caret follows the drag and clamps at the text length -> the whole text is
        // selected. start<end proves the drag built a real selection (not just a caret move).
        d.dragInside(tf, 2, midY, w - 2, midY);
        assertThat(t.getInteger(tf, "start"))
                .as("drag anchor at the press offset (start of text)")
                .isZero();
        assertThat(t.getInteger(tf, "end"))
                .as("drag caret at the release offset (text length)")
                .isEqualTo(len);
    }

    @Test
    void textareaEnterInsertsNewlineAndBackspaceJoinsLines() throws IOException {
        InputDriver d = focused("ta");
        Thinlet t = d.thinlet();
        Object ta = d.find("ta");
        d.type("ab");
        d.enter(); // multiline: inserts a newline
        d.type("c");
        assertThat(t.getString(ta, "text")).isEqualTo("ab\nc");
        d.arrowLeft(); // caret between the newline and 'c'
        d.press(KeyEvent.VK_BACK_SPACE); // deletes the newline, joining the lines
        assertThat(t.getString(ta, "text")).isEqualTo("abc");
    }
}
