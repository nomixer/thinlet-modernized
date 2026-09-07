# The Thinlet XML dialect

**Status: characterized (2026-09-06); the parser it describes is being replaced
(D98, 2026-09-07).** What the 2005 parser in `Thinlet.java` actually accepts, and
how that differs both from XML 1.0 and from the DTD the library ships. Rationale:
`DECISIONS.md` **D96**. Documentation only — no behavior changed to produce it.

`main` (0.2.x) replaces this parser with a JAXP-backed one as a clean break: 0.2.x
reads XML, and **v0.1.x stays the line that reads the dialect below** (D98). Until
that swap lands, every rule here still describes `main`. Afterwards this file is
the specification of the frozen line and of what a 2005 document must be migrated
*from* — which is why the divergence tables are the migration list, and why the
seventeen pins that fail under a conforming parser are catalogued in D98 rather
than here.

Every claim below cites the test that pins it. Two test classes carry them:

- **`thinlet.ParserDialectTest`** — the lexical and structural rules: markup
  declarations, mixed content, entity scope, attribute lexis, the declared
  encoding, the resource bundle.
- **`thinlet.XmlDialectGrammarTest`** — the element/attribute grammar, the
  divergences from the shipped DTD, and the corpus conformance run.

Where a claim is read from the code rather than pinned by a test, it says so and
says why.

## Why this document exists

Thinlet's XML is the library's whole public authoring surface, and until now the
only written account of it was `thinlet.dtd` — a file that is **shipped in the
jar but never read by the library** (`XmlDialectGrammarTest#theShippedDtdIsNeverReadByTheLibrary`
asserts both halves: the resource is on the classpath, and no source file under
`src/main/java` so much as mentions `.dtd`). A DTD nothing loads cannot be wrong
at runtime, so nothing ever forced it to agree with the parser. It does not.

The parser is not an XML parser that happens to be small. It is a
character-at-a-time reader — `parse(InputStream, char, Object)`, one loop, no
tokenizer — that accepts a **superset** of well-formed XML in some places and a
**subset** in others, and reports most disagreements as
`IllegalArgumentException` with a one-word message. Authors who assume XML get
silent data loss; authors who assume the DTD get attributes rejected that it
declares, and no complaint about attributes it omits.

## The three artifacts, and which one is authoritative

| Artifact | What it is | Authority |
|---|---|---|
| `thinlet-core/src/main/resources/thinlet.dtd` | the 2005 file, byte-identical (D8), published in the jar | **none at runtime** — never loaded, and wrong in the ways catalogued below |
| `Thinlet.parse` + `DescriptorTable` + `Thinlet.addImpl` | the parser, the definition table, the parent/child rule | **the dialect** |
| `thinlet-core/src/test/resources/dialect/thinlet-dialect.dtd` | a faithful grammar for the dialect, generated from the code | a test resource — never shipped, never a substitute for the frozen artifact |

The dialect grammar is **generated**, not hand-written:
`XmlDialectGrammarTest.DialectGrammar.render()` walks `DescriptorTable.WIDGETS`
for element and attribute names and calls `Thinlet.addImpl` on all 35 × 35
parent/child pairs for the content models.
`XmlDialectGrammarTest#theDialectGrammarMatchesTheDefinitionTableAndTheAddRules`
regenerates it on every run and fails when the committed file and the code
disagree, so the grammar cannot drift the way the shipped DTD did.

It pins **structure and attribute names only**. The value layer — types,
enumerated tokens, defaults — stays single-homed in `DescriptorTable`, pinned by
`DescriptorContractTest` and (for the eight public choice enums) by
`PublicVocabularyContractTest`; this file does not restate it (D57).

## The corpus conforms — with exactly one exception

All 42 vendored corpus documents were validated against the generated grammar
with a validating `javax.xml.parsers.DocumentBuilder`
(`XmlDialectGrammarTest#everyCorpusDocumentIsValidAgainstTheDialectGrammar`; the
files carry no `DOCTYPE`, so the test injects one). **41 of 42 validate with no
error.**

