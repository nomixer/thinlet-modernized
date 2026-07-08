/* Thinlet (modernized) — interaction-state golden capture (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * For every {@link InteractionScenarios} scenario, replays the fixture + gesture
 * script and asserts the captured paint trace matches the committed golden under
 * {@code src/test/resources/trace/interaction/} within the D7 tolerance —
 * guarding the hover/press/focus/selection/caret paint branches the static
 * corpus goldens never exercise (DECISIONS.md D45). Goldens are (re)written only
 * by {@link GoldenInteractionRecordMode} ({@code -Dtrace.record=true}), inside
 * the CI container image so the pinned fonts match CI (D44).
 */
@ExtendWith(XvfbDisplayExtension.class)
class GoldenInteractionTraceTest {

    @TestFactory
    List<DynamicTest> matchesCommittedInteractionGoldens() throws IOException {
        double tol = TraceComparator.loadDefaultPx(GoldenTraceRecorder.readClasspath("/trace/trace-tolerance.json"));
        List<DynamicTest> tests = new ArrayList<>();
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            tests.add(dynamicTest(scenario.name, () -> {
                File golden = InteractionScenarios.goldenFile(scenario.name);
                assertThat(golden)
                        .as(
                                "missing golden for scenario %s — record with -DtraceRecord=true "
                                        + "inside the CI container (see local-ci.sh / D44)",
                                scenario.name)
                        .exists();
                Trace expected = TraceJson.read(GoldenTraceRecorder.readFile(golden));
                Trace actual = InteractionScenarios.capture(scenario);
                assertThat(TraceComparator.compare(expected, actual, tol))
                        .as("interaction trace diffs for %s", scenario.name)
                        .isEmpty();
            }));
        }
        return tests;
    }

    /** Hygiene: every committed interaction golden must belong to a scenario. */
    @Test
    void noOrphanInteractionGoldens() {
        TreeSet<String> known = new TreeSet<>();
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            known.add(scenario.name + ".json");
        }
        File dir = new File(GoldenTraceRecorder.TRACE_DIR, "interaction");
        File[] files = dir.listFiles();
        if (files == null) {
            return; // nothing committed yet
        }
        List<String> orphans = new ArrayList<>();
        for (File f : files) {
            if (f.getName().endsWith(".json") && !known.contains(f.getName())) {
                orphans.add(f.getName());
            }
        }
        assertThat(orphans)
                .as("interaction goldens without a scenario (delete or re-add the scenario)")
                .isEmpty();
    }
}
