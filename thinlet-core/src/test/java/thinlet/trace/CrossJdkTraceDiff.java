/* Thinlet (modernized) — Phase 2 cross-JDK trace diff (test scope). */
package thinlet.trace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Aggregates the per-JDK trace dumps (see {@link GoldenTraceDumpMode}) into a
 * cross-JDK divergence report. Disabled on a normal build; run with {@code
 * -Dtrace.diff.inputDir=<dir>} where {@code <dir>} holds one sub-directory per
 * runtime whose name contains {@code jdk-<N>} (CI downloads the {@code
 * trace-dump-jdk-N} artifacts into it). The committed goldens are the baseline
 * (reference) column and {@code trace-tolerance.json} supplies {@code defaultPx}.
 *
 * <p>Pure JSON I/O — it renders nothing, so it needs no display or toolchain.
 * It is <strong>informational</strong> (DECISIONS.md D33): it never asserts on
 * divergence, only writes {@code report.json} + {@code report.md} to {@code
 * -Dtrace.diff.out} (default {@code target/trace-diff}). It fails only on a real
 * tooling error (no dump dirs found).
 */
@EnabledIfSystemProperty(named = "trace.diff.inputDir", matches = ".+")
class CrossJdkTraceDiff {

    private static final Pattern JDK_DIR = Pattern.compile(".*jdk-(\\d+).*");

    /** One numeric position (a single coord/size arg) and the value each runtime produced there. */
    private static final class Pos {

        final String file;
        final String where;
        final String op;
        final double baseline;
        final Map<String, Double> jdkVals = new LinkedHashMap<>();

        Pos(String file, String where, String op, double baseline) {
            this.file = file;
            this.where = where;
            this.op = op;
            this.baseline = baseline;
        }
    }

    @Test
    void writeCrossJdkReport() throws IOException {
        File inputDir = new File(System.getProperty("trace.diff.inputDir"));
        File outDir = new File(System.getProperty("trace.diff.out", "target/trace-diff"));
        double tol = TraceComparator.loadDefaultPx(GoldenTraceRecorder.readClasspath("/trace/trace-tolerance.json"));

        Map<String, File> jdkDirs = discoverJdkDirs(inputDir);
        if (jdkDirs.isEmpty()) {
            throw new IOException("no per-JDK dump dirs (.../jdk-N/...) found under " + inputDir.getAbsolutePath());
        }
        List<String> jdks = new ArrayList<>(jdkDirs.keySet());
        jdks.sort(Comparator.comparingInt(Integer::parseInt));

        Map<String, Pos> positions = new LinkedHashMap<>();
        Map<String, List<String>> presentByFile = new LinkedHashMap<>();
        List<String> structural = new ArrayList<>();
        Map<String, List<String>> coverageGaps = new TreeMap<>();
        int filesCompared = 0;

        for (File golden : GoldenTraceRecorder.goldenFiles()) {
            String res = GoldenTraceRecorder.corpusResourceFor(golden);
            Trace baseline = TraceJson.read(GoldenTraceRecorder.readFile(golden));
            List<String> present = new ArrayList<>();
            for (String jdk : jdks) {
                File f = GoldenTraceRecorder.dumpFileFor(jdkDirs.get(jdk), res);
                if (!f.isFile()) {
                    coverageGaps.computeIfAbsent(jdk, k -> new ArrayList<>()).add(res);
                    continue;
                }
                present.add(jdk);
                Trace other = TraceJson.read(GoldenTraceRecorder.readFile(f));
                TraceComparator.TraceDelta td = TraceComparator.deltas(baseline, other);
                for (String s : td.structural) {
                    structural.add(res + " [jdk-" + jdk + "] " + s);
                }
                for (TraceComparator.Delta d : td.numeric) {
                    String key = res + " | " + d.where;
                    Pos p = positions.computeIfAbsent(key, k -> new Pos(res, d.where, d.op, d.baseline));
                    p.jdkVals.put(jdk, d.other);
                }
            }
            presentByFile.put(res, present);
            if (!present.isEmpty()) {
                filesCompared++;
            }
        }

        Report report = summarize(positions, presentByFile, structural, coverageGaps, jdks, filesCompared, tol);
        outDir.mkdirs();
        write(new File(outDir, "report.json"), report.json);
        write(new File(outDir, "report.md"), report.md);
        System.out.println("[trace.diff] " + report.headline + " -> " + new File(outDir, "report.md"));
    }