The 42nd, `corpus/drafts/lists.xml`, is **not well-formed XML at all**, for
exactly one reason: two buttons carry raw angle brackets in attribute values —
`text="<"` and `text=">"`. Escaping those two occurrences makes the file
well-formed and valid. Thinlet reads it without complaint
(`XmlDialectGrammarTest#theOneCorpusDocumentAStandardXmlParserRejectsIsStillParsedByThinlet`).

That is the shape of the whole dialect in one file: the corpus is XML by
convention, not by enforcement, and the one place an author reached past the
convention nothing stopped them.

## Where the dialect departs from XML 1.0

### It accepts what XML rejects

| Dialect | XML 1.0 | Pinned by |
|---|---|---|
| Raw `<` and `>` inside an attribute value | `<` must be escaped | `ParserDialectTest#attributeValuesKeepRawAngleBracketsThatXmlRequiresEscaped` |
| Literal newlines and tabs survive in an attribute value | normalized to spaces | `…#attributeValuesKeepLiteralNewlinesAndTabsThatXmlNormalizesToSpaces` |
| A repeated attribute silently keeps the last value | well-formedness error | `…#aRepeatedAttributeKeepsTheLastValueInsteadOfBeingRejected` |
| Anything after the root's end tag is never read | only comments/PIs allowed | `…#contentAfterTheRootElementIsNeverRead` |
| A byte-order mark before the root is swallowed as text | (BOM is handled by the encoding layer) | `…#aByteOrderMarkBeforeTheRootIsSwallowedAsText` |
| Text content anywhere, including in elements the DTD declares `EMPTY` | content model applies | `XmlDialectGrammarTest#theShippedDtdDeclaresNoPcdataYetEveryElementToleratesText` |
| Child order and cardinality are unchecked | the content model is a grammar | `ParserDialectTest#parentChildPairsAreCheckedOneAtATimeSoOrderAndCardinalityGoUnchecked` |

### It rejects, or loses, what XML accepts

| Dialect | XML 1.0 | Pinned by |
|---|---|---|
| Entity references are decoded **only inside attribute values**; in element text `&amp;` stays six literal characters | decoded everywhere | `ParserDialectTest#entityReferencesAreDecodedInAttributeValuesButNotInElementText` |
| `&#x41;` works, `&#X41;` throws `NumberFormatException` | both are legal | `…#aHexadecimalCharacterReferenceIsRecognizedOnlyWithALowercaseX` |
| Only the five predefined entities plus numeric references; anything else throws | a DTD may declare more | `…#anEmptyOrUnknownEntityNameIsRejected` |
| A `CDATA` section is skipped as a markup declaration and its content is lost | content is text | `…#aCdataSectionIsSkippedAsAMarkupDeclarationAndItsContentIsLost` |
| A markup declaration (`<!…>`) ends at its **first** `>`, whatever the context | comments end at `-->`, quoted system ids may contain `>` | `…#aMarkupDeclarationEndsAtItsFirstAngleBracketSoTheTailBecomesElementText` |
| Text preceding a child element is discarded in every mode | mixed content is preserved | `…#textPrecedingAChildElementIsDiscardedInEveryMode` |
| End-tag names are compared character by character, so case must match and no whitespace may precede the name | same (case-sensitive), but the error is a parse error, not `IllegalArgumentException("panel")` | `…#endTagNamesAreMatchedCharacterByCharacterAndAreCaseSensitive` |
| A processing instruction is skipped only when its body parses as `key="value"` pairs — `<?php echo 1; ?>` throws | any character data is allowed | `…#aProcessingInstructionIsSkippedOnlyWhenItsBodyLooksLikeAttributes` |
| An end tag with nothing open throws `NullPointerException` | a parse error | `…#anEndTagWithNoOpenElementThrowsNullPointerException` |
| The declared `encoding` does not select the charset the stream is decoded with | the declaration governs decoding | `…#theDeclaredEncodingDoesNotSelectTheCharsetTheStreamIsDecodedWith` |

Four of these are catalogued as quirks with an undecided disposition:
`KNOWN-QUIRKS.md` **Q18** (markup declarations), **Q19** (the declared
encoding), **Q20** (the stray end tag), **Q21** (text before a child), alongside
the pre-existing **Q16** (GUI mode discards body text).

## The lexical rules, in the order the parser applies them

