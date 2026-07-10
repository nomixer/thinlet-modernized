/* Thinlet (modernized) — interaction-state golden capture (test scope). */
package thinlet.trace;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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
        final Supplier<Object> handler; // parse-time event handler for the fixture

        Scenario(String name, String fixture, Script script) {
            this(name, fixture, script, InputHandler::new);
        }

        // Corpus-driven scenarios (D53) pass CorpusHandler::new: the vendored
        // corpus XML binds demo action/init methods that Thinlet resolves at
        // parse time, so the minimal InputHandler would throw on load.
        Scenario(String name, String fixture, Script script, Supplier<Object> handler) {
            this.name = name;
            this.fixture = fixture;
            this.script = script;
            this.handler = handler;
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
        // Scrollbar + spinbox arrows (D51). Hover holds are timer-free; press
        // holds use NO-OP presses: at the scroll/spin extreme processScroll/
        // processSpin return false before any model write and the 300/375 ms
        // auto-repeat timer is never armed, so the held frame is
        // time-independent. Aim points derive from the :vertical/:horizontal
        // part rectangles and the widget size — bounds, never text metrics.
        s.add(new Scenario("scroll-vup-arrow-hover", "/input/scroll.xml", d -> {
            Object lst = d.find("slist");
            Rectangle v = (Rectangle) d.property(lst, ":vertical");
            d.hoverAt(lst, v.x + v.width / 2, v.y + 3);
        }));
        s.add(new Scenario("scroll-vup-arrow-press-attop", "/input/scroll.xml", d -> {
            Object lst = d.find("slist");
            Rectangle v = (Rectangle) d.property(lst, ":vertical");
            d.pressAndHoldAt(lst, v.x + v.width / 2, v.y + 3);
        }));
        s.add(new Scenario("scroll-vdown-arrow-hover", "/input/scroll.xml", d -> {
            Object lst = d.find("slist");
            Rectangle v = (Rectangle) d.property(lst, ":vertical");
            d.hoverAt(lst, v.x + v.width / 2, v.y + v.height - 3);
        }));
        s.add(new Scenario("scroll-vdown-arrow-press-atbottom", "/input/scroll.xml", d -> {
            Object lst = d.find("slist");
            // wheel far past the end: the scroll clamp leaves the view at the
            // exact bottom on every JDK, making the down-press a no-op
            for (int i = 0; i < 200; i++) {
                d.scroll(lst, 1);
            }
            Rectangle v = (Rectangle) d.property(lst, ":vertical");
            d.pressAndHoldAt(lst, v.x + v.width / 2, v.y + v.height - 3);
        }));
        s.add(new Scenario("arrows-hleft-arrow-hover", "/input/arrows.xml", d -> {
            Object lst = d.find("hlist");
            Rectangle h = (Rectangle) d.property(lst, ":horizontal");
            d.hoverAt(lst, h.x + 3, h.y + h.height / 2);
        }));
        s.add(new Scenario("arrows-hleft-arrow-press-atleft", "/input/arrows.xml", d -> {
            Object lst = d.find("hlist");
            Rectangle h = (Rectangle) d.property(lst, ":horizontal");
            d.pressAndHoldAt(lst, h.x + 3, h.y + h.height / 2);
        }));
        // Spinbox arrows occupy the right-edge block column, split at half height
        s.add(new Scenario("arrows-spin-up-arrow-hover", "/input/arrows.xml", d -> {
            Object sp = d.find("spmax");
            Dimension dim = d.size(sp);
            d.hoverAt(sp, dim.width - 4, dim.height / 4);
        }));
        s.add(new Scenario("arrows-spin-up-arrow-press-atmax", "/input/arrows.xml", d -> {
            Object sp = d.find("spmax");
            Dimension dim = d.size(sp);
            d.pressAndHoldAt(sp, dim.width - 4, dim.height / 4);
        }));
        s.add(new Scenario("arrows-spin-down-arrow-press-atmin", "/input/arrows.xml", d -> {
            Object sp = d.find("spmin");
            Dimension dim = d.size(sp);
            d.pressAndHoldAt(sp, dim.width - 4, dim.height * 3 / 4);
        }));
        // Tree/table model-state + focus renders (Package A): guard the
        // port-content painter's tree/table row paths ahead of its extraction
        s.add(new Scenario("tree-selected-lead-focus", "/input/tree.xml", d -> {
            d.focusGained();
            d.click(d.find("nb1"));
        }));
        // expand by keyboard: the mouse handle band is FontMetrics-fragile
        // (see InputTreeTest); Right sets `expanded` via the keyboard path
        s.add(new Scenario("tree-node-expanded", "/input/tree.xml", d -> {
            d.focusGained();
            d.click(d.find("na"));
            d.arrowRight();
        }));
        s.add(new Scenario("table-selected-lead-focus", "/input/table.xml", d -> {
            d.focusGained();
            d.click(d.thinlet().getItem(d.find("tbl"), 1));
        }));
        // Combobox transients (D50 g2): body hover tint on the non-editable
        // combobox; the tinted arrow is only drawn by the editable one
        s.add(new Scenario("combobox-body-hover", "/input/combobox.xml", d -> d.hover(d.find("cb"))));
        s.add(new Scenario("combobox2-arrow-hover", "/input/combobox2.xml", d -> {
            Object cb = d.find("cb2");
            Dimension dim = d.size(cb);
            d.hoverAt(cb, dim.width - 4, dim.height / 2);
        }));
        // pressing the editable combobox arrow also opens the popup (held;
        // no auto-repeat on combobox arrows, so no D51 no-op trick needed)
        s.add(new Scenario("combobox2-arrow-press", "/input/combobox2.xml", d -> {
            Object cb = d.find("cb2");
            Dimension dim = d.size(cb);
            d.pressAndHoldAt(cb, dim.width - 4, dim.height / 2);
        }));
        // editable-field caret: click the empty field (caret index 0 regardless
        // of FontMetrics), then type — indices stay keyboard-driven (D41)
        s.add(new Scenario("combobox2-editable-caret", "/input/combobox2.xml", d -> {
            d.focusGained();
            Object cb = d.find("cb2");
            Dimension dim = d.size(cb);
            d.clickAt(cb, 4, dim.height / 2);
            d.type("Hi");
        }));
        // Tab + menubar transients (Package C — the last unguarded hover
        // states). Tab hover gates on insidepart == the tab object, and only a
        // NON-selected tab renders the hover tint; hover the tab itself (its
        // bounds are the header rect)
        s.add(new Scenario("tabs-tab-hover", "/input/tabs.xml", d -> d.hover(d.find("t2"))));
        s.add(new Scenario("menu-title-hover", "/input/menu.xml", d -> d.hover(d.find("m1"))));
        // clicking a menubar title arms it and opens its popup, which stays
        // open after release — held model/popup state, no timer involvement
        s.add(new Scenario("menu-armed-open", "/input/menu.xml", d -> d.click(d.find("m1"))));
        // Focus rects not yet captured (Package D): slider knob/track focus via
        // the public requestFocus (no click — a click would also move the value),
        // and the splitpane divider focus via the proven child-click + F8 route
        s.add(new Scenario("slider-focus", "/input/slider.xml", d -> {
            d.focusGained();
            d.thinlet().requestFocus(d.find("sl"));
        }));
        s.add(new Scenario("splitpane-divider-focus", "/input/splitpane.xml", d -> {
            d.focusGained();
            d.click(d.find("bL"));
            d.press(KeyEvent.VK_F8);
        }));
        // Custom-font textarea: guards the port-content painter's per-widget
        // font path (the "font" attribute key), which the #57 extraction
        // corrupted to "t.font" — a fallback-to-default-font regression the
        // corpus goldens could not see (their only custom-font textareas sit on
        // non-selected tabs, never painted). A plain paint suffices; font="bold"
        // derives from the default family, so metrics stay within the ±2px gate.
        s.add(new Scenario("textarea-custom-font", "/input/fonttext.xml", d -> {}));

        // --- Corpus-driven coverage (D53): drive the vendored drafts corpus
        // through CorpusHandler and interact, to paint content the static net
        // never reaches (a non-selected tab / collapsed subtree is unpainted —
        // the D52 blind-spot class). Deterministic: stub handler = no dynamic
        // content, no timer-coupled state; a held-state paint after the gesture.
        // Reach unnamed tabs by index off root() (looks.xml's tabbedpane and its
        // tabs carry no name).
        //
        // looks.xml (root IS the tabbedpane, index 0 selected/painted). tab 2
        // (index 1) is the D52 twin: a font="bold" textarea plus spinbox/
        // progressbar/slider — the exact never-painted shape that hid the
        // "font" -> "t.font" corruption. The rest are the other blind tabs.
        s.add(corpusTab("corpus-looks-tab2", "/corpus/drafts/looks.xml", InputDriver::root, 1));
        s.add(corpusTab("corpus-looks-listtree", "/corpus/drafts/looks.xml", InputDriver::root, 2));
        s.add(corpusTab("corpus-looks-menu", "/corpus/drafts/looks.xml", InputDriver::root, 3));
        s.add(corpusTab("corpus-looks-splitpane", "/corpus/drafts/looks.xml", InputDriver::root, 4));
        s.add(corpusTab("corpus-looks-scrollable", "/corpus/drafts/looks.xml", InputDriver::root, 5));
        // widgets.xml (unnamed tabbedpane below root): Three = tree+list+table,
        // Fonts = a whole font="bold" tab.
        s.add(corpusTab("corpus-widgets-three", "/corpus/drafts/widgets.xml", TABBEDPANE, 2));
        s.add(corpusTab("corpus-widgets-fonts", "/corpus/drafts/widgets.xml", TABBEDPANE, 4));
        // demo.xml (selected="1" = Lists): Texts = textarea+checkboxes, Values =
        // sliders/spinboxes/progressbars + a font="bold" preview label.
        s.add(corpusTab("corpus-demo-texts", "/corpus/demo/demo.xml", TABBEDPANE, 0));
        s.add(corpusTab("corpus-demo-values", "/corpus/demo/demo.xml", TABBEDPANE, 2));
        // tabbedpane.xml (named "tabbed"): Two = textarea+checkbox, Five =
        // scrollable button panel.
        s.add(corpusTab("corpus-tabbedpane-two", "/corpus/drafts/tabbedpane.xml", d -> d.find("tabbed"), 1));
        s.add(corpusTab("corpus-tabbedpane-five", "/corpus/drafts/tabbedpane.xml", d -> d.find("tabbed"), 4));
        // eventlogger.xml (unnamed tabbedpane, action="tabChanged" — a no-op in
        // CorpusHandler): "2" = list+tree+table, "3" = textfields/areas, "4" = menus.
        s.add(corpusTab("corpus-eventlogger-lists", "/corpus/drafts/eventlogger.xml", TABBEDPANE, 1));
        s.add(corpusTab("corpus-eventlogger-text", "/corpus/drafts/eventlogger.xml", TABBEDPANE, 2));
        s.add(corpusTab("corpus-eventlogger-menu", "/corpus/drafts/eventlogger.xml", TABBEDPANE, 3));
        return Collections.unmodifiableList(s);
    }

    /** Locator for the first (usually only) tabbedpane in a fixture with no name. */
    private static final Function<InputDriver, Object> TABBEDPANE = d -> d.first("tabbedpane");

    /**
     * A corpus-driven scenario (D53) that selects tab {@code index} of the
     * tabbedpane found by {@code tabbedpane}, then paints — reaching content the
     * static net never draws (a non-selected tab). Driven by {@link CorpusHandler}
     * since corpus XML binds demo methods Thinlet resolves at parse time.
     */
    private static Scenario corpusTab(
            String name, String fixture, Function<InputDriver, Object> tabbedpane, int index) {
        return new Scenario(
                name, fixture, d -> d.click(d.thinlet().getItem(tabbedpane.apply(d), index)), CorpusHandler::new);
    }

    /** Runs a scenario and captures the post-gesture paint trace. */
    static Trace capture(Scenario scenario) throws Exception {
        InputDriver d = InputDriver.load(scenario.fixture, scenario.handler.get());
        scenario.script.run(d);
        return d.paint();
    }

    static File goldenFile(String name) {
        return new File(GoldenTraceRecorder.TRACE_DIR, "interaction/" + name + ".json");
    }
}
