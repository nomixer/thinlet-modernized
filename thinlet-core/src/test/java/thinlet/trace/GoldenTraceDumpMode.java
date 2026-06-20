/* Thinlet (modernized) — Phase 2 cross-JDK trace diff (test scope). */
package thinlet.trace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Dumps the full render trace of every corpus file to a directory, for the
 * cross-JDK trace diff. Disabled on a normal build; run on each JDK runtime with
 * {@code -Dtrace.dump.dir=<dir>} (CI passes a per-JDK dir like {@code
 * target/trace-dump/jdk-8}). The output mirrors the golden layout and format
 * exactly ({@link TraceJson#write}), so a dump and a golden are directly
 * comparable. Files that cannot parse/paint headless are skipped and reported
 * rather than failing the run, matching {@link GoldenTraceRecordMode}.
 *
 * <p>This only <em>persists</em> what the regression test already renders in
 * memory; it changes nothing about the comparison gate (DECISIONS.md D33).
 */
@ExtendWith(XvfbDisplayExtension.class)
@EnabledIfSystemProperty(named = "trace.dump.dir", matches = ".+")
class GoldenTraceDumpMode {

    @Test
    void dumpAllTraces() throws IOException {
        File dumpDir = new File(System.getProperty("trace.dump.dir"));
        List<String> skipped = new ArrayList<>();
        int written = 0;
        for (String resource : GoldenTraceRecorder.corpusResources()) {
            try {
                Trace trace = GoldenTraceRecorder.render(resource);
                GoldenTraceRecorder.writeGolden(
                        GoldenTraceRecorder.dumpFileFor(dumpDir, resource), TraceJson.write(trace));
                written++;
            } catch (Exception e) {
                skipped.add(resource + " -> " + e);
            }
        }
        System.out.println("[trace.dump] wrote " + written + " trace(s) to " + dumpDir + "; skipped " + skipped.size());
        for (String s : skipped) {
            System.out.println("[trace.dump] SKIP " + s);
        }
    }
}
