---
name: java-refactor
description: >-
  Expert guidance for refactoring Java so it is idiomatic at a specific language
  floor (8, 11, 17, or 21) — with honest payoff judgment (big win vs. minor vs.
  cosmetic vs. false gain / regression) and strict behavior-preservation. Use
  this whenever you are refactoring, modernizing, cleaning up, de-crufting, or
  "making more idiomatic" any Java code; whenever you must judge whether a
  proposed Java change is worth it, idiomatic, or safe at the target version; and
  especially for legacy, single-class, or hot-path code where an obvious-looking
  modernization can quietly regress performance or change behavior. Reach for it
  even when the user says "tidy up", "is this good Java?", "should I use streams
  here", "extract this method", or "bring this up to Java N" without naming a
  skill.
---

# Java refactoring, pinned to a version floor

Refactoring is **changing the shape of code without changing what it observably
does**, targeted at the exact Java version the project compiles against. Two
disciplines run through everything here:

1. **Idiomatic at the floor** — write what a fluent Java engineer would write *at
   the project's compile target*, and never reach for syntax or APIs newer than
   that floor. Idiomatic-for-21 code that won't compile at a Java-8 floor is not
   an improvement; it is a build break.
2. **Honest payoff** — most "modernizations" are cosmetic, some are real wins, a
   few are outright regressions dressed up as cleanups. Say which is which
   plainly. A refactor that trades a real property (speed, allocations, clarity,
   a load-bearing contract) for style is a *loss*, and naming it as such is the
   single most valuable thing this skill does.

Legacy code is the hard case and the main target: old code often looks ugly
*because it encodes something* — an interning contract, an allocation budget, a
serialization shape. Assume the ugliness is load-bearing until you've proven it
isn't.

---

## Step 0 — Always establish the floor first

Before proposing anything, determine the version the code must compile at. The
floor is a **hard ceiling** on syntax and APIs, not a suggestion.

Look, in order:

- **Maven:** `maven.compiler.release`, or `<source>`/`<target>`, or a
  `<release>` argument to `maven-compiler-plugin` (`pom.xml`).
- **Gradle:** `sourceCompatibility` / `targetCompatibility`, or
  `java { toolchain { languageVersion … } }` / `options.release`.
- **Plain javac / CI:** a `--release N` flag anywhere in the build.
- **No build file** (a loose snippet): **ask** what floor to target. Do not guess
  from the code's current style — old-looking code is often *required* to stay old.

`--release N` (or `release N`) is stricter than `-source/-target`: it also blocks
APIs added after N, so it is the truth. If the project uses it, trust it over
everything else.

Then discover the project's **refactor guardrails** — how it is built and how
regressions are caught — so your "behavior-preserving" claim is actually
checkable:

- the build/verify command (e.g. `./mvnw -B verify`, `./gradlew check`);
- the regression net (unit tests, golden/snapshot tests, characterization suites);
- any **behavior-change protocol** the repo enforces (a decision log, a
  disposition process, pinned "this is the contract" tests).

**In this repository, read `references/thinlet.md` for the concrete floor,
build command, regression net, behavior-change protocol, and the specific
load-bearing traps in `Thinlet.java`.** It points at the repo's own docs rather
than restating them, so it stays current.

---

## Idiomatic at each floor

Operate **only at the detected floor**. Use that floor's idioms; do not annotate
proposals with "this gets nicer at 17/21" — forward-looking noise is not the job,
and the project bumps the floor deliberately, on its own schedule. When the floor
moves, retarget.

You need per-floor knowledge to serve whichever floor a project is on. The full
availability matrix — what each version *added* and, just as importantly, what is
**not yet available** below it — lives in `references/java-versions.md`. Read it
whenever you are unsure whether an API or syntax is legal at the floor. The
headline traps:

- **Java 8 floor:** no `var`, no `List.of`/`Map.of` (Java 9), no
  `Files.readString`/`Optional.isEmpty` (11), no records/`switch` expressions
  (17), no text blocks (15/17), no pattern matching. You *do* have lambdas,
  method references, streams, `Optional`, `default` methods, try-with-resources,
  the diamond operator, `StringBuilder`, and `java.time`. Reaching one API past
  the floor is the most common legacy-refactor mistake — the code compiles on the
  author's JDK 21 and breaks under `--release 8`.
