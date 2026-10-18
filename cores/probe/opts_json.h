/*
 * nx-mobile - core option tables captured by nxprobe, and their JSON writer.
 * Copyright (C) 2026 Mohammad Syuhada.
 *
 * Ported from NX Redux workspace/all/minarch/ma_opts_schema.c (same author).
 * Changes from the port: every value is listed (no int range compaction), no
 * section split, and the output keys are the ones the app's CoreOptionTables
 * reads.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See the LICENSE file at the repository root.
 */
#ifndef NXPROBE_OPTS_JSON_H
#define NXPROBE_OPTS_JSON_H

#include "libretro.h"

/* Every string is owned (malloc'd) and never NULL; an absent label is "". */
typedef struct {
   char *key, *label, *label_cat, *info, *def;
   int n;          /* number of values */
   char **values;  /* n entries */
   char **labels;  /* n entries, "" when the core gave none */
} OptItem;

typedef struct {
   char *key, *label, *info; /* key "" = uncategorized */
   OptItem *items;
   int n;
} OptSection;

typedef struct {
   char *core, *emulator;
   OptSection *sections;
   int n;
} OptSchema;

/* Builders. Each returns a new schema (caller frees with schema_free), with
 * options that declare no values skipped and empty sections omitted. NULL only
 * on allocation failure. core/emulator may be NULL (written as ""). */
OptSchema *schema_from_v1(const struct retro_core_option_definition *defs,
                          const char *core, const char *emulator);
OptSchema *schema_from_v2(const struct retro_core_options_v2 *v2,
                          const char *core, const char *emulator);
OptSchema *schema_from_vars(const struct retro_variable *vars,
                            const char *core, const char *emulator);

/* Removes every item whose key starts with prefix, and any section left empty. */
void schema_drop_prefix(OptSchema *schema, const char *prefix);

/* Total item count across sections. */
int schema_item_count(const OptSchema *schema);

/* The default of key, or NULL when the schema has no such item. */
const char *schema_default(const OptSchema *schema, const char *key);

/* Writes path.tmp then renames it over path. 0 on success, -1 on I/O error. */
int schema_write_json(const OptSchema *schema, const char *path);

void schema_free(OptSchema *schema);

/* The core name for the JSON "core" field: the .so basename cut at
 * "_libretro", else without ".so". Caller frees. */
char *schema_core_name(const char *so_path);

#endif
