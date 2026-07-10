# Icon provenance — the vendored 2005 `/icon/*.gif` assets

The Thinlet corpus/demo/draft XML scenes reference toolbar/list/tree icons as
`icon="/icon/<name>.gif"`. Those GIFs were **not** vendored when the corpus was
imported (D9), so every reference resolved to `null` — a silent failure (the loader
`Thinlet.getIcon` swallows a missing resource with no log or throw; see
KNOWN-QUIRKS.md **Q3**), and every golden trace was captured with icons blank.

**D54** restores the authentic 2005 bytes. This file is the audit trail: it lets a
reviewer verify each opaque binary by hash rather than by eye, and records exactly
where each byte-stream came from.

## Source

- **Archive:** `github.com/nomixer/thinlet-archive`
- **Commit:** `6ad9565a554752cc3196632f903ed5acbe5d7a76`
- **Path:** `thinlet-2005-03-28/lib/` — the icons live inside the demo/draft jars
  (the scenes' 2005 runtime classpath), spread across three of them:

  | jar | sha256 |
  |-----|--------|
  | `amazon.jar` | `2893e5248004cb57983c01ee655754a4693135fb81457834d58001f8458026fd` |
  | `demo.jar`   | `7ef1a1b2e9fc33fddfa5c8923257e7e9cb880d6e9a1efda4df8ae98014a00823` |
  | `drafts.jar` | `afda0681c4dd0aff995f8c8745fbf56da26754e238a2507e43ea88a29a44ff51` |

  (`thinlet.jar` in the same `lib/` contains no icons.) Icons that appear in more
  than one jar are **byte-identical** across them — a single canonical byte-stream
  per name — so the "source jar(s)" column below lists every jar that carries it.

## Extraction

`unzip`/`jar x` preserve the stored bytes verbatim (no re-encode). The same 24
byte-verbatim files are placed at three classpath roots, each `icon/` at the root:

- `thinlet-core/src/test/resources/icon/` — drives the golden-trace harness
  (test-scope; does **not** enter the published core jar).
- `thinlet-demos/src/main/resources/icon/` — runtime `demo` + `amazon` scenes.
- `thinlet-drafts/src/main/resources/icon/` — runtime `drafts` scenes.

The three copies are byte-identical (their per-name sha256 matches the table below —
a provenance invariant checked at placement time). `*.gif` is marked `binary` in
`.gitattributes` so the bytes are never EOL-normalized or text-diffed.

## The 24 restored icons

All are 16×16. `loading.gif` is the only animated one (4 frames); the golden trace
records geometry only (16×16, frame-independent), so animation does not perturb it.

| resource | dims | frames | bytes | source jar(s) | sha256 |
|----------|------|--------|-------|---------------|--------|
| `icon/about.gif` | 16x16 | 1 | 229 | demo | `3078e1ca49725fa2b91e1951b2c820a6d6414e8a14dadf57383b6c45da9a17ba` |
| `icon/add.gif` | 16x16 | 1 | 117 | demo | `57e6a0c7cb76e34bebede133d9559571c439467b5a8f79892c9d548c4d89ba31` |
| `icon/back.gif` | 16x16 | 1 | 108 | amazon | `2a66c9a20434bc6e09042291198a99c12f7c275eab7b4fdb87ca154cd18b62f1` |
| `icon/bold.gif` | 16x16 | 1 | 99 | drafts | `bb3642a6a636468c7b3488bef8eb1d62176e303f40da2b73712d0a75ea468084` |
| `icon/copy.gif` | 16x16 | 1 | 167 | demo, drafts | `7744f66711cd51305569bb85ee254d85ba7804a424eab7128937199f58e0f5bd` |
| `icon/cut.gif` | 16x16 | 1 | 152 | demo, drafts | `60dd9e6ac633f14e79a2e689e0375de2f141833f344ce6c42e172f4e3b8261c4` |
| `icon/delete.gif` | 16x16 | 1 | 91 | demo, drafts | `de1ca9c7a1591ff75a3ed7d92ed70d825643072403e2a18f5040305fb09e2ea7` |
| `icon/find.gif` | 16x16 | 1 | 157 | amazon | `c9829c49fcf033a3f9fda65a36915fdea57290ae84dfa90d7cca3346bc3e2d83` |
| `icon/forward.gif` | 16x16 | 1 | 138 | amazon | `388b3307184e6af2d0f9a5c63d0dda15fd3e5eb2e983968c5f24aed028064fde` |
| `icon/help.gif` | 16x16 | 1 | 166 | amazon, demo | `8df90499e311d5cf442ca8f5caf19a4cd6846979e09c002f97f8904b0d0b376f` |
| `icon/italic.gif` | 16x16 | 1 | 125 | drafts | `167079b9fbd2d3fe9ddc25caf59ca7dba60b470ccaef33d9ee34c8c8d69d2421` |
| `icon/library.gif` | 16x16 | 1 | 174 | amazon, demo, drafts | `2b76b1eb865909be47e4c03e2ff8d51640fa5243f664b62a99fb883f70781731` |
| `icon/loading.gif` | 16x16 | 4 | 288 | drafts | `decfa233ff4da029b60b7df749cd0e93f3aab6c5e6dc60941a54c8e9ba2ebb35` |
| `icon/new.gif` | 16x16 | 1 | 148 | demo, drafts | `1751eb4c8322385db19d7abc04f846e9ff7ed6de83255168988011f89088c67b` |
| `icon/open.gif` | 16x16 | 1 | 152 | demo, drafts | `0498be18575c9df9254570f2bc56346332c8dfe1f1ca89c438eebd39b76c0af0` |
| `icon/pagesetup.gif` | 16x16 | 1 | 172 | demo | `be5b525592f32f8fb3fee0b4866b59e24db2ca28566b3810fc6c3ccda2745b73` |
| `icon/paste.gif` | 16x16 | 1 | 245 | demo, drafts | `bd62a7ce93f4af6b36f479a6efef202bfcca77ba896aafff690fe42ccd1279a1` |
| `icon/play.gif` | 16x16 | 1 | 128 | drafts | `a75fd2580662a5f93c0be7f72ca135ba77dd3ecc67a2e2f41edc8d7b858fdab6` |
| `icon/print.gif` | 16x16 | 1 | 227 | demo | `40c5503bcdb3cbf90dfc97142abc08baad1c10efe6c89efb21822d961769c484` |
| `icon/save.gif` | 16x16 | 1 | 187 | demo | `f6dd09981d2a80067f135a4ff2f589817d0845144f8b59ea0cc30e43a811029b` |
| `icon/saveas.gif` | 16x16 | 1 | 187 | demo | `e4cafb8035b1348706671f245f025a93fc07aa42d92112e8e4fe364ed3333d73` |
| `icon/search.gif` | 16x16 | 1 | 153 | demo | `f512d542b67a98926549b526520519eb1fc6d63b8a98c29a4c49d9da17a230f8` |
| `icon/searchnext.gif` | 16x16 | 1 | 245 | demo | `c6e2989b7d3c9b2fc836a68221efe61298c915cdc68388e06c9cfee113938f9e` |
| `icon/undo.gif` | 16x16 | 1 | 147 | demo | `71bcf18dabf679a8075812330f8e1e6186f4c8c42e1496d22f3b1cee2e777ab0` |

## Not restored — `icon/volume.gif` (a genuine 2005 gap)

`drafts/widgets.xml` (a table column header) references `icon="/icon/volume.gif"`,
but **no jar in the entire archive — any version — contains it**. `drafts.jar` (the
jar on `widgets.xml`'s 2005 classpath) never shipped it, so that column was already
a silent-null in 2005. Preserving it as icon-less is therefore the *faithful* 2005
behavior; fabricating a substitute would be an infidelity. It is intentionally left
absent, allowlisted in `thinlet.trace.CorpusResourceResolutionTest.KNOWN_ABSENT_2005`
so the resource-resolution guard still fails on any *new* unresolved reference.

## Reproduce

```sh
git clone https://github.com/nomixer/thinlet-archive
cd thinlet-archive && git checkout 6ad9565a554752cc3196632f903ed5acbe5d7a76
cd thinlet-2005-03-28/lib
for j in amazon demo drafts; do unzip -o "$j.jar" 'icon/*.gif' -d "extracted-$j"; done
# Compare sha256 of extracted icons against the table above and against the three
# vendored roots (thinlet-{core/src/test,demos/src/main,drafts/src/main}/resources/icon).
```