- **Java 11 floor:** adds `var` (local), `List.of`, `String.isBlank`/`strip`,
  `Files.readString`, `Optional.isEmpty`, the HTTP client. Still no records,
  sealed types, or `switch` expressions.
- **Java 17 floor:** adds records, sealed classes, `switch` expressions +
  pattern matching for `instanceof`, text blocks. This is where a lot of the
  boilerplate-killing idioms finally arrive.
- **Java 21 floor:** adds record patterns, pattern matching in `switch`, virtual
  threads, sequenced collections.

When in doubt about legality at the floor, check the reference — do not assume.

---

## Payoff tiers — grade every candidate

Grade each opportunity into one of four tiers and lead your proposal with the
grade. This is what separates useful refactoring from churn.

**Big win** — materially improves correctness-safety, readability, or
performance, at low risk:
- Extracting a named, intent-revealing method from a long span of a 6000-line
  method (the reader gains a name; behavior is untouched).
- try-with-resources replacing a hand-rolled `finally { close(); }` that leaks on
  an exception path (this can even fix a latent bug — flag it if so).
- Collapsing genuinely duplicated logic into one helper.
- Replacing a raw type with a generic one where it removes unchecked casts.

**Real but minor** — a small, honest improvement; batch these, don't PR them one
at a time:
- Diamond operator on the right-hand side of a `new HashMap<...>()`.
- `StringBuilder` for a loop that concatenates in `+=`.
- Multi-catch where two `catch` blocks are identical.

**Cosmetic only** — a wash; do it only when already touching the lines, never as
its own change:
- Lambda vs. anonymous class where the anonymous class is fine and clear.
- Reordering modifiers, renaming a local for taste.
- Enhanced-for vs. indexed-for when the index isn't used *and nothing else needs
  the index shape*.

**False gain / regression** — looks modern, actually costs something. Call these
out and recommend **against**:
- **Streams on a hot path.** A `stream().filter().map().collect()` in a paint /
  layout / per-frame / tight-inner-loop allocates lambdas, an iterator, and a
  collection where the old indexed `for` allocated nothing. In UI rendering or
  any per-event code this is a real regression. The plain loop is the idiom there.
- **`Optional` as a field or in a hot getter.** `Optional` is designed for return
  values at API boundaries; as a field it adds an allocation and a dereference
  per access and doesn't serialize. Don't.
- **Over-abstraction.** Introducing an interface + factory for a single
  implementation, or a "strategy" for two branches, adds indirection a reader
  must now chase for no payoff.
- **"Map-ifying" a hand-rolled structure** whose representation is load-bearing
  (see behavior preservation, below).

When a payoff claim is genuinely contested — "is a stream actually slower here?",
"was this replaced by a faster API in a recent JDK?" — that is exactly when to
**research** (below), rather than asserting.

---

## Behavior preservation — the line between a refactor and a change

A refactor must be **observably null**. If a proposed cleanup alters any of the
following, it is a *behavior change*, not a refactor — stop, say so, and route it
through the project's change process (its disposition / decision log / review
gate) instead of smuggling it in under "tidy-up":

- **`==` vs `.equals()`** — identity comparison is sometimes deliberate
  (interned tokens, sentinel identity). Switching it changes semantics.
- **Null handling** — where nulls flow, whether a path NPEs, what a method
  returns for absent input.
- **Exception type or timing** — which exception is thrown, and *when* (eager vs.
  lazy), including short-circuit evaluation order (`&&`/`||`) and side-effect
  ordering.
- **Iteration / encounter order** — `HashMap` → `LinkedHashMap` and back,
  stream ordering, sort stability.
- **Numeric behavior** — integer vs. floating division, widening/narrowing,
  overflow, `float` vs. `double`, autoboxing identity (`Integer` cache).
- **Serialization shape** — field layout, `serialVersionUID`, what
  `Serializable` writes. Refactoring fields on a serialized class breaks
  compatibility.
- **Reflection-visible surface** — public/protected signatures, method and field
  *names* reached by reflection or by name from config/XML, enum constant names.

The test: *could any existing test, or any caller, observe the difference?* If
yes, it is not a refactor. When the repo has a regression net (golden traces,
characterization tests), your claim of "observably null" is only credible once
that net is green — run it.

