#!/bin/sh
# Usage: verify.sh <path-to-llvm-readelf> file.so [file.so ...]
#        verify.sh --options-only   (only the option-table warnings below; gmake verify-options)
# Exit 1 if any file is not AArch64 or has a PT_LOAD segment aligned below 0x4000 (16 KB).
# Also warns (never fails) when the option table in $OPTIONS_DIR of a core shipped in $JNILIBS_DIR is missing or was
# probed from another build (jniLibs holds lib<artifact>.so). OPTIONS_IDS ("<artifact>.so=<coreId> ...") comes from the Makefile; an artifact not in it
# maps to its name without _libretro_android.so.
set -u
options_only=0
if [ "${1:-}" = --options-only ]; then options_only=1; shift; else READELF="$1"; shift; fi
here=$(dirname "$0")
OPTIONS_DIR=${OPTIONS_DIR:-$here/../androidApp/src/main/assets/core-options}
JNILIBS_DIR=${JNILIBS_DIR:-$here/../androidApp/src/main/jniLibs/arm64-v8a}
OPTIONS_IDS=${OPTIONS_IDS:-}
status=0
[ "$options_only" -eq 1 ] || [ "$#" -gt 0 ] || { echo "verify: no files to check"; exit 1; }
[ "$options_only" -eq 0 ] || set --
for f in "$@"; do
  machine=$("$READELF" -h "$f" | awk -F: '/Machine:/{gsub(/^[ \t]+/,"",$2); print $2}')
  case "$machine" in *AArch64*) ;; *) echo "FAIL $f: machine is '$machine', expected AArch64"; status=1; continue;; esac
  file_ok=1
  # llvm-readelf -l prints one line per program header; Align is the last field of every LOAD line.
  for al in $("$READELF" -l "$f" | awk '/^ *LOAD/ {print $NF}'); do
    dec=$(( al ))          # POSIX arithmetic accepts 0x-prefixed hex
    if [ "$dec" -lt 16384 ]; then echo "FAIL $f: LOAD align $al < 0x4000"; status=1; file_ok=0; fi
  done
  [ "$file_ok" -eq 1 ] && echo "OK   $f"
done

# Option tables (gmake options): <coreId>.json carries the soSha256 of the shipped core it was probed from.
for f in "$JNILIBS_DIR"/lib*_libretro_android.so; do
  [ -f "$f" ] || continue
  base=$(basename "$f")
  base=${base#lib}   # make install adds the lib prefix; OPTIONS_IDS names the artifact
  core=${base%_libretro_android.so}
  for pair in $OPTIONS_IDS; do
    [ "${pair%%=*}" = "$base" ] && core=${pair#*=}
  done
  json="$OPTIONS_DIR/$core.json"
  if [ ! -f "$json" ]; then
    echo "options: $core.json missing — run gmake options-$core"
    continue
  fi
  want=$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1], encoding="utf-8")).get("soSha256", ""))' "$json" 2>/dev/null)
  have=$(shasum -a 256 "$f" | cut -d' ' -f1)
  [ "$want" = "$have" ] || echo "options: $core.json is from another build — run gmake options-$core"
done
exit $status
