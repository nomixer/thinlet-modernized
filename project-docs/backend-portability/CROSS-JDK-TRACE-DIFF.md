# Cross-JDK trace diff

How the per-runtime render traces are captured, shared, and compared into a
divergence report. This is the data-production mechanism behind the
backend-portability inventory: it quantifies *where* and *how much* Thinlet's
rendering drifts across JDK runtimes (the `FontMetrics` pixel-metric variance
that D7's ±2px tolerance absorbs). Authoritative rationale: `DECISIONS.md` **D33**
(and **D7** for the tolerance model).

## What it produces

An **informational**, regenerable report (not committed) with two faces:

- `report.md` — a human summary: runtimes compared, max cross-JDK spread and
  where, any position exceeding tolerance, drift ranked by drawing op, and a
  per-runtime delta-vs-baseline table.
- `report.json` — the same data structured for tooling (the eventual
  `trace-curator` will read this to populate `RENDERING-PRIMITIVES.md` etc.).

It never gates a build. The per-JDK golden regression tests already enforce
"±2px vs the baseline golden" on each runtime; this report exists to *surface*
the sub-tolerance drift those tests deliberately ignore, not to add a second gate.

## Design (why it's shaped this way)

The golden harness renders each runtime's full `Trace` in memory, asserts it
against the single committed golden within tolerance, and discards it. Two
consequences shaped this tool:

1. **The gate is left untouched.** `TraceComparator.compare()` only reports
   numeric mismatches that *exceed* tolerance — correct for its job as the
   regression gate. We did **not** change it. Instead `TraceComparator.deltas()`
   is a separate, report-only path that enumerates *every* numeric difference
   (including sub-tolerance) and collects structural/categorical mismatches
   separately (those must stay exact across JDKs).
2. **The per-JDK trace is persisted, not recomputed centrally.** Nothing is lost
   today (renders are deterministic — the self-consistency test proves it), but a
   cross-JDK comparison needs all runtimes' traces in one place. The three test
   rows (8/11/17) and the `build` job (21) run in separate CI jobs with no shared
   filesystem, so each *dumps* its trace and uploads it as an artifact; a
   downstream job aggregates them. The aggregator renders nothing — it only reads
   JSON — so it needs no display, toolchain, or dev container.

## Data flow

```
 per runtime (CI job, on that JDK)          aggregator (plain JDK-21 job)
 ┌───────────────────────────┐              ┌──────────────────────────────┐
 │ GoldenTraceDumpModeTest         │  artifact   │ CrossJdkTraceDiffTest             │
 │  -DtraceDumpDir=…/jdk-N   │ ─────────▶  │  -DtraceDiffInputDir=…      │
 │  renders corpus → TraceJson │ trace-dump- │  reads dumps + committed      │
 │  writes …/jdk-N/<path>.json │  jdk-N      │  goldens (baseline) + tol     │
 └───────────────────────────┘              │  → report.md / report.json    │
                                             └──────────────────────────────┘
```

- **`GoldenTraceDumpModeTest`** (`thinlet-core/src/test`, gated by `-DtraceDumpDir`)
  — renders every corpus file via the existing `GoldenTraceRecorder.render()` and
  writes one JSON per file, byte-identical in format to the goldens.
- **`CrossJdkTraceDiffTest`** (gated by `-DtraceDiffInputDir`) — treats the
  committed goldens as the baseline column, reads each `…/jdk-N/` dump, computes
  `deltas()` per runtime, and writes the report. Discovers runtimes from any
  immediate sub-directory whose name contains `jdk-<N>` (so both the local
  `jdk-8` and CI's downloaded `trace-dump-jdk-8` work).
- **CI** (`.github/workflows/ci.yml`): the `build` (21) and `test` (8/11/17) jobs
  add `-DtraceDumpDir=target/trace-dump/jdk-N` and upload `trace-dump-jdk-N`;
  the `trace-diff` job (`needs: [build, test]`) downloads them all and uploads
  `trace-diff-report`.

## Regenerating it

### Locally (dev container — has `/opt/jdk8|11|17`)

```sh
cd thinlet-core
for N in 8 11 17; do
  ../mvnw -B -pl . -Pcrossjdk -Djdk.target=$N -t ../.mvn/toolchains.xml \
      -Dtest=GoldenTraceDumpModeTest -DtraceDumpDir=target/trace-dump/jdk-$N \
      -DfailIfNoTests=false test
done
# JDK 21 = the base JVM, no toolchain:
../mvnw -B -pl . -Dtest=GoldenTraceDumpModeTest -DtraceDumpDir=target/trace-dump/jdk-21 \
    -DfailIfNoTests=false test
# aggregate:
../mvnw -B -pl . -Dtest=CrossJdkTraceDiffTest -DfailIfNoTests=false \
    -DtraceDiffInputDir="$PWD/target/trace-dump" \
    -DtraceDiffOut="$PWD/target/trace-diff" test
# read target/trace-diff/report.md
```

### From CI

Open the `trace-diff` job's run and download the **`trace-diff-report`** artifact.

## Reading the report

- **Max cross-JDK spread** is the headline: `max(values) − min(values)` over
  `{baseline} ∪ {per-runtime values}` at the worst single coordinate. Within
  ±`defaultPx` means every runtime renders that corpus identically *enough*.
- **Positions exceeding tolerance** is the count to watch. A non-zero count is a
  genuine finding — the first real candidate for a `perOp` entry in
  `trace-tolerance.json` (D7's reserved hook), **not** something to silence by
  widening `defaultPx` or re-recording goldens.
- **Structural / categorical** must read "identical across all runtimes" — any
  entry there is a real regression (a new/missing/reordered call, or a differing
  color/string), unrelated to pixel drift.
