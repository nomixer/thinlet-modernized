/* Thinlet (modernized) — XML dialect grammar characterization (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Pins the structural grammar of the GUI-mode XML dialect and its divergences from the
 * shipped 2005 {@code thinlet.dtd}. Prose: {@code backend-portability/XML-DIALECT.md};
 * decision record: {@code DECISIONS.md} D96.
 */
class XmlDialectGrammarTest {

    /** The generated dialect grammar, as committed. */
    private static final String DIALECT_RESOURCE = "/dialect/thinlet-dialect.dtd";

    /** Where that resource is authored — named so a mismatch failure says what to regenerate. */
    private static final String DIALECT_SOURCE = "thinlet-core/src/test/resources/dialect/thinlet-dialect.dtd";

    /** The frozen 2005 artifact, shipped in the library jar (D8). */
    private static final String SHIPPED_RESOURCE = "/thinlet.dtd";

    private static final File CORPUS_DIR = new File("src/test/resources/corpus");

    /** The one corpus document a standards-conforming XML parser rejects. */
    private static final String NOT_WELL_FORMED = "drafts/lists.xml";

    // ----- the committed grammar tracks the code ---------------------------------

    @Test
    void theDialectGrammarMatchesTheDefinitionTableAndTheAddRules() throws Exception {
        assertThat(readResource(DIALECT_RESOURCE))
                .as("%s is stale — regenerate it from DialectGrammar.render()", DIALECT_SOURCE)
                .isEqualTo(DialectGrammar.render());
    }

    // ----- the corpus conforms to the grammar ------------------------------------

    @Test
    void everyCorpusDocumentIsValidAgainstTheDialectGrammar() throws Exception {
        Map<String, List<String>> failures = new LinkedHashMap<>();
        for (File corpusFile : corpusFiles()) {
            String name = corpusName(corpusFile);
            if (NOT_WELL_FORMED.equals(name)) {
                continue;
            }
            List<String> errors = validateAgainstDialect(corpusFile);
            if (!errors.isEmpty()) {
                failures.put(name, errors);
            }
        }
        assertThat(failures).isEmpty();
    }

    @Test
    void theCorpusIsTheFortyTwoDocumentsTheGoldenNetRenders() {
        assertThat(corpusFiles()).hasSize(42);
    }

    @Test
    void theOneCorpusDocumentAStandardXmlParserRejectsIsStillParsedByThinlet() throws Exception {
        File corpusFile = new File(CORPUS_DIR, NOT_WELL_FORMED);

        // Not well-formed for exactly one reason: two buttons carry raw angle brackets.
        String source = new String(Files.readAllBytes(corpusFile.toPath()), StandardCharsets.UTF_8);
        assertThat(source).contains("text=\"<\"").contains("text=\">\"");
        assertThatThrownBy(() -> validateAgainstDialect(corpusFile))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("<");

        // Thinlet reads the same bytes to the root's end tag without complaint. DOM mode
        // isolates the XML layer: GUI mode would go on to resolve this scene's action
        // handlers, which belong to the Drafts app, not to a bare Thinlet.
        try (InputStream in = Files.newInputStream(corpusFile.toPath())) {
            Object root = new Dom().read(in);
            assertThat(new Dom().getDOMCount(root, "tab")).isEqualTo(3);
        }
    }

    /** Subclassed to reach the protected {@code parseDOM} entry point. */
    private static final class Dom extends Thinlet {

        Object read(InputStream in) throws IOException {
            return parseDOM(in);
        }
    }

    // ----- divergences from the shipped 2005 DTD ---------------------------------

