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
 * Input regression net — Ctrl+C/X/V clipboard: exact text/caret getters (re-paint
 * diff skipped). Copy writes Thinlet's private clipboard field AND the system
 * clipboard; paste falls back to the field — deterministic on Xvfb either way (D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputClipboardTest {

    private static final String FIXTURE = "/input/textedit.xml";

    private static void ctrl(InputDriver d, int keyCode) {
        d.press(keyCode, InputEvent.CTRL_DOWN_MASK);
    }

    @Test
    void copyThenPasteDuplicatesTheSelection() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.focusGained();
        d.click(tf);
        d.type("hello");
        d.home();
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        ctrl(d, KeyEvent.VK_C);
        assertThat(t.getString(tf, "text")).as("copy leaves the text untouched").isEqualTo("hello");
        d.end();
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text"))
                .as("paste inserts the copied selection")
                .isEqualTo("hellohe");
        assertThat(t.getInteger(tf, "end"))
                .as("the caret lands after the insert")
                .isEqualTo(7);
    }

    @Test
    void cutRemovesTheSelectionAndPastesItBack() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.focusGained();
        d.click(tf);
        d.type("abcd");
        ctrl(d, KeyEvent.VK_A);
        ctrl(d, KeyEvent.VK_X);
        assertThat(t.getString(tf, "text"))
                .as("cut empties the selected-all field")
                .isEmpty();
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text")).as("paste restores the cut text").isEqualTo("abcd");
    }

    @Test
    void cutAndPasteRequireEditable_copyDoesNot() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.focusGained();
        d.click(tf);
        d.type("abc");
        t.setBoolean(tf, "editable", false);
        ctrl(d, KeyEvent.VK_A);
        ctrl(d, KeyEvent.VK_X);
        assertThat(t.getString(tf, "text"))
                .as("cut is gated on a non-editable field")
                .isEqualTo("abc");
        ctrl(d, KeyEvent.VK_C);
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text")).as("paste is gated too").isEqualTo("abc");
        t.setBoolean(tf, "editable", true);
        d.end();
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text"))
                .as("re-enabled paste inserts what the non-editable copy captured")
                .isEqualTo("abcabc");
    }

    @Test
    void passwordfieldNeverExportsItsText() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        Object pf = d.find("pf");
        d.focusGained();
        d.click(tf);
        d.type("public");
        ctrl(d, KeyEvent.VK_A);
        ctrl(d, KeyEvent.VK_C);

        d.click(pf);
        d.type("secret");
        ctrl(d, KeyEvent.VK_A);
        ctrl(d, KeyEvent.VK_C); // blocked: hidden field
        ctrl(d, KeyEvent.VK_X); // blocked too (needs editable && !hidden)
        assertThat(t.getString(pf, "text"))
                .as("the password text survives the blocked cut")
                .isEqualTo("secret");

        // Re-paint before aiming back at tf: its earlier Ctrl+A validate() negated
        // its stored bounds width until the next paint (InputSplitPaneTest note).
        d.paint();
        d.click(tf);
        d.end();
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text"))
                .as("the paste is the earlier public copy — the password never entered the clipboard")
                .isEqualTo("publicpublic");
    }

    @Test
    void pastingMultilineContentIntoASingleLineFieldDropsTheNewline() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object ta = d.find("ta");
        Object tf = d.find("tf");
        d.focusGained();
        d.click(ta);
        d.type("ab");
        d.enter();
        d.type("cd");
        assertThat(t.getString(ta, "text")).as("the textarea holds two lines").isEqualTo("ab\ncd");
        ctrl(d, KeyEvent.VK_A);
        ctrl(d, KeyEvent.VK_C);

        d.paint(); // re-validate before aiming (see the note in passwordfieldNeverExportsItsText)
        d.click(tf);
        ctrl(d, KeyEvent.VK_V);
        assertThat(t.getString(tf, "text"))
                .as("the single-line paste filter drops the newline character")
                .isEqualTo("abcd");
    }
}
