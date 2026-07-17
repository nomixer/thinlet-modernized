# Vocabulary inventory — the interned-string surfaces and where each becomes typed

> **Source-derived research, not a behavior contract** (decision record:
> `DECISIONS.md` **D67**). Read directly from
> `thinlet-core/src/main/java/thinlet/Thinlet.java`, `Renderer.java`, and
> `DescriptorTable.java`; counts come from a scripted simple-argument match
> over `is(…, "literal")` call sites and are **lower bounds** (nested-call
> first arguments are not matched). The *meanings* of the tokens live
> in-source at their consumers (D57): the widget-model schema above
> `createImpl`, the part tokens above `processScroll`, the invalidation modes
> above `update`, the paint-state chars in `IconTextSpec`, the event-name
> rows in `DescriptorTable`.

Three purposes:

1. The candidate list (with sizes) for any constants/enum/typing pass.
2. The collision table — words whose meanings must not be merged by a naive
   word→constant mapping.
3. A per-vocabulary recommendation for *which cut* should absorb it, argued
   from which cut rewrites that vocabulary's consumers anyway.

## Why the internal vocabularies are not converted (recorded reasoning)

The two rows marked 3c are published (D74): the choice values as public enums,
the event names as `EventNames` constants — both DTD-anchored, so fork-proof.
The internal vocabularies stay interned Strings:

- Every comparison already routes through the `is()` identity chokepoint with
  the strict-intern tripwire (D43), so named constants add no safety the net
  can measure — D58 recorded exactly this ("zero net-strength gain") when it
  kept the type tokens as interned Strings.
- Cut 5's success criterion *deletes* most of these vocabularies (typed
  `Widget` replaces the `Object[]` model and the interned-`String` `==`
  contract, `project-docs/PHASE-3-GOALS.md`); internal constants introduced now
  are scaffolding that cut demolishes, paying the D52-style mechanical-audit
  cost twice. (The published value enums are not that scaffolding: a later
  typed API consumes them — D74.)
- Anything `public` is de-facto frozen API the moment it ships (D43, applied
  deliberately by D69), so public names beyond the DTD-frozen 3c rows wait for
  fork mapping + Cut 5 shapes.

## The vocabularies

