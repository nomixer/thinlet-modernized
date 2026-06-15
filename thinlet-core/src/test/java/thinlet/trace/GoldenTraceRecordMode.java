/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regenerates the golden traces. Disabled on a normal build; run with {@code
 * -Dtrace.record=true} to (re)write goldens. Every corpus file is attempted;
 * files that cannot parse/paint headless are skipped and reported rather than
 * failing the run.
 */
@ExtendWith(XvfbDisplayExtension.class)
@EnabledIfSystemProperty(named = "trace.record", matches = "true")
class GoldenTraceRecordMode {

    @Test
    void recordAllGoldens() throws IOException {
        List<String> skipped = new ArrayList<>();
        int written = 0;
        for (String resource : GoldenTraceRecorder.corpusResources()) {
            try {
                Trace trace = GoldenTraceRecorder.render(resource);
                GoldenTraceRecorder.writeGolden(GoldenTraceRecorder.goldenFileFor(resource), TraceJson.write(trace));
                written++;
            } catch (Exception e) {
                skipped.add(resource + " -> " + e);
            }
        }
        System.out.println("[trace.record] wrote " + written + " golden(s); skipped " + skipped.size());
        for (String s : skipped) {
            System.out.println("[trace.record] SKIP " + s);
        }
    }
}
