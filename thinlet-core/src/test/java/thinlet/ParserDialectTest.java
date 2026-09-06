/* Thinlet (modernized) — XML dialect characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes where the 2005 XML dialect departs from XML 1.0 — markup declarations,
 * mixed content, entity scope, attribute lexis, the declared encoding. Prose:
 * {@code backend-portability/XML-DIALECT.md}; decision record: {@code DECISIONS.md} D96.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ParserDialectTest {

    /** Subclassed to reach the protected {@code parseDOM} / {@code parseXML} entry points. */
    private static final class Modes extends Thinlet {

        private final List<String> events = new ArrayList<>();

        Object gui(String xml) throws Exception {
            return parse(stream(xml));
        }

        Object dom(String xml) throws Exception {
            return parseDOM(stream(xml));
        }

        List<String> sax(String xml) throws Exception {
            events.clear();
            parseXML(stream(xml));
            return events;
        }

        @Override
        protected void startElement(String name, Hashtable attributelist) {
            events.add("start:" + name);
        }

        @Override
        protected void endElement() {
            events.add("end");
        }

        @Override
        protected void characters(String text) {
            events.add("text:" + text);
        }
    }

    private static final class Bundle extends ListResourceBundle {

        @Override
        protected Object[][] getContents() {
            return new Object[][] {{"greeting", "Hello"}};
        }
    }

    private static InputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    // ----- markup declarations ----------------------------------------------------

    @Test
    @Tag("documents-current-behavior")
    void aMarkupDeclarationEndsAtItsFirstAngleBracketSoTheTailBecomesElementText() throws Exception {
        Modes thinlet = new Modes();

        // The '<!' branch reads to the first '>', which here sits inside the comment.
        assertThat(thinlet.getDOMText(thinlet.dom("<label>x <!-- a > b --> y</label>")))
                .isEqualTo("x b --> y");
    }

    @Test
    @Tag("documents-current-behavior")
    void aMarkupDeclarationTailContainingATagIsParsedAsMarkup() {
        // The leaked tail is re-read as content, so a commented-out element is created.
        assertThatThrownBy(() -> new Modes().gui("<label text='t'>x <!-- a > <button/> --> y</label>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("button add label");
    }

    @Test
    void aCommentWithNoAngleBracketInsideIsSkippedWholesale() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.sax("<panel><!-- note --><label/></panel>"))
                .containsExactly("start:panel", "start:label", "end", "end");
    }

    @Test
    @Tag("documents-current-behavior")
    void aCdataSectionIsSkippedAsAMarkupDeclarationAndItsContentIsLost() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getDOMText(thinlet.dom("<label><![CDATA[hello]]></label>")))
                .isNull();
        // With a '>' inside, the tail leaks the same way a comment's does.
        assertThat(thinlet.getDOMText(thinlet.dom("<label><![CDATA[a > b]]></label>")))
                .isEqualTo("b]]>");
    }

    @Test
    void aDoctypeInternalSubsetIsReadOnlyToItsFirstAngleBracket() throws Exception {
        Modes thinlet = new Modes();

        // The residue ("]>") is text, which the following start tag clears — so this
        // document survives by luck, not by the parser understanding the subset.
        assertThat(thinlet.getDOMText(thinlet.dom("<!DOCTYPE label [<!ELEMENT label EMPTY>]><label>t</label>")))
                .isEqualTo("t");
    }

    // ----- mixed content ----------------------------------------------------------

    @Test
    @Tag("documents-current-behavior")
    void textPrecedingAChildElementIsDiscardedInEveryMode() throws Exception {
        Modes thinlet = new Modes();

        // A start tag clears the text buffer, so only the run before the END tag survives.
        assertThat(thinlet.getDOMText(thinlet.dom("<panel>alpha<label/>omega</panel>")))
                .isEqualTo("omega");
        assertThat(thinlet.sax("<panel>alpha<label/>omega</panel>"))
                .containsExactly("start:panel", "start:label", "end", "text:omega", "end");
    }

    @Test
    void whitespaceRunsInElementTextCollapseToOneSpaceAndTheTrailingOneIsTrimmed() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getDOMText(thinlet.dom("<label>  a \n\t b  </label>")))
                .isEqualTo("a b");
        assertThat(thinlet.getDOMText(thinlet.dom("<label>   </label>"))).isNull();
    }

    // ----- entities ---------------------------------------------------------------

    @Test
    void entityReferencesAreDecodedInAttributeValuesButNotInElementText() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getString(thinlet.gui("<label text='a &amp; b &#65;'/>"), "text"))
                .isEqualTo("a & b A");
        assertThat(thinlet.getDOMText(thinlet.dom("<label>a &amp; b &#65;</label>")))
                .isEqualTo("a &amp; b &#65;");
    }

    @Test
    void aHexadecimalCharacterReferenceIsRecognizedOnlyWithALowercaseX() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getString(thinlet.gui("<label text='&#x41;'/>"), "text"))
                .isEqualTo("A");
        assertThatThrownBy(() -> new Modes().gui("<label text='&#X41;'/>"))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("X41");
    }

    @Test
    void anEmptyOrUnknownEntityNameIsRejected() {
        assertThatThrownBy(() -> new Modes().gui("<label text='&;'/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown entity ");
        assertThatThrownBy(() -> new Modes().gui("<label text='&nbsp;'/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown entity nbsp");
    }

    // ----- attribute lexis --------------------------------------------------------

    @Test
    void aRepeatedAttributeKeepsTheLastValueInsteadOfBeingRejected() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getString(thinlet.gui("<label text='a' text='b'/>"), "text"))
                .isEqualTo("b");
        assertThat(thinlet.getDOMAttribute(thinlet.dom("<label text='a' text='b'/>"), "text"))
                .isEqualTo("b");
    }

    @Test
    void attributeValuesKeepRawAngleBracketsThatXmlRequiresEscaped() throws Exception {
        Modes thinlet = new Modes();

        // The value loop reads to the closing quote; only '&' is special inside it.
        assertThat(thinlet.getString(thinlet.gui("<label text=\"<>\"/>"), "text"))
                .isEqualTo("<>");
    }

    @Test
    void attributeValuesKeepLiteralNewlinesAndTabsThatXmlNormalizesToSpaces() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getString(thinlet.gui("<label text=\"a\nb\"/>"), "text"))
                .isEqualTo("a\nb");
        assertThat(thinlet.getString(thinlet.gui("<label text=\"a\tb\"/>"), "text"))
                .isEqualTo("a\tb");
    }

    // ----- document structure -----------------------------------------------------

    @Test
    void contentAfterTheRootElementIsNeverRead() throws Exception {
        Modes thinlet = new Modes();

        // parse returns the moment the root closes, so nothing beyond it can fail.
        assertThat(Thinlet.getClass(thinlet.gui("<panel/>trailing garbage <not-a-tag")))
                .isEqualTo("panel");
        assertThat(Thinlet.getClass(thinlet.gui("<panel/><label/>"))).isEqualTo("panel");
    }

    @Test
    @Tag("documents-current-behavior")
    void anEndTagWithNoOpenElementThrowsNullPointerException() {
        assertThatThrownBy(() -> new Modes().gui("</panel>")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void endTagNamesAreMatchedCharacterByCharacterAndAreCaseSensitive() {
        assertThatThrownBy(() -> new Modes().gui("<panel></PANEL>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("panel");
        assertThatThrownBy(() -> new Modes().gui("<panel></ panel>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("panel");
    }

    @Test
    void aByteOrderMarkBeforeTheRootIsSwallowedAsText() throws Exception {
        Modes thinlet = new Modes();

        assertThat(Thinlet.getClass(thinlet.gui("\uFEFF<panel/>"))).isEqualTo("panel");
    }

    @Test
    void aProcessingInstructionIsSkippedOnlyWhenItsBodyLooksLikeAttributes() throws Exception {
        Modes thinlet = new Modes();

        assertThat(Thinlet.getClass(thinlet.gui("<?target?><panel/>"))).isEqualTo("panel");
        assertThat(Thinlet.getClass(thinlet.gui("<?target key='v'?><panel/>"))).isEqualTo("panel");
        assertThatThrownBy(() -> new Modes().gui("<?php echo 1; ?><panel/>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----- the content model ------------------------------------------------------

    @Test
    void theAbstractComponentElementCanBeInstantiatedFromXml() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getCount(thinlet.gui("<panel><component/></panel>"))).isEqualTo(1);
    }

    @Test
    void parentChildPairsAreCheckedOneAtATimeSoOrderAndCardinalityGoUnchecked() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getCount(thinlet.gui("<table><row/><header/></table>")))
                .isEqualTo(1);
        Object tabbedpane = thinlet.gui("<tabbedpane><tab><label/><label/></tab></tabbedpane>");
        assertThat(thinlet.getCount(thinlet.getItem(tabbedpane, 0))).isEqualTo(2);
        Object label = thinlet.gui("<label><popupmenu name='a'/><popupmenu name='b'/></label>");
        assertThat(thinlet.getString(thinlet.getWidget(label, "popupmenu"), "name"))
                .isEqualTo("b");
    }

    @Test
    void aPopupmenuMayNestInsideAnotherPopupmenu() throws Exception {
        Modes thinlet = new Modes();

        assertThat(Thinlet.getClass(thinlet.gui("<popupmenu><popupmenu/></popupmenu>")))
                .isEqualTo("popupmenu");
    }

    @Test
    void aTabIsTheOneContainerThatRejectsAPopupmenu() {
        // 'tab' descends from 'choice', not 'component', so the popupmenu arm never fires.
        assertThatThrownBy(() -> new Modes().gui("<tabbedpane><tab><popupmenu/></tab></tabbedpane>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("popupmenu add tab");
    }

    // ----- encoding ---------------------------------------------------------------

    @Test
    void theDeclaredEncodingDoesNotSelectTheCharsetTheStreamIsDecodedWith() {
        String declared = "<?xml version='1.0' encoding='ISO-8859-1'?><label \u00e9='1'/>";
        String plain = "<label \u00e9='1'/>";

        // An undeclared attribute names itself in the message, showing how the reader
        // decoded those bytes: the same bytes give the same key, declaration or not.
        assertThat(messageFor(declared.getBytes(StandardCharsets.ISO_8859_1)))
                .isEqualTo(messageFor(plain.getBytes(StandardCharsets.ISO_8859_1)));
        assertThat(messageFor(declared.getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(messageFor(plain.getBytes(StandardCharsets.UTF_8)));
        assertThat(messageFor(declared.getBytes(StandardCharsets.ISO_8859_1)))
                .isNotEqualTo(messageFor(declared.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @Tag("documents-current-behavior")
    void aDeclaredEncodingReEncodesStringAttributesUsingTheCharacterCountAsAByteCount() throws Exception {
        byte[] encoded = "\u00e9".getBytes(Charset.defaultCharset());
        assumeTrue(encoded.length > 1, "the platform default charset must be multi-byte for this to bite");

        Modes thinlet = new Modes();
        String document = "<?xml version='1.0' encoding='ISO-8859-1'?><label text='\u00e9'/>";
        Object label = thinlet.parse(new ByteArrayInputStream(document.getBytes(Charset.defaultCharset())));

        // new String(value.getBytes(), 0, value.length(), encoding) — value.length() counts
        // CHARACTERS, so every byte past the first of a multi-byte character is dropped.
        assertThat(thinlet.getString(label, "text"))
                .isEqualTo(new String(encoded, 0, 1, "ISO-8859-1"))
                .isNotEqualTo("\u00e9");
    }

    @Test
    void anUnsupportedDeclaredEncodingIsReportedAndThenIgnored() throws Exception {
        Modes thinlet = new Modes();
        String document = "<?xml version='1.0' encoding='NO-SUCH-CHARSET'?><label text='x'/>";

        assertThat(thinlet.getString(thinlet.gui(document), "text")).isEqualTo("x");
    }

    // ----- resource bundle --------------------------------------------------------

    @Test
    void anAttributeValueStartingWithI18nIsResolvedThroughTheResourceBundle() throws Exception {
        Modes thinlet = new Modes();
        thinlet.setResourceBundle(new Bundle());

        // Every attribute goes through the lookup, not only the text of a label.
        assertThat(thinlet.getString(thinlet.gui("<label text='i18n.greeting'/>"), "text"))
                .isEqualTo("Hello");
        assertThat(thinlet.getString(thinlet.gui("<label name='i18n.greeting'/>"), "name"))
                .isEqualTo("Hello");
    }

    @Test
    void anI18nValueIsLeftLiteralWhenNoBundleIsSet() throws Exception {
        Modes thinlet = new Modes();

        assertThat(thinlet.getString(thinlet.gui("<label text='i18n.greeting'/>"), "text"))
                .isEqualTo("i18n.greeting");
    }

    @Test
    void anI18nKeyMissingFromTheBundleAbortsTheParse() {
        Modes thinlet = new Modes();
        thinlet.setResourceBundle(new Bundle());

        assertThatThrownBy(() -> thinlet.gui("<label text='i18n.absent'/>"))
                .isInstanceOf(MissingResourceException.class);
    }

    private static String messageFor(byte[] document) {
        try {
            new Modes().parse(new ByteArrayInputStream(document));
            return "parsed";
        } catch (Exception failure) {
            return failure.getMessage();
        }
    }
}
