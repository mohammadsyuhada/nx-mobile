// Writes and reads RZIP files with libretro-common's rzip_stream.c, the code minarch uses, so mobile's codec is
// checked against the real thing. Build: see README.md.
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <streams/rzip_stream.h>
#include <streams/file_stream.h>

static void* slurp(const char* path, int64_t* len) {
    FILE* f = fopen(path, "rb"); if (!f) { perror(path); exit(1); }
    fseek(f, 0, SEEK_END); *len = ftell(f); fseek(f, 0, SEEK_SET);
    void* buf = malloc(*len ? *len : 1); fread(buf, 1, *len, f); fclose(f); return buf;
}

int main(int argc, char** argv) {
    if (argc != 4) { fprintf(stderr, "usage: %s compress|decompress IN OUT\n", argv[0]); return 2; }
    if (!strcmp(argv[1], "compress")) {
        int64_t len; void* buf = slurp(argv[2], &len);
        return rzipstream_write_file(argv[3], buf, len) ? 0 : 1;
    }
    void* out = NULL; int64_t len = 0;
    if (!rzipstream_read_file(argv[2], &out, &len)) return 1;
    FILE* f = fopen(argv[3], "wb"); fwrite(out, 1, len, f); fclose(f); free(out); return 0;
}