    private static Map<String, File> discoverJdkDirs(File inputDir) {
        Map<String, File> out = new LinkedHashMap<>();
        File[] children = inputDir.listFiles();
        if (children == null) {
            return out;
        }
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            Matcher m = JDK_DIR.matcher(child.getName());
            if (m.matches()) {
                out.put(m.group(1), child);
            }
        }
        return out;
    }

    private static final class Report {

        String headline;
        String md;
        String json;
    }

    private static Report summarize(
            Map<String, Pos> positions,
            Map<String, List<String>> presentByFile,
            List<String> structural,
            Map<String, List<String>> coverageGaps,
            List<String> jdks,
            int filesCompared,
            double tol) {
        // Finalize per-position spread + per-op / per-jdk aggregates.
        double maxSpread = 0.0;
        String maxSpreadAt = "(none)";
        int overTol = 0;
        List<Pos> ordered = new ArrayList<>(positions.values());
        ordered.sort(Comparator.comparing((Pos p) -> p.file).thenComparing(p -> p.where));

        Map<String, double[]> perOp = new TreeMap<>(); // op -> {maxSpread, count}
        Map<String, double[]> perJdk = new LinkedHashMap<>(); // jdk -> {maxAbsDelta, differingPositions}
        for (String jdk : jdks) {
            perJdk.put(jdk, new double[] {0.0, 0.0});
        }
        List<Pos> overTolPositions = new ArrayList<>();

        for (Pos p : ordered) {
            List<String> present = presentByFile.getOrDefault(p.file, jdks);
            double min = p.baseline;
            double max = p.baseline;
            for (String jdk : present) {
                double v = p.jdkVals.getOrDefault(jdk, p.baseline);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            double spread = max - min;
            if (spread > maxSpread) {
                maxSpread = spread;
                maxSpreadAt = p.file + " | " + p.where;
            }
            if (spread > tol) {
                overTol++;
                overTolPositions.add(p);
            }
            double[] op = perOp.computeIfAbsent(p.op, k -> new double[] {0.0, 0.0});
            op[0] = Math.max(op[0], spread);
            op[1] += 1;
            for (Map.Entry<String, Double> e : p.jdkVals.entrySet()) {
                double[] jd = perJdk.get(e.getKey());
                if (jd != null) {
                    jd[0] = Math.max(jd[0], Math.abs(e.getValue() - p.baseline));
                    jd[1] += 1;
                }
            }
        }

        Report r = new Report();
        r.headline = "max cross-JDK spread " + px(maxSpread) + " over " + filesCompared + " file(s), JDK "
                + String.join("/", jdks) + "; " + overTol + " position(s) > " + px(tol)
                + (structural.isEmpty()
                        ? "; structural/categorical identical"
                        : "; " + structural.size() + " STRUCTURAL");
        r.md = markdown(
                jdks,
                filesCompared,
                tol,
                maxSpread,
                maxSpreadAt,
                overTol,
                structural,
                overTolPositions,
                perOp,
                perJdk,
                presentByFile,
                coverageGaps);
        r.json = json(
                jdks,
                filesCompared,
                tol,
                maxSpread,
                maxSpreadAt,
                overTol,
                structural,
                overTolPositions,
                perOp,
                perJdk,
                coverageGaps);
        return r;
    }

    private static String markdown(
            List<String> jdks,
            int filesCompared,
            double tol,
            double maxSpread,
            String maxSpreadAt,
            int overTol,
            List<String> structural,
            List<Pos> overTolPositions,
            Map<String, double[]> perOp,
            Map<String, double[]> perJdk,
            Map<String, List<String>> presentByFile,
            Map<String, List<String>> coverageGaps) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Cross-JDK trace diff\n\n");
        sb.append("Generated by `CrossJdkTraceDiff` (informational; see DECISIONS.md D33). ");
        sb.append("Baseline = the committed goldens; each JDK column is that runtime's dump.\n\n");
        sb.append("- **Runtimes compared:** ").append(String.join(", ", jdks)).append('\n');
        sb.append("- **Files compared:** ").append(filesCompared).append('\n');
        sb.append("- **Tolerance (`defaultPx`):** ").append(px(tol)).append('\n');
        sb.append("- **Max cross-JDK spread:** ")
                .append(px(maxSpread))
                .append("  \n  at `")
                .append(maxSpreadAt)
                .append("`\n");
        sb.append("- **Positions exceeding tolerance:** ").append(overTol).append('\n');
        sb.append("- **Structural / categorical:** ")
                .append(
                        structural.isEmpty()
                                ? "identical across all runtimes ✅"
                                : (structural.size() + " mismatch(es) ⚠️"))
                .append("\n\n");

        if (!structural.isEmpty()) {
            sb.append("## Structural / categorical mismatches (should be none)\n\n");
            for (String s : structural) {
                sb.append("- ").append(s).append('\n');
            }
            sb.append('\n');
        }

        sb.append("## Positions exceeding ±").append(px(tol)).append(" spread\n\n");
        if (overTolPositions.isEmpty()) {
            sb.append("_None — every numeric position agrees across runtimes within tolerance._\n\n");
        } else {
            sb.append("| file | position | baseline | ")
                    .append(String.join(" | ", jdks))
                    .append(" | spread |\n");
            sb.append("|---|---|---|").append(repeat("---|", jdks.size())).append("---|\n");
            for (Pos p : overTolPositions) {
                sb.append("| `")
                        .append(p.file)
                        .append("` | `")
                        .append(p.where)
                        .append("` | ")
                        .append(px(p.baseline));
                List<String> present = presentByFile.getOrDefault(p.file, jdks);
                double min = p.baseline;
                double max = p.baseline;
                for (String jdk : jdks) {
                    if (present.contains(jdk)) {
                        double v = p.jdkVals.getOrDefault(jdk, p.baseline);
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                        sb.append(" | ").append(px(v));
                    } else {
                        sb.append(" | –");
                    }
                }
                sb.append(" | ").append(px(max - min)).append(" |\n");
            }
            sb.append('\n');
        }

        sb.append("## Drift by drawing op (max spread)\n\n");
        sb.append("| op | max spread | differing positions |\n|---|---|---|\n");
        List<Map.Entry<String, double[]>> ops = new ArrayList<>(perOp.entrySet());
        ops.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));
        for (Map.Entry<String, double[]> e : ops) {
            sb.append("| `")
                    .append(e.getKey())
                    .append("` | ")
                    .append(px(e.getValue()[0]))
                    .append(" | ")
                    .append((long) e.getValue()[1])
                    .append(" |\n");
        }
        if (ops.isEmpty()) {
            sb.append("| _(no differences)_ | 0 px | 0 |\n");
        }
        sb.append('\n');

        sb.append("## Per-runtime vs baseline\n\n");
        sb.append("| jdk | max abs delta | differing positions |\n|---|---|---|\n");
        for (Map.Entry<String, double[]> e : perJdk.entrySet()) {
            sb.append("| ")
                    .append(e.getKey())
                    .append(" | ")
                    .append(px(e.getValue()[0]))
                    .append(" | ")
                    .append((long) e.getValue()[1])
                    .append(" |\n");
        }
        sb.append('\n');

        if (!coverageGaps.isEmpty()) {
            sb.append("## Coverage gaps (golden present, dump missing)\n\n");
            for (Map.Entry<String, List<String>> e : coverageGaps.entrySet()) {
                sb.append("- **jdk-")
                        .append(e.getKey())
                        .append("** missing ")
                        .append(e.getValue().size())
                        .append(": ")
                        .append(String.join(", ", e.getValue()))
                        .append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String json(
            List<String> jdks,
            int filesCompared,
            double tol,
            double maxSpread,
            String maxSpreadAt,
            int overTol,
            List<String> structural,
            List<Pos> overTolPositions,
            Map<String, double[]> perOp,
            Map<String, double[]> perJdk,
            Map<String, List<String>> coverageGaps) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"jdks\": ").append(strArray(jdks)).append(",\n");
        sb.append("  \"defaultPx\": ").append(num(tol)).append(",\n");
        sb.append("  \"filesCompared\": ").append(filesCompared).append(",\n");
        sb.append("  \"maxSpreadPx\": ").append(num(maxSpread)).append(",\n");
        sb.append("  \"maxSpreadAt\": ").append(q(maxSpreadAt)).append(",\n");
        sb.append("  \"positionsOverTolerance\": ").append(overTol).append(",\n");
        sb.append("  \"structuralIssues\": ").append(strArray(structural)).append(",\n");
        sb.append("  \"perOp\": [");
        List<Map.Entry<String, double[]>> ops = new ArrayList<>(perOp.entrySet());
        ops.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));
        for (int i = 0; i < ops.size(); i++) {
            Map.Entry<String, double[]> e = ops.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"op\": ")
                    .append(q(e.getKey()))
                    .append(", \"maxSpreadPx\": ")
                    .append(num(e.getValue()[0]))
                    .append(", \"differingPositions\": ")
                    .append((long) e.getValue()[1])
                    .append("}");
        }
        sb.append(ops.isEmpty() ? "" : "\n  ").append("],\n");
        sb.append("  \"perJdk\": [");
        int j = 0;
        for (Map.Entry<String, double[]> e : perJdk.entrySet()) {
            sb.append(j++ == 0 ? "\n" : ",\n");
            sb.append("    {\"jdk\": ")
                    .append(q(e.getKey()))
                    .append(", \"maxAbsDeltaPx\": ")
                    .append(num(e.getValue()[0]))
                    .append(", \"differingPositions\": ")
                    .append((long) e.getValue()[1])
                    .append("}");
        }
        sb.append(perJdk.isEmpty() ? "" : "\n  ").append("],\n");
        sb.append("  \"overTolerance\": [");
        for (int i = 0; i < overTolPositions.size(); i++) {
            Pos p = overTolPositions.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"file\": ")
                    .append(q(p.file))
                    .append(", \"where\": ")
                    .append(q(p.where))
                    .append(", \"baseline\": ")
                    .append(num(p.baseline))
                    .append(", \"values\": {");
            int k = 0;
            for (Map.Entry<String, Double> e : p.jdkVals.entrySet()) {
                sb.append(k++ == 0 ? "" : ", ")
                        .append(q(e.getKey()))
                        .append(": ")
                        .append(num(e.getValue()));
            }
            sb.append("}}");
        }
        sb.append(overTolPositions.isEmpty() ? "" : "\n  ").append("],\n");
        sb.append("  \"coverageGaps\": {");
        int g = 0;
        for (Map.Entry<String, List<String>> e : coverageGaps.entrySet()) {
            sb.append(g++ == 0 ? "\n" : ",\n");
            sb.append("    ").append(q("jdk-" + e.getKey())).append(": ").append(strArray(e.getValue()));
        }
        sb.append(coverageGaps.isEmpty() ? "" : "\n  ").append("}\n}\n");
        return sb.toString();
    }

    private static String px(double d) {
        return num(d) + " px";
    }

    private static String num(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    private static String strArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(q(values.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static void write(File f, String content) throws IOException {
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
