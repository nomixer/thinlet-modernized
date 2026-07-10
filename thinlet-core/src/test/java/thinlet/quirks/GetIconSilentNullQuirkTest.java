/* Thinlet (modernized) — Phase 1 quirk-locking tests (test scope). */
package thinlet.quirks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.awt.Image;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Locks KNOWN-QUIRKS.md Q3: {@link Thinlet#getIcon(String, boolean)} returns
 * {@code null} for a missing or unloadable resource, swallowing the failure in
 * empty {@code catch (Throwable e) {}} blocks — no log, no throw. The widget then
 * lays out and paints as if icon-less. This is genuine 2005 behavior; D54 vendors
 * the resources the corpus references so nothing relies on it in practice, but the
 * library semantics are preserved and pinned here so they cannot drift silently.
 *
 * <p>These tests assert the <em>current</em> behavior — see KNOWN-QUIRKS.md.
 */
@Tag("documents-current-behavior")
@ExtendWith(XvfbDisplayExtension.class)
class GetIconSilentNullQuirkTest {

    @Test
    void missingResourceReturnsNullWithoutThrowing() {
        Thinlet thinlet = new Thinlet();
        // No such resource anywhere on the classpath: the classpath lookup fails, the
        // new URL(path) fallback throws MalformedURLException (no protocol), and both
        // are swallowed — the caller just gets null.
        assertThatCode(() -> {
                    Image icon = thinlet.getIcon("/icon/__does_not_exist_zzz__.gif", true);
                    assertThat(icon).isNull();
                })
                .doesNotThrowAnyException();
    }

    @Test
    void nullOrEmptyPathReturnsNull() {
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getIcon(null, false)).isNull();
        assertThat(thinlet.getIcon("", false)).isNull();
    }

    @Test
    void presentResourceResolvesNonNull() {
        // Control: a resource that IS on the (test) classpath resolves to a real image,
        // so the null above is specific to the missing-resource path, not getIcon in
        // general. /icon/library.gif is vendored under src/test/resources/icon (D54).
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getIcon("/icon/library.gif", true)).isNotNull();
    }
}
