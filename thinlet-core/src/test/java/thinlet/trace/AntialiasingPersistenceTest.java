/* Thinlet (modernized) — quirk pin for the Q15 antialiasing fix (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;

/**
 * Pins the fixed Q15 contract (0.2.x, D88): painting through one {@code Graphics2D}
 * implementation must not disable antialiasing for every later paint.
 */
@ExtendWith(XvfbDisplayExtension.class)
class AntialiasingPersistenceTest {

    private static int hintsRecordedFor(Thinlet thinlet) {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        List<TraceCall> sink = new ArrayList<>();
        TracingGraphics2D g = new TracingGraphics2D((Graphics2D) image.getGraphics(), sink);
        g.setClip(0, 0, 200, 200);
        thinlet.setBounds(0, 0, 200, 200);
        thinlet.paint(g);
        int hints = 0;
        for (TraceCall call : sink) {
            if ("setRenderingHint".equals(call.op)) {
                hints++;
            }
        }
        return hints;
    }

    @Test
    void antialiasingSurvivesAPaintThroughADifferentGraphicsImplementation() {
        Thinlet thinlet = new Thinlet();
        thinlet.add(thinlet.create("panel"));
        assertThat(hintsRecordedFor(thinlet)).isEqualTo(2);

        // The 2005 code cached one reflectively-resolved setRenderingHint Method in a
        // static, keyed to the first Graphics class it ever saw. A second class made
        // invoke() throw, and the catch set TXT_AA = null — disabling antialiasing for
        // the whole JVM, not just that paint. Painting through the raw Graphics2D here
        // is what used to poison it; the input and robot suites do the same thing.
        BufferedImage other = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D raw = (Graphics2D) other.getGraphics();
        raw.setClip(0, 0, 200, 200);
        thinlet.paint(raw);

        assertThat(hintsRecordedFor(thinlet)).isEqualTo(2);
    }
}
