#!/usr/bin/env python3
"""Summarise a JaCoCo XML report for thinlet-core (DECISIONS.md D90/D93).

Prints per-class instruction/branch coverage worst-first; then, per environment
switch, either the blind-spot list or the loop's worklist:

  UNCOVERED=true  every method with zero covered instructions
  WORKLIST=true   every partially-covered method with the exact source lines
                  still missed, worst branch coverage first — the rows
                  loop-characterise hands to a slice as its assigned target
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


def method_ranges(cls):
    """[(start, end, name, desc)] per method. JaCoCo records only a method's first
    line, so each range runs to the next method's start; good enough to attribute
    a missed line to the method containing it."""
    ms = []
    for m in cls.findall("method"):
        line = m.get("line")
        if line is not None:
            ms.append([int(line), None, m.get("name"), m.get("desc")])
    ms.sort()
    for i, m in enumerate(ms):
        m[1] = ms[i + 1][0] - 1 if i + 1 < len(ms) else 10 ** 9
    return ms


def worklist(root):
    """Partially-covered methods with the source lines still missed."""
    missed_lines = {}
    for sf in root.iter("sourcefile"):
        rows = []
        for ln in sf.findall("line"):
            mi, mb = int(ln.get("mi", 0)), int(ln.get("mb", 0))
            if mi or mb:
                rows.append((int(ln.get("nr")), mi, mb))
        missed_lines[sf.get("name")] = rows

    out = []
    for cls in root.iter("class"):
        rows = missed_lines.get(cls.get("sourcefilename"), [])
        if not rows:
            continue
        name = cls.get("name", "").replace("/", ".")
        for start, end, mname, desc in method_ranges(cls):
            here = [(nr, mi, mb) for nr, mi, mb in rows if start <= nr <= end]
            if not here:
                continue
            mi = sum(x[1] for x in here)
            mb = sum(x[2] for x in here)
            out.append((mb, mi, name, mname, desc, [x[0] for x in here]))
    out.sort(reverse=True)
    return out


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

    if os.environ.get("WORKLIST") == "true":
        rows = worklist(root)
        print(f"\nWorklist ({len(rows)} methods with missed lines), worst branches first:")
        for mb, mi, cls, mname, desc, lines in rows:
            shown = ",".join(str(x) for x in lines[:40])
            more = f",+{len(lines) - 40}" if len(lines) > 40 else ""
            print(f"  {cls}.{mname}{desc}  missedInstr={mi} missedBranch={mb} lines={shown}{more}")

    if os.environ.get("UNCOVERED") == "true":
        dead.sort(reverse=True)
        print(f"\nMethods with ZERO covered instructions ({len(dead)}), largest first:")
        for missed, cls, mname, desc in dead:
            print(f"  {missed:5d} instr  {cls}.{mname}{desc}")


if __name__ == "__main__":
    main(sys.argv[1])
