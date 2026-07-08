/* Thinlet (modernized) — interaction-state golden capture (test scope). */
package thinlet.trace;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regenerates the interaction-state goldens (DECISIONS.md D45). Disabled on a
 * normal build; run with {@code -Dtrace.record=true} — and, so the pinned fonts
 * match what CI compares against, run it INSIDE the CI container image (D44),
 * scoped to this class so the static-corpus record mode is not also triggered:
 *
 * <pre>
 * docker run --rm --user vscode -v "$PWD":/ws -w /ws \
 *   ghcr.io/nomixer/thinlet-modernized/devcontainer-ci:latest bash -c \
 *   'MAVEN_USER_HOME="$PWD/.m2" ./mvnw -B -Dmaven.repo.local=.m2/repository \
 *    -pl thinlet-core -am test -Dtest=GoldenInteractionRecordMode -DtraceRecord=true'
 * </pre>
 */
@ExtendWith(XvfbDisplayExtension.class)
@EnabledIfSystemProperty(named = "trace.record", matches = "true")
class GoldenInteractionRecordMode {

    @Test
    void recordInteractionGoldens() {
        List<String> failed = new ArrayList<>();
        int written = 0;
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            try {
                Trace trace = InteractionScenarios.capture(scenario);
                GoldenTraceRecorder.writeGolden(InteractionScenarios.goldenFile(scenario.name), TraceJson.write(trace));
                written++;
            } catch (Exception e) {
                failed.add(scenario.name + " -> " + e);
            }
        }
        System.out.println("[trace.record] wrote " + written + " interaction golden(s); failed " + failed.size());
        for (String s : failed) {
            System.out.println("[trace.record] FAIL " + s);
        }
        if (!failed.isEmpty()) {
            throw new IllegalStateException("interaction golden recording failed for: " + failed);
        }
    }
}
