/* Thinlet (modernized) — Robot fidelity cross-check (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Fidelity cross-check for the synthetic {@link InputDriver}. The regression net drives
 * scripted AWT events straight into {@code processEvent}; this test runs the same
 * gestures through a <em>real</em> {@link Robot} — native OS input into a shown {@link
 * Frame} — and asserts the model outcome is identical. It validates the driver's
 * shortcuts (the synthesized {@code FOCUS_GAINED}, the KEY_PRESSED/KEY_TYPED split, the
 * priming MOUSE_MOVED) against the genuine native path.
 *
 * <p>Tagged {@code @Tag("robot")} and run on the base JDK-21 build only — it is excluded
 * from the cross-JDK matrix, which forks on Xvfb where native focus/timing add
 * variability (e.g. X keyboard auto-repeat: keys are pressed+released with zero delay
 * here so a held key can't repeat). Robot needs a real X server, so it works on the
 * controlled Xvfb {@code :99} but not under true {@code -Djava.awt.headless=true}.
 */
@Tag("robot")
@ExtendWith(XvfbDisplayExtension.class)
class InputRobotFidelityTest {

    private static final String FIXTURE = "/input/smoke.xml";

    @Test
    void nativeClickTogglesCheckbox_matchesSyntheticDriver() throws Exception {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        d.click(d.find("c1"));
        boolean synthetic = d.thinlet().getBoolean(d.find("c1"), "selected");

        boolean nativeOutcome = onShownFrame((thinlet, robot) -> {
            Object c1 = thinlet.find("c1");
            clickNative(robot, thinlet, c1);
            return thinlet.getBoolean(c1, "selected");
        });

        assertThat(nativeOutcome)
                .as("a real native click toggles the checkbox exactly like the synthetic driver")
                .isEqualTo(synthetic)
                .isTrue();
    }

    @Test
    void nativeTypingUpdatesTextfield_matchesSyntheticDriver() throws Exception {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object t1 = d.find("t1");
        d.focusGained();
        d.click(t1);
        d.type("hi");
        String synthetic = d.thinlet().getString(t1, "text");

        String nativeOutcome = onShownFrame((thinlet, robot) -> {
            Object field = thinlet.find("t1");
            clickNative(robot, thinlet, field); // native focus — no synthesized FOCUS_GAINED
            typeNative(robot, KeyEvent.VK_H, KeyEvent.VK_I);
            return thinlet.getString(field, "text");
        });

        assertThat(nativeOutcome)
                .as("real native focus + typing matches the synthetic driver (validates the "
                        + "synthesized FOCUS_GAINED and the KEY_PRESSED/KEY_TYPED split)")
                .isEqualTo(synthetic)
                .isEqualTo("hi");
    }

    // ---------------- native (Robot) harness ----------------

    @FunctionalInterface
    private interface OnFrame<T> {
        T run(Thinlet thinlet, Robot robot) throws Exception;
    }

    private static <T> T onShownFrame(OnFrame<T> action) throws Exception {
        Thinlet thinlet = new Thinlet();
        try (InputStream in = InputRobotFidelityTest.class.getResourceAsStream(FIXTURE)) {
            thinlet.add(thinlet.parse(in, new InputHandler()));
        }
        Frame frame = new Frame();
        frame.setUndecorated(true); // no title bar → thinlet fills the frame at screen (0,0)
        frame.setLayout(new BorderLayout());
        frame.add(thinlet, BorderLayout.CENTER);
        frame.setSize(InputDriver.WIDTH, InputDriver.HEIGHT);
        try {
            frame.setVisible(true);
            Robot robot = new Robot();
            robot.setAutoDelay(40);
            robot.setAutoWaitForIdle(true);
            robot.waitForIdle();
            return action.run(thinlet, robot);
        } finally {
            frame.dispose();
        }
    }

    private static void clickNative(Robot robot, Thinlet thinlet, Object widget) {
        Rectangle b = null;
        for (int i = 0; i < 80 && b == null; i++) { // bounds are computed on the first paint
            b = abs(thinlet, widget);
            if (b == null) {
                robot.delay(25);
            }
        }
        Point loc = thinlet.getLocationOnScreen();
        robot.mouseMove(loc.x + b.x + b.width / 2, loc.y + b.y + b.height / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    private static void typeNative(Robot robot, int... keyCodes) {
        robot.setAutoWaitForIdle(false);
        robot.setAutoDelay(0); // press+release back-to-back so X auto-repeat can't fire
        for (int k : keyCodes) {
            robot.keyPress(k);
            robot.keyRelease(k);
        }
        robot.setAutoDelay(40);
        robot.setAutoWaitForIdle(true);
        robot.waitForIdle();
    }

    /** Absolute-in-Thinlet bounds of a widget, summed from the Object[] "bounds" chain. */
    private static Rectangle abs(Thinlet t, Object w) {
        Rectangle self = rect(w);
        if (self == null) {
            return null;
        }
        int x = 0;
        int y = 0;
        for (Object c = w; c != null; c = t.getParent(c)) {
            Rectangle b = rect(c);
            if (b != null) {
                x += b.x;
                y += b.y;
            }
        }
        return new Rectangle(x, y, self.width, self.height);
    }

    private static Rectangle rect(Object w) {
        Object[] entry = (Object[]) w;
        while (entry != null) {
            if ("bounds".equals(entry[0])) {
                return (Rectangle) entry[1];
            }
            entry = (Object[]) entry[2];
        }
        return null;
    }
}
