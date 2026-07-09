/* Thinlet (modernized) — input-capture regression suite (test scope). */
package thinlet.trace;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import thinlet.Thinlet;

/**
 * Test-scope input driver for the input-capture regression suite (cf.
 * {@code project-docs/backend-portability/INPUT-HARNESS-PROBE.md}). It synthesizes
 * AWT input events and pushes them through Thinlet's real {@code protected
 * processEvent} funnel on a headless Thinlet, so the suite exercises the same
 * dispatch path a live user would, not internal handler shortcuts.
 *
 * <p>Targets are resolved by widget {@code name} via {@link Thinlet#find(String)}
 * or by index via {@link Thinlet#getItem(Object, int)}; there is no public bounds
 * getter, so absolute event coordinates are summed from the {@code Object[]}
 * {@code "bounds"} chain exactly as {@link LayoutTrace} reads it (correct while the
 * target is not scrolled, i.e. its viewport offset {@code :view} is zero — which
 * holds for every selection fixture here). Post-event state is rendered to a {@link
 * Trace} by reusing the Phase 1 {@link TracingGraphics2D} recorder. Lives in {@code
 * thinlet.trace} to reuse the package-private trace types without widening their
 * visibility.
 *
 * <p>Keyboard dispatch caveat (Thinlet {@code processEvent}): navigation/control
 * keys (arrows, Home/End, PageUp/Down, Enter, Esc) carry a control {@code keychar}
 * and are processed <em>only on KEY_PRESSED</em>, so {@link #press(int, int)}
 * sends KEY_PRESSED with {@code CHAR_UNDEFINED}; printable characters (incl. the
 * space bar) are processed <em>only on KEY_TYPED</em>, so they go through {@link
 * #type(String)}.
 */
final class InputDriver {

    static final int WIDTH = 1024;
    static final int HEIGHT = 768;

    private final Funnel thinlet;
    private final Object root;
    private long when = 1L;

    private InputDriver(Funnel thinlet, Object root) {
        this.thinlet = thinlet;
        this.root = root;
    }

    /** Thinlet subclass that exposes the protected event funnel to the driver. */
    private static final class Funnel extends Thinlet {
        void dispatch(AWTEvent e) {
            processEvent(e);
        }
    }

    static InputDriver load(String resource, Object handler) throws IOException {
        return load(resource, handler, 1.0);
    }

