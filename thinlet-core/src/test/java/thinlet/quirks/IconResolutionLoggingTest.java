/* Thinlet (modernized) — Enhanced-line behavior pins (test scope). */
package thinlet.quirks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import thinlet.Thinlet;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Pins the 0.2.x half of KNOWN-QUIRKS Q3 (D81): an icon that cannot be resolved or cannot
 * be loaded is reported at WARNING instead of vanishing into an empty catch block. What
 * {@code getIcon} <em>returns</em> is unchanged — {@link GetIconSilentNullQuirkTest} still
 * pins the null, which stays until the missing-image indicator lands.
 */
@ExtendWith(XvfbDisplayExtension.class)
class IconResolutionLoggingTest {

    private static final String LOGGER_NAME = "thinlet.Thinlet";

    private Logger logger;
    private CapturingHandler handler;
    private boolean useParentHandlers;

    @BeforeEach
    void captureThinletLog() {
        logger = Logger.getLogger(LOGGER_NAME);
        handler = new CapturingHandler();
        logger.addHandler(handler);
        // Keep the captured warnings out of the surefire console; restored in release().
        useParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
    }

    @AfterEach
    void release() {
        logger.removeHandler(handler);
        logger.setUseParentHandlers(useParentHandlers);
    }

    @Test
    void anUnresolvableIconPathLogsAWarningNamingThePath() {
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getIcon("/icon/__does_not_exist_zzz__.gif", true)).isNull();

        assertThat(handler.warnings()).hasSize(1);
        assertThat(handler.warnings().get(0)).contains("/icon/__does_not_exist_zzz__.gif");
    }

    @Test
    void aResolvableIconPathLogsNothing() {
        // /icon/library.gif is vendored under src/test/resources/icon (D54).
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getIcon("/icon/library.gif", true)).isNotNull();

        assertThat(handler.warnings()).isEmpty();
    }

    @Test
    void anAbsentIconPathLogsNothing() {
        // A widget with no icon attribute at all reaches getIcon with null/"": no icon was
        // asked for, so there is no miss to report.
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getIcon(null, true)).isNull();
        assertThat(thinlet.getIcon("", true)).isNull();

        assertThat(handler.warnings()).isEmpty();
    }

    @Test
    void anImageThatResolvesButCannotBeDecodedLogsAWarning(@TempDir Path dir) throws IOException {
        // The "unloadable" half of Q3: Toolkit.getImage returns a non-null Image for a URL
        // that is not an image at all, and only the MediaTracker knows it failed. A file:
        // URL misses both classpath lookups and reaches the new URL(path) fallback.
        File notAnImage = dir.resolve("not-an-image.txt").toFile();
        Files.write(notAnImage.toPath(), "this is not a GIF".getBytes("UTF-8"));
        String url = notAnImage.toURI().toURL().toString();

        Thinlet thinlet = new Thinlet();
        thinlet.getIcon(url, true);

        assertThat(handler.warnings()).hasSize(1);
        assertThat(handler.warnings().get(0)).contains("could not be loaded").contains(url);
    }

    private static final class CapturingHandler extends Handler {

        private final List<String> warnings = new ArrayList<String>();

        List<String> warnings() {
            return warnings;
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                warnings.add(record.getMessage());
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
