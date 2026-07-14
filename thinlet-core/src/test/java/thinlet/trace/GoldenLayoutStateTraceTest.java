/* Thinlet (modernized) — layout-state sidecar net (test scope). */
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
 * Guards the D61 layout-state sidecars: for every committed static golden and
 * every interaction scenario, re-renders and asserts the :port/:view/:widths/
 * :offset walk matches the sidecar under {@code trace/layout-state/} — in both
 * directions (a non-empty walk requires a sidecar; an empty walk forbids one).
 * Sidecars are (re)written only by {@link GoldenLayoutStateRecordMode} inside
 * the CI container image (D44).
 */
@ExtendWith(XvfbDisplayExtension.class)
class GoldenLayoutStateTraceTest {

    @TestFactory
    List<DynamicTest> matchesCommittedStaticLayoutState() throws IOException {
        double tol = loadTol();
        List<DynamicTest> tests = new ArrayList<>();
        for (File golden : GoldenTraceRecorder.goldenFiles()) {
            String resource = GoldenTraceRecorder.corpusResourceFor(golden);
            tests.add(dynamicTest(resource, () -> {
                List<LayoutStateNode> actual = GoldenTraceRecorder.renderAll(resource).state;
                assertMatchesSidecar(GoldenTraceRecorder.layoutStateFileFor(resource), actual, tol, resource);
            }));
        }
        return tests;
    }

    @TestFactory
    List<DynamicTest> matchesCommittedInteractionLayoutState() throws IOException {
        double tol = loadTol();
        List<DynamicTest> tests = new ArrayList<>();
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            tests.add(dynamicTest(scenario.name, () -> {
                List<LayoutStateNode> actual = InteractionScenarios.captureAll(scenario).state;
                assertMatchesSidecar(InteractionScenarios.layoutStateFile(scenario.name), actual, tol, scenario.name);
            }));
        }
        return tests;
    }

    private static double loadTol() throws IOException {
        return TraceComparator.loadDefaultPx(GoldenTraceRecorder.readClasspath("/trace/trace-tolerance.json"));
    }

    private static void assertMatchesSidecar(File sidecar, List<LayoutStateNode> actual, double tol, String what)
            throws IOException {
        if (actual.isEmpty()) {
            assertThat(sidecar)
                    .as("stale layout-state sidecar for %s (its walk carries no state)", what)
                    .doesNotExist();
            return;
        }
        assertThat(sidecar)
                .as(
                        "missing layout-state sidecar for %s — record with -DtraceRecord=true "
                                + "inside the CI container (see GoldenInteractionRecordMode / D44)",
                        what)
                .exists();
        List<LayoutStateNode> expected = TraceJson.readLayoutState(GoldenTraceRecorder.readFile(sidecar));
        assertThat(TraceComparator.compareLayoutState(expected, actual, tol))
                .as("layout-state diffs for %s", what)
                .isEmpty();
    }

    /** Hygiene: every committed sidecar must belong to a static golden or a scenario. */
    @Test
    void noOrphanLayoutStateGoldens() {
        TreeSet<String> scenarioNames = new TreeSet<>();
        for (InteractionScenarios.Scenario scenario : InteractionScenarios.all()) {
            scenarioNames.add(scenario.name + ".json");
        }
        File base = new File(GoldenTraceRecorder.TRACE_DIR, "layout-state");
        List<String> orphans = new ArrayList<>();
        collectOrphans(base, base, scenarioNames, orphans);
        assertThat(orphans)
                .as("layout-state sidecars without a static golden / scenario (delete or re-record)")
                .isEmpty();
    }

    private static void collectOrphans(File base, File dir, TreeSet<String> scenarioNames, List<String> orphans) {
        File[] files = dir.listFiles();
        if (files == null) {
            return; // nothing committed yet
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectOrphans(base, f, scenarioNames, orphans);
                continue;
            }
            String rel = base.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
            boolean known = rel.startsWith("interaction/")
                    ? scenarioNames.contains(f.getName())
                    : new File(GoldenTraceRecorder.TRACE_DIR, rel).isFile();
            if (!known) {
                orphans.add(rel);
            }
        }
    }

    /**
     * Coverage guard, not a one-time check: the committed sidecar set must keep
     * exercising all four keys, a scrolled :view, and the positive (scrolled)
     * :offset branch — losing any of these silently thins the Cut 4 net.
     */
    @Test
    void allFourKeysExercised() throws IOException {
        File base = new File(GoldenTraceRecorder.TRACE_DIR, "layout-state");
        List<File> sidecars = new ArrayList<>();
        collectJson(base, sidecars);
        assertThat(sidecars)
                .as("no layout-state sidecars committed — record inside the CI container (D44)")
                .isNotEmpty();
        boolean port = false;
        boolean view = false;
        boolean widths = false;
        boolean offset = false;
        boolean scrolledView = false;
        boolean positiveOffset = false;
        for (File f : sidecars) {
            for (LayoutStateNode n : TraceJson.readLayoutState(GoldenTraceRecorder.readFile(f))) {
                port |= n.port != null;
                widths |= n.widths != null;
                offset |= n.offset != null;
                positiveOffset |= n.offset != null && n.offset > 0;
                view |= n.view != null;
                scrolledView |= n.view != null && (n.view[0] != 0 || n.view[1] != 0);
            }
        }
        assertThat(port).as(":port exercised").isTrue();
        assertThat(view).as(":view exercised").isTrue();
        assertThat(widths).as(":widths exercised").isTrue();
        assertThat(offset).as(":offset exercised").isTrue();
        assertThat(scrolledView).as("a non-zero :view scroll offset exercised").isTrue();
        assertThat(positiveOffset)
                .as("the positive (scrolled) :offset branch exercised")
                .isTrue();
    }

    private static void collectJson(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectJson(f, out);
            } else if (f.getName().endsWith(".json")) {
                out.add(f);
            }
        }
    }
}