    /**
     * Loads a fixture with the base font scaled by {@code fontScale} (1.0 = normal).
     * A larger font is the simplest deterministic proxy for display scaling: it
     * grows every FontMetrics-driven dimension (row heights, text widths, preferred
     * sizes, the splitpane's content-derived divider) without a real HiDPI device
     * transform. Assertions stay getter-based, so this adds no cross-JDK fragility.
     */
    static InputDriver load(String resource, Object handler, double fontScale) throws IOException {
        Funnel thinlet = new Funnel();
        Object root;
        InputStream in = InputDriver.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("input fixture not found: " + resource);
        }
        try {
            root = thinlet.parse(in, handler);
        } finally {
            in.close();
        }
        thinlet.add(root);
        thinlet.setSize(WIDTH, HEIGHT);
        if (fontScale != 1.0) {
            Font base = thinlet.getFont();
            thinlet.setFont(base.deriveFont((float) (base.getSize2D() * fontScale)));
        }
        pump();
        InputDriver driver = new InputDriver(thinlet, root);
        // Thinlet computes widget bounds during paint, not on resize. Run one
        // throwaway paint so coordinates are available before the first event.
        driver.paint();
        return driver;
    }

    Thinlet thinlet() {
        return thinlet;
    }

    Object find(String name) {
        return thinlet.find(name);
    }

    /**
     * Makes Thinlet believe it owns the keyboard focus. Headless {@code
     * requestFocus()} delivers no native FOCUS_GAINED, and Thinlet drops key events
     * unless {@code focusinside} is set — so the driver synthesizes the focus gain.
     */
    void focusGained() {
        dispatch(new FocusEvent(thinlet, FocusEvent.FOCUS_GAINED));
    }

    /**
     * Single primary click at the widget centre. A MOUSE_MOVED is sent first
     * because Thinlet caches the hit-tested {@code mouseinside} from motion events;
     * MOUSE_PRESSED/RELEASED reuse it rather than re-hit-testing, so without the
     * move the press lands on nothing.
     */
    void click(Object widget) {
        Point p = center(widget);
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, p.x, p.y, 0, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_PRESSED, when++, 0, p.x, p.y, 1, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_RELEASED, when++, 0, p.x, p.y, 1, false));
    }

    /**
     * Primary click at ({@code xOffset},{@code yOffset}) relative to the widget's
     * top-left, so a test can aim at a specific spot — e.g. a character position in a
     * text field — rather than the widget centre that {@link #click(Object)} uses. The
     * MOUSE_MOVED prime is the same: Thinlet reuses the hit-tested {@code mouseinside}
     * from motion rather than re-testing on press. Thinlet's text mouse-press path
     * self-primes its caret reference ({@code setReference} + {@code :offset}=0 in
     * {@code processField}), so the click alone repositions the caret — no extra state
     * priming is needed, only an accurate absolute x.
     */
    void clickAt(Object widget, int xOffset, int yOffset) {
        Point o = origin(widget);
        int px = o.x + xOffset;
        int py = o.y + yOffset;
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, px, py, 0, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_PRESSED, when++, 0, px, py, 1, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_RELEASED, when++, 0, px, py, 1, false));
    }

    /** Convenience: {@link #clickAt(Object, int, int)} at the widget's vertical centre. */
    void clickAt(Object widget, int xOffset) {
        clickAt(widget, xOffset, size(widget).height / 2);
    }

    /**
     * Moves the pointer to the widget centre and holds it there — a bare
     * MOUSE_MOVED with no press. Thinlet caches the hit-tested {@code
     * mouseinside}/{@code insidepart} from motion events, so a subsequent {@link
     * #paint()} renders the hover visuals (the D45 held-state capture). The state
     * persists until another gesture moves the pointer.
     */
    void hover(Object widget) {
        Point p = center(widget);
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, p.x, p.y, 0, false));
    }

    /** {@link #hover(Object)} at ({@code xOffset},{@code yOffset}) from the widget's top-left. */
    void hoverAt(Object widget, int xOffset, int yOffset) {
        Point o = origin(widget);
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, o.x + xOffset, o.y + yOffset, 0, false));
    }

    /**
     * Presses the primary button on the widget centre and holds it — MOUSE_PRESSED
     * with no release, after the usual priming MOUSE_MOVED — so {@code
     * mousepressed}/{@code pressedpart} stay set and a subsequent {@link #paint()}
     * renders the pressed visuals (the D45 held-state capture). Scenarios use a
     * fresh driver each, so the un-released press never leaks between tests.
     */
    void pressAndHold(Object widget) {
        Point p = center(widget);
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, p.x, p.y, 0, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_PRESSED, when++, 0, p.x, p.y, 1, false));
    }

    /**
     * {@link #pressAndHold(Object)} at ({@code xOffset},{@code yOffset}) from the
     * widget's top-left — for pressing a <em>part</em> (scrollbar or spinbox
     * arrow) rather than the widget centre. Caution for captures: pressing a
     * scroll/spin arrow that can act arms the 300/375 ms auto-repeat timer; a
     * deterministic held frame needs a no-op press at the scroll/spin extreme,
     * where {@code processScroll}/{@code processSpin} return false before any
     * model write and the timer is never armed (D51).
     */
    void pressAndHoldAt(Object widget, int xOffset, int yOffset) {
        Point o = origin(widget);
        int x = o.x + xOffset;
        int y = o.y + yOffset;
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, x, y, 0, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_PRESSED, when++, 0, x, y, 1, false));
    }

    /** Types literal characters as KEY_TYPED events into the focus owner. */
    void type(String text) {
        for (int i = 0; i < text.length(); i++) {
            dispatch(new KeyEvent(thinlet, KeyEvent.KEY_TYPED, when++, 0, KeyEvent.VK_UNDEFINED, text.charAt(i)));
        }
    }

    /**
     * Presses and releases a navigation/control key by its {@code VK_*} keycode,
     * carrying the given modifier mask (e.g. {@link java.awt.event.InputEvent#SHIFT_DOWN_MASK}).
     * The {@code CHAR_UNDEFINED} keychar forces Thinlet's {@code control} branch so
     * the press is routed to {@code processKeyPress} with the keycode set; the
     * release is inert in Thinlet but mirrors real AWT.
     */
    void press(int keyCode, int modifiers) {
        dispatch(new KeyEvent(thinlet, KeyEvent.KEY_PRESSED, when++, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED));
        dispatch(new KeyEvent(thinlet, KeyEvent.KEY_RELEASED, when++, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    void press(int keyCode) {
        press(keyCode, 0);
    }

    void arrowDown() {
        press(KeyEvent.VK_DOWN);
    }

    void arrowUp() {
        press(KeyEvent.VK_UP);
    }

    void arrowLeft() {
        press(KeyEvent.VK_LEFT);
    }

    void arrowRight() {
        press(KeyEvent.VK_RIGHT);
    }

    void home() {
        press(KeyEvent.VK_HOME);
    }

    void end() {
        press(KeyEvent.VK_END);
    }

    void enter() {
        press(KeyEvent.VK_ENTER);
    }

    /**
     * Scrolls the widget under the pointer by {@code notches} wheel detents
     * (positive = down/right). Thinlet detects the wheel by {@code getID() ==
     * MOUSE_WHEEL} and reads {@code getWheelRotation()} reflectively, so a real
     * {@link MouseWheelEvent} is required; the priming MOUSE_MOVED sets {@code
     * mouseinside} (the wheel path reads its {@code :port}, with no fallback
     * hit-test).
     */
    void scroll(Object widget, int notches) {
        Point p = center(widget);
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, p.x, p.y, 0, false));
        dispatch(new MouseWheelEvent(
                thinlet,
                MouseEvent.MOUSE_WHEEL,
                when++,
                0,
                p.x,
                p.y,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                notches,
                notches));
    }

    /**
     * Drags inside {@code widget} from offset (fromX,fromY) to (toX,toY), both
     * relative to the widget's top-left, as a primary-button MOUSE_PRESSED →
     * MOUSE_DRAGGED → MOUSE_RELEASED. The priming MOUSE_MOVED sets {@code
     * mouseinside}; the press must land on the target (e.g. a splitpane's divider
     * strip), since press/drag reuse the hit-tested component rather than re-testing.
     */
    void dragInside(Object widget, int fromX, int fromY, int toX, int toY) {
        Point o = origin(widget);
        int fx = o.x + fromX;
        int fy = o.y + fromY;
        int tx = o.x + toX;
        int ty = o.y + toY;
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_MOVED, when++, 0, fx, fy, 0, false));
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_PRESSED, when++, 0, fx, fy, 1, false));
        // Two drags at the destination: when the cursor leaves the grabbed strip,
        // processEvent dispatches MOUSE_EXITED on the first drag and only routes
        // MOUSE_DRAGGED to the pressed component on the next — the OS streams many,
        // we send the minimum two. (See processEvent's MOUSE_DRAGGED branch.)
        for (int i = 0; i < 2; i++) {
            dispatch(new MouseEvent(
                    thinlet, MouseEvent.MOUSE_DRAGGED, when++, InputEvent.BUTTON1_DOWN_MASK, tx, ty, 0, false));
        }
        dispatch(new MouseEvent(thinlet, MouseEvent.MOUSE_RELEASED, when++, 0, tx, ty, 1, false));
    }

    /**
     * Resizes the root and re-runs layout through the real COMPONENT_RESIZED path
     * (the same one a window resize takes), so resize-dependent behavior — e.g. the
     * splitpane divider's clamp-on-shrink — is exercised exactly as in production.
     */
    void resize(int width, int height) {
        thinlet.setSize(width, height);
        dispatch(new ComponentEvent(thinlet, ComponentEvent.COMPONENT_RESIZED));
        paint();
    }

    /** Renders the current widget state to a Trace, reusing the Phase 1 recorder. */
    Trace paint() {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D raw = img.createGraphics();
        raw.setClip(0, 0, WIDTH, HEIGHT);
        List<TraceCall> sink = new ArrayList<>();
        TracingGraphics2D g = new TracingGraphics2D(raw, sink);
        try {
            thinlet.paint(g);
        } finally {
            g.dispose();
        }
        return new Trace(sink, LayoutTrace.walk(root));
    }

    /** Reads a widget's {@code :view} scroll rectangle (no public getter exists). */
    Rectangle viewRect(Object widget) {
        return (Rectangle) property(widget, ":view");
    }

    /**
     * Reads a property value straight from the {@code Object[]} chain (key matched
     * by {@code .equals} on the same interned literals Thinlet stores under),
     * mirroring {@link LayoutTrace}. Returns {@code null} when absent — e.g. a
     * combobox's {@code :combolist} is present only while its popup is open.
     */
    Object property(Object widget, String key) {
        Object[] entry = (Object[]) widget;
        while (entry != null) {
            if (key.equals(entry[0])) {
                return entry[1];
            }
            entry = (Object[]) entry[2];
        }
        return null;
    }

    private void dispatch(AWTEvent e) {
        on(() -> thinlet.dispatch(e));
        pump();
    }

    private Point center(Object widget) {
        Point o = origin(widget);
        Rectangle tb = bounds(widget);
        if (tb == null) {
            throw new IllegalStateException("target widget has no computed bounds");
        }
        return new Point(o.x + tb.width / 2, o.y + tb.height / 2);
    }

    /** Absolute top-left of a widget, summed from the Object[] "bounds" chain. */
    private Point origin(Object widget) {
        int x = 0;
        int y = 0;
        for (Object w = widget; w != null; w = thinlet.getParent(w)) {
            Rectangle b = bounds(w);
            if (b != null) {
                x += b.x;
                y += b.y;
            }
        }
        return new Point(x, y);
    }

    /** Reads the {@code "bounds"} Rectangle from the Object[] chain, like LayoutTrace. */
    private Rectangle bounds(Object widget) {
        return (Rectangle) property(widget, "bounds");
    }

    /**
     * Width/height of a widget's computed bounds, for choosing click offsets (there is
     * no public size getter; read from the same {@code "bounds"} chain as everything else).
     */
    Dimension size(Object widget) {
        Rectangle b = bounds(widget);
        if (b == null) {
            throw new IllegalStateException("target widget has no computed bounds");
        }
        return new Dimension(b.width, b.height);
    }

    private static void on(Runnable r) {
        try {
            EventQueue.invokeAndWait(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private static void pump() {
        try {
            for (int i = 0; i < 3; i++) {
                EventQueue.invokeAndWait(() -> {});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ignored) {
            // best-effort flush
        }
    }
}
