/* Thinlet (modernized) — live-Drafts playthrough (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.drafts.DraftsHost;

/**
 * Boots the live Drafts app headlessly through the thinlet-core test-jar seam
 * (DECISIONS.md D65): proves the tests-classifier dependency edge, the split
 * {@code thinlet.trace} package, and the Xvfb fork in this module's surefire.
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class DraftsBootTest {

    @Test
    void liveDraftsBootsHeadlessAndPaintsItsLauncher() {
        DraftsHost host = new DraftsHost(); // ctor parses drafts.xml and adds it
        Object content = host.find("content");
        assertThat(content).as("the launcher splitpane parsed and mounted").isNotNull();
        assertThat(host.getCount(host.getDesktop()))
                .as("exactly the launcher on the desktop — no ExceptionDialog")
                .isEqualTo(1);

        InputDriver d = InputDriver.attach(host, content, host::funnel);
        Object nav = host.getItem(content, 0);
        assertThat(thinlet.Thinlet.getClass(nav))
                .as("the nav tree is the first pane")
                .isEqualTo("tree");
        assertThat(host.getCount(nav)).as("all ten top-level draft nodes").isEqualTo(10);

        Trace boot = d.paint();
        assertThat(boot.calls).as("the live launcher paints a real frame").isNotEmpty();
    }
}
