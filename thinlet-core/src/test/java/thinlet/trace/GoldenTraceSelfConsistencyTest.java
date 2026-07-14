/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates the harness pipeline end to end: rendering the same corpus file twice
 * must produce traces that the comparator considers equal, after a round trip
 * through the JSON writer and reader. This proves recorder → serializer →
 * comparator is deterministic before goldens are scaled across the corpus.
 */
@ExtendWith(XvfbDisplayExtension.class)
class GoldenTraceSelfConsistencyTest {

    @Test
    void renderingIsDeterministicThroughJsonRoundTrip() throws Exception {
        Trace first = GoldenTraceRecorder.render("/corpus/drafts/looks.xml");
        Trace second = GoldenTraceRecorder.render("/corpus/drafts/looks.xml");
        Trace roundTripped = TraceJson.read(TraceJson.write(first));

        assertThat(first.calls).as("the demo should produce drawing calls").isNotEmpty();

        List<String> diffs = TraceComparator.compare(roundTripped, second, 2.0);
        assertThat(diffs).as("trace diffs").isEmpty();
    }

    // widgets.xml carries table :widths and alignment-branch (negative) :offset
    // nodes, round-tripping every sparse field the sidecar format has (D61).
    @Test
    void layoutStateIsDeterministicThroughJsonRoundTrip() throws Exception {
        List<LayoutStateNode> first = GoldenTraceRecorder.renderAll("/corpus/drafts/widgets.xml").state;
        List<LayoutStateNode> second = GoldenTraceRecorder.renderAll("/corpus/drafts/widgets.xml").state;
        List<LayoutStateNode> roundTripped = TraceJson.readLayoutState(TraceJson.writeLayoutState(first));

        assertThat(first)
                .as("widgets.xml should carry scroll/table/offset state")
                .isNotEmpty();

        List<String> diffs = TraceComparator.compareLayoutState(roundTripped, second, 2.0);
        assertThat(diffs).as("layout-state diffs").isEmpty();
    }
}
