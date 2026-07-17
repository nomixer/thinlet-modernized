/* Thinlet (modernized) — live-Drafts playthrough (test scope). */
package thinlet.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.io.File;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.Thinlet;
import thinlet.drafts.DraftsHost;

/**
 * Drives the LIVE Drafts app (real handlers, not CorpusHandler stubs) over the
 * deterministic-page allowlist, getter-asserted; hazard pages are navigated to
 * but never clicked into (DECISIONS.md D65).
 */
@Tag("input")
@ExtendWith(XvfbDisplayExtension.class)
class DraftsPlaythroughTest {

    private DraftsHost host;
    private InputDriver d;

    private InputDriver boot() {
        host = new DraftsHost();
        d = InputDriver.attach(host, host.find("content"), host::funnel);
        return d;
    }

    /**
     * Selects a nav-tree node by index path and waits for the real loadDraft to
     * splice the page into the splitpane. The text assertion guards the paths
     * against the two same-text "Widgets" nodes (folder vs leaf).
     */
    private Object navigate(String expectedText, int... path) {
        Object content = host.find("content");
        Object node = host.getItem(content, 0);
        for (int i : path) {
            node = host.getItem(node, i);
        }
        assertThat(host.getString(node, "text"))
                .as("nav path resolves %s", expectedText)
                .isEqualTo(expectedText);
        d.focusGained();
        d.click(node);
        d.paint(); // re-validate before any in-page aiming (bounds are computed at paint)
        Object page = host.getItem(content, 1);
        assertThat(page)
                .as("%s page loaded by the real loadDraft", expectedText)
                .isNotNull();
        return page;
    }

    /** Drafts.handleException swallows handler errors into a dialog — fail loud instead. */
    private void assertNoExceptionDialog() {
        assertThat(host.getCount(host.getDesktop()))
                .as("no ExceptionDialog popped during the scenario")
                .isEqualTo(1);
    }

    @Test
    void landingPagesLoadWithoutTouchingTheirHazards() {
        boot();
        assertThat(Thinlet.getClass(navigate("Folder browser", 0, 6))).isEqualTo("tree");
        assertThat(Thinlet.getClass(navigate("Choosers", 0, 7))).isEqualTo("panel");
        assertThat(Thinlet.getClass(navigate("Bean test", 0, 8))).isEqualTo("splitpane");
        assertThat(Thinlet.getClass(navigate("Focus test", 0, 9))).isEqualTo("panel");
        assertThat(Thinlet.getClass(navigate("Modal dialog", 5))).isEqualTo("panel");
        assertThat(Thinlet.getClass(navigate("Widgets", 6))).isEqualTo("panel");
        assertThat(Thinlet.getClass(navigate("Progress Monitor", 8))).isEqualTo("panel");
        assertNoExceptionDialog();
    }

    @Test
    void tabbedPanePageChangesAndAddsTabs() {
        boot();
        Object page = navigate("Tabbed pane", 0, 1);
        Object tabbed = host.find("tabbed");
        Object controls = host.getItem(page, 2);
        Trace before = d.paint();

        d.click(host.getItem(controls, 3)); // "Change text"
        assertThat(host.getString(host.getItem(tabbed, 0), "text"))
                .as("changeText renames the selected tab")
                .isEqualTo("First!");

        d.paint();
        d.click(host.getItem(controls, 5)); // "Add"
        assertThat(host.getCount(tabbed)).as("addTab appends a sixth tab").isEqualTo(6);
        assertThat(host.getString(host.getItem(tabbed, 5), "text")).isEqualTo("New tab");

        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the page visibly changed")
                .isNotEmpty();
        assertNoExceptionDialog();
    }

    @Test
    void treeDemoPopupCollapsesTheNode() {
        boot();
        navigate("Tree demo", 0, 2);
        Object tree = host.find("tree");
        Object nodeA = host.getItem(tree, 0);
        assertThat(host.getBoolean(nodeA, "expanded"))
                .as("Node A starts expanded")
                .isTrue();

        d.click(nodeA);
        d.paint();
        d.metaClick(nodeA);
        Object popup = d.property(host.find("treepopup"), ":popup");
        assertThat(popup).as("the context menu opened and menushown ran").isNotNull();
        assertThat(host.getBoolean(host.find("collapse"), "enabled"))
                .as("updatePopup enables Collapse on an expanded node")
                .isTrue();
        assertThat(host.getBoolean(host.find("expand"), "enabled"))
                .as("and disables Expand")
                .isFalse();

        Rectangle ib = (Rectangle) d.property(host.find("collapse"), "bounds");
        d.clickAt(popup, ib.x + ib.width / 2, ib.y + ib.height / 2);
        assertThat(host.getBoolean(nodeA, "expanded")).as("Collapse ran").isFalse();
        assertThat(d.property(host.find("treepopup"), ":popup"))
                .as("the popup closed")
                .isNull();
        assertNoExceptionDialog();
    }

