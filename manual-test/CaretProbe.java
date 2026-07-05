import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import thinlet.FrameLauncher;
import thinlet.Thinlet;

/**
 * Manual probe (NOT part of the build): launches a Thinlet XML with real AWT on your
 * desktop and prints a textfield/textarea's caret (start = selection anchor, end =
 * caret) whenever it changes. Use it to see what a real mouse click does to the caret —
 * the one input path the headless synthetic driver can't reproduce (it doesn't prime the
 * field's :offset/referencex). See DECISIONS.md D40.
 *
 * <p>Default package + only the public Thinlet API, so it compiles against thinlet-core's
 * classes with no test dependencies. Run it directly (NOT through surefire — surefire
 * forces DISPLAY=:99). See manual-test instructions.
 */
public final class CaretProbe {

    private static final String[] NAMES = {"tf", "ta"};

    public static void main(String[] args) throws Exception {
        String xml = (args.length > 0) ? args[0] : "manual-test/caret.xml";
        Thinlet thinlet = new Thinlet();
        Object root;
        try (InputStream in = new FileInputStream(xml)) {
            root = thinlet.parse(in, new Object());
        }
        thinlet.add(root);
        new FrameLauncher("Caret probe — type, then CLICK inside the text; watch this console", thinlet, 720, 320);

        System.out.println("Caret probe running (" + xml + ").");
        System.out.println("Type into a field, then click in the MIDDLE of the text.");
        System.out.println("Each line = a caret change:  <field>  start=<anchor> end=<caret>  text=\"...\"");
        System.out.println("Close the window to quit.\n");

        int[] lastStart = new int[NAMES.length];
        int[] lastEnd = new int[NAMES.length];
        Arrays.fill(lastStart, Integer.MIN_VALUE);
        Arrays.fill(lastEnd, Integer.MIN_VALUE);
        while (true) {
            for (int i = 0; i < NAMES.length; i++) {
                Object w = thinlet.find(NAMES[i]);
                if (w == null) {
                    continue;
                }
                int s = caret(thinlet, w, "start");
                int e = caret(thinlet, w, "end");
                if ((s != lastStart[i]) || (e != lastEnd[i])) {
                    System.out.println(NAMES[i] + "  start=" + s + " end=" + e + "  text=\""
                            + thinlet.getString(w, "text") + "\"");
                    lastStart[i] = s;
                    lastEnd[i] = e;
                }
            }
            Thread.sleep(120L);
        }
    }

    /** Public getInteger throws if the property is unset; treat unset as -1. */
    private static int caret(Thinlet thinlet, Object widget, String key) {
        try {
            return thinlet.getInteger(widget, key);
        } catch (RuntimeException unset) {
            return -1;
        }
    }

    private CaretProbe() {}
}
