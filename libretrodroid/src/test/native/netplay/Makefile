# nx-mobile - netplay engine and link host tests. GPL-3.0-or-later.
# Run through `make -C libretrodroid/src/test/native/netplay test`.
CC ?= cc
OUT := out
SRC := ../../../main/cpp/netplay
CFLAGS := -std=c11 -Wall -Wextra -O1 -pthread -I$(SRC)
all: $(OUT)/test_engine $(OUT)/test_link
$(OUT):
	mkdir -p $(OUT)
$(OUT)/test_engine: test_engine.c $(SRC)/np_engine.c $(SRC)/np_net.c $(SRC)/np_engine.h $(SRC)/np_net.h | $(OUT)
	$(CC) $(CFLAGS) -o $@ test_engine.c $(SRC)/np_engine.c $(SRC)/np_net.c
$(OUT)/test_link: test_link.c $(SRC)/np_link.c $(SRC)/np_net.c $(SRC)/np_link.h $(SRC)/np_net.h | $(OUT)
	$(CC) $(CFLAGS) -o $@ test_link.c $(SRC)/np_link.c $(SRC)/np_net.c
test: all
	sh run.sh
clean:
	rm -rf $(OUT)
.PHONY: all test clean
