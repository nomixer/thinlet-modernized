/* Thinlet (modernized) — Phase 1 quirk-locking tests (test scope). */
package thinlet.quirks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Locks KNOWN_QUIRKS.md Q1: the 2005 XML parser throws {@link
 * NullPointerException} when handed an unreadable source, instead of a
 * descriptive {@link java.io.IOException}. A {@code null} stream reaches {@code
 * new InputStreamReader(...)} in the core parser; {@code parse(String)} can
 * produce that {@code null} when the path is neither a classpath resource nor a
 * valid URL (the source even carries a {@code "thows nullpointerexception"}
 * comment there). These tests assert the <em>current</em> behavior — see
 * KNOWN_QUIRKS.md — so it cannot drift silently.
 */
@Tag("documents-current-behavior")
@ExtendWith(XvfbDisplayExtension.class)
class ParserNullSourceQuirkTest {

    @Test
    void parseUnresolvablePathThrowsNpe() {
        Thinlet thinlet = new Thinlet();
        assertThatThrownBy(() -> thinlet.parse("no/such/thinlet-resource-zzz.xml"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseNullStreamThrowsNpe() {
        Thinlet thinlet = new Thinlet();
        assertThatThrownBy(() -> thinlet.parse((InputStream) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseValidStreamReturnsRoot() throws Exception {
        // Control: a well-formed source parses to a non-null root, so the quirk is
        // specific to the unreadable-source path, not parsing in general.
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<panel/>".getBytes(StandardCharsets.UTF_8));
        assertThat(thinlet.parse(in)).isNotNull();
    }
}
