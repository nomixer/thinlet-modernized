/* Thinlet (modernized) — list/tree keyboard-navigation characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes {@code getListItem} — the target-row lookup behind list/tree
 * keyboard navigation (page up/down, home, end) — as a direct package-private
 * call over a hand-laid-out model. Decision record: {@code DECISIONS.md} D91.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ListNavigationTest {

    /** Five 20px-tall rows stacked at y=0,20,40,60,80, built through the public GUI parser. */
    private static final class FiveItemList {

        final Thinlet thinlet;
        final Object list;
        final Object[] items = new Object[5];

        FiveItemList() throws Exception {
            thinlet = new Thinlet();
            StringBuilder xml = new StringBuilder("<list>");
            for (int i = 0; i < 5; i++) {
                xml.append("<item text='").append(i).append("'/>");
            }
            xml.append("</list>");
            InputStream in = new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8));
            list = thinlet.parse(in);
            Object item = Thinlet.get(list, ":comp");
            for (int i = 0; i < 5; i++) {
                Thinlet.set(item, "bounds", new Rectangle(0, i * 20, 100, 20));
                items[i] = item;
                item = Thinlet.get(item, ":next");
            }
        }

        void viewport(int viewY, int portHeight) {
            Thinlet.set(list, ":view", new Rectangle(0, viewY, 100, portHeight));
            Thinlet.set(list, ":port", new Rectangle(0, 0, 100, portHeight));
        }

        String textOf(Object row) {
            return thinlet.getString(row, "text");
        }
    }

    @Test
    void pageDownWithNoLeadStopsAtTheLastRowWhoseTopFitsInTheViewport() throws Exception {
        FiveItemList l = new FiveItemList();
        l.viewport(0, 40);
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_DOWN, null, false);
        assertThat(l.textOf(row)).isEqualTo("2");
    }

    @Test
    void pageDownWithALeadAlreadyAtTheViewportBottomAdvancesByAFullExtraPage() throws Exception {
        FiveItemList l = new FiveItemList();
        l.viewport(0, 40);
        // item[1] spans y=20..40, exactly reaching the viewport bottom (0+40=40):
        // the target page is pushed one port.height further.
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_DOWN, l.items[1], false);
        assertThat(l.textOf(row)).isEqualTo("4");
    }

    @Test
    void pageDownWithALeadWellShortOfTheViewportBottomDoesNotAdvanceAnExtraPage() throws Exception {
        FiveItemList l = new FiveItemList();
        l.viewport(0, 40);
        // item[0] (y=0..20) is nowhere near the viewport bottom (40), so this
        // behaves exactly like the no-lead case: no extra-page bump.
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_DOWN, l.items[0], false);
        assertThat(l.textOf(row)).isEqualTo("2");
    }

    @Test
    void pageDownBreaksOnARowsTopEdgeUnlikePageUpsBottomEdgeCheck() throws Exception {
        // Gapped rows (not contiguous) separate the two possible break tests: PAGE_DOWN
        // stops as soon as a row's TOP passes vy, unlike PAGE_UP's bottom-edge check.
        FiveItemList l = new FiveItemList();
        Thinlet.set(l.items[0], "bounds", new Rectangle(0, 0, 100, 20));
        Thinlet.set(l.items[1], "bounds", new Rectangle(0, 50, 100, 20));
        Thinlet.set(l.items[2], "bounds", new Rectangle(0, 100, 100, 20));
        Thinlet.set(l.items[3], "bounds", new Rectangle(0, 150, 100, 20));
        Thinlet.set(l.items[4], "bounds", new Rectangle(0, 200, 100, 20));
        l.viewport(0, 80);
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_DOWN, null, false);
        assertThat(l.textOf(row)).isEqualTo("1");
    }

    @Test
    void pageUpWithNoLeadStopsAtTheFirstRowThatCrossesTheViewportTop() throws Exception {
        FiveItemList l = new FiveItemList();
        l.viewport(40, 40);
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_UP, null, false);
        assertThat(l.textOf(row)).isEqualTo("2");
    }

    @Test
    void pageUpWithALeadAlreadyAtTheViewportTopJumpsAFullExtraPageBack() throws Exception {
        FiveItemList l = new FiveItemList();
        l.viewport(40, 40);
        // item[2]'s top (y=40) is at or above the viewport top (view.y=40), so the
        // target line is pushed a further port.height above it, landing on row 0.
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_PAGE_UP, l.items[2], false);
        assertThat(l.textOf(row)).isEqualTo("0");
    }

    @Test
    void homeSelectsTheFirstItemRegardlessOfTheCurrentLead() throws Exception {
        FiveItemList l = new FiveItemList();
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_HOME, l.items[3], false);
        assertThat(l.textOf(row)).isEqualTo("0");
    }

    @Test
    void endWithALeadWalksForwardToTheLastItem() throws Exception {
        FiveItemList l = new FiveItemList();
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_END, l.items[1], false);
        assertThat(l.textOf(row)).isEqualTo("4");
    }

    @Test
    @Tag("documents-current-behavior")
    void endWithNoCurrentLeadReturnsNullInsteadOfTheLastItem() throws Exception {
        // Unlike VK_HOME (which reads the first child unconditionally), the VK_END
        // loop starts at `lead` itself: with no lead it never iterates, and `row`
        // keeps its initial null. KNOWN-QUIRKS Q17.
        FiveItemList l = new FiveItemList();
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_END, null, false);
        assertThat(row).isNull();
    }

    @Test
    void anUnrecognizedKeycodeReturnsNull() throws Exception {
        FiveItemList l = new FiveItemList();
        Object row = Thinlet.getListItem(l.thinlet, l.list, l.list, KeyEvent.VK_ESCAPE, l.items[1], false);
        assertThat(row).isNull();
    }
}
