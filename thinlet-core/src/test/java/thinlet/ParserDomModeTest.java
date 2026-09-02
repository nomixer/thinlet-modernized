/* Thinlet (modernized) — parser characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes {@code parseDOM} — the 'D' branch of {@code parse} — through the
 * {@code getDOM*} accessors that are its documented reading surface.
 * Decision record: {@code DECISIONS.md} D86.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ParserDomModeTest {

    /** Subclassed only to reach the protected parse entry point and accessors. */
    private static final class Dom extends Thinlet {

        Object read(String xml) throws Exception {
            InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            return parseDOM(in);
        }
    }

    @Test
    void parseDomReturnsTheRootTag() throws Exception {
        assertThat(new Dom().read("<panel/>")).isNotNull();
    }

    @Test
    void parseDomExposesElementTextThroughGetDomText() throws Exception {
        Object root = new Dom().read("<label>hello</label>");
        assertThat(Thinlet.getDOMText(root)).isEqualTo("hello");
    }

    @Test
    void parseDomReportsNullTextForAnElementWithNoContent() throws Exception {
        Object root = new Dom().read("<label></label>");
        assertThat(Thinlet.getDOMText(root)).isNull();
    }

    @Test
    void parseDomCollapsesWhitespaceAndTrimsTheTrailingSpaceFromText() throws Exception {
        Object root = new Dom().read("<label>  hello   world \n </label>");
        assertThat(Thinlet.getDOMText(root)).isEqualTo("hello world");
    }

    @Test
    void parseDomExposesAttributesThroughGetDomAttribute() throws Exception {
        Object root = new Dom().read("<panel name='a' text='hello'/>");
        assertThat(Thinlet.getDOMAttribute(root, "name")).isEqualTo("a");
        assertThat(Thinlet.getDOMAttribute(root, "text")).isEqualTo("hello");
    }

    @Test
    void parseDomReportsNullForAnAbsentAttribute() throws Exception {
        Object root = new Dom().read("<panel name='a'/>");
        assertThat(Thinlet.getDOMAttribute(root, "nosuchkey")).isNull();
    }

    @Test
    void parseDomCountsChildElementsByTagName() throws Exception {
        Object root = new Dom().read("<panel><label/><label/><button/></panel>");
        assertThat(Thinlet.getDOMCount(root, "label")).isEqualTo(2);
        assertThat(Thinlet.getDOMCount(root, "button")).isEqualTo(1);
        assertThat(Thinlet.getDOMCount(root, "checkbox")).isEqualTo(0);
    }

    @Test
    void parseDomIndexesChildElementsByTagNameAndPosition() throws Exception {
        Object root = new Dom().read("<panel><label text='first'/><label text='second'/></panel>");
        assertThat(Thinlet.getDOMAttribute(Thinlet.getDOMNode(root, "label", 0), "text"))
                .isEqualTo("first");
        assertThat(Thinlet.getDOMAttribute(Thinlet.getDOMNode(root, "label", 1), "text"))
                .isEqualTo("second");
    }

    @Test
    void parseDomKeepsNestedElementsReachableFromTheRoot() throws Exception {
        Object root = new Dom().read("<panel><panel><label>deep</label></panel></panel>");
        Object inner = Thinlet.getDOMNode(root, "panel", 0);
        assertThat(Thinlet.getDOMText(Thinlet.getDOMNode(inner, "label", 0))).isEqualTo("deep");
    }

    @Test
    void parseDomAcceptsTagAndAttributeNamesTheGuiVocabularyDoesNotDefine() throws Exception {
        // DOM mode is a generic XML reader: unlike the 'T' branch it never consults
        // the DTD, so an unknown tag is data rather than an error.
        Object root = new Dom().read("<invoice currency='GBP'><line sku='x1'>widget</line></invoice>");
        assertThat(Thinlet.getDOMAttribute(root, "currency")).isEqualTo("GBP");
        Object line = Thinlet.getDOMNode(root, "line", 0);
        assertThat(Thinlet.getDOMAttribute(line, "sku")).isEqualTo("x1");
        assertThat(Thinlet.getDOMText(line)).isEqualTo("widget");
    }
}
