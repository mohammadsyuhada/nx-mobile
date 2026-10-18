# rzip-fixture

`rzip_tool.c` writes and reads RZIP files through libretro-common's own `rzipstream` (the code minarch uses), so
`Rzip.kt` in androidApp is checked against the real implementation.

Build, from the repo root (`LC` is the bundled submodule). `-DHAVE_ZLIB=1` is required: without it
`trans_stream_get_zlib_*_backend` returns NULL and compress silently writes a header-only file.

```bash
LC=libretrodroid/src/main/cpp/libretro/libretro-common
clang -O1 -DHAVE_ZLIB=1 -I$LC/include -o /tmp/rzip_tool tools/rzip-fixture/rzip_tool.c \
  $LC/streams/rzip_stream.c $LC/streams/file_stream.c $LC/streams/trans_stream.c $LC/streams/trans_stream_zlib.c \
  $LC/streams/trans_stream_pipe.c $LC/file/file_path_io.c \
  $LC/vfs/vfs_implementation.c $LC/file/file_path.c $LC/string/stdstring.c $LC/compat/compat_strl.c \
  $LC/compat/compat_strcasestr.c $LC/encodings/encoding_utf.c $LC/time/rtime.c -lz
```

Fixtures (`androidApp/src/test/resources/rzip/`):

```bash
head -c 32768 /dev/urandom > androidApp/src/test/resources/rzip/small.raw
python3 -c "import sys; sys.stdout.buffer.write(bytes(range(256))*1500)" > androidApp/src/test/resources/rzip/big.raw   # 384000 bytes: 3 chunks
/tmp/rzip_tool compress androidApp/src/test/resources/rzip/small.raw androidApp/src/test/resources/rzip/small.rzip
/tmp/rzip_tool compress androidApp/src/test/resources/rzip/big.raw androidApp/src/test/resources/rzip/big.rzip
```

Chunks are zlib streams with the zlib header (`deflateInit` / `inflateInit2` with default window bits).
