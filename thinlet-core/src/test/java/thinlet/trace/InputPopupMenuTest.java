/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — context menu ({@code popupmenu} attribute): open state is
 * the getter-less {@code ":popup"} chain plus {@code menushown}/action recordings;
 * the trigger is a meta press ({@link InputDriver#metaClick}; DECISIONS.md D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputPopupMenuTest {

    private static final String FIXTURE = "/input/contextmenu.xml";

    @Test
    void thePopupmenuAttributeIsPubliclyReadable() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        assertThat(d.thinlet().getWidget(d.find("lst"), "popupmenu"))
                .as("getWidget exposes the attached popupmenu")
                .isSameAs(d.find("pm"));
    }

    @Test
    void metaPressOpensTheMenuAtThePointerAndFiresMenushown() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object lst = d.find("lst");
        Object pm = d.find("pm");
        Trace before = d.paint();
        d.metaClick(lst);
        Object popup = d.property(pm, ":popup");
        assertThat(popup).as("the meta press opens the context popup").isNotNull();
        assertThat(h.events).as("and fires menushown").containsExactly("pm");
        Point center = pressPoint(d, lst);
        Rectangle pb = (Rectangle) d.property(popup, "bounds");
        assertThat(new Point(pb.x, pb.y))
                .as("the popup opens at the press point (un-clamped this far from the desktop edge)")
                .isEqualTo(center);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the open popup repaints visibly")
                .isNotEmpty();
    }

    @Test
    void itemClickFiresItsActionAndClosesThePopup() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object pm = d.find("pm");
        d.metaClick(d.find("lst"));
        Object popup = d.property(pm, ":popup");
        Rectangle ib = (Rectangle) d.property(d.find("pmi1"), "bounds");
        d.clickAt(popup, ib.x + ib.width / 2, ib.y + ib.height / 2);
        assertThat(h.events).as("menushown then the item's action").containsExactly("pm", "pmi1");
        assertThat(d.property(pm, ":popup")).as("the popup is closed").isNull();
    }

    @Test
    void aPlainClickDoesNotTriggerTheMenu() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        d.click(d.find("lst"));
        assertThat(d.property(d.find("pm"), ":popup"))
                .as("a primary-button click never opens the context menu")
                .isNull();
        assertThat(h.events).as("and menushown never fires").isEmpty();
    }

    @Test
    void aPressOutsideTheOpenPopupClosesItWithoutFiring() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object pm = d.find("pm");
        d.metaClick(d.find("lst"));
        assertThat(d.property(pm, ":popup")).as("open before the outside press").isNotNull();
        d.clickAt(d.find("lst"), 2, 2); // top-left corner, away from the centered popup
        assertThat(d.property(pm, ":popup")).as("the outside press closes it").isNull();
        assertThat(h.events).as("with no item fired").containsExactly("pm");
    }

    /** The widget-centre point metaClick pressed, in desktop coordinates. */
    private static Point pressPoint(InputDriver d, Object widget) {
        Rectangle b = (Rectangle) d.property(widget, "bounds");
        Object parent = d.thinlet().getParent(widget);
        int x = b.x + b.width / 2;
        int y = b.y + b.height / 2;
        for (Object w = parent; w != null; w = d.thinlet().getParent(w)) {
            Rectangle pb = (Rectangle) d.property(w, "bounds");
            if (pb != null) {
                x += pb.x;
                y += pb.y;
            }
        }
        return new Point(x, y);
    }
}