    @Test
    void theShippedDtdIsNeverReadByTheLibrary() throws Exception {
        assertThat(Thinlet.class.getResourceAsStream(SHIPPED_RESOURCE))
                .as("the 2005 DTD ships in the jar")
                .isNotNull();

        List<String> referencing = new ArrayList<>();
        for (File source : javaSources(new File("src/main/java"))) {
            if (new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8).contains(".dtd")) {
                referencing.add(source.getPath());
            }
        }
        assertThat(referencing)
                .as("no library source resolves, loads or validates against the DTD")
                .isEmpty();
    }

    @Test
    void theShippedDtdDeclaresGtAsAnEqualsSignWhileTheParserDecodesItAsAGreaterThan() throws Exception {
        assertThat(readResource(SHIPPED_RESOURCE)).contains("<!ENTITY gt \"&#61;\">");
        assertThat((char) 61).isEqualTo('=');

        Thinlet thinlet = new Thinlet();
        Object label = thinlet.parse(xml("<label text='a &gt; b'/>"));
        assertThat(thinlet.getString(label, "text")).isEqualTo("a > b");
    }

    @Test
    void theShippedDtdDeclaresNoPcdataYetEveryElementToleratesText() throws Exception {
        assertThat(readResource(SHIPPED_RESOURCE)).doesNotContain("#PCDATA");

        Thinlet thinlet = new Thinlet();
        Object label = thinlet.parse(xml("<label text='x'>body text</label>"));
        assertThat(thinlet.getString(label, "text")).isEqualTo("x");
    }

    @Test
    void theShippedDtdAndTheDefinitionTableDisagreeAboutExactlyTwoElementsAttributes() throws Exception {
        Map<String, List<String>> declaredNotAccepted = new LinkedHashMap<>();
        Map<String, List<String>> acceptedNotDeclared = new LinkedHashMap<>();
        ShippedDtd dtd = ShippedDtd.read(readResource(SHIPPED_RESOURCE));

        for (String element : DialectGrammar.names()) {
            List<String> accepted = DialectGrammar.attributesOf(element);
            List<String> declared = dtd.attributesOf(element);
            List<String> extra = new ArrayList<>(declared);
            extra.removeAll(accepted);
            List<String> missing = new ArrayList<>(accepted);
            missing.removeAll(declared);
            if (!extra.isEmpty()) {
                declaredNotAccepted.put(element, extra);
            }
            if (!missing.isEmpty()) {
                acceptedNotDeclared.put(element, missing);
            }
        }

        // The DTD gives desktop the panel attribute list; the definition table makes
        // desktop a direct child of component, so the parser rejects all ten.
        assertThat(declaredNotAccepted)
                .containsOnlyKeys("desktop")
                .containsEntry(
                        "desktop",
                        Arrays.asList(
                                "columns",
                                "top",
                                "left",
                                "bottom",
                                "right",
                                "gap",
                                "text",
                                "icon",
                                "border",
                                "scrollable"));
        assertThatThrownBy(() -> new Thinlet().parse(xml("<desktop columns='2'/>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown columns");

        // The DTD omits label's 'for' from button's attribute list; the parser inherits it.
        assertThat(acceptedNotDeclared).containsOnlyKeys("button").containsEntry("button", Arrays.asList("for"));
        Thinlet thinlet = new Thinlet();
        Object panel = thinlet.parse(xml("<panel><button for='l'/><label name='l'/></panel>"));
        assertThat(thinlet.getCount(panel)).isEqualTo(2);
    }

    @Test
    void theShippedDtdDeclaresDialogsTextAndIconTwice() throws Exception {
        ShippedDtd dtd = ShippedDtd.read(readResource(SHIPPED_RESOURCE));

        assertThat(dtd.duplicateAttributesOf("dialog")).containsExactly("text", "icon");
        for (String element : DialectGrammar.names()) {
            if (!"dialog".equals(element)) {
                assertThat(dtd.duplicateAttributesOf(element))
                        .as("%s has no duplicate attribute declaration", element)
                        .isEmpty();
            }
        }
    }

    @Test
    void theShippedDtdDeclaresTheSameThirtyFiveElementsAsTheDefinitionTable() throws Exception {
        ShippedDtd dtd = ShippedDtd.read(readResource(SHIPPED_RESOURCE));

        assertThat(dtd.elementNames()).containsExactlyInAnyOrderElementsOf(DialectGrammar.names());
        assertThat(DialectGrammar.names()).hasSize(35);
    }

    // ----- the generator ---------------------------------------------------------

    /**
     * Renders the dialect grammar from {@link DescriptorTable} and {@link Thinlet#addImpl}.
     * The committed resource is this text; nothing else may author it.
     */
    static final class DialectGrammar {

        private static final String HEADER =
                "<!-- Thinlet (modernized) — the GUI-mode XML dialect, as a DTD. TEST RESOURCE.\n"
                        + "\n"
                        + "     Generated from DescriptorTable.WIDGETS and Thinlet.addImpl and regenerated\n"
                        + "     on every run by XmlDialectGrammarTest, which fails when this file and the\n"
                        + "     code disagree. Never shipped: the published artifact is the byte-identical\n"
                        + "     2005 src/main/resources/thinlet.dtd (DECISIONS.md D8), which this file\n"
                        + "     deliberately does not match. The divergences are catalogued in\n"
                        + "     project-docs/backend-portability/XML-DIALECT.md (DECISIONS.md D96).\n"
                        + "\n"
                        + "     Content models are mixed and unordered because the parser is: it checks\n"
                        + "     each parent/child pair on its own (Thinlet.addImpl) and enforces neither\n"
                        + "     order nor cardinality, and it tolerates text anywhere (discarding it in\n"
                        + "     GUI mode — KNOWN-QUIRKS Q16).\n"
                        + "\n"
                        + "     Attributes are CDATA #IMPLIED throughout: this grammar pins element\n"
                        + "     structure and attribute NAMES only. The value layer — types, enumerated\n"
                        + "     tokens, defaults — is single-homed in DescriptorTable and pinned by\n"
                        + "     DescriptorContractTest and PublicVocabularyContractTest (D57).\n"
                        + "     Attribute order is the resolution order of Thinlet.getDefinition: an\n"
                        + "     element's own rows first, then each ancestor's, deduplicated so that a\n"
                        + "     shadowing row (button's own `alignment`) appears once, where it wins.\n"
                        + "\n"
                        + "     The five predefined XML entities are deliberately NOT declared here.\n"
                        + "     -->\n";

        private DialectGrammar() {}

        /** Every element name the GUI parser can instantiate, in definition-table order. */
        static List<String> names() {
            List<String> names = new ArrayList<>();
            for (WidgetDescriptor widget : DescriptorTable.WIDGETS) {
                names.add(widget.name);
            }
            return names;
        }

        /** The children {@code addImpl} accepts under {@code parent}, in definition-table order. */
        static List<String> childrenOf(String parent) {
            List<String> accepted = new ArrayList<>();
            for (String child : names()) {
                try {
                    Thinlet.addImpl(Thinlet.create(parent), Thinlet.create(child), -1);
                    accepted.add(child);
                } catch (IllegalArgumentException rejected) {
                    // not a legal parent/child pair
                }
            }
            return accepted;
        }

        /** The attribute names {@code getDefinition} resolves for {@code classname}, own rows first. */
        static List<String> attributesOf(String classname) {
            LinkedHashSet<String> attributes = new LinkedHashSet<>();
            String current = classname;
            while (current != null) {
                String parent = null;
                for (WidgetDescriptor widget : DescriptorTable.WIDGETS) {
                    if (widget.name.equals(current)) {
                        if (widget.attributes != null) {
                            for (AttributeDescriptor attribute : widget.attributes) {
                                attributes.add(attribute.name);
                            }
                        }
                        parent = widget.parent;
                        break;
                    }
                }
                current = parent;
            }
            return new ArrayList<>(attributes);
        }

        static String render() {
            StringBuilder out = new StringBuilder(HEADER);
            for (String name : names()) {
                out.append('\n');
                List<String> children = childrenOf(name);
                out.append("<!ELEMENT ").append(name).append(' ');
                if (children.isEmpty()) {
                    out.append("(#PCDATA)>\n");
                } else {
                    out.append("(#PCDATA");
                    for (String child : children) {
                        out.append(" | ").append(child);
                    }
                    out.append(")*>\n");
                }
                List<String> attributes = attributesOf(name);
                if (!attributes.isEmpty()) {
                    out.append("<!ATTLIST ").append(name);
                    for (String attribute : attributes) {
                        out.append("\n        ").append(attribute).append(" CDATA #IMPLIED");
                    }
                    out.append(">\n");
                }
            }
            return out.toString();
        }
    }

    // ----- reading the shipped DTD -----------------------------------------------

    /**
     * Just enough DTD reader for the shipped artifact: parameter-entity expansion plus the
     * element and attribute names it declares. Not a general DTD parser.
     */
    static final class ShippedDtd {

        private static final Pattern PARAMETER_ENTITY =
                Pattern.compile("<!ENTITY\\s+%\\s+(\\w+)\\s+\"(.*?)\">", Pattern.DOTALL);
        private static final Pattern ELEMENT = Pattern.compile("<!ELEMENT\\s+(\\w+)\\s");
        private static final Pattern ATTLIST = Pattern.compile("<!ATTLIST\\s+(\\w+)\\s+(.*?)>", Pattern.DOTALL);
        private static final Pattern REFERENCE = Pattern.compile("%(\\w+);");
        private static final Pattern DECLARATION = Pattern.compile(
                "([\\w:.-]+)\\s+(\\([^)]*\\)|CDATA|ID|IDREFS?|NMTOKENS?)\\s+"
                        + "(#REQUIRED|#IMPLIED|#FIXED\\s+\\S+|'[^']*'|\"[^\"]*\")",
                Pattern.DOTALL);

        private final List<String> elements = new ArrayList<>();
        private final Map<String, List<String>> attributes = new LinkedHashMap<>();

        static ShippedDtd read(String source) {
            ShippedDtd dtd = new ShippedDtd();
            Map<String, String> parameters = new LinkedHashMap<>();
            Matcher entities = PARAMETER_ENTITY.matcher(source);
            while (entities.find()) {
                parameters.put(entities.group(1), entities.group(2));
            }
            Matcher elements = ELEMENT.matcher(source);
            while (elements.find()) {
                dtd.elements.add(elements.group(1));
            }
            Matcher attlists = ATTLIST.matcher(source);
            while (attlists.find()) {
                List<String> declared = new ArrayList<>();
                Matcher rows = DECLARATION.matcher(expand(attlists.group(2), parameters));
                while (rows.find()) {
                    declared.add(rows.group(1));
                }
                dtd.attributes.put(attlists.group(1), declared);
            }
            return dtd;
        }

        private static String expand(String body, Map<String, String> parameters) {
            String previous = null;
            String current = body;
            for (int pass = 0; pass < 20 && !current.equals(previous); pass++) {
                previous = current;
                StringBuffer out = new StringBuffer();
                Matcher references = REFERENCE.matcher(current);
                while (references.find()) {
                    String replacement = parameters.get(references.group(1));
                    references.appendReplacement(
                            out, Matcher.quoteReplacement(replacement != null ? replacement : references.group()));
                }
                references.appendTail(out);
                current = out.toString();
            }
            return current;
        }

        List<String> elementNames() {
            return Collections.unmodifiableList(elements);
        }

        /** Declared attribute names for {@code element}, deduplicated, in declaration order. */
        List<String> attributesOf(String element) {
            return new ArrayList<>(
                    new LinkedHashSet<>(attributes.getOrDefault(element, Collections.<String>emptyList())));
        }

        /** Names {@code element} declares more than once — a DTD-authoring error. */
        List<String> duplicateAttributesOf(String element) {
            List<String> declared = attributes.getOrDefault(element, Collections.<String>emptyList());
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            LinkedHashSet<String> duplicates = new LinkedHashSet<>();
            for (String name : declared) {
                if (!seen.add(name)) {
                    duplicates.add(name);
                }
            }
            return new ArrayList<>(duplicates);
        }
    }

    // ----- helpers ----------------------------------------------------------------

    private static InputStream xml(String document) {
        return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream in = XmlDialectGrammarTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("resource %s is on the test classpath", resource).isNotNull();
            StringBuilder out = new StringBuilder();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                char[] buffer = new char[8192];
                for (int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                    out.append(buffer, 0, read);
                }
            }
            return out.toString();
        }
    }

    private static List<File> corpusFiles() {
        List<File> found = new ArrayList<>();
        collect(CORPUS_DIR, found);
        Collections.sort(found);
        return found;
    }

    private static void collect(File directory, List<File> found) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collect(entry, found);
            } else if (entry.getName().endsWith(".xml")) {
                found.add(entry);
            }
        }
    }

    private static List<File> javaSources(File directory) {
        List<File> found = new ArrayList<>();
        File[] entries = directory.listFiles();
        if (entries == null) {
            return found;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                found.addAll(javaSources(entry));
            } else if (entry.getName().endsWith(".java")) {
                found.add(entry);
            }
        }
        return found;
    }

    private static String corpusName(File corpusFile) {
        return CORPUS_DIR.toPath().relativize(corpusFile.toPath()).toString().replace(File.separatorChar, '/');
    }

    /**
     * Validates one corpus document against the dialect grammar, which the corpus files
     * cannot name themselves: they carry no DOCTYPE, so one is injected here.
     */
    private static List<String> validateAgainstDialect(File corpusFile) throws Exception {
        String source = new String(Files.readAllBytes(corpusFile.toPath()), StandardCharsets.UTF_8);
        String declaration = "";
        Matcher xmlDeclaration =
                Pattern.compile("\\A\\s*<\\?xml.*?\\?>", Pattern.DOTALL).matcher(source);
        if (xmlDeclaration.find()) {
            declaration = xmlDeclaration.group();
            source = source.substring(xmlDeclaration.end());
        }
        String withoutComments = source.replaceAll("(?s)<!--.*?-->", "");
        Matcher rootElement = Pattern.compile("<([A-Za-z_][\\w.-]*)").matcher(withoutComments);
        assertThat(rootElement.find()).as("%s has a root element", corpusFile).isTrue();
        String document =
                declaration + "\n<!DOCTYPE " + rootElement.group(1) + " SYSTEM \"thinlet-dialect.dtd\">\n" + source;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(
                (publicId, systemId) -> new InputSource(new StringReader(readResource(DIALECT_RESOURCE))));
        List<String> errors = new ArrayList<>();
        builder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                // DTD-authoring warnings are not validity failures.
            }

            @Override
            public void error(SAXParseException exception) {
                errors.add(exception.getLineNumber() + ": " + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXParseException {
                throw exception;
            }
        });
        builder.parse(new InputSource(new StringReader(document)));
        return errors;
    }
}
