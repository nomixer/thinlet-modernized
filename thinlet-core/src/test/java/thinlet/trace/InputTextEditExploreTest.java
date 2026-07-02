/* Thinlet (modernized) — Phase 2.y text-editing exploration (temporary). */
package thinlet.trace;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

@Tag("explore")
@ExtendWith(XvfbDisplayExtension.class)
class InputTextEditExploreTest {

    private static final String FIXTURE = "/input/textedit.xml";

    private static String dump(Thinlet t, Object w) {
        return "text=\"" + t.getString(w, "text") + "\" start=" + t.getInteger(w, "start") + " end="
                + t.getInteger(w, "end");
    }

    @Test
    void observe() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tf = d.find("tf");
        d.focusGained();
        d.click(tf);
        d.type("hello");
        System.out.println("[tx] after type hello : " + dump(t, tf));
        d.press(KeyEvent.VK_BACK_SPACE);
        System.out.println("[tx] after backspace   : " + dump(t, tf));
        d.press(KeyEvent.VK_HOME);
        System.out.println("[tx] after HOME        : " + dump(t, tf));
        d.press(KeyEvent.VK_RIGHT);
        System.out.println("[tx] after RIGHT       : " + dump(t, tf));
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        d.press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
        System.out.println("[tx] after shift+R+R   : " + dump(t, tf));
        d.type("X");
        System.out.println("[tx] type X over sel   : " + dump(t, tf));
        d.press(KeyEvent.VK_END);
        System.out.println("[tx] after END         : " + dump(t, tf));
        d.press(KeyEvent.VK_DELETE);
        System.out.println("[tx] delete at end     : " + dump(t, tf));

        Object pf = d.find("pf");
        d.click(pf);
        d.type("secret");
        System.out.println("[tx] password          : " + dump(t, pf));

        Object ta = d.find("ta");
        d.click(ta);
        d.type("ab");
        d.press(KeyEvent.VK_ENTER);
        d.type("c");
        System.out.println("[tx] textarea          : " + dump(t, ta));
    }
}
