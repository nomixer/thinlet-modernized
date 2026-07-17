/* Thinlet (modernized) — Phase 1 quirk-locking tests (test scope). */
package thinlet.quirks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Locks the fixed Q1 contract (0.2.x, D71): an unreadable source is a descriptive
 * {@link IOException}, never the 2005 {@link NullPointerException}.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ParserUnreadableSourceTest {

    @Test
    void parseUnresolvablePathThrowsDescriptiveIOException() {
        Thinlet thinlet = new Thinlet();
        assertThatThrownBy(() -> thinlet.parse("no/such/thinlet-resource-zzz.xml"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("no/such/thinlet-resource-zzz.xml");
    }

    @Test
    void parseNullStreamThrowsDescriptiveIOException() {
        Thinlet thinlet = new Thinlet();
        assertThatThrownBy(() -> thinlet.parse((InputStream) null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("null input stream");
    }

    @Test
    void parseValidStreamReturnsRoot() throws Exception {
        // Control: a well-formed source still parses to a non-null root — the
        // guards fire only on the unreadable-source paths.
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<panel/>".getBytes(StandardCharsets.UTF_8));
        assertThat(thinlet.parse(in)).isNotNull();
    }
}
