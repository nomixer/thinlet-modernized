/* Thinlet (modernized) — parser characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes {@code parseXML} — the 'S' branch of {@code parse} — through the
 * {@code startElement}/{@code characters}/{@code endElement} callbacks it drives.
 * Decision record: {@code DECISIONS.md} D86.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ParserSaxModeTest {

    /**
     * Records the callback sequence as text. The transcript is the assertion
     * surface: these callbacks are the only observable output of SAX mode, which
     * returns nothing and mutates no widget tree.
     */
    private static final class Transcript extends Thinlet {

        private final List<String> events = new ArrayList<>();

        @Override
        @SuppressWarnings("rawtypes") // the 2005 signature is a raw Hashtable
        protected void startElement(String name, Hashtable attributelist) {
            events.add("start " + name + attributes(attributelist));
        }

        @Override
        protected void characters(String text) {
            events.add("text [" + text + "]");
        }

        @Override
        protected void endElement() {
            events.add("end");
        }

        /** Hashtable iteration order is unspecified, so sort before asserting. */
        @SuppressWarnings("rawtypes")
        private static String attributes(Hashtable attributelist) {
            if (attributelist == null) {
                return " (no attributes)";
            }
            TreeMap<String, String> sorted = new TreeMap<String, String>();
            for (Enumeration keys = attributelist.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                sorted.put(key, (String) attributelist.get(key));
            }
            return " " + sorted;
        }

        List<String> read(String xml) throws Exception {
            InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            parseXML(in);
            return events;
        }
    }

    @Test
    void parseXmlReportsStartAndEndForAStandaloneTag() throws Exception {
        assertThat(new Transcript().read("<panel/>")).containsExactly("start panel (no attributes)", "end");
    }

    @Test
    void parseXmlReportsTheTagNameItWasGiven() throws Exception {
        // A tag name outside the GUI vocabulary still reaches startElement verbatim:
        // SAX mode reports the name it read rather than resolving a widget from it.
        assertThat(new Transcript().read("<mywidget></mywidget>"))
                .containsExactly("start mywidget (no attributes)", "end");
    }

    @Test
    void parseXmlPassesAttributeKeysAndValuesToStartElement() throws Exception {
        assertThat(new Transcript().read("<panel name='a' text='hello'/>"))
                .containsExactly("start panel {name=a, text=hello}", "end");
    }

    @Test
    void parseXmlReportsElementTextThroughCharacters() throws Exception {
        assertThat(new Transcript().read("<label>hello</label>"))
                .containsExactly("start label (no attributes)", "text [hello]", "end");
    }

    @Test
    void parseXmlCollapsesWhitespaceAndTrimsTheTrailingSpaceFromText() throws Exception {
        // Interior whitespace runs arrive collapsed to one space, and the trailing
        // space is gone: `parse` trims one before it reports the text.
        assertThat(new Transcript().read("<label>  hello   world \n </label>"))
                .containsExactly("start label (no attributes)", "text [hello world]", "end");
    }

    @Test
    void parseXmlReportsNoCharactersForAnEmptyElement() throws Exception {
        assertThat(new Transcript().read("<label></label>")).containsExactly("start label (no attributes)", "end");
    }

    @Test
    void parseXmlNestsChildElementsBetweenTheParentStartAndEnd() throws Exception {
        assertThat(new Transcript().read("<panel><label>a</label><button/></panel>"))
                .containsExactly(
                        "start panel (no attributes)",
                        "start label (no attributes)",
                        "text [a]",
                        "end",
                        "start button (no attributes)",
                        "end",
                        "end");
    }

    @Test
    void parseXmlDoesNotReportTheXmlDeclarationAsAnElement() throws Exception {
        // `parse` assigns no tag name when its `pi` flag is set, so the declaration
        // produces no start/end pair — only the element after it does.
        assertThat(new Transcript().read("<?xml version='1.0' encoding='UTF-8'?><panel/>"))
                .containsExactly("start panel (no attributes)", "end");
    }

    @Test
    void parseXmlSkipsCommentsAndDoctypeDeclarations() throws Exception {
        assertThat(new Transcript().read("<!DOCTYPE temp SYSTEM 'x.dtd'><panel><!-- note --><label/></panel>"))
                .containsExactly("start panel (no attributes)", "start label (no attributes)", "end", "end");
    }
}