The loop reads one character at a time. Only the outermost loop tests for
end-of-stream; everything below is what happens once a `<` is seen.

1. **`</`** — an end tag. The parser reads exactly `tagname.length()` characters
   and compares them one by one with the open element's name, then skips
   whitespace and requires `>`. A mismatch throws
   `IllegalArgumentException(tagname)`. It never reads a name and compares: it
   compares as it reads, which is why `</ panel>` fails on the space and
   `</PANEL>` fails on the `P`.
2. **`<!`** — a markup declaration: doctype, comment, or `CDATA` alike. The
   parser reads to the **first `>`** and resumes. Everything after that `>` and
   before the real end of the declaration re-enters the document as content. A
   comment containing `>` therefore leaks its tail; if the tail contains a tag,
   that tag is *built* (`ParserDialectTest#aMarkupDeclarationTailContainingATagIsParsedAsMarkup`
   shows `<!-- a > <button/> -->` inside a `<label>` failing with
   `button add label`). In GUI mode a leaked *text* tail is invisible, because
   GUI mode discards text anyway (Q16) — which is why the corpus survives.
3. **`<?`** — a processing instruction, recognized by the tag name's first
   character. Its body goes through the same attribute reader as an element's,
   so it is skipped only if it is empty or looks like `key="value"`. On the XML
   declaration specifically (`?xml`), an `encoding` pseudo-attribute is captured
   — see below.
4. **anything else** — a start or standalone tag. The name runs to the first
   character in `">/ \t\n\r"`; there is no character-class check, so any byte
   outside that set of six is part of the name. Attributes follow, each
   requiring preceding whitespace, a `=`, and a `"`- or `'`-quoted value. Inside
   the value only `&` is special.
5. **anything not after a `<`** — text. Whitespace runs collapse to one space,
   and the trailing space is trimmed at the end tag
   (`ParserDialectTest#whitespaceRunsInElementTextCollapseToOneSpaceAndTheTrailingOneIsTrimmed`).
   The text buffer is cleared at every **start** tag, so only the run
   immediately before an end tag can survive — and in GUI mode not even that.

### The comment branch inside the tag-name loop is unreachable

`parse` contains a second, more careful comment reader: inside the tag-name loop,
if the accumulated name reaches `!--` it scans forward for `-->`. It can never
run. The `<!` test at step 2 is an `else if` that precedes the start-tag branch,
so the first character appended to the name buffer is never `!`, so
`text.charAt(0) == '!'` is never true. D95 already observed the consequence from
the other end — seven of `parse`'s mutants report `NO_COVERAGE`, "not reached
rather than a failure"; this is the reason. Read from the code, not test-pinned:
unreachable code cannot be pinned by a test.

### Truncated input does not fail — it hangs

`BufferedReader.read()` returns `-1` at end of stream, and `(char) -1` is
`\uFFFF`, which matches none of the terminator sets. Five loops therefore spin
forever on a truncated document rather than reporting an error:

| Loop | Truncation that reaches it |
|---|---|
| `while ((c = reader.read()) != '>')` | inside a markup declaration |
| the tag-name loop | inside a tag name |
| the attribute-key loop | inside an attribute name |
| `while (quote != (c = reader.read()))` | inside an attribute value |
| `while (';' != (c = reader.read()))` | inside an entity reference |

Four of the five append to a `StringBuffer` as they go, so they exhaust the heap
rather than merely burning a core; the markup-declaration loop has an empty body
and simply spins. Truncation anywhere else — between tags, or in text — reaches
the outer loop's `c != -1` test and ends in `IllegalArgumentException`.

**Deliberately not pinned by a test.** A test would either hang the suite or
`OutOfMemoryError` the shared Surefire JVM; there is no bounded input that
demonstrates the behavior. This is read from the code. It is recorded in
`KNOWN-QUIRKS.md` under "Triaged for Enhanced Thinlet (not behavior-locked)".

## Encoding

Two separate mechanisms, neither of which is "decode the stream as declared".

