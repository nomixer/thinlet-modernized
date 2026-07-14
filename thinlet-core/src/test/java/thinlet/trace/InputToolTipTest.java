/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — tooltip HIDE triggers (the shown state is paint-pinned by
 * D62): {@code :tooltipbounds} presence is the binary observable, awaited through
 * the real 750ms timer; timing semantics stay excluded as non-deterministic (D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputToolTipTest {

    private static final String FIXTURE = "/input/tooltip.xml";

    @Test
    void mousePressHidesTheTooltip() throws IOException, InterruptedException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object btn = d.find("tip");
        d.hover(btn);
        d.awaitTooltip(btn);
        assertThat(d.property(btn, ":tooltipbounds"))
                .as("shown after the 750ms delay")
                .isNotNull();
        d.click(btn);
        assertThat(d.property(btn, ":tooltipbounds")).as("the press hides it").isNull();
    }

    @Test
    void keyPressHidesTheTooltip() throws IOException, InterruptedException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object btn = d.find("tip");
        d.focusGained();
        d.click(btn); // focus the button; the click's entry armed the timer, the press hid nothing yet
        d.awaitTooltip(btn);
        d.press(KeyEvent.VK_DOWN);
        assertThat(d.property(btn, ":tooltipbounds"))
                .as("any key press while focus is inside hides the tooltip")
                .isNull();
    }

    @Test
    void mouseExitHidesTheTooltip() throws IOException, InterruptedException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object btn = d.find("tip");
        d.hover(btn);
        d.awaitTooltip(btn);
        d.hoverAt(btn, -10, -10); // onto the surrounding panel
        assertThat(d.property(btn, ":tooltipbounds"))
                .as("leaving the widget hides it")
                .isNull();
    }
}
