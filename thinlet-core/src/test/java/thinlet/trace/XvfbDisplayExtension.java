/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.io.File;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Owns the controlled headless X server for trace tests (DECISIONS.md D22). When
 * {@code DISPLAY} is {@code :99} and no server is already listening there, this
 * starts {@code Xvfb :99} with a fixed resolution and no window manager, so
 * window-manager chrome never perturbs pixel metrics. Idempotent: if a server is
 * already up it does nothing.
 *
 * <p>Xvfb is launched <em>detached</em> (via {@code sh ... &}) rather than as a
 * direct child of the forked test JVM. A long-lived child process confuses
 * surefire's fork lifecycle management (it reports "error occurred in starting
 * fork" even when tests pass); detaching it into its own process avoids that. The
 * server then outlives the JVM and is reused by later forks — fine for the
 * ephemeral CI/dev containers it runs in.
 *
 * <p>Surefire sets {@code DISPLAY=:99} for the forked JVM; this runs in {@code
 * beforeAll}, before any AWT/Toolkit initialization in the tests.
 */
public final class XvfbDisplayExtension implements BeforeAllCallback {

    private static final String DISPLAY = ":99";
    private static final File SOCKET = new File("/tmp/.X11-unix/X99");
    private static final long READY_TIMEOUT_MS = 15_000L;

    private static boolean initialized;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ensureStarted();
    }

    private static synchronized void ensureStarted() throws Exception {
        if (initialized) {
            return;
        }
        String display = System.getenv("DISPLAY");
        // Only manage the controlled :99 display; if surefire was not configured
        // for it, assume an X server is already provided and do nothing.
        if (!DISPLAY.equals(display) || SOCKET.exists()) {
            initialized = true;
            return;
        }
        ProcessBuilder pb = new ProcessBuilder(
                "sh", "-c", "Xvfb " + DISPLAY + " -screen 0 1280x1024x24 -nolisten tcp >/dev/null 2>&1 &");
        Process launcher = pb.start();
        launcher.waitFor();
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;
        while (!SOCKET.exists() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100L);
        }
        if (!SOCKET.exists()) {
            throw new IllegalStateException(
                    "Xvfb " + DISPLAY + " did not become ready within " + READY_TIMEOUT_MS + "ms");
        }
        initialized = true;
    }
}
