/* Thinlet (modernized) — Cut 3 characterization net (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Characterization net for the widget/attribute definition table and the typed accessor façade,
 * pinned before the Cut 3 typed-descriptor refactor (DECISIONS.md D57). Method names are the
 * contract sentences; there is deliberately no prose restatement of this contract elsewhere.
 */
@Tag("documents-current-behavior")
@ExtendWith(XvfbDisplayExtension.class)
class DescriptorContractTest {

    private Object parse(Thinlet thinlet, String xml) throws IOException {
        return thinlet.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    // ----- definition lookup: inheritance walk -----

    @Test
    void attributesInheritThroughTheClassnameChain() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button"); // "enabled" is declared on component: button -> label -> component
        thinlet.setBoolean(button, "enabled", false);
        assertThat(thinlet.getBoolean(button, "enabled")).isFalse();

        Object togglebutton = Thinlet.create("togglebutton"); // its own attribute row is null; walk must skip it
        thinlet.setString(togglebutton, "name", "x");
        assertThat(thinlet.getString(togglebutton, "name")).isEqualTo("x");
    }

    // ----- defaults: getters fall back to the table, setters remove at the default -----

    @Test
    void absentAttributesFallBackToTableDefaults() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        Object label = Thinlet.create("label");
        assertThat(thinlet.getBoolean(button, "enabled")).isTrue();
        assertThat(thinlet.getInteger(button, "colspan")).isEqualTo(1);
        assertThat(thinlet.getInteger(label, "mnemonic")).isEqualTo(-1);
        assertThat(thinlet.getString(label, "text")).isNull();
    }

    @Test
    void absentChoiceDefaultsToTheFirstAllowedValueIdentically() {
        Thinlet thinlet = new Thinlet();
        assertThat(thinlet.getChoice(Thinlet.create("list"), "selection")).isSameAs("single");
        assertThat(thinlet.getChoice(Thinlet.create("button"), "halign")).isSameAs("fill");
    }

    @Test
    void settingTheBooleanDefaultRemovesTheModelEntry() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        thinlet.setBoolean(button, "enabled", true); // true is the declared default
        assertThat(Thinlet.get(button, "enabled")).isNull();
        thinlet.setBoolean(button, "enabled", false);
        assertThat(Thinlet.get(button, "enabled")).isEqualTo(Boolean.FALSE);
        thinlet.setBoolean(button, "enabled", true);
        assertThat(Thinlet.get(button, "enabled")).isNull();
    }

    @Test
    void settingTheIntegerDefaultRemovesTheModelEntry() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        thinlet.setInteger(button, "colspan", 5);
        assertThat(Thinlet.get(button, "colspan")).isEqualTo(5);
        thinlet.setInteger(button, "colspan", 1); // 1 is the declared default
        assertThat(Thinlet.get(button, "colspan")).isNull();
    }

    @Test
    void stringSettersStoreEvenTheDefaultValue() {
        // The private setter's default parameter is dead 2005 code ("// use defaultvalue"):
        // strings are stored verbatim, never omitted-at-default like booleans/integers.
        Thinlet thinlet = new Thinlet();
        Object textfield = Thinlet.create("textfield"); // declared text default is ""
        thinlet.setString(textfield, "text", "");
        assertThat(Thinlet.get(textfield, "text")).isEqualTo("");
        thinlet.setString(textfield, "text", null);
        assertThat(Thinlet.get(textfield, "text")).isNull();
    }

    // ----- parse-vs-setter storage asymmetry -----

    @Test
    void parseStoresIntegersEvenAtDefault() throws IOException {
        Thinlet thinlet = new Thinlet();
        Object button = parse(thinlet, "<button colspan=\"1\"/>"); // 1 is the declared default
        assertThat(Thinlet.get(button, "colspan")).isEqualTo(1);
        thinlet.setInteger(button, "colspan", 1); // the setter then removes the same value
        assertThat(Thinlet.get(button, "colspan")).isNull();
    }

    @Test
    void parseOmitsBooleansAtTheirDefault() throws IOException {
        Thinlet thinlet = new Thinlet();
        assertThat(Thinlet.get(parse(thinlet, "<button enabled=\"true\"/>"), "enabled"))
                .isNull();
        assertThat(Thinlet.get(parse(thinlet, "<button enabled=\"false\"/>"), "enabled"))
                .isEqualTo(Boolean.FALSE);
    }

    // ----- canonicalization: the interning contract -----

    @Test
    void settersStoreUnderTheCanonicalKeyLiteral() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        thinlet.setBoolean(button, new String("enabled"), false); // de-interned caller key
        assertThat(Thinlet.get(button, "enabled")).isEqualTo(Boolean.FALSE); // identity-keyed literal read
    }

    @Test
    void createCanonicalizesTheClassnameToken() {
        assertThat(Thinlet.getClass(Thinlet.create(new String("button")))).isSameAs("button");
    }

    @Test
    void parseStoresAttributesUnderCanonicalKeys() throws IOException {
        Thinlet thinlet = new Thinlet();
        Object button = parse(thinlet, "<button enabled=\"false\"/>");
        assertThat(Thinlet.get(button, "enabled")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void choiceSetterStoresTheTableLiteralNotTheCallersString() {
        Thinlet thinlet = new Thinlet();
        Object list = Thinlet.create("list");
        thinlet.setChoice(list, "selection", new String("multiple"));
        assertThat(Thinlet.get(list, "selection")).isSameAs("multiple");
    }

    @Test
    void nullChoiceValueStoresTheDefaultRatherThanRemoving() {
        Thinlet thinlet = new Thinlet();
        Object list = Thinlet.create("list");
        thinlet.setChoice(list, "selection", null);
        assertThat(Thinlet.get(list, "selection")).isSameAs("single");
    }

    // ----- error contract: exact messages -----

    @Test
    void unknownChoiceValueMessageNamesValueAndKey() {
        Thinlet thinlet = new Thinlet();
        Object list = Thinlet.create("list");
        assertThatThrownBy(() -> thinlet.setChoice(list, "selection", "bogus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown bogus for selection");
    }

    @Test
    void wrongTypeMessageNamesTheDeclaredType() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        assertThatThrownBy(() -> thinlet.getString(button, "enabled"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boolean");
        assertThatThrownBy(() -> thinlet.setInteger(button, "text", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("string");
        assertThatThrownBy(() -> thinlet.getString(button, "action"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method"); // the 2-element method row still carries its type token
    }

    @Test
    void unknownKeyMessageNamesTheOriginalClassname() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        assertThatThrownBy(() -> thinlet.getBoolean(button, "nosuch"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown nosuch boolean for button"); // button, not the exhausted ancestor
    }

    @Test
    void parseUnknownAttributeMessageHasNullType() {
        Thinlet thinlet = new Thinlet();
        assertThatThrownBy(() -> parse(thinlet, "<button nosuch=\"1\"/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown nosuch null for button");
    }

    @Test
    void createRejectsUnknownClassnames() {
        assertThatThrownBy(() -> Thinlet.create("unknownwidget"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown unknownwidget");
    }

    @Test
    void createAcceptsNonWidgetTableRows() {
        assertThat(Thinlet.getClass(Thinlet.create("header"))).isSameAs("header");
    }

    // ----- deferred definitions: finishParse and method bindings -----

    @Test
    void labelForResolvesForwardReferencesAfterParse() throws IOException {
        Thinlet thinlet = new Thinlet();
        Object panel = parse(thinlet, "<panel><label for=\"b\" text=\"l\"/><button name=\"b\" text=\"B\"/></panel>");
        Object label = Thinlet.get(panel, ":comp");
        Object button = Thinlet.get(label, ":next");
        assertThat(Thinlet.getClass(button)).isSameAs("button");
        assertThat(Thinlet.get(label, "for")).isSameAs(button);
    }

    @Test
    void initHandlerRunsDuringParse() throws IOException {
        Thinlet thinlet = new Thinlet();
        ParseInitHandler handler = new ParseInitHandler();
        thinlet.parse(new ByteArrayInputStream("<button init=\"onInit\"/>".getBytes(StandardCharsets.UTF_8)), handler);
        assertThat(handler.inited).isTrue();
    }

    @Test
    void methodTypedBindingParameterIsRejectedWithTheTypeToken() {
        // Deliberate Cut 3 divergence (DECISIONS.md D58): a binding arg naming a
        // method-type attribute threw ArrayIndexOutOfBoundsException in 2005 (the
        // 2-element table row had no default slot); the typed row reads null and
        // the type ladder rejects it instead.
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        assertThatThrownBy(() -> thinlet.setMethod(button, "action", "doIt(this.action)", button, new ActionHandler()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method");
    }

    @Test
    void methodBindingRecordsCanonicalParameterSpecs() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        thinlet.setMethod(button, "action", "doIt(this.enabled)", button, new ActionHandler());
        Object[] method = (Object[]) Thinlet.get(button, "action");
        assertThat(method[2]).isSameAs(button); // the resolved target component
        assertThat(method[3]).isSameAs("enabled"); // canonical parameter name from the table
        assertThat(method[4]).isSameAs(Boolean.TRUE); // the table's declared default
    }

    // ----- remaining typed accessors: light round-trips -----

    @Test
    void colorRoundTripsThroughTheTypedAccessor() {
        Thinlet thinlet = new Thinlet();
        Object label = Thinlet.create("label");
        thinlet.setColor(label, "foreground", Color.red);
        assertThat(thinlet.getColor(label, "foreground")).isEqualTo(Color.red);
    }

    @Test
    void keystrokeParsesModifierPlusKeyIntoPackedLong() {
        Thinlet thinlet = new Thinlet();
        Object menuitem = Thinlet.create("menuitem");
        thinlet.setKeystroke(menuitem, "accelerator", "ctrl F5");
        long expected = ((long) InputEvent.CTRL_MASK) << 32 | KeyEvent.VK_F5;
        assertThat(Thinlet.get(menuitem, "accelerator")).isEqualTo(expected);
    }

    public static class ParseInitHandler {
        boolean inited;

        public void onInit() {
            inited = true;
        }
    }

    public static class ActionHandler {
        public void doIt(boolean enabled) {}
    }
}
