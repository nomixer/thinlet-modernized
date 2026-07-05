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
 * no scaling dimension is needed here (splitpane carries that). Deferred: mouse click →
 * caret index ({@code getCaretLocation}), the one font-dependent path — it depends on the
 * field's {@code :offset} scroll + {@code referencex} state that a bare synthetic press
 * doesn't prime; a good follow-up, and a candidate for the Robot cross-check to validate.
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
