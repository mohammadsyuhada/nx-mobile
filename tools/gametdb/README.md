# GameTDB controls table

`build_controls.py` turns GameTDB's `wiitdb.xml` into `androidApp/src/main/assets/wii/controls.tsv`, the table the app
reads to pick a Wii game's controller automatically. Each row is `ID6<TAB>FLAGS`; lines starting with `#` are comments
(the first names the wiitdb version). Rows are sorted by ID. Wii entries only (custom entries are left out).

## Flags

Any subset of `CNMU`, in that order, possibly empty:

- `C`: a Classic Controller is listed for the game (required or optional).
- `N`: the Nunchuk is required.
- `M`: MotionPlus is required.
- `U`: the game is on `upright.tsv`, held upright for pointing.

## Refreshing the table

From the repo root. GameTDB refuses requests without a browser user agent. Treat the download as untrusted: use a new
empty directory and `python3 -I`.

```bash
SCR=$(mktemp -d)
curl -sL -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 Safari/605.1.15" -o "$SCR/wiitdb.zip" "https://www.gametdb.com/wiitdb.zip?LANG=EN"
unzip -o -q "$SCR/wiitdb.zip" -d "$SCR/wiitdb"
python3 -I tools/gametdb/build_controls.py "$SCR/wiitdb/wiitdb.xml"
python3 -I tools/gametdb/test_build_controls.py
```

The script refuses a file that declares a DTD or entities.

## The upright list

`upright.tsv` is hand-kept. Add a game by its 3-character game code (the first three characters of its ID, so one line
covers every region), then a tab and a name for humans. Re-run the script afterwards.

## Terms

GameTDB's FAQ: "Can I use the database/artwork in my software? Yes, please add a link to GameTDB so that users know
where to add new game entries and artwork." The link is in the root `README.md` credits and `LICENSES/gametdb.txt`.
