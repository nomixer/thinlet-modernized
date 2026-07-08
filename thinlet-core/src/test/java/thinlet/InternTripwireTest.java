package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Guards the {@code Thinlet.is} interned-token tripwire (DECISIONS.md D43).
 *
 * <p>The tripwire only protects the net while two things hold: the surefire
 * argLine actually delivers {@code thinlet.strictIntern=true} into this
 * (possibly toolchain-forked) test JVM, and a de-interned token then fails
 * loud. Both are asserted here — if the wiring assertion fails, the entire
 * suite is running without its tripwire. Needs the Xvfb display like every
 * Thinlet-touching test: class-initializing {@code Thinlet} reaches AWT (the
 * RenderingHints static block), and a failed init would poison the class for
 * the whole forked JVM.
 */
@ExtendWith(XvfbDisplayExtension.class)
class InternTripwireTest {

    @Test
    void argLineDeliversTheTripwireFlag() {
        assertThat(Boolean.getBoolean("thinlet.strictIntern"))
                .as("thinlet.strictIntern must reach the forked test JVM as true "
                        + "(surefire argLine, thinlet-core/pom.xml); without it the "
                        + "whole net runs untripwired")
                .isTrue();
    }

    @Test
    void deInternedTokenFailsLoud() {
        Assumptions.assumeTrue(Boolean.getBoolean("thinlet.strictIntern"), "tripwire disabled for this run");
        String deinterned = new String("single");
        assertThatIllegalStateException()
                .isThrownBy(() -> Thinlet.is(deinterned, "single"))
                .withMessageContaining("single");
    }

    @Test
    void internedAndMismatchSemanticsAreUntouched() {
        assertThat(Thinlet.is("single", "single")).isTrue();
        assertThat(Thinlet.is("multiple", "single")).isFalse();
        assertThat(Thinlet.is(null, "single")).isFalse();
        assertThat(Thinlet.is(Integer.valueOf(42), "single")).isFalse();
    }
}
