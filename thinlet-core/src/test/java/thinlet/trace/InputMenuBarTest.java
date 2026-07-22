/* Thinlet (modernized) — Phase 2.y input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Input regression net — menubar: armed title ({@code "selected"}) and open chain
 * ({@code ":popup"}) have no public getter, so assertions chain-walk via
 * {@link InputDriver#property} plus recorded action firings (DECISIONS.md D64).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputMenuBarTest {

    private static final String FIXTURE = "/input/menuaction.xml";

    /** Clicks a popup item at its centre — item bounds are stored popup-relative. */
    private static void clickPopupItem(InputDriver d, Object menubar, Object item) {
        Object popup = d.property(menubar, ":popup");
        Rectangle ib = (Rectangle) d.property(item, "bounds");
        d.clickAt(popup, ib.x + ib.width / 2, ib.y + ib.height / 2);
    }

    @Test
    void mousePressOnATitleArmsItAndOpensItsPopup() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        Trace before = d.paint();
        d.click(d.find("m1"));
        assertThat(d.property(mb, "selected")).as("the pressed title is armed").isSameAs(d.find("m1"));
        assertThat(d.property(mb, ":popup")).as("and its popup chain is open").isNotNull();
        assertThat(h.events).as("arming alone fires nothing").isEmpty();
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the open popup repaints visibly")
                .isNotEmpty();
    }

    @Test
    void mouseReleaseOnAnItemFiresItsActionAndClosesTheMenu() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        d.click(d.find("m1"));
        clickPopupItem(d, mb, d.find("mi1"));
        assertThat(h.events).as("the item's action fires exactly once").containsExactly("mi1");
        assertThat(d.property(mb, ":popup")).as("the popup chain is closed").isNull();
        assertThat(d.property(mb, "selected")).as("and the title is disarmed").isNull();
    }

    @Test
    void clickingTheOpenTitleTogglesTheMenuClosed() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        d.click(d.find("m1"));
        d.click(d.find("m1"));
        assertThat(d.property(mb, ":popup"))
                .as("the second press closes the popup")
                .isNull();
        assertThat(h.events).as("no action fires either way").isEmpty();
    }

    /**
     * 0.2.x behavior (D77): a disabled item swallows the release and the menu stays open,
     * so the user can retarget. 2005 fired nothing but tore the popup down anyway, which
     * read as "the menu accepted the click and did nothing" (KNOWN-QUIRKS Q13).
     */
    @Test
    void releaseOverADisabledItemFiresNothingAndLeavesTheMenuOpen() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        d.click(d.find("m1"));
        clickPopupItem(d, mb, d.find("mi2"));
        assertThat(h.events).as("the disabled item's action never fires").isEmpty();
        assertThat(d.property(mb, ":popup"))
                .as("and the menu stays open for a retarget")
                .isNotNull();
        // The retarget lands: the enabled sibling still fires and closes as it always did.
        clickPopupItem(d, mb, d.find("mi1"));
        assertThat(h.events).as("the enabled item fires").containsExactly("mi1");
        assertThat(d.property(mb, ":popup"))
                .as("and that release closes the menu")
                .isNull();
    }

    @Test
    void f10OpensTheFirstMenu_arrowsNavigateSkippingDisabled_enterFires() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        d.focusGained();
        d.click(d.find("body"));
        d.press(KeyEvent.VK_F10);
        assertThat(d.property(mb, "selected")).as("F10 arms the first title").isSameAs(d.find("m1"));
        Object popup = d.property(mb, ":popup");
        assertThat(popup).as("and opens its popup").isNotNull();

        d.arrowDown();
        assertThat(d.property(popup, "selected")).as("Down arms the first item").isSameAs(d.find("mi1"));
        d.arrowDown();
        assertThat(d.property(popup, "selected"))
                .as("Down again skips the disabled item")
                .isSameAs(d.find("mi3"));
        d.enter();
        assertThat(h.events).as("Enter fires the armed item").containsExactly("mi3");
        assertThat(d.property(mb, ":popup")).as("and closes the chain").isNull();
    }

    @Test
    void arrowRightMovesToTheNextMenu() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new RecordingHandler());
        Object mb = d.find("mb");
        d.focusGained();
        d.click(d.find("body"));
        d.press(KeyEvent.VK_F10);
        d.arrowRight();
        assertThat(d.property(mb, "selected")).as("Right arms the next title").isSameAs(d.find("m2"));
        assertThat(d.property(mb, ":popup")).as("with a fresh popup open").isNotNull();
    }

    @Test
    void escapeClosesTheMenuWithoutFiring() throws IOException {
        RecordingHandler h = new RecordingHandler();
        InputDriver d = InputDriver.load(FIXTURE, h);
        Object mb = d.find("mb");
        d.focusGained();
        d.click(d.find("body"));
        d.press(KeyEvent.VK_F10);
        d.press(KeyEvent.VK_ESCAPE);
        assertThat(d.property(mb, ":popup")).as("Escape closes the chain").isNull();
        assertThat(h.events).as("without firing anything").isEmpty();
    }
}
