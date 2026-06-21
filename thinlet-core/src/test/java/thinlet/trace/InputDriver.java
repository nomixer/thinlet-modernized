/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
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
 * Test-scope input driver for the Phase 2.x input-capture regression suite (cf.
 * {@code project-docs/backend-portability/input-harness-probe.md}). It synthesizes
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
        int x = 0;
        int y = 0;
        for (Object w = widget; w != null; w = thinlet.getParent(w)) {
            Rectangle b = bounds(w);
            if (b != null) {
                x += b.x;
                y += b.y;
            }
        }
        Rectangle tb = bounds(widget);
        if (tb == null) {
            throw new IllegalStateException("target widget has no computed bounds");
        }
        return new Point(x + tb.width / 2, y + tb.height / 2);
    }

    /** Reads the {@code "bounds"} Rectangle from the Object[] chain, like LayoutTrace. */
    private Rectangle bounds(Object widget) {
        return (Rectangle) property(widget, "bounds");
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
