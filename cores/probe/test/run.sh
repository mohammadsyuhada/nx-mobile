#!/bin/sh
# nx-mobile - nxprobe host tests. GPL-3.0-or-later.
# Run through `make -C cores/probe test` (it builds out/ first).
set -u

here=$(cd "$(dirname "$0")" && pwd)
root=$(dirname "$here")
out="$root/out"
res="$out/results"
rm -rf "$res"
mkdir -p "$res/system"

fails=0
pass() { echo "PASS $1"; }
fail() { echo "FAIL $1: $2"; fails=$((fails + 1)); }

# same_json <got> <expected>: equal after sort-keys normalisation
same_json() {
	python3 -m json.tool --sort-keys "$1" > "$1.norm" 2>&1 || return 1
	python3 -m json.tool --sort-keys "$2" > "$1.want" || return 1
	diff -u "$1.want" "$1.norm"
}

# probe_case <name> <core> <want exit> <expected json | -> [game]
probe_case() {
	name=$1 core=$2 want=$3 expected=$4
	shift 4
	json="$res/$name.json"
	"$out/nxprobe" "$out/$core.so" "$json" "$res/system" "$@" 2> "$res/$name.log"
	rc=$?
	if [ "$rc" != "$want" ]; then
		fail "$name" "exit $rc, want $want (log: $res/$name.log)"
		return
	fi
	if [ -e "$json.tmp" ]; then
		fail "$name" "left $json.tmp behind"
		return
	fi
	if [ "$expected" = "-" ]; then
		if [ -e "$json" ]; then fail "$name" "wrote $json, want no file"; else pass "$name"; fi
		return
	fi
	if [ ! -e "$json" ]; then
		fail "$name" "no $json"
		return
	fi
	if same_json "$json" "$here/$expected"; then pass "$name"; else fail "$name" "JSON differs"; fi
}

probe_case v2 fake_core_v2 0 fake_core_v2.expected.json
# NXPROBE_SKIP_INIT: a retro_init that would abort never runs, and the table declared in retro_set_environment
# is still written.
export NXPROBE_SKIP_INIT=1 FAKE_CORE_ABORT_INIT=1
probe_case v2_skip_init fake_core_v2 0 fake_core_v2.expected.json
unset NXPROBE_SKIP_INIT
probe_case v2_init_aborts fake_core_v2 134 fake_core_v2.expected.json
unset FAKE_CORE_ABORT_INIT
probe_case v1 fake_core_v1 0 fake_core_v1.expected.json
probe_case vars fake_core_vars 0 fake_core_vars.expected.json
probe_case load_no_game fake_core_load 2 -
probe_case load_game fake_core_load 0 fake_core_load.expected.json "$here/dummy.bin"
# retro_load_game returns false after declaring options: exit 1 though a table was written.
# need_fullpath is set, so the (absent) file is never read.
probe_case load_fail fake_core_load 1 fake_core_load.expected.json "$res/load-fail.bin"
# retro_load_game needs SET_PIXEL_FORMAT accepted (and SET_HW_RENDER refused) before it declares its options.
probe_case pixfmt fake_core_pixfmt 0 fake_core_pixfmt.expected.json "$here/dummy.bin"
probe_case missing_core no_such_core 1 -

# usage error
if "$out/nxprobe" "$out/fake_core_v2.so" 2> /dev/null; then
	fail usage "exit 0 with too few arguments"
else
	pass usage
fi

# GET_SAVE_DIRECTORY lands next to the output
if [ -d "$res/nxprobe-save" ]; then pass save_dir; else fail save_dir "no $res/nxprobe-save"; fi

# opts_json unit checks: escaping, value-less options, prefix drop, core name
if "$out/test_opts_json" "$res/escape.json" && same_json "$res/escape.json" "$here/opts_json_escape.expected.json"; then
	pass opts_json
else
	fail opts_json "see above"
fi

if [ "$fails" -ne 0 ]; then
	echo "$fails test(s) failed"
	exit 1
fi
echo "all tests passed"
