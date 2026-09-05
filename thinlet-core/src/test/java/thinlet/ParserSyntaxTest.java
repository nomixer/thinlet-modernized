/* Thinlet (modernized) — parser characterization tests (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterizes tag/attribute syntax shared by every {@code parse} mode: end-tag
 * matching, attribute delimiters, entity decoding, and malformed-input errors.
 * Decision record: {@code DECISIONS.md} D86.
 */
@ExtendWith(XvfbDisplayExtension.class)
class ParserSyntaxTest {

    /** Subclassed to reach protected {@code parseDOM}; the accessors need only the package. */
    private static final class Dom extends Thinlet {

        Object read(String xml) throws Exception {
            InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            return parseDOM(in);
        }
    }

    /** Records whether the SAX {@code characters} callback fires during a GUI-mode ('T') parse. */
    private static final class CharactersSpy extends Thinlet {

        private final List<String> calls = new ArrayList<>();

        @Override
        protected void characters(String text) {
            calls.add(text);
        }

        Object read(String xml) throws Exception {
            InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            return parse(in);
        }
    }

    @Test
    void parseThrowsOnAnEmptyStream() {
        Thinlet thinlet = new Thinlet();
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertThatThrownBy(() -> thinlet.parse(empty)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseThrowsOnAnEmptyStartTag() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("documents-current-behavior")
    void parseInGuiModeSilentlyDropsTextBetweenTagsUnlikeDomAndSaxMode() throws Exception {
        // Neither the 'D' nor the 'S' branch of the endtag text flush matches
        // mode 'T', so the accumulated body text is thrown away: KNOWN-QUIRKS Q16.
        // A spy overriding `characters` proves the 'S' branch is not the one
        // taken either — mode 'T' calls neither text-flush arm.
        CharactersSpy thinlet = new CharactersSpy();
        Object label = thinlet.read("<label>ignored body text</label>");
        assertThat(Thinlet.getClass(label)).isEqualTo("label");
        assertThat(thinlet.getString(label, "text")).isNull();
        assertThat(thinlet.calls).isEmpty();
    }

    @Test
    void parseThrowsWhenTheClosingTagNameDoesNotMatchTheOpeningTag() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<panel></wrong>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("panel");
    }

    @Test
    void parseToleratesWhitespaceBeforeTheClosingAngleBracketOfAnEndTag() throws Exception {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<label text='hi'></label  >".getBytes(StandardCharsets.UTF_8));
        Object label = thinlet.parse(in);
        assertThat(thinlet.getString(label, "text")).isEqualTo("hi");
    }

    @Test
    void parseThrowsWhenACharacterOtherThanTheClosingAngleBracketFollowsAnEndTagsWhitespace() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<label></label X>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseThrowsWhenAStandaloneTagsSlashIsNotFollowedByTheClosingAngleBracket() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<panel/X>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseThrowsWhenAnAttributeImmediatelyFollowsAnotherWithNoWhitespace() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<panel name='a'text='b'/>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseReadsAnAttributeWithWhitespaceSurroundingTheEqualsSign() throws Exception {
        Object root = new Dom().read("<panel name = 'value'/>");
        assertThat(Thinlet.getDOMAttribute(root, "name")).isEqualTo("value");
    }

    @Test
    void parseThrowsWhenAnAttributeKeyIsNotFollowedByAnEqualsSign() {
        Dom dom = new Dom();
        assertThatThrownBy(() -> dom.read("<panel name 'value'/>")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseThrowsWhenAnAttributeValueIsNotQuoted() {
        Dom dom = new Dom();
        assertThatThrownBy(() -> dom.read("<panel name=value/>")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDecodesTheNamedAndNumericXmlEntitiesInAnAttributeValue() throws Exception {
        Object root = new Dom().read("<panel name='&lt;&gt;&amp;&quot;&apos;&#65;'/>");
        assertThat(Thinlet.getDOMAttribute(root, "name")).isEqualTo("<>&\"'A");
    }

    @Test
    void parseThrowsWhenAProcessingInstructionsQuestionMarkIsNotFollowedByTheClosingAngleBracket() {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<?xml ?X>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> thinlet.parse(in)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDoesNotMistakeATagNameStartingWithADashPairForACommentOpener() throws Exception {
        // The comment sniff only ever fires on a leading '!', which the outer
        // dispatch already routed to the doctype branch, so `charAt(0) == '!'`
        // can never be true here (D94) — but the tag name's own first three
        // characters still walk that check on every parse. A name whose 2nd and
        // 3rd characters are both '-' isolates it: the other two conjuncts pass,
        // so this is the one input where negating this term actually flips the
        // result, from "not a comment" to "read it as one".
        Object root = new Dom().read("<x--/>");
        assertThat(Thinlet.getClass(root)).isEqualTo("x--");
    }

    @Test
    void parseSkipsTheXmlDeclarationBeforeParsingTheRootTag() throws Exception {
        Thinlet thinlet = new Thinlet();
        InputStream in = new ByteArrayInputStream("<?xml version='1.0'?><panel/>".getBytes(StandardCharsets.UTF_8));
        Object root = thinlet.parse(in);
        assertThat(Thinlet.getClass(root)).isEqualTo("panel");
    }
}
