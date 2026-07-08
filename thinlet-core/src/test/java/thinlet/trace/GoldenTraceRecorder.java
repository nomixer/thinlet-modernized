/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import thinlet.Thinlet;

/**
 * Renders a corpus XML headlessly through {@link TracingGraphics2D} and produces
 * a {@link Trace}, plus filesystem helpers for recording and locating golden
 * files. Corpus inputs are loaded from the test classpath ({@code /corpus/...});
 * goldens live under {@code src/test/resources/trace/...} (surefire runs with the
 * module dir as the working directory).
 */
final class GoldenTraceRecorder {

    static final int WIDTH = 1024;
    static final int HEIGHT = 768;
    static final File CORPUS_DIR = new File("src/test/resources/corpus");
    static final File TRACE_DIR = new File("src/test/resources/trace");

    private GoldenTraceRecorder() {}

    static Trace render(String resource) throws IOException {
        Thinlet thinlet = new Thinlet();
        Object root;
        InputStream in = GoldenTraceRecorder.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("corpus resource not found: " + resource);
        }
        try {
            root = thinlet.parse(in, new CorpusHandler());
        } finally {
            in.close();
        }
        thinlet.add(root);
        thinlet.setSize(WIDTH, HEIGHT);
        // setSize posts an async COMPONENT_RESIZED event (Thinlet enables component
        // events); its handler sets the content bounds that paint needs. Flush the
        // event queue so layout is always applied before painting — otherwise the
        // direct paint() races the EDT and intermittently renders an empty tree.
        pumpEventQueue();
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D raw = img.createGraphics();
        // Thinlet.paint dereferences the clip bounds, and a fresh BufferedImage
        // graphics has a null clip. Set it on the raw graphics before wrapping so
        // the setup does not appear as a spurious leading setClip in the trace.
        raw.setClip(0, 0, WIDTH, HEIGHT);
        List<TraceCall> sink = new ArrayList<>();
        TracingGraphics2D g = new TracingGraphics2D(raw, sink);
        try {
            thinlet.paint(g);
        } finally {
            g.dispose();
        }
        return new Trace(sink, LayoutTrace.walk(root));
    }

    private static void pumpEventQueue() {
        try {
            // A few passes drain any events the resize handler itself posts.
            for (int i = 0; i < 3; i++) {
                java.awt.EventQueue.invokeAndWait(() -> {});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException ignored) {
            // best-effort flush
        }
    }

    static String readClasspath(String resource) throws IOException {
        InputStream in = GoldenTraceRecorder.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("classpath resource not found: " + resource);
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    static List<String> corpusResources() {
        List<String> out = new ArrayList<>();
        collect(CORPUS_DIR, ".xml", out);
        List<String> resources = new ArrayList<>();
        for (String absRel : out) {
            resources.add("/corpus/" + absRel);
        }
        Collections.sort(resources);
        return resources;
    }

    static List<File> goldenFiles() {
        List<File> out = new ArrayList<>();
        collectFiles(TRACE_DIR, out);
        Collections.sort(out);
        return out;
    }

    static File goldenFileFor(String corpusResource) {
        String rel = corpusResource.substring("/corpus/".length());
        rel = rel.substring(0, rel.length() - ".xml".length()) + ".json";
        return new File(TRACE_DIR, rel);
    }

    /**
     * Maps a corpus resource to its trace file under an arbitrary base directory,
     * preserving the {@code demo/drafts/amazon} sub-path. Used by the cross-JDK
     * dump mode to write a per-runtime trace tree that mirrors the golden layout.
     */
    static File dumpFileFor(File baseDir, String corpusResource) {
        String rel = corpusResource.substring("/corpus/".length());
        rel = rel.substring(0, rel.length() - ".xml".length()) + ".json";
        return new File(baseDir, rel);
    }

    static String corpusResourceFor(File goldenFile) {
        String rel =
                TRACE_DIR.toPath().relativize(goldenFile.toPath()).toString().replace(File.separatorChar, '/');
        rel = rel.substring(0, rel.length() - ".json".length()) + ".xml";
        return "/corpus/" + rel;
    }

    static String readFile(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    static void writeGolden(File f, String json) throws IOException {
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Files.write(f.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    private static void collect(File base, String suffix, List<String> out) {
        File[] files = base.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, suffix, out);
            } else if (f.getName().endsWith(suffix)) {
                out.add(CORPUS_DIR.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/'));
            }
        }
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                // trace/interaction/ holds the D45 interaction-state goldens, which
                // map to scenarios (GoldenInteractionTraceTest), not corpus XML —
                // corpusResourceFor would resolve them to nonexistent files.
                if (!"interaction".equals(f.getName())) {
                    collectFiles(f, out);
                }
            } else if (f.getName().endsWith(".json") && !"trace-tolerance.json".equals(f.getName())) {
                out.add(f);
            }
        }
    }
}
