#!/usr/bin/env python3
"""flycast naomi_roms.cpp -> sets.tsv: name, parent, title, biosFile, sorted by name.

A game row's biosFile is the archive its BIOS loads from (BIOS_t.filename, else the BIOS name; a game without a BIOS uses
"naomi", as naomi_cart.cpp does). A BIOS row (from BIOS[]) has an empty parent and title and its own archive as biosFile.
"""
import re
import sys

STRING = re.compile(r'"((?:[^"\\]|\\.)*)"')

def strip_comments(src):
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            m = STRING.match(src, i)
            out.append(m.group(0)); i = m.end(); continue
        if src.startswith("//", i):
            j = src.find("\n", i); i = n if j < 0 else j; continue
        if src.startswith("/*", i):
            j = src.find("*/", i + 2); i = n if j < 0 else j + 2; continue
        out.append(c); i += 1
    return "".join(out)

def split_top(body):
    """Top-level comma-separated fields of body (depth counted over braces and parentheses, strings skipped)."""
    fields, depth, start, i = [], 0, 0, 0
    while i < len(body):
        c = body[i]
        if c == '"':
            i = STRING.match(body, i).end(); continue
        if c in "{(":
            depth += 1
        elif c in "})":
            depth -= 1
        elif c == "," and depth == 0:
            fields.append(body[start:i].strip()); start = i + 1
        i += 1
    tail = body[start:].strip()
    if tail:
        fields.append(tail)
    return fields

def array_entries(src, decl):
    """The initialisers of the array `decl[] = { {...}, {...} }`, each without its outer braces."""
    m = re.search(re.escape(decl) + r"\s*\[\s*\]\s*=\s*\{", src)
    if not m:
        sys.exit(f"{decl}[] not found")
    i, depth, start, entries = m.end(), 1, None, []
    while depth:
        c = src[i]
        if c == '"':
            i = STRING.match(src, i).end(); continue
        if c == "{":
            depth += 1
            if depth == 2:
                start = i + 1
        elif c == "}":
            if depth == 2:
                entries.append(src[start:i])
            depth -= 1
        i += 1
    return entries

def text(field):
    m = STRING.fullmatch(field.strip())
    return None if m is None else m.group(1).replace('\\"', '"')

def clean(s):
    return " ".join((s or "").replace("\t", " ").split())

def main(src_path, out):
    src = strip_comments(open(src_path, encoding="utf-8", errors="replace").read())
    bios_file = {}
    for entry in array_entries(src, "BIOS"):
        f = split_top(entry)
        name = text(f[0]) if f else None
        if name is None:            # { nullptr } terminator
            continue
        filename = text(f[2]) if len(f) > 2 else None
        bios_file[name] = filename or name
    rows = {}
    for name, archive in bios_file.items():
        if name == archive:         # naomi-jp-d, naomidev ... live in naomi.zip: only the archive names are files
            rows[name.lower()] = (name.lower(), "", "", archive.lower())
    for entry in array_entries(src, "Games"):
        f = split_top(entry)
        name = text(f[0]) if f else None
        if name is None:
            continue
        parent = text(f[1]) or ""
        bios = text(f[5]) or "naomi"
        if bios not in bios_file:
            sys.exit(f"{name}: unknown BIOS {bios}")
        title = clean(text(f[2]) if len(f) > 2 else None)
        if not title:               # an empty title would read as a BIOS row in the app
            sys.exit(f"{name}: no plain-string title")
        rows[name.lower()] = (name.lower(), parent.lower(), title, bios_file[bios].lower())
    ordered = sorted(rows.values())
    with open(out, "w", encoding="utf-8", newline="\n") as fh:
        for r in ordered:
            fh.write("\t".join(r) + "\n")
    games = sum(1 for r in ordered if r[2])
    print(f"{out}: {games} sets, {len(ordered) - games} BIOS")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
