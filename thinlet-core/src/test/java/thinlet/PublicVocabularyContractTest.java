/* Thinlet (modernized) — 3c public-vocabulary contract net (test scope). */
package thinlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import thinlet.trace.XvfbDisplayExtension;

/**
 * Contract net for the 3c public vocabulary (DECISIONS.md D74): every published token is
 * anchored to the live definition table, so drift on either side fails here. Deliberate
 * new-API contract — not a documents-current-behavior pin.
 */
@ExtendWith(XvfbDisplayExtension.class)
class PublicVocabularyContractTest {

    // KEY -> the enum's tokens in declaration order, one entry per published choice enum.
    private static final Map<String, List<String>> TOKENS = tokenTable();

    private static Map<String, List<String>> tokenTable() {
        Map<String, List<String>> tokens = new LinkedHashMap<String, List<String>>();
        tokens.put(Alignment.KEY, tokensOf(Alignment.values()));
        tokens.put(HorizontalAlignment.KEY, tokensOf(HorizontalAlignment.values()));
        tokens.put(VerticalAlignment.KEY, tokensOf(VerticalAlignment.values()));
        tokens.put(Orientation.KEY, tokensOf(Orientation.values()));
        tokens.put(TabPlacement.KEY, tokensOf(TabPlacement.values()));
        tokens.put(SelectionMode.KEY, tokensOf(SelectionMode.values()));
        tokens.put(ButtonType.KEY, tokensOf(ButtonType.values()));
        tokens.put(SortOrder.KEY, tokensOf(SortOrder.values()));
        return tokens;
    }