    @Test
    void listsPageMovesItemsAndFillsTheTable() {
        boot();
        Object page = navigate("List, Table & Tree", 0, 3);
        Object rightbutton = host.find("rightbutton");
        assertThat(host.getBoolean(rightbutton, "enabled"))
                .as("the move button starts disabled")
                .isFalse();

        d.click(host.getItem(host.find("leftlist"), 0));
        assertThat(host.getBoolean(rightbutton, "enabled"))
                .as("selecting a row enables the move button")
                .isTrue();
        d.paint();
        d.click(rightbutton);
        assertThat(host.getCount(host.find("leftlist")))
                .as("Item-A left the left list")
                .isEqualTo(3);
        assertThat(host.getCount(host.find("rightlist"))).isEqualTo(1);
        assertThat(host.getString(host.getItem(host.find("rightlist"), 0), "text"))
                .isEqualTo("Item-A");

        d.paint();
        d.click(host.getItem(page, 1)); // Table tab header
        d.paint();
        Object buttons = host.getItem(host.getItem(host.getItem(page, 1), 0), 1);
        d.click(host.getItem(buttons, 0)); // Add
        assertThat(host.getCount(host.find("table")))
                .as("addTableRows adds 30 rows")
                .isEqualTo(30);
        Object firstCell = host.getItem(host.getItem(host.find("table"), 0), 0);
        assertThat(host.getString(firstCell, "text")).isEqualTo("Cell-1-1");
        d.paint();
        d.click(host.getItem(buttons, 1)); // Clear
        assertThat(host.getCount(host.find("table")))
                .as("clearTable empties it")
                .isZero();
        assertNoExceptionDialog();
    }

    @Test
    void mdiPageOpensDialogsOnItsNestedDesktop() {
        boot();
        Object page = navigate("MDI", 0, 4);
        Object nested = host.find("desktop");
        assertThat(host.getCount(nested)).as("the nested desktop starts empty").isZero();

        Object toolbarNew = host.getItem(host.getItem(page, 1), 0);
        d.click(toolbarNew);
        assertThat(host.getCount(nested)).as("New opens an MDI dialog").isEqualTo(1);
        Object dlg = host.getItem(nested, 0);
        assertThat(Thinlet.getClass(dlg)).isEqualTo("dialog");
        assertThat(host.getString(dlg, "text")).isEqualTo("Dialog");
        d.paint();
        d.click(toolbarNew);
        assertThat(host.getCount(nested))
                .as("a second New stacks a second dialog")
                .isEqualTo(2);
        assertNoExceptionDialog();
    }

    @Test
    void dialogDemoTogglesLiveDialogFlags() {
        boot();
        navigate("Dialog demo", 0, 10);
        Object dlg = host.find("dialog");
        assertThat(host.getBoolean(dlg, "closable")).as("closable starts off").isFalse();
        Trace before = d.paint();

        d.click(host.getItem(dlg, 5)); // Closable checkbox
        assertThat(host.getBoolean(dlg, "closable")).as("setClosable ran").isTrue();
        d.paint();
        d.click(host.getItem(dlg, 7)); // Iconifiable checkbox
        assertThat(host.getBoolean(dlg, "iconifiable")).as("setIconifiable ran").isTrue();

        assertThat(TraceComparator.compare(before, d.paint(), 0.0))
                .as("the title glyphs painted (cross-ref Q7: paint-only)")
                .isNotEmpty();
        assertNoExceptionDialog();
    }

