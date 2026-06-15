# Text encoding inventory

A one-time audit (2026-06-15) of every tracked text file's character encoding,
plus the policy that keeps encodings predictable going forward. Motivated by two
charset hiccups: Spotless choking on a non-UTF-8 file (DECISIONS.md D12) and the
JDK-8 default charset diverging the golden traces (D25). The recurring gap was
not "files aren't UTF-8" so much as *"there was no declared, discoverable record
of which file is which encoding"* — this document and the `.gitattributes`
annotations close that gap. See DECISIONS.md **D26**.

## Policy

- **Authored files → UTF-8** (ASCII is a subset and fine). Project-authored
  sources — POMs, `config/`, Java, Markdown, the `docs/` website, YAML, JSON —
  are UTF-8. Spotless already enforces UTF-8 (it reads UTF-8 and fails on
  non-UTF-8 bytes) for Java, project XML, and `docs/**/*.{html,css}` + `**/*.md`
  (D18); `.gitattributes` normalizes EOL to LF.
- **Vendored 2005 artifacts → byte-verbatim.** The 2005 source, the XML corpus
  (D9), and `thinlet.dtd` (D8) are preserved as imported. Most are ASCII/UTF-8.
  Any that are **not** UTF-8 are behavior-relevant (Thinlet's parser reads XML
  with the *platform-default* charset, so the raw bytes drive rendering and the
  golden traces) and are **listed below + annotated in `.gitattributes`**, never
  transcoded.
- **Adding a file:** author it UTF-8. If you must vendor a 2005 artifact in a
  legacy encoding, add it to the table below and to `.gitattributes` with `-text`
  so git never normalizes it.

## Result

Scanned all tracked files; every one is **US-ASCII or valid UTF-8 except the two
copies of the ISO-8859-2 i18n demo** (which are byte-identical to each other):

| Path | Encoding | Status |
| --- | --- | --- |
| `thinlet-core/src/test/resources/corpus/drafts/internationalization.xml` | ISO-8859-2 | Vendored 2005 artifact — preserve verbatim (`-text`) |
| `thinlet-drafts/src/main/resources/thinlet/drafts/internationalization.xml` | ISO-8859-2 | Vendored 2005 artifact — preserve verbatim (`-text`) |
| everything else tracked | US-ASCII or UTF-8 | OK |

Both i18n files declare their encoding in the XML prolog
(`<?xml version="1.0" encoding="ISO-8859-2"?>`). They are *intentionally* not
UTF-8: the golden-trace harness pins `-Dfile.encoding=UTF-8` (D25) so the parser
reads these legacy bytes as UTF-8 *consistently across JDKs* — that
deterministic (mangled) reading is the locked 2005 behavior. Transcoding them to
UTF-8 would make the parser render them "correctly" and silently change the
goldens, so it must not be done.

## How to determine a file's codeset

1. Check the table above and `.gitattributes` (the `-text`-annotated files are
   the deliberate non-UTF-8 ones).
2. For XML, read the prolog's `encoding="…"`.
3. Re-run the scan:

   ```sh
   git ls-files | while read -r f; do
     enc=$(file --mime-encoding -b "$f")
     case "$enc" in us-ascii|binary) ;; *) printf '%s\t%s\n' "$enc" "$f";; esac
   done
   ```

   `utf-8` rows are fine (valid UTF-8 with some non-ASCII bytes); anything that
   is neither `us-ascii` nor `utf-8` should appear in the table above or be
   converted to UTF-8.

## Possible follow-up (not done here)

A small CI guard that fails if a tracked file is neither ASCII/UTF-8 nor on an
allow-list (the two i18n files) would turn this one-time audit into a standing
check. Deferred — Spotless already covers the main authored file types.
