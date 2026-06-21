/* Thinlet (modernized) — Phase 2.x input-capture feasibility probe (test scope). */
package thinlet.trace;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import thinlet.Thinlet;

/**
 * Test-scope input driver for the Phase 2.x feasibility probe (cf. the plan in
 * {@code project-docs/backend-portability/input-harness-probe.md}). It synthesizes
 * AWT input events and pushes them through Thinlet's real {@code protected
 * processEvent} funnel on a headless Thinlet, so the probe exercises the same
 * dispatch path a live user would, not internal handler shortcuts.
 *
 * <p>Targets are resolved by widget {@code name} via {@link Thinlet#find(String)};
 * there is no public bounds getter, so absolute event coordinates are summed from
 * the {@code Object[]} {@code "bounds"} chain exactly as {@link LayoutTrace} reads
 * it. Post-event state is rendered to a {@link Trace} by reusing the Phase 1
 * {@link TracingGraphics2D} recorder. Lives in {@code thinlet.trace} to reuse the
 * package-private trace types without widening their visibility.
 */
final class InputProbeDriver {

    static final int WIDTH = 1024;
    static final int HEIGHT = 768;

    private final Funnel thinlet;
    private final Object root;
    private long when = 1L;

    private InputProbeDriver(Funnel thinlet, Object root) {
        this.thinlet = thinlet;
        this.root = root;
    }

    /** Thinlet subclass that exposes the protected event funnel to the driver. */
    private static final class Funnel extends Thinlet {
        void dispatch(AWTEvent e) {
            processEvent(e);
        }
    }

    static InputProbeDriver load(String resource, Object handler) throws IOException {
        Funnel thinlet = new Funnel();
        Object root;
        InputStream in = InputProbeDriver.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("probe fixture not found: " + resource);
        }
        try {
            root = thinlet.parse(in, handler);
        } finally {
            in.close();
        }
        thinlet.add(root);
        thinlet.setSize(WIDTH, HEIGHT);
        pump();
        InputProbeDriver driver = new InputProbeDriver(thinlet, root);
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
     * unless {@code focusinside} is set — so the probe synthesizes the focus gain.
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
    private static Rectangle bounds(Object widget) {
        Object[] entry = (Object[]) widget;
        while (entry != null) {
            if ("bounds".equals(entry[0])) {
                return (Rectangle) entry[1];
            }
            entry = (Object[]) entry[2];
        }
        return null;
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
