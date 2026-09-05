#!/usr/bin/env python3
"""Summarise a JaCoCo XML report for thinlet-core (DECISIONS.md D90).

Prints per-class instruction/branch coverage worst-first, then — with
UNCOVERED=true in the environment — every method with zero covered
instructions, which is the list a blind-spot hunt actually wants.
"""
import os
import sys
import xml.etree.ElementTree as ET


def counter(node, kind):
    for c in node.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("covered")), int(c.get("missed"))
    return 0, 0


def pct(cov, missed):
    total = cov + missed
    return 100.0 * cov / total if total else 100.0


def main(path):
    root = ET.parse(path).getroot()
    rows, dead = [], []
    for cls in root.iter("class"):
        name = cls.get("name", "").replace("/", ".")
        ic, im = counter(cls, "INSTRUCTION")
        bc, bm = counter(cls, "BRANCH")
        rows.append((pct(ic, im), name, ic + im, im, pct(bc, bm), bm))
        for m in cls.findall("method"):
            mc, mm = counter(m, "INSTRUCTION")
            if mc == 0 and mm > 0:
                dead.append((mm, name, m.get("name"), m.get("desc")))

    rows.sort()
    print(f"{'inst%':>7} {'missed':>7} {'br%':>7} {'brMiss':>7}  class")
    for p, name, _tot, im, bp, bm in rows:
        print(f"{p:7.1f} {im:7d} {bp:7.1f} {bm:7d}  {name}")

    tc, tm = counter(root, "INSTRUCTION")
    bc, bm = counter(root, "BRANCH")
    mc, mm = counter(root, "METHOD")
    print()
    print(f"TOTAL instructions {pct(tc, tm):.1f}% ({tm} missed)   "
          f"branches {pct(bc, bm):.1f}% ({bm} missed)   "
          f"methods {pct(mc, mm):.1f}% ({mm} never entered)")

    if os.environ.get("UNCOVERED") == "true":
        dead.sort(reverse=True)
        print(f"\nMethods with ZERO covered instructions ({len(dead)}), largest first:")
        for missed, cls, mname, desc in dead:
            print(f"  {missed:5d} instr  {cls}.{mname}{desc}")


if __name__ == "__main__":
    main(sys.argv[1])
