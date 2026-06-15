/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Owns the controlled headless X server for trace tests (DECISIONS.md D22). When
 * {@code DISPLAY} is {@code :99} and no live server is running there, this starts
 * {@code Xvfb :99} with a fixed resolution and no window manager, so
 * window-manager chrome never perturbs pixel metrics.
 *
 * <p>Liveness is decided by {@code pgrep} for the Xvfb process, not by the socket
 * file — a crashed server can leave a stale {@code /tmp/.X11-unix/X99} behind that
 * would otherwise fool a file-existence check. Stale socket/lock files are cleared
 * before (re)starting.
 *
 * <p>Xvfb is launched <em>detached</em> (via {@code sh ... &}), not as a direct
 * child of the forked test JVM: a long-lived child confuses surefire's fork
 * lifecycle (it reports "error occurred in starting fork" even when tests pass).
 * The server then outlives the JVM and is reused by later forks — fine for the
 * ephemeral CI/dev containers it runs in. Surefire sets {@code DISPLAY=:99}; this
 * runs in {@code beforeAll}, before any AWT/Toolkit initialization.
 */
public final class XvfbDisplayExtension implements BeforeAllCallback {

    private static final String DISPLAY = ":99";
    private static final File SOCKET = new File("/tmp/.X11-unix/X99");
    private static final File LOCK = new File("/tmp/.X99-lock");
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
        // Only manage the controlled :99 display; otherwise trust the environment.
        if (!DISPLAY.equals(System.getenv("DISPLAY")) || xvfbAlive()) {
            initialized = true;
            return;
        }
        SOCKET.delete();
        LOCK.delete();
        new ProcessBuilder("sh", "-c", "Xvfb " + DISPLAY + " -screen 0 1280x1024x24 -nolisten tcp >/dev/null 2>&1 &")
                .start()
                .waitFor();
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

    private static boolean xvfbAlive() {
        try {
            return new ProcessBuilder("pgrep", "-f", "Xvfb " + DISPLAY).start().waitFor() == 0;
        } catch (IOException pgrepMissing) {
            // No pgrep available to probe; fall back to the socket file.
            return SOCKET.exists();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SOCKET.exists();
        }
    }
}