    private static List<String> tokensOf(Enum<?>[] constants) {
        List<String> tokens = new ArrayList<String>();
        for (Enum<?> constant : constants) {
            try {
                tokens.add((String) constant.getClass().getMethod("token").invoke(constant));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        return tokens;
    }

    // ----- the table anchor: enums and table can never drift apart -----

    @Test
    void everyChoiceRowsAllowedValuesAreExactlyTheMatchingEnumsTokens() {
        Set<String> matchedKeys = new LinkedHashSet<String>();
        for (WidgetDescriptor widget : DescriptorTable.WIDGETS) {
            if (widget.attributes == null) {
                continue;
            }
            for (AttributeDescriptor attribute : widget.attributes) {
                if (!"choice".equals(attribute.type)) {
                    continue;
                }
                List<String> tokens = TOKENS.get(attribute.name);
                assertThat(tokens)
                        .as("a published enum for choice attribute %s on %s", attribute.name, widget.name)
                        .isNotNull();
                assertThat((String[]) attribute.defaultValue)
                        .as("%s on %s", attribute.name, widget.name)
                        .containsExactlyInAnyOrderElementsOf(tokens);
                matchedKeys.add(attribute.name);
            }
        }
        assertThat(matchedKeys).containsExactlyInAnyOrderElementsOf(TOKENS.keySet());
    }

    @Test
    void enumDeclarationOrderFollowsTheTablesFirstRowForItsKey() {
        // The first-declared row per key is the canonical order; button's later
        // alignment row re-orders only to move the default and is set-checked above.
        Map<String, List<String>> firstRows = new LinkedHashMap<String, List<String>>();
        for (WidgetDescriptor widget : DescriptorTable.WIDGETS) {
            if (widget.attributes == null) {
                continue;
            }
            for (AttributeDescriptor attribute : widget.attributes) {
                if ("choice".equals(attribute.type) && !firstRows.containsKey(attribute.name)) {
                    List<String> values = new ArrayList<String>();
                    for (String value : (String[]) attribute.defaultValue) {
                        values.add(value);
                    }
                    firstRows.put(attribute.name, values);
                }
            }
        }
        for (Map.Entry<String, List<String>> vocabulary : TOKENS.entrySet()) {
            assertThat(vocabulary.getValue())
                    .as("declaration order for %s", vocabulary.getKey())
                    .containsExactlyElementsOf(firstRows.get(vocabulary.getKey()));
        }
    }

    // ----- the accessor round-trip: the tokens work through the real public API -----

    @Test
    void everyEnumTokenRoundTripsThroughTheRealChoiceAccessors() {
        Thinlet thinlet = new Thinlet();
        Map<String, String> widgetForKey = new LinkedHashMap<String, String>();
        widgetForKey.put(Alignment.KEY, "label");
        widgetForKey.put(HorizontalAlignment.KEY, "button");
        widgetForKey.put(VerticalAlignment.KEY, "button");
        widgetForKey.put(Orientation.KEY, "progressbar");
        widgetForKey.put(TabPlacement.KEY, "tabbedpane");
        widgetForKey.put(SelectionMode.KEY, "list");
        widgetForKey.put(ButtonType.KEY, "button");
        widgetForKey.put(SortOrder.KEY, "column");
        for (Map.Entry<String, String> vocabulary : widgetForKey.entrySet()) {
            Object widget = Thinlet.create(vocabulary.getValue());
            for (String token : TOKENS.get(vocabulary.getKey())) {
                thinlet.setChoice(widget, vocabulary.getKey(), token);
                assertThat(thinlet.getChoice(widget, vocabulary.getKey()))
                        .as("%s on %s", vocabulary.getKey(), vocabulary.getValue())
                        .isEqualTo(token);
            }
        }
    }

    @Test
    void fromTokenMapsEveryPublishedTokenBackToItsConstant() {
        for (Alignment value : Alignment.values()) {
            assertThat(Alignment.fromToken(value.token())).isSameAs(value);
        }
        for (HorizontalAlignment value : HorizontalAlignment.values()) {
            assertThat(HorizontalAlignment.fromToken(value.token())).isSameAs(value);
        }
        for (VerticalAlignment value : VerticalAlignment.values()) {
            assertThat(VerticalAlignment.fromToken(value.token())).isSameAs(value);
        }
        for (Orientation value : Orientation.values()) {
            assertThat(Orientation.fromToken(value.token())).isSameAs(value);
        }
        for (TabPlacement value : TabPlacement.values()) {
            assertThat(TabPlacement.fromToken(value.token())).isSameAs(value);
        }
        for (SelectionMode value : SelectionMode.values()) {
            assertThat(SelectionMode.fromToken(value.token())).isSameAs(value);
        }
        for (ButtonType value : ButtonType.values()) {
            assertThat(ButtonType.fromToken(value.token())).isSameAs(value);
        }
        for (SortOrder value : SortOrder.values()) {
            assertThat(SortOrder.fromToken(value.token())).isSameAs(value);
        }
    }

    @Test
    void fromTokenRejectsUnknownTokensWithTheChoiceSetterMessageShape() {
        assertRejects(() -> Alignment.fromToken("bogus"), "unknown bogus for alignment");
        assertRejects(() -> HorizontalAlignment.fromToken("bogus"), "unknown bogus for halign");
        assertRejects(() -> VerticalAlignment.fromToken("bogus"), "unknown bogus for valign");
        assertRejects(() -> Orientation.fromToken("bogus"), "unknown bogus for orientation");
        assertRejects(() -> TabPlacement.fromToken("bogus"), "unknown bogus for placement");
        assertRejects(() -> SelectionMode.fromToken("bogus"), "unknown bogus for selection");
        assertRejects(() -> ButtonType.fromToken("bogus"), "unknown bogus for type");
        assertRejects(() -> SortOrder.fromToken("bogus"), "unknown bogus for sort");
        assertRejects(() -> Alignment.fromToken(null), "unknown null for alignment");
    }

    private static void assertRejects(ThrowableAssert.ThrowingCallable call, String message) {
        assertThatThrownBy(call).isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    // ----- the event vocabulary -----

    @Test
    void theEventNameConstantsAreExactlyTheTablesMethodTypedRowNames() throws IllegalAccessException {
        Set<String> tableNames = new LinkedHashSet<String>();
        for (WidgetDescriptor widget : DescriptorTable.WIDGETS) {
            if (widget.attributes == null) {
                continue;
            }
            for (AttributeDescriptor attribute : widget.attributes) {
                if ("method".equals(attribute.type)) {
                    tableNames.add(attribute.name);
                }
            }
        }
        Set<String> published = new LinkedHashSet<String>();
        for (Field field : EventNames.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                published.add((String) field.get(null));
            }
        }
        assertThat(published).hasSize(11).containsExactlyInAnyOrderElementsOf(tableNames);
    }

    @Test
    void eventNameConstantsBindThroughTheRealMethodAccessor() {
        Thinlet thinlet = new Thinlet();
        Object button = Thinlet.create("button");
        thinlet.setMethod(button, EventNames.ACTION, "onAction", button, new ActionHandler());
        assertThat(Thinlet.get(button, "action")).isNotNull();
    }

    public static class ActionHandler {
        public void onAction() {}
    }
}
