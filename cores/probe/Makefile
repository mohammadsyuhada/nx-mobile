# nx-mobile - nxprobe host build and tests. GPL-3.0-or-later.
#
#   make            build out/nxprobe and the fake cores (host cc)
#   make test       build, then run test/run.sh
#   make clean
#

CC ?= cc
OUT := out
CFLAGS := -std=c11 -Wall -Wextra -O1
FAKES := fake_core_v2 fake_core_v1 fake_core_vars fake_core_load fake_core_pixfmt

all: $(OUT)/nxprobe $(OUT)/test_opts_json $(FAKES:%=$(OUT)/%.so)

$(OUT):
	mkdir -p $(OUT)

$(OUT)/nxprobe: nxprobe.c opts_json.c opts_json.h libretro.h | $(OUT)
	$(CC) $(CFLAGS) -o $@ nxprobe.c opts_json.c -ldl

$(OUT)/test_opts_json: test/test_opts_json.c opts_json.c opts_json.h libretro.h | $(OUT)
	$(CC) $(CFLAGS) -I. -o $@ test/test_opts_json.c opts_json.c

$(OUT)/%.so: test/%.c test/fake_core.h libretro.h | $(OUT)
	$(CC) $(CFLAGS) -I. -shared -fPIC -o $@ $<

test: all
	sh test/run.sh

clean:
	rm -rf $(OUT)

.PHONY: all test clean
