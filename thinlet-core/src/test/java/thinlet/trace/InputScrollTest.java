/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — mouse-wheel scrolling of an overflowing {@code list}. The
 * scroll offset has no public getter; it lives in the private {@code :view}
 * rectangle, read here off the {@code Object[]} model. Assertions are on the
 * <em>direction</em> of {@code :view.y} (and the no-op at the top), never on an
 * exact pixel, so JDK-variable wheel/line-height differences cannot break them.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputScrollTest {

    private static final String FIXTURE = "/input/scroll.xml";

    @Test
    void wheelScrollsViewportDown() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object slist = d.find("slist");
        Rectangle view = d.viewRect(slist);
        assertThat(view).as("the overflowing list has a scroll viewport").isNotNull();
        assertThat(view.y).as("starts scrolled to the top").isZero();
        Trace before = d.paint();
        d.scroll(slist, 1); // one detent down
        assertThat(d.viewRect(slist).y)
                .as("wheel-down advances the scroll offset")
                .isGreaterThan(0);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("scrolling shifts the visible rows, repainting the list")
                .isNotEmpty();
    }

    @Test
    void wheelUpAtTopIsNoOp() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object slist = d.find("slist");
        assertThat(d.viewRect(slist).y).as("starts at the top").isZero();
        d.scroll(slist, -1); // wheel up while already at the top
        assertThat(d.viewRect(slist).y).as("cannot scroll above the top").isZero();
    }
}
