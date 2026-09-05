/* Thinlet (modernized) — list keyboard-navigation dispatch characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes {@code processList} — the key dispatcher behind list/tree keyboard
 * handling — as a direct package-private call over a hand-laid-out model.
 * Decision record: {@code DECISIONS.md} D91.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ListProcessKeyTest {

    /** Five 20px-tall rows stacked at y=0,20,40,60,80, built through the public GUI parser. */
    private static final class FiveItemList {

        final Thinlet thinlet;
        final Object list;
        final Object[] items = new Object[5];

        FiveItemList(boolean multipleSelection) throws Exception {
            thinlet = new Thinlet();
            StringBuilder xml = new StringBuilder("<list");
            if (multipleSelection) {
                xml.append(" selection='multiple'");
            }
            xml.append('>');
            for (int i = 0; i < 5; i++) {
                xml.append("<item text='").append(i).append("'/>");
            }
            xml.append("</list>");
            InputStream in = new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8));
            list = thinlet.parse(in);
            Thinlet.set(list, "bounds", new Rectangle(0, 0, 100, 100));
            Thinlet.set(list, ":view", new Rectangle(0, 0, 100, 100));
            Thinlet.set(list, ":port", new Rectangle(0, 0, 100, 100));
            Thinlet.set(list, ":horizontal", new Rectangle(0, 0, 20, 10));
            Object item = Thinlet.get(list, ":comp");
            for (int i = 0; i < 5; i++) {
                Thinlet.set(item, "bounds", new Rectangle(0, i * 20, 100, 20));
                items[i] = item;
                item = Thinlet.get(item, ":next");
            }
        }

        void setLead(int index) {
            Thinlet.set(list, ":lead", items[index]);
        }

        Object lead() {
            return Thinlet.get(list, ":lead");
        }

        boolean selected(int index) {
            return thinlet.getBoolean(items[index], "selected", false);
        }

        void preselect(int index) {
            Thinlet.set(items[index], "selected", Boolean.TRUE);
        }

        boolean process(boolean shiftdown, boolean controldown, int keychar, int keycode) {
            return Thinlet.processList(thinlet, list, shiftdown, controldown, keychar, keycode, false);
        }
    }

    @Test
    void pressingUpAtTheFirstRowDoesNothing() throws Exception {
        FiveItemList l = new FiveItemList(false);
        l.setLead(0);
        boolean handled = l.process(false, false, 0, KeyEvent.VK_UP);
        assertThat(handled).isFalse();
        assertThat(l.lead()).isEqualTo(l.items[0]);
        assertThat(l.selected(0)).isFalse();
    }

    @Test
    void pressingDownWithoutModifiersSelectsTheNextRowAndMovesTheLead() throws Exception {
        FiveItemList l = new FiveItemList(false);
        l.setLead(0);
        boolean handled = l.process(false, false, 0, KeyEvent.VK_DOWN);
        assertThat(handled).isTrue();
        assertThat(l.lead()).isEqualTo(l.items[1]);
        assertThat(l.selected(1)).isTrue();
        assertThat(l.selected(0)).isFalse();
    }

    @Test
    void pressingDownWithControlHeldMovesTheLeadWithoutChangingSelection() throws Exception {
        FiveItemList l = new FiveItemList(false);
        l.setLead(0);
        l.preselect(0);
        boolean handled = l.process(false, true, 0, KeyEvent.VK_DOWN);
        assertThat(handled).isTrue();
        assertThat(l.lead()).isEqualTo(l.items[1]);
        assertThat(l.selected(0)).isTrue();
        assertThat(l.selected(1)).isFalse();
    }

    @Test
    void shiftDownWithMultipleSelectionExtendsTheRangeFromTheOldLead() throws Exception {
        FiveItemList l = new FiveItemList(true);
        l.setLead(0);
        boolean handled = l.process(true, false, 0, KeyEvent.VK_DOWN);
        assertThat(handled).isTrue();
        assertThat(l.lead()).isEqualTo(l.items[1]);
        assertThat(l.selected(0)).isTrue();
        assertThat(l.selected(1)).isTrue();
        assertThat(l.selected(2)).isFalse();
    }

    @Test
    void shiftDownOnASingleSelectionListFallsBackToPlainSelection() throws Exception {
        FiveItemList l = new FiveItemList(false);
        l.setLead(0);
        boolean handled = l.process(true, false, 0, KeyEvent.VK_DOWN);
        assertThat(handled).isTrue();
        assertThat(l.lead()).isEqualTo(l.items[1]);
        assertThat(l.selected(1)).isTrue();
        assertThat(l.selected(0)).isFalse();
    }

    @Test
    void leftArrowScrollsTheViewportLeftAndReportsWhetherThereIsMoreRoom() throws Exception {
        FiveItemList l = new FiveItemList(false);
        Thinlet.set(l.list, ":view", new Rectangle(20, 0, 100, 100));
        Thinlet.set(l.list, ":port", new Rectangle(0, 0, 80, 100));
        boolean handled = l.process(false, false, 0, KeyEvent.VK_LEFT);
        assertThat(handled).isTrue();
        assertThat(((Rectangle) Thinlet.get(l.list, ":view")).x).isEqualTo(10);
    }

    @Test
    void leftArrowThatReachesTheLeftEdgeReportsNoMoreRoom() throws Exception {
        // Scrolling exactly to x=0 leaves nothing further left to reveal, so unlike
        // the test above, processScroll's own return value here is false.
        FiveItemList l = new FiveItemList(false);
        Thinlet.set(l.list, ":view", new Rectangle(10, 0, 100, 100));
        Thinlet.set(l.list, ":port", new Rectangle(0, 0, 80, 100));
        boolean handled = l.process(false, false, 0, KeyEvent.VK_LEFT);
        assertThat(handled).isFalse();
        assertThat(((Rectangle) Thinlet.get(l.list, ":view")).x).isEqualTo(0);
    }

    @Test
    void rightArrowScrollsTheViewportRightAndReportsWhetherThereIsMoreRoom() throws Exception {
        FiveItemList l = new FiveItemList(false);
        Thinlet.set(l.list, ":view", new Rectangle(0, 0, 100, 100));
        Thinlet.set(l.list, ":port", new Rectangle(0, 0, 80, 100));
        boolean handled = l.process(false, false, 0, KeyEvent.VK_RIGHT);
        assertThat(handled).isTrue();
        assertThat(((Rectangle) Thinlet.get(l.list, ":view")).x).isEqualTo(10);
    }

    @Test
    void rightArrowWithNoRoomLeftDoesNotScroll() throws Exception {
        // view.x is already at its maximum (view.width - port.width), so there is
        // nothing further right to reveal: processList must report false, not the
        // "true" that every other page-scroll test in this class expects.
        FiveItemList l = new FiveItemList(false);
        Thinlet.set(l.list, ":view", new Rectangle(20, 0, 100, 100));
        Thinlet.set(l.list, ":port", new Rectangle(0, 0, 80, 100));
        boolean handled = l.process(false, false, 0, KeyEvent.VK_RIGHT);
        assertThat(handled).isFalse();
        assertThat(((Rectangle) Thinlet.get(l.list, ":view")).x).isEqualTo(20);
    }

    @Test
    void spaceKeySelectsTheCurrentLead() throws Exception {
        FiveItemList l = new FiveItemList(false);
        l.setLead(2);
        boolean handled = l.process(false, false, KeyEvent.VK_SPACE, 0);
        assertThat(handled).isTrue();
        assertThat(l.selected(2)).isTrue();
        assertThat(l.selected(0)).isFalse();
        assertThat(l.lead()).isEqualTo(l.items[2]);
    }

    @Test
    void controlASelectsAllRowsWhenMultipleSelectionIsEnabled() throws Exception {
        FiveItemList l = new FiveItemList(true);
        boolean handled = l.process(false, true, 0, KeyEvent.VK_A);
        assertThat(handled).isTrue();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isTrue();
        }
    }

    @Test
    void controlWithTheAlternateSlashKeycodeAlsoSelectsAllRows() throws Exception {
        FiveItemList l = new FiveItemList(true);
        boolean handled = l.process(false, true, 0, 0xBF);
        assertThat(handled).isTrue();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isTrue();
        }
    }

    @Test
    void controlAOnASingleSelectionListDoesNothing() throws Exception {
        FiveItemList l = new FiveItemList(false);
        boolean handled = l.process(false, true, 0, KeyEvent.VK_A);
        assertThat(handled).isFalse();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isFalse();
        }
    }

    @Test
    void controlBackslashDeselectsAllRows() throws Exception {
        FiveItemList l = new FiveItemList(true);
        for (int i = 0; i < 5; i++) {
            l.preselect(i);
        }
        boolean handled = l.process(false, true, 0, 0xDC);
        assertThat(handled).isTrue();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isFalse();
        }
    }

    @Test
    void controlWithAnUnrelatedKeycodeDoesNothing() throws Exception {
        FiveItemList l = new FiveItemList(true);
        boolean handled = l.process(false, true, 0, KeyEvent.VK_ESCAPE);
        assertThat(handled).isFalse();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isFalse();
        }
    }

    @Test
    void typingACharacterSelectsTheFirstRowWhoseTextStartsWithIt() throws Exception {
        FiveItemList l = new FiveItemList(false);
        boolean handled = l.process(false, false, '2', 0);
        assertThat(handled).isTrue();
        assertThat(l.selected(2)).isTrue();
        assertThat(l.selected(0)).isFalse();
    }

    @Test
    void typingACharacterWithNoMatchingRowDoesNothing() throws Exception {
        FiveItemList l = new FiveItemList(false);
        boolean handled = l.process(false, false, '9', 0);
        assertThat(handled).isFalse();
        for (int i = 0; i < 5; i++) {
            assertThat(l.selected(i)).isFalse();
        }
    }
}