**The stream** is read through `new InputStreamReader(inputstream)` — no charset
argument, so the **platform default** decodes every byte, whatever the XML
declaration says. `ParserDialectTest#theDeclaredEncodingDoesNotSelectTheCharsetTheStreamIsDecodedWith`
pins this without depending on what that default is: an undeclared attribute
names itself in the exception message, and the same bytes produce the same
message with or without the declaration, while the same characters in a different
encoding produce a different one.

**String-typed attribute values** are then re-encoded, but only when an
`encoding` was declared and only for the `string` type:

```java
value = new String(value.getBytes(), 0, value.length(), encoding);
```

`value.getBytes()` uses the platform default; `value.length()` counts
**characters**. For any value whose default-charset encoding is longer than its
character count, every byte past the first of each multi-byte character is
dropped before the reinterpretation. Declaring `encoding="ISO-8859-1"` on a
UTF-8 platform turns `é` into `Ã` — a single character, not the two the
mis-reinterpretation alone would give
(`ParserDialectTest#aDeclaredEncodingReEncodesStringAttributesUsingTheCharacterCountAsAByteCount`,
skipped where the platform default is single-byte).

**The corpus trips this in two documents, and their goldens have recorded the
corruption since Phase 1.** 32 of the 42 files declare an encoding — 21
`ISO-8859-1`, 10 `UTF-8` and one `ISO-8859-2` — and the other 10 declare none.

| Document | What it carries | What the golden records |
|---|---|---|
| `drafts/internationalization.xml` | a Hungarian pangram in ISO-8859-2 bytes, declared `encoding="ISO-8859-2"` | `drawString` of `ďż˝RVďż˝ZTďż˝Rďż˝ …ďż˝r` — mangled *and* truncated |
| `drafts/widgets.xml` | `text="Label &#169;"`, declared `encoding="ISO-8859-1"` | `drawString` of `Label Â` |