| # | Vocabulary | Size / sites (lower bound) | Absorb at | Shape recommendation (reasoning) |
|---|---|---|---|---|
| 1 | Widget classnames | 35 descriptor names (33 concrete tags + abstract `component`/`choice`); ≥281 `is(…, "<classname>")` sites | **Cut 5** | Become *types* (the typed `Widget` hierarchy), not String constants — constants would be scaffolding Cut 5 demolishes. A 3c compatibility layer keeping string `create` can expose a public constants holder mirroring the concrete tags. |
| 2 | Attribute keys | 109 `AttributeDescriptor` rows | **Cut 5** | Typed per-widget accessors; string keys survive only in the compatibility layer. |
| 3 | Choice values | 8 sets: `alignment` (3), `halign`/`valign` (4 each), `orientation` (2), `placement` (5), `selection` (3), button `type` (4), column `sort` (3) | **3c — shipped (D74)** | Published as the public enums `Alignment`/`HorizontalAlignment`/`VerticalAlignment`/`Orientation`/`TabPlacement`/`SelectionMode`/`ButtonType`/`SortOrder`, each carrying its `KEY` + DTD tokens; table-anchored by `PublicVocabularyContractTest`. The DTD strings remain the wire format — they are the *user's* vocabulary, frozen by the byte-identical DTD (D8). |
| 4 | Scroll/spin/combo/desktop part tokens | 14 tokens (`up down left right uptrack downtrack lefttrack righttrack hknob vknob corner icon text` + desktop's `modal`) | **Cut 6** | Internal-only; hit-testing (`findScroll`/`findComponent`) owns them — event/input territory, not Cut 5. Internal enum when the mouse-state fields are typed. |
| 5 | Dialog part tokens | 10 (`"header"`, `":close"` (D73) + compass `":n" ":s" ":e" ":w" ":nw" ":ne" ":sw" ":se"`) | **Cut 6** | Same owner as #4. The `:` prefix here marks part tokens, *not* model keys — any typing pass must not merge them with #8. |
| 6 | `update` invalidation modes | 4 (`validate paint layout parent`); sourced from the `AttributeDescriptor.invalidate` column (34/25/7/5 rows), 3 literal call sites | **Cut 4/5** | Intent-named methods (or an enum) on whatever owns invalidation after the layout cut; the `invalidate` column then becomes typed with it. |
| 7 | Attribute-type tokens | 12 (`string integer boolean choice icon method component property font color keystroke bean`) | **Cut 5** | The D58-deferred enum; it rewrites three verbatim `is()` ladders, so it lands only when those ladders are being rewritten anyway. |
| 8 | Reserved `:`-model keys | ~20 live keys (census above `createImpl`) | **Cut 5** | Disappear into typed `Widget` fields (`:comp`/`:next` → child list; `:port`/`:view`/`:widths`/`:offset` → a layout-state object; `:bind` → listener map; `:lead`/`:anchor` → selection state). |
| 9 | Event names | 11 distinct (`init focusgained focuslost action insert remove caret perform expand collapse menushown` — the names of the 18 `"method"`-typed table rows, D58; `init` bypasses `invoke`) | **3c — shipped (D74)** | Published as the `EventNames` constants, set-anchored to the method-typed rows by `PublicVocabularyContractTest`. A typed listener API remains a separate future decision. |
| 10 | Method-binding argument-target tags | 3 (`thinlet constant item` + component refs) | **Cut 6** | Internal to the `getMethod`/`invoke` binding interpreter. |
| 11 | Paint-state char mode | 12 chars (glossed in `IconTextSpec`) | **Cut 5+** | D56 already recorded the deferral: an enum rewrites the verbatim `switch` bodies; revisit when those bodies are rewritten. |

## Collision table (same word, unrelated meanings)

A word→constant mapping that ignores these rows would silently merge
unrelated concepts. From D67's decode:

| Word | Distinct meanings |
|---|---|
| `block` | ① the font-height layout-unit field (assigned in `setFont`) ② the slider DTD attribute (page step, default 25) ③ `getPreviousFocusable`'s recursion-boundary parameter |
| `choice` | ① abstract widget classname (base of `tab item column cell node menu menuitem`) ② attribute-type token |
| `bean` | ① widget classname ② attribute name holding the AWT `Component` ③ attribute-type token |
| `left right up down` | ① scrollbar arrow part tokens ② spinbox step buttons (`up`/`down`) ③ combobox drop button (`down`) ④ `placement`/`alignment`/`halign` choice values ⑤ panel/dialog integer inset attributes (`top`/`left`/`bottom`/`right`) ⑥ `paintRect` edge-boolean parameter names |
| `horizontal vertical` | ① `orientation` choice values ② `":horizontal"`/`":vertical"` scrollbar-Rectangle model keys ③ `repaint` part tokens ("repaint that bar + port") ④ `boolean horizontal` paint parameter (gradient axis) |
| `text icon` | ① ubiquitous attribute names ② part tokens (spinbox field region; combobox icon glyph) ③ `":text"` reserved key; `icon` is also an attribute-type token |
| `combobox` | ① widget classname ② `repaint` dispatch tag ③ the model key linking a `:combolist` back to its owner |
| `modal` | ① dialog boolean attribute ② desktop part token ("a modal dialog blocks this click") |
| `parent` | ① `update` mode value ② `":parent"` tree-link key ③ `WidgetDescriptor.parent` inheritance edge |
| `paint layout validate` | ① `update` mode values ② the operations themselves |

## Count methodology

Scripted match (2026-07-15, D67): `is\(<simple-arg>, "literal"\)` over
`Thinlet.java` + `Renderer.java`, where `<simple-arg>` is an identifier or a
single nested no-comma call; classname subtotal ≥281 across 35 distinct
names. `:`-key census by quoted-literal grep over the same files plus
`DescriptorTable.java`. Both under-count literals reached through locals or
multi-level calls — treat every number as a floor, and re-run the match
rather than trusting these snapshots after any Cut 4+ code motion.
