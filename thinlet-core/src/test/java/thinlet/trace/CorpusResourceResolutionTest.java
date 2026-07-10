/* Thinlet (modernized) — corpus resource-resolution guard (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Fails loudly if any corpus scene references a classpath resource (e.g.
 * {@code icon="/icon/cut.gif"}) that does not resolve on the test classpath.
 *
 * <p>The 2005 loader ({@code Thinlet.getIcon}) swallows a missing resource to
 * {@code null} with no log and no throw (KNOWN-QUIRKS Q3): the widget then lays
 * out and paints as if icon-less. That is the exact silent failure that left
 * every corpus icon unrendered — and every golden captured blank — before D54.
 * This guard turns "a fixture references a resource we never vendored" into a red
 * build. It does <em>not</em> change library behavior (the silent-null stays,
 * quirk-locked); it only asserts our own fixtures resolve.
 *
 * <p>Display-independent: pure classpath resolution plus a decode check, so it
 * needs no Xvfb and runs in every {@code verify}.
 */
class CorpusResourceResolutionTest {

    /**
     * Resources genuinely absent from the entire 2005 archive — no shipped jar
     * (amazon/demo/drafts) ever contained them — so they were silent-nulls in 2005
     * too and are preserved as such (see DECISIONS.md D54 / ICON-PROVENANCE.md).
     * Exempting them here keeps the guard catching <em>new</em> silent failures
     * without falsely flagging an authentic 2005 gap.
     */
    static final Set<String> KNOWN_ABSENT_2005 =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("/icon/volume.gif")));

    /**
     * A resource-typed attribute value: a classpath-absolute path to an image
     * asset. The corpus vocabulary is {@code icon="/icon/*.gif"}; the broader
     * shape (any {@code ="/....(gif|png|jpg|bmp)"}) defensively catches a future
     * resource attribute so a new resource kind cannot regress silently either.
     * Values that are URLs (http:/file:) or not classpath-absolute (no leading
     * {@code /}) are excluded by construction.
     */
    private static final Pattern RESOURCE_REF =
            Pattern.compile("=\"(/[^\"]*\\.(?:gif|png|jpe?g|bmp))\"", Pattern.CASE_INSENSITIVE);

    @Test
    void everyCorpusResourceResolvesOnTheClasspath() throws IOException {
        // path -> corpus scenes that reference it (sorted, for a legible failure).
        TreeMap<String, Set<String>> refs = new TreeMap<String, Set<String>>();
        for (String resource : GoldenTraceRecorder.corpusResources()) {
            Matcher m = RESOURCE_REF.matcher(GoldenTraceRecorder.readClasspath(resource));
            while (m.find()) {
                Set<String> scenes = refs.get(m.group(1));
                if (scenes == null) {
                    scenes = new TreeSet<String>();
                    refs.put(m.group(1), scenes);
                }
                scenes.add(resource);
            }
        }

        assertThat(refs)
                .as("corpus references at least one resource (sanity check on the sweep)")
                .isNotEmpty();

        List<String> unresolved = new ArrayList<String>();
        List<String> undecodable = new ArrayList<String>();
        for (Map.Entry<String, Set<String>> e : refs.entrySet()) {
            String path = e.getKey();
            if (KNOWN_ABSENT_2005.contains(path)) {
                continue;
            }
            URL url = getClass().getResource(path);
            if (url == null) {
                unresolved.add(path + "  <- " + e.getValue());
                continue;
            }
            // A resolvable-but-corrupt/truncated byte stream would decode to null or
            // 0x0 and re-introduce a silent layout failure — assert it is a real image.
            try {
                BufferedImage img = ImageIO.read(url);
                if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
                    undecodable.add(path + " (decoded=" + img + ")");
                }
            } catch (IOException ioe) {
                undecodable.add(path + " (" + ioe + ")");
            }
        }

        assertThat(unresolved)
                .as("corpus scenes reference /icon resources missing from the test classpath — "
                        + "vendor the asset under src/test/resources/icon, or (if genuinely absent "
                        + "in the 2005 archive) add it to KNOWN_ABSENT_2005 with a DECISIONS.md note")
                .isEmpty();
        assertThat(undecodable)
                .as("resolvable icon resources that fail to decode to a real image (corrupt/truncated bytes)")
                .isEmpty();
    }

    @Test
    void knownAbsentEntriesStayHonest() throws IOException {
        // Keep the allowlist from rotting: every KNOWN_ABSENT_2005 entry must still be
        // referenced by some corpus scene AND must still fail to resolve. If a scene
        // stops referencing it, or the asset later appears, drop it from the allowlist.
        Set<String> referenced = new TreeSet<String>();
        for (String resource : GoldenTraceRecorder.corpusResources()) {
            Matcher m = RESOURCE_REF.matcher(GoldenTraceRecorder.readClasspath(resource));
            while (m.find()) {
                referenced.add(m.group(1));
            }
        }
        assertThat(referenced)
                .as("every KNOWN_ABSENT_2005 entry is still referenced by the corpus")
                .containsAll(KNOWN_ABSENT_2005);
        for (String path : KNOWN_ABSENT_2005) {
            assertThat(getClass().getResource(path))
                    .as("KNOWN_ABSENT_2005 entry %s must still be absent (else remove it from the allowlist)", path)
                    .isNull();
        }
    }
}
