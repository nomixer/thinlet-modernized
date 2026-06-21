/* Thinlet (modernized) — Phase 2.x input-capture regression suite (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Input regression net — {@code tree} selection and keyboard expand/collapse.
 * Expand/collapse is driven by Right/Left arrows rather than the mouse: the mouse
 * toggle is gated on hitting the handle band {@code [r.x-block, r.x)}, which is
 * FontMetrics-fragile, whereas the keyboard path sets the {@code expanded} boolean
 * directly. Selection/expansion are asserted via public getters.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class InputTreeTest {

    private static final String FIXTURE = "/input/tree.xml";

    @Test
    void clickSelectsNode() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tr = d.find("tr");
        Object na = d.find("na");
        Trace before = d.paint();
        d.click(na);
        assertThat(t.getSelectedItem(tr)).as("clicking a node selects it").isSameAs(na);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("selecting a node repaints the tree")
                .isNotEmpty();
    }

    @Test
    void rightArrowExpandsCollapsedNode() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object na = d.find("na");
        d.focusGained();
        d.click(na);
        assertThat(t.getBoolean(na, "expanded")).as("Node A starts collapsed").isFalse();
        Trace before = d.paint();
        d.arrowRight();
        assertThat(t.getBoolean(na, "expanded"))
                .as("Right arrow expands the collapsed node")
                .isTrue();
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("expanding reveals children, repainting the tree")
                .isNotEmpty();
    }

    @Test
    void leftArrowCollapsesExpandedNode() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object nb = d.find("nb");
        d.focusGained();
        d.click(nb);
        assertThat(t.getBoolean(nb, "expanded")).as("Node B starts expanded").isTrue();
        Trace before = d.paint();
        d.arrowLeft();
        assertThat(t.getBoolean(nb, "expanded"))
                .as("Left arrow collapses the expanded node")
                .isFalse();
        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("collapsing hides children, repainting the tree")
                .isNotEmpty();
    }

    @Test
    void downArrowTraversesIntoExpandedChildren() throws IOException {
        InputDriver d = InputDriver.load(FIXTURE, new InputHandler());
        Thinlet t = d.thinlet();
        Object tr = d.find("tr");
        Object nb = d.find("nb"); // expanded, so nb1 is the next visible row
        d.focusGained();
        d.click(nb);
        d.arrowDown();
        assertThat(t.getSelectedItem(tr))
                .as("Down from an expanded node descends to its first child")
                .isSameAs(d.find("nb1"));
    }
}
