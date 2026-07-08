/* Thinlet (modernized) — interaction-state golden capture (test scope). */
package thinlet.trace;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The interaction-state golden scenarios (DECISIONS.md D45,
 * {@code project-docs/INTERACTION-GOLDENS-DESIGN.md}): each is a fixture plus a
 * gesture script that leaves the transient interaction state held — hover =
 * bare MOUSE_MOVED, press = MOUSE_PRESSED without release, focus = synthetic
 * FOCUS_GAINED + click — after which the paint trace is captured and compared
 * against a committed golden under {@code src/test/resources/trace/interaction/}.
 *
 * <p>Scenario discipline: gestures aim at widget bounds (never text metrics),
 * and carets are placed by keyboard (Home/typing), not by pixel-aimed clicks —
 * a pixel-aimed caret lands on a FontMetrics-dependent character index, which
 * would move the caret by whole characters across JDKs (the D41 lesson). Text
 * kept short so selection-highlight widths stay within the D7 ±2 px tolerance.
 */
final class InteractionScenarios {

    /** A gesture script run against a freshly loaded driver. */
    interface Script {
        void run(InputDriver d) throws Exception;
    }

    static final class Scenario {
        final String name; // golden file stem under trace/interaction/
        final String fixture; // classpath resource
        final Script script;

        Scenario(String name, String fixture, Script script) {
            this.name = name;
            this.fixture = fixture;
            this.script = script;
        }
    }

    private InteractionScenarios() {}

    static List<Scenario> all() {
        List<Scenario> s = new ArrayList<>();
        // Hover (mouseinside/insidepart held; renders only while no press is in flight)
        s.add(new Scenario("smoke-button-hover", "/input/smoke.xml", d -> d.hover(d.find("b1"))));
        s.add(new Scenario("smoke-checkbox-hover", "/input/smoke.xml", d -> d.hover(d.find("c1"))));
        // Press-and-hold (mousepressed/pressedpart held; checkbox also paints the
        // transient check-mark preview while pressed)
        s.add(new Scenario("smoke-button-press", "/input/smoke.xml", d -> d.pressAndHold(d.find("b1"))));
        s.add(new Scenario("smoke-checkbox-press", "/input/smoke.xml", d -> d.pressAndHold(d.find("c1"))));
        // Focus rect + toggled state (click completes; checkbox keeps focusowner)
        s.add(new Scenario("smoke-checkbox-focus", "/input/smoke.xml", d -> {
            d.focusGained();
            d.click(d.find("c1"));
        }));
        // Caret at index 0 in an empty field — the caret bar position is
        // FontMetrics-independent at the left edge
        s.add(new Scenario("smoke-textfield-focus-caret", "/input/smoke.xml", d -> {
            d.focusGained();
            d.click(d.find("t1"));
        }));
        // Selection highlight + caret collapsed to 0: type a short word, then
        // Shift+Home selects it right-to-left (keyboard-placed, JDK-invariant indices)
        s.add(new Scenario("textedit-field-selection", "/input/textedit.xml", d -> {
            d.focusGained();
            d.click(d.find("tf"));
            d.type("Hello");
            d.press(KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK);
        }));
        // Textarea under focus: two lines, caret at the end of the second
        s.add(new Scenario("textedit-textarea-caret", "/input/textedit.xml", d -> {
            d.focusGained();
            d.click(d.find("ta"));
            d.type("one");
            d.enter();
            d.type("two");
        }));
        // Focused list: selected row fill + lead-row focus rect (model state via
        // gesture; :lead set by the click)
        s.add(new Scenario("list-selected-lead-focus", "/input/list.xml", d -> {
            d.focusGained();
            d.click(d.thinlet().getItem(d.find("lst"), 1));
        }));
        // Open combolist popup with the lead moved one down — paints the popup
        // subtree with the lead-row highlight
        s.add(new Scenario("combobox-open-lead", "/input/combobox.xml", d -> {
            d.focusGained();
            d.click(d.find("cb"));
            d.arrowDown();
        }));
        return Collections.unmodifiableList(s);
    }

    /** Runs a scenario and captures the post-gesture paint trace. */
    static Trace capture(Scenario scenario) throws Exception {
        InputDriver d = InputDriver.load(scenario.fixture, new InputHandler());
        scenario.script.run(d);
        return d.paint();
    }

    static File goldenFile(String name) {
        return new File(GoldenTraceRecorder.TRACE_DIR, "interaction/" + name + ".json");
    }
}
