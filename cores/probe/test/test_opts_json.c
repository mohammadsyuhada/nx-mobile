/*
 * nx-mobile - unit checks for opts_json.c that the fake cores don't reach:
 * JSON string escaping, value-less options, the DIP-switch prefix drop
 * emptying a section, and the core name rule.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 *
 * Usage: test_opts_json <out.json>. Exits non-zero on a failed check; run.sh
 * then compares out.json with test/opts_json_escape.expected.json.
 */
#include "opts_json.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int failures = 0;

#define CHECK(cond)                                                     \
   do {                                                                 \
      if (!(cond)) {                                                    \
         fprintf(stderr, "%s:%d: CHECK failed: %s\n", __FILE__, __LINE__, #cond); \
         failures++;                                                    \
      }                                                                 \
   } while (0)

static void check_name(const char *path, const char *want) {
   char *got = schema_core_name(path);
   if (!got || strcmp(got, want) != 0) {
      fprintf(stderr, "core name of %s: got %s, want %s\n", path, got ? got : "(null)", want);
      failures++;
   }
   free(got);
}

int main(int argc, char **argv) {
   if (argc != 2) {
      fprintf(stderr, "usage: %s <out.json>\n", argv[0]);
      return 1;
   }

   check_name("out/fake_core_v2.so", "fake_core_v2");
   check_name("/data/local/tmp/nxprobe/mupen64plus_next_gles3_libretro_android.so", "mupen64plus_next_gles3");
   check_name("gpsp_libretro.so", "gpsp");
   check_name("plain", "plain");

   /* escaping: quote, backslash, newline, tab, a control char, UTF-8 */
   static struct retro_core_option_v2_category cats[] = {
      {"dip", "DIP", NULL},
      {NULL, NULL, NULL},
   };
   static struct retro_core_option_v2_definition defs[] = {
      {"esc_a", "Say \"hi\"", NULL, "back\\slash\nline\ttab\x01 caf\xc3\xa9", NULL, NULL,
       {{"a/b", "A \"b\""}, {NULL, NULL}}, NULL},
      {"esc_novalues", "Nothing", NULL, NULL, NULL, NULL, {{NULL, NULL}}, NULL},
      {"fbneo-dipswitch-x-Lives", "Lives", NULL, NULL, NULL, "dip",
       {{"3", NULL}, {NULL, NULL}}, "3"},
      {NULL, NULL, NULL, NULL, NULL, NULL, {{NULL, NULL}}, NULL},
   };
   static struct retro_core_options_v2 v2 = {cats, defs};

   OptSchema *s = schema_from_v2(&v2, "esc", "Esc \"Core\"");
   CHECK(s != NULL);
   if (!s)
      return 1;
   CHECK(s->n == 2);                       /* uncategorized + dip */
   CHECK(schema_item_count(s) == 2);       /* esc_novalues skipped */
   CHECK(schema_default(s, "esc_a") && strcmp(schema_default(s, "esc_a"), "a/b") == 0); /* first value */
   CHECK(schema_default(s, "fbneo-dipswitch-x-Lives") != NULL);
   schema_drop_prefix(s, "fbneo-dipswitch-");
   CHECK(s->n == 1);                       /* the emptied dip section is gone */
   CHECK(schema_default(s, "fbneo-dipswitch-x-Lives") == NULL);
   CHECK(schema_write_json(s, argv[1]) == 0);
   schema_free(s);

   if (failures)
      fprintf(stderr, "test_opts_json: %d check(s) failed\n", failures);
   return failures ? 1 : 0;
}