---

## Research discipline — selective, and cited

Lead with what you know. Java's settled idioms don't need a web search. Reach for
`WebSearch` / `WebFetch` when, and only when:

- a **payoff claim is contested** ("is `String.format` really slower than
  concatenation here", "does `stream().toList()` copy") — verify rather than
  assert;
- an **API may have been superseded or deprecated** at or below the floor (e.g.
  `new Integer(int)` → `Integer.valueOf`; `Date` → `java.time`) and you want the
  current recommendation;
- a **micro-optimization's direction is non-obvious** on modern JITs (escape
  analysis, loop unrolling can invert naive intuition);
- the user asks for current best practice.

Weight sources: **JEPs and JDK release notes** (authoritative on what changed and
when), **official Javadoc**, **JMH-based benchmarks** and *Effective Java* over
undated blog posts. Prefer primary sources for "when was X added" questions —
version availability is a fact, not an opinion. **Always cite** what you looked up
so the recommendation is auditable. Do **not** research settled basics (that
try-with-resources closes resources, that `StringBuilder` beats `+=` in a loop) —
it wastes the user's time and tokens.

---

## Workflow — one reviewable slice at a time

Refactor in **small, single-concern slices**. A slice changes one thing, for one
reason, and can be reviewed and verified on its own. Resist the mega-"modernize
everything" diff — it is unreviewable and buries any real win under noise.

1. **Discover** — floor + guardrails (Step 0). Once per session/file is enough.
2. **Survey & rank** — scan the target for candidates, grade each by tier, and
   present the ranked list. Lead with big wins; name the false gains you're
   *not* going to do and why (this builds trust and prevents someone else
   "fixing" them later).
3. **Propose one slice** — using the template below. Wait for go-ahead.
4. **Apply** — the minimal edit. One concern. Match surrounding style; don't
   reformat lines you aren't changing.
5. **Verify** — run the build and the regression net. "Behavior-preserving" is a
   claim you *check*, not one you assert. Report the actual result — if a golden
   diff or test moved, that's a signal you changed behavior; investigate, don't
   paper over.
6. **Document** — record the change where the project expects it (decision log,
   changelog, comment pass). Legacy refactors especially deserve a one-line "why
   this was safe" so the next reader doesn't undo it or re-question it.

Default posture: **propose → (approval) → apply → verify → document.** Only skip
the approval pause when the user has explicitly said to run autonomously.

---

## Proposal template

Use this shape when proposing a slice — it forces the payoff and safety questions
to the surface:

```
Slice: <one-line what & where — file:line>
Tier:  <big win | real-but-minor | cosmetic | false gain (recommend against)>
Why:   <the concrete payoff, or the concrete cost if recommending against>
Floor-safe? <yes — legal at Java N | uses <API/syntax> added in N+X → NOT safe>
Behavior-preserving? <yes — observably null | NO, this changes <X> → needs the change process>
Verify by: <which build/test/golden command proves it>

Before:
  <minimal snippet>
After:
  <minimal snippet>
```

---

## Anti-patterns — do not do these

- **Drive-by reformatting.** Never restyle lines you aren't functionally
  changing; it destroys blame history and bloats the diff. (Formatting is the
  formatter's job, run separately.)
- **Scope creep mid-slice.** Saw something else? Note it for a later slice.
- **Stream-ifying hot loops** (see false-gain tier).
- **`Optional` fields / boxing in tight loops.**
- **Blind `==` → `.equals()`** without checking whether identity is deliberate.
- **Reaching past the floor** — the #1 legacy mistake; re-check every API against
  `references/java-versions.md` when the floor is 8 or 11.
- **Mega-modernization PRs.** Unreviewable; slice it.

---

## Reference files

- **`references/java-versions.md`** — the per-floor availability matrix (8/11/17/
  21): what each version added, and what is *not* legal below it. Consult it
  whenever floor-legality is in question.
- **`references/thinlet.md`** — this repository's concrete floor, build/verify
  command, regression net, behavior-change protocol, and the specific
  load-bearing traps in the single `Thinlet.java` class. Pointer-shaped: it
  cites the repo's own docs so it never goes stale.
