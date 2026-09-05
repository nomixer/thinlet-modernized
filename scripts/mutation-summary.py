#!/usr/bin/env python3
"""Summarise a PIT mutations.xml (DECISIONS.md D92).

A scoped run mutates every class but only *executes* mutants the named test
covers, so NO_COVERAGE is the ordinary state of the rest of the codebase and is
counted, never listed. The finding is SURVIVED: the test ran that line and did
not detect the change. The score is therefore killed / (killed + survived) over
covered mutants only — including NO_COVERAGE would report a scoped run as ~1%.
"""
import collections
import os
import sys
import xml.etree.ElementTree as ET


def verdict(muts, gate, lines=None):
    """The loop's gate, scoped to the lines a slice was assigned.

    A flat score threshold cannot work: D92 measured an assertion-free test at
    50%, because PIT counts a thrown exception as a kill.

    Scoping to the whole method does not work either, which the first dry run
    proved (D94): `parse` carries equivalent mutants — line 5276's negated
    conditional is covered by every start tag and unkillable, because the `<!`
    opener is taken by the doctype branch above it — so "no survivor in the
    method" is unreachable however good the test is. GATE_LINES narrows the
    verdict to the missed lines the worklist actually handed this slice; an
    already-covered line like 5276 was never its job.
    """
    here = [m for m in muts if m.findtext("mutatedMethod") == gate]
    if lines:
        here = [m for m in here if int(m.findtext("lineNumber", "-1")) in lines]
    killed = [m for m in here if m.get("status") in ("KILLED", "TIMED_OUT")]
    survived = [m for m in here if m.get("status") == "SURVIVED"]
    none = [m for m in here if m.get("status") == "NO_COVERAGE"]
    scope = f"{gate}" + (f" lines {min(lines)}-{max(lines)}" if lines else "")
    print(f"gate {scope}: killed {len(killed)}   survived {len(survived)}"
          f"   not reached {len(none)}")
    if not here:
        print(f"FAIL: no mutant exists in scope ({scope}) — check the method name"
              " and whether those lines carry mutable code.")
        return 1
    for m in survived:
        print(f"  SURVIVED :{m.findtext('lineNumber')}  {m.findtext('description')}")
    if not killed:
        print(f"FAIL: the test kills no mutant in {scope} — it does not watch it at all.")
        return 1
    if survived:
        print(f"FAIL: {len(survived)} mutant(s) in {scope} run without being detected.")
        return 1
    print(f"PASS: every mutant reached in {scope} was detected.")
    return 0


def main(path):
    muts = ET.parse(path).getroot().findall("mutation")
    tally = collections.Counter(m.get("status") for m in muts)
    killed = tally["KILLED"] + tally["TIMED_OUT"]
    survived = tally["SURVIVED"]
    covered = killed + survived
    score = 100.0 * killed / covered if covered else 0.0

    print(f"covered mutants {covered}   killed {killed}   survived {survived}   score {score:.1f}%")
    print(f"  out of scope (NO_COVERAGE) {tally['NO_COVERAGE']}"
          f"   non-viable {tally['NON_VIABLE']}   run errors {tally['RUN_ERROR']}")
    if covered == 0:
        print("\nNo mutant ran. The test did not execute under PIT — this is a broken"
              "\nrun, not a clean one. Check the minion log before trusting a green gate.")
        return 1

    gate = os.environ.get("GATE_METHOD")
    if gate:
        raw = os.environ.get("GATE_LINES", "")
        # The worklist truncates a long line list with a trailing "+N"; drop it
        # rather than crash, and gate on the lines actually named.
        lines = {int(x) for x in raw.replace(" ", "").split(",") if x.isdigit()}
        return verdict(muts, gate, lines or None)

    weak = [m for m in muts if m.get("status") == "SURVIVED"]
    if not weak:
        print("\nNo survivors: every mutant on the covered lines was detected.")
        return 0

    by_class = collections.Counter(m.findtext("mutatedClass") for m in weak)
    print(f"\nSurvivors ({len(weak)}) — lines the test runs but does not watch:")
    for cls, n in by_class.most_common():
        print(f"  {n:4d}  {cls}")
    print()
    for m in weak:
        print(f"  {m.findtext('mutatedClass')}.{m.findtext('mutatedMethod')}"
              f":{m.findtext('lineNumber')}  {m.findtext('description')}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
