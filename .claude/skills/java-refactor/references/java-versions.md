# Java version availability matrix (floors 8 → 21)

Use this to answer one question precisely: **is this syntax/API legal at the
floor?** A feature is legal at floor N only if it was added in version ≤ N. Under
`--release N`, javac enforces the API side of this too (not just syntax), so
"compiles on my JDK 21" proves nothing about a Java-8 floor.

Interim releases (9, 10, 12–16, 18–20) matter because APIs land in them even
though projects usually *floor* on an LTS (8/11/17/21). An API added in 9 is
illegal at a Java-8 floor even though you'll never floor on 9.

- [Syntax by version](#syntax-by-version)
- [Common APIs by version](#common-apis-by-version)
- [Deprecations / removals to know](#deprecations--removals-to-know)
- [Quick "is it legal at floor 8?" checklist](#quick-is-it-legal-at-floor-8-checklist)

---

## Syntax by version

| Feature | Added | Notes for refactoring |
| --- | --- | --- |
| Lambdas, method references | 8 | Fine at floor 8. Don't force them where an anonymous class reads clearer. |
| Default / static interface methods | 8 | Fine at 8. |
| Diamond operator `new ArrayList<>()` | 7 | Always legal at 8+. Free minor win. |
| try-with-resources | 7 | Legal at 8+. Can fix leak-on-exception bugs — flag when it does. |
| Multi-catch `catch (A \| B e)` | 7 | Legal at 8+. |
| Effectively-final in try-with-resources | 9 | The bare-variable form `try (existingReader) {}` is **9+**. At 8 you must declare the resource in the header with an explicit type — `try (Reader r = existingReader) {}`. Not `var`: that is Java 10 (see the row below). |
| Private interface methods | 9 | **Not at 8.** |
| `var` (local variable inference) | 10 | **Not at 8.** Legal at 11+. Don't use for fields, params, or returns ever. |
| `var` in lambda params | 11 | 11+. |
| `switch` expressions (`->`, `yield`) | 14 | **Not at 8/11.** 17+. |
| Text blocks `"""..."""` | 15 | **Not at 8/11.** 17+. |
| Pattern matching for `instanceof` | 16 | **Not at 8/11.** 17+. Kills the cast-after-instanceof boilerplate. |
| Records | 16 | **Not at 8/11.** 17+. |
| Sealed classes | 17 | 17+. |
| Pattern matching in `switch` | 21 | 21+ (previews earlier). |
| Record patterns / deconstruction | 21 | 21+. |
| Virtual threads | 21 | 21+. Not a refactor of CPU-bound UI code — it's for blocking I/O concurrency. |
| Sequenced collections (`getFirst`/`getLast`) | 21 | 21+. |

## Common APIs by version

| API | Added | Floor-8 substitute |
| --- | --- | --- |
| `Integer.valueOf`, `Boolean.valueOf` | 1.4/5 | Use these instead of `new Integer(int)` / `new Boolean(bool)` (those constructors are deprecated since 9). Legal and preferred at 8. |
| Streams, `Collectors`, `Optional` | 8 | Legal at 8. But see the hot-path/field cautions in SKILL.md. |
| `java.time` (`LocalDate`, `Instant`, `Duration`) | 8 | Legal at 8. Prefer over `Date`/`Calendar`. |
| `Map.getOrDefault`, `computeIfAbsent`, `merge` | 8 | Legal at 8. Genuine readability wins over get-null-check-put. |
| `String.join` | 8 | Legal at 8. |
| `Collection.removeIf` | 8 | Legal at 8. Cleaner than an `Iterator.remove` loop. |
| `List.of` / `Map.of` / `Set.of` | 9 | **Not at 8.** At 8 use `Collections.unmodifiableList(Arrays.asList(...))` or `Collections.emptyList()`. |
| `Arrays.asList(...)` | 1.2 | Legal at 8 (fixed-size view — not a drop-in for a growable list). |
| `Stream.takeWhile`/`dropWhile`/`iterate(seed,pred,next)` | 9 | **Not at 8.** |
| `Optional.or`, `ifPresentOrElse`, `stream` | 9 | **Not at 8.** |
| `Optional.isEmpty` | 11 | **Not at 8/9.** At 8 use `!opt.isPresent()`. |
| `String.isBlank`/`strip`/`repeat`/`lines` | 11 | **Not at 8.** At 8: `trim().isEmpty()` for blank-ish (note: `strip` is Unicode-aware, `trim` isn't — not identical). |
| `Files.readString`/`writeString` | 11 | **Not at 8.** At 8 use `new String(Files.readAllBytes(path), UTF_8)`. |
| `Collectors.toUnmodifiableList` | 10 | **Not at 8.** |
| `Stream.toList()` | 16 | **Not at 8/11.** At 8/11 use `.collect(Collectors.toList())` (note: `toList()` returns an *unmodifiable* list — not identical semantics). |
| `Collectors.teeing` | 12 | **Not at 8/11.** |
| `Objects.requireNonNullElse` | 9 | **Not at 8.** |
| HttpClient (`java.net.http`) | 11 | **Not at 8.** |

## Deprecations / removals to know

- `new Integer(...)`, `new Long(...)`, `new Boolean(...)`, etc. — deprecated in 9,
  `forRemoval` later. Replace with `valueOf`. Safe and preferred at any floor ≥ 8.
- `Thread.stop/suspend/resume`, `Runtime.runFinalization`, applets — legacy;
  don't reintroduce.
- `finalize()` — deprecated; prefer `Cleaner`/try-with-resources (Cleaner is 9+).
- Boxing constructor autoboxing identity: `Integer` values −128..127 are cached;
  `==` on boxed integers is a trap regardless of floor.

## Quick "is it legal at floor 8?" checklist

If the code uses any of these, it is **NOT** Java-8-legal — do not introduce them
at a Java-8 floor:

- `var`
- `List.of` / `Map.of` / `Set.of` / `Collectors.toUnmodifiableList`
- `String.isBlank` / `strip` / `repeat` / `lines`
- `Files.readString` / `Files.writeString`
- `Optional.isEmpty` / `Optional.or` / `Optional.ifPresentOrElse`
- `Stream.toList` / `takeWhile` / `dropWhile`
- `switch` expressions, text blocks, records, sealed classes, `instanceof`
  patterns
- `Objects.requireNonNullElse`, `HttpClient`

When unsure, assume post-8 and verify against the tables above before proposing.
