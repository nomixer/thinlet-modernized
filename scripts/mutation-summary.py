#!/usr/bin/env python3
"""Summarise a PIT mutations.xml (DECISIONS.md D92).

A scoped run mutates every class but only *executes* mutants the named test
covers, so NO_COVERAGE is the ordinary state of the rest of the codebase and is
counted, never listed. The finding is SURVIVED: the test ran that line and did
not detect the change. The score is therefore killed / (killed + survived) over
covered mutants only — including NO_COVERAGE would report a scoped run as ~1%.
"""
import collections
import sys
import xml.etree.ElementTree as ET


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