    @Test
    void eventLoggerLogsExactActionLines() {
        boot();
        Object page = navigate("Event logger", 2);
        Object logarea = host.getItem(page, 1);
        Object tab1panel = host.getItem(host.getItem(host.getItem(page, 0), 0), 0);
        // tab1panel is the first tab's content panel; its children are the widgets
        d.click(host.getItem(tab1panel, 0)); // "Button"
        assertThat(host.getString(logarea, "text"))
                .as("the six-arg action logs its exact argument rendering")
                .isEqualTo("button action Button 'string' 12 1234567890 12.34 45.6");
        d.paint();
        d.click(host.getItem(tab1panel, 1)); // "CheckBox"
        assertThat(host.getString(logarea, "text"))
                .isEqualTo("button action Button 'string' 12 1234567890 12.34 45.6\nbutton action CheckBox");
        assertNoExceptionDialog();
    }

    @Test
    void internationalizationSwitchesToHungarian() {
        boot();
        navigate("Internationalization", 3);
        Object panel = host.find("panel");
        assertThat(host.getCount(panel))
                .as("label + combobox before any selection")
                .isEqualTo(2);
        Object combo = host.getItem(panel, 1);

        d.focusGained();
        d.click(combo);
        d.arrowDown();
        d.enter();
        assertThat(host.getInteger(combo, "selected")).as("Hungarian committed").isEqualTo(1);
        assertThat(host.getCount(panel))
                .as("update() appended the bundle panel")
                .isEqualTo(3);
        Object sub = host.getItem(panel, 2);
        assertThat(host.getString(host.getItem(sub, 0), "text")).isEqualTo("Angol");
        assertThat(host.getString(host.getItem(sub, 1), "text")).isEqualTo("Magyar");
        assertNoExceptionDialog();
    }

    @Test
    void looksPageSwitchesTabs() {
        boot();
        Object page = navigate("Looks", 7);
        assertThat(host.getInteger(page, "selected")).isZero();
        Trace before = d.paint();
        d.click(host.getItem(page, 1));
        assertThat(host.getInteger(page, "selected")).as("second tab selected").isEqualTo(1);
        assertThat(TraceComparator.compare(before, d.paint(), 0.0)).isNotEmpty();
        assertNoExceptionDialog();
    }

    @Test
    void revisitingAPageReusesTheCachedTree() {
        boot();
        Object first = navigate("Tabbed pane", 0, 1);
        navigate("Looks", 7);
        Object again = navigate("Tabbed pane", 0, 1);
        assertThat(again)
                .as("loadDraft caches the parsed page per nav node (putProperty draft)")
                .isSameAs(first);
        assertNoExceptionDialog();
    }

    /** The fixed Q8 contract (0.2.x, D72): the tree roots at the real filesystem and expands gracefully. */
    @Test
    void folderBrowserExpandsTheRealFilesystemRootGracefully() {
        boot();
        Object page = navigate("Folder browser", 0, 6);
        Object rootNode = host.getItem(page, 0);
        assertThat(host.getString(rootNode, "text"))
                .as("the tree roots at the platform's first filesystem root, not a hardcoded C:")
                .isEqualTo(File.listRoots()[0].getPath());
        assertThat(host.getCount(rootNode))
                .as("the lazy 'loading...' placeholder child")
                .isEqualTo(1);

        d.focusGained();
        d.click(rootNode);
        d.arrowRight(); // keyboard expand fires FolderBrowser.expand — now graceful

        assertThat(host.getCount(host.getDesktop()))
                .as("no ExceptionDialog: the expansion succeeded")
                .isEqualTo(1);
        assertThat(host.getCount(rootNode))
                .as("the real listing replaced the placeholder")
                .isGreaterThanOrEqualTo(1);
        for (int i = 0; i < host.getCount(rootNode); i++) {
            assertThat(host.getString(host.getItem(rootNode, i), "text"))
                    .as("the placeholder is gone")
                    .isNotEqualTo("loading...");
        }
    }

    @Test
    void playthroughIsDeterministicAcrossTwoHosts() {
        boot();
        navigate("Tabbed pane", 0, 1);
        Trace first = d.paint();

        DraftsHost host2 = new DraftsHost();
        InputDriver d2 = InputDriver.attach(host2, host2.find("content"), host2::funnel);
        DraftsHost prev = host;
        InputDriver prevD = d;
        host = host2;
        d = d2;
        navigate("Tabbed pane", 0, 1);
        Trace second = d.paint();
        host = prev;
        d = prevD;

        assertThat(TraceComparator.compare(first, second, 0.0))
                .as("two fresh hosts render the identical live frame")
                .isEmpty();
    }
}
