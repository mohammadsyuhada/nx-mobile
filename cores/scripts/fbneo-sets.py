#!/usr/bin/env python3
"""FBNeo arcade DAT -> sets.tsv: name, title, cloneof, romof, isbios (1 or empty), sorted by name."""
import sys
import xml.etree.ElementTree as ET

def clean(s):
    return " ".join((s or "").replace("\t", " ").split())

def main(dat, out):
    rows = []
    for _, el in ET.iterparse(dat, events=("end",)):
        if el.tag != "game":
            continue
        name = el.get("name", "").strip().lower()
        if name:
            rows.append((name, clean(el.findtext("description")), (el.get("cloneof") or "").lower(),
                         (el.get("romof") or "").lower(), "1" if el.get("isbios") == "yes" else ""))
        el.clear()
    rows.sort()
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        for r in rows:
            f.write("\t".join(r) + "\n")
    print(f"{out}: {len(rows)} sets")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
