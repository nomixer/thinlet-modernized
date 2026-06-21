/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — {@code combobox} popup open and keyboard selection. The
 * open/closed state has no public getter, so it is read from the {@code :combolist}
 * property (present only while the drop-down is showing) straight off the {@code
 * Object[]} model, the same black-box read {@link LayoutTrace} uses for bounds; the
 * committed selection is asserted via {@link Thinlet#getSelectedIndex(Object)}.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputComboBoxTest {

    private static final String FIXTURE = "/input/combobox.xml";

    @Test
    void clickOpensPopup() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Object cb = d.find("cb");
        assertThat(d.property(cb, ":combolist")).as("popup is closed initially").isNull();
        Trace before = d.paint();
        d.click(cb);
        assertThat(d.property(cb, ":combolist"))
                .as("clicking the combobox opens its drop-down")
                .isNotNull();
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the open drop-down repaints the screen")
                .isNotEmpty();
    }

    @Test
    void downThenEnterSelectsAndCloses() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object cb = d.find("cb");
        assertThat(t.getSelectedIndex(cb)).as("nothing selected initially").isEqualTo(-1);
        d.focusGained();
        d.click(cb); // open the drop-down (and take focus)
        d.arrowDown(); // highlight the first choice
        d.enter(); // commit it
        assertThat(d.property(cb, ":combolist"))
                .as("committing closes the drop-down")
                .isNull();
        assertThat(t.getSelectedIndex(cb))
                .as("the highlighted choice is now selected")
                .isEqualTo(0);
    }
}