The first is the plain case: the bytes are not ASCII, the platform default (UTF-8
under D25's pinned test charset) cannot decode them, each character becomes
U+FFFD, and the re-encode then reinterprets those replacement bytes and truncates
at the character count.

The second is the instructive one. `drafts/widgets.xml` **is** pure ASCII as
bytes — and the defect fires anyway, because `&#169;` is decoded to `©` *before*
the re-encode runs. A character reference is enough, so "the source is ASCII"
never implied the round trip was the identity. This document once claimed the
corpus never tripped the defect on exactly that reasoning; it was wrong on both
halves (`DECISIONS.md` **D98**).

An unsupported `encoding` value prints the `UnsupportedEncodingException`
message on `System.err` and leaves `encoding` null, so the parse continues with
no re-encoding at all
(`ParserDialectTest#anUnsupportedDeclaredEncodingIsReportedAndThenIgnored` pins
the "continues" half; the `System.err` write is read from the code).

## The resource bundle is part of the dialect

When a `ResourceBundle` is set, **any** attribute value beginning `i18n.` is
replaced by the bundle's value for the remainder of the key — not just a label's
`text`, and with no way to escape a literal that happens to start that way. With
no bundle set the literal survives; with a bundle set and the key absent,
`MissingResourceException` aborts the parse
(`ParserDialectTest#anAttributeValueStartingWithI18nIsResolvedThroughTheResourceBundle`,
`#anI18nValueIsLeftLiteralWhenNoBundleIsSet`, `#anI18nKeyMissingFromTheBundleAbortsTheParse`).

## The structural grammar

35 element names, exactly the ones in `DescriptorTable.WIDGETS` — and exactly the
ones the shipped DTD declares
(`XmlDialectGrammarTest#theShippedDtdDeclaresTheSameThirtyFiveElementsAsTheDefinitionTable`).

The content model is **not a grammar over children**. `addImpl` is called once
per parent/child pair as the pair is met, and it decides on that pair alone:

- the container set — `panel`, `desktop`, `dialog`, `splitpane`, `tab` — accepts
  any element whose class descends from `component`, except `popupmenu`;
- every element that itself descends from `component` accepts one `popupmenu`,
  stored under the `popupmenu` key rather than in the child list, so a second one
  overwrites the first;
- the remaining pairs are a fixed list: `combobox`/`choice`,
  `tabbedpane`/`tab`, `list`/`item`, `table`/`row`, `table`/`header`,
  `header`/`column`, `row`/`cell`, `tree`|`node`/`node`, `menubar`/`menu`, and
  `menu`|`popupmenu`/{`menu`, `menuitem`, `checkboxmenuitem`, `separator`};
- everything else throws `IllegalArgumentException("<child> add <parent>")`.

Because the check is pairwise, **order and cardinality are unenforced**: a
`<row>` may precede the `<header>`, a `<tab>` may hold any number of children,
and a second `<popupmenu>` silently replaces the first. That is why the generated
grammar's content models are mixed and unordered — `(#PCDATA | a | b)*` — rather
than the sequences the shipped DTD declares.

Two consequences of the rule are worth naming because they surprise:

- **`tab` is the one container that rejects a `popupmenu`.** `tab` descends from
  `choice`, not from `component`, so the popupmenu arm — which requires a
  `component` parent — never fires for it
  (`ParserDialectTest#aTabIsTheOneContainerThatRejectsAPopupmenu`).
- **A `popupmenu` may nest inside another `popupmenu`**, because `popupmenu` is
  itself a `component` (`…#aPopupmenuMayNestInsideAnotherPopupmenu`). The shipped
  DTD forbids it.

And `component`, the abstract base of the class table, is a perfectly ordinary
element name: `create("component")` succeeds and `<panel><component/></panel>`
builds a child (`…#theAbstractComponentElementCanBeInstantiatedFromXml`). The
shipped DTD declares it `EMPTY` and leaves it out of the entity that lists a
container's legal children.

## Where the shipped DTD is wrong

Four defects, each pinned:

1. **`<!ENTITY gt "&#61;">` — decimal 61 is `=`, not `>`.** A document that took
   the DTD at its word would read `&gt;` as an equals sign. The parser is right
   and the DTD is wrong: `&gt;` decodes to `>`
   (`XmlDialectGrammarTest#theShippedDtdDeclaresGtAsAnEqualsSignWhileTheParserDecodesItAsAGreaterThan`).
   The declaration is doubly invalid — XML 1.0 requires `lt` and `amp`, if
   declared at all, to be declared with doubly-escaped replacement text, which is
   why `xmllint` warns about three of the five. The generated dialect grammar
   declares none of them: they are predefined.
2. **No `#PCDATA` anywhere**, so body text was never valid by the DTD — while the
   parser accepts it silently in every element and discards it in GUI mode
   (`…#theShippedDtdDeclaresNoPcdataYetEveryElementToleratesText`, Q16).
3. **`desktop` carries the panel attribute list.** The DTD gives `desktop` all
   ten of `columns`, `top`, `left`, `bottom`, `right`, `gap`, `text`, `icon`,
   `border`, `scrollable`; the definition table makes `desktop` a direct child of
   `component`, so the parser rejects every one of them —
   `<desktop columns="2"/>` throws `unknown columns null for desktop`.
4. **`button` is missing `for`.** The DTD's `button` attribute list omits the
   `for` it inherits from `label`; the parser accepts it.

Points 3 and 4 are the **complete** set of attribute-name disagreements between
the DTD and the definition table — the comparison is exhaustive over all 35
elements, not a spot check
(`XmlDialectGrammarTest#theShippedDtdAndTheDefinitionTableDisagreeAboutExactlyTwoElementsAttributes`).
A fifth, harmless, authoring error: `dialog` declares `text` and `icon` twice,
once through `%panelattlist;` and once in its own list, and it is the only
element that declares any attribute twice
(`…#theShippedDtdDeclaresDialogsTextAndIconTwice`).

**The DTD is not being corrected.** It is a verbatim 2005 artifact (D8);
changing it would be a behavior decision needing its own D-entry, and it has no
runtime effect to change. The divergences are findings, not a bug list.

## What this document does not cover

- **Attribute value types and tokens** — `DescriptorTable` and
  `DescriptorContractTest` / `PublicVocabularyContractTest` (D57, D74).
- **`parse(String path)`'s resource resolution** — how a path becomes a stream,
  including the Q1 null-source fix (D71).
- **Event and method binding** (`action=`, `init=`, the `for` reference) —
  resolved after the document is read, in `finishParse` / `getMethod`.
- **The AWT input surface** — `INPUT-SURFACE.md` in this directory.
