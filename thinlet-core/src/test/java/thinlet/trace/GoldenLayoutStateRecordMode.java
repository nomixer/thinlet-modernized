/* Thinlet (modernized) — layout-state sidecar net (test scope). */
package thinlet.trace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regenerates the layout-state sidecar goldens (DECISIONS.md D61): run with
 * {@code -Dtrace.record=true} INSIDE the CI container image, scoped via
 * {@code -Dtest=...} — same invocation as {@link GoldenInteractionRecordMode}
 * (see its javadoc for the docker command; D44).
 */
@ExtendWith(XvfbDisplayExtension.class)
@EnabledIfSystemProperty(named = "trace.record", matches = "true")
class GoldenLayoutStateRecordMode {

    @Test
    void recordLayoutStateGoldens() {
        List<String> failed = new ArrayList<>();
        int written = 0;
        int empty = 0;
        // Enumerate the committed {calls, layout} goldens (not the raw corpus)
        // so the sidecar set stays 1:1 with the existing net.
        for (File golden : GoldenTraceRecorder.goldenFiles()) {
            String resource = GoldenTraceRecorder.corpusResourceFor(golden);
            try {
                List<LayoutStateNode> state = GoldenTraceRecorder.renderAll(resource).state;
                if (writeOrDelete(GoldenTraceRecorder.layoutStateFileFor(resource), state)) {
                    written++;
                } else {
                    empty++;
                }
            } catch (Exception e) {
                failed.add(resource + " -> " + e);
            }
        }
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            try {
                List<LayoutStateNode> state = InteractionScenarios.captureAll(scenario).state;
                if (writeOrDelete(InteractionScenarios.layoutStateFile(scenario.name), state)) {
                    written++;
                } else {
                    empty++;
                }
            } catch (Exception e) {
                failed.add(scenario.name + " -> " + e);
            }
        }
        System.out.println("[trace.record] wrote " + written + " layout-state sidecar(s); " + empty
                + " empty (no sidecar); failed " + failed.size());
        for (String s : failed) {
            System.out.println("[trace.record] FAIL " + s);
        }
        if (!failed.isEmpty()) {
            throw new IllegalStateException("layout-state recording failed for: " + failed);
        }
    }

    /** Writes the sidecar when the state is non-empty; deletes any stale file when empty. */
    private static boolean writeOrDelete(File f, List<LayoutStateNode> state) throws IOException {
        if (state.isEmpty()) {
            Files.deleteIfExists(f.toPath());
            return false;
        }
        GoldenTraceRecorder.writeGolden(f, TraceJson.writeLayoutState(state));
        return true;
    }
}
