/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * For every committed golden under {@code src/test/resources/trace/...}, renders
 * the corresponding corpus file afresh and asserts the trace matches within the
 * D7 tolerance. Same-JDK comparison for this slice; the cross-JDK matrix is a
 * later slice. If no goldens are committed yet, this contributes no dynamic
 * tests.
 */
@ExtendWith(XvfbDisplayExtension.class)
class GoldenTraceRegressionTest {

    @TestFactory
    List<DynamicTest> matchesCommittedGoldens() throws IOException {
        double tol = TraceComparator.loadDefaultPx(GoldenTraceRecorder.readClasspath("/trace/trace-tolerance.json"));
        List<DynamicTest> tests = new ArrayList<>();
        for (File golden : GoldenTraceRecorder.goldenFiles()) {
            String resource = GoldenTraceRecorder.corpusResourceFor(golden);
            tests.add(dynamicTest(resource, () -> {
                Trace expected = TraceJson.read(GoldenTraceRecorder.readFile(golden));
                Trace actual = GoldenTraceRecorder.render(resource);
                assertThat(TraceComparator.compare(expected, actual, tol))
                        .as("trace diffs for %s", resource)
                        .isEmpty();
            }));
        }
        return tests;
    }
}
