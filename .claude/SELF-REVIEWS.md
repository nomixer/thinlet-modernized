# Independent self-reviews — rolling log

> Lull-time audits of recent decisions, run on a model **independent of the session
> model** (per the maintainer's 2026-07-09 standing instruction: Opus, not Fable), with
> permission to document the outcome, open a PR, and merge. One section per review,
> newest last. Outcomes that change anything material are also recorded in
> `DECISIONS.md` (the authority); this file keeps the full findings.

---

## 2026-07-09 — Review 1 (Opus 4.8): D42–D49 + PR #50 — HOLD, four guardrails (→ D50)

**Scope audited:** (a) interning tripwire (D43), (b) hoist-don't-relocate (D48),
(c) stateless explicit-context seam style (D48), (d) interaction goldens (D45/D47),
(e) Renderer slices incl. the palette-wide widening (D49, PR #50), (f) sequencing
(Cut 2 now, Cut 3 overlappable, Cuts 4/5/6 behind the fork-mapping gate).

**Verdicts:** all six **HOLD** against the charter (behavior+API frozen, Java 8 floor,
net-before-refactor, visibility discipline). Overall plan: **KEEP** — no change to cut
order, 3a/3b/3c staging, or the fork-mapping gate.

**Findings worth keeping (beyond the D50 summary):**

- **Verbatim instance→static moves are largely compiler-protected.** No inner classes /
  bare `this` in the moved branches, so a missed `t.`/`Thinlet.` prefix is a compile
  error, not a silent rebind. The one silent-rebind vector — dual static/instance
  overloads — was checked: only `get` is name-adjacent, and every moved call is
  arity-unambiguous. SpotBugs excludes use the `~thinlet\..*` class regex, so the
  reference-comparison suppressions travel to `Renderer` with no gap. Serialization
  unaffected (widened fields are `transient`; `Renderer` is stateless and never
  instantiated). Re-run this checklist mentally on every future slice.
- **The palette-wide widening is compliant, not drift:** japicmp gates public+protected
  only; the three colors checkbox doesn't read are consumed by the already-scheduled
  field/textarea/list slices (verified against the inline branches). Guardrail: re-narrow
  unused package-private members at 3a close (D50 item 4).
- **Precision nit on D43:** `STRICT_INTERN` is runtime-gated (`Boolean.getBoolean` is a
  method call, not a compile-time constant), so "dead-code-eliminated" was imprecise;
  the off-state behavioral guarantee is unaffected. Recorded in D50.
- **Net blind-spot inventory (unchanged from the charter, now explicit):** repaint
  timing & event→state→repaint ordering (the sharpest gap — see D50 item 3 for the
  `:lead` race now pinned by input tests); tooltip paint (timer-coupled); remaining
  transient states (scrollbar/spinbox arrows, tab hover, menubar, slider); unasserted
  input paths (menus, dialog drag/resize, focus traversal, clipboard); JDK 25+ runtime;
  serialization form (matters at Cut 5).
- **Cross-JDK gating clarification:** the golden *tests* on JDK 8/11/17 are required
  checks (D46); only the D33 cross-JDK *diff artifact* is informational.

**Guardrails adopted → D50:** (1) shared-helper gate for `paintScroll`/`paintArrow`;
(2) combobox reframed as *partially* guarded; (3) `:lead` race pinned by two
`InputListTest` tests, hoist-vs-relocate stays review-enforced; (4) 3a-closing
re-narrowing checklist item.
