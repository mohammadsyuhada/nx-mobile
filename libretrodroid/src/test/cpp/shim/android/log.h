// Host stand-in for <android/log.h> so the achievements sources build under host clang for the tests.
#ifndef LIBRETRODROID_TEST_ANDROID_LOG_SHIM_H
#define LIBRETRODROID_TEST_ANDROID_LOG_SHIM_H

#include <cstdarg>
#include <cstdio>

enum { ANDROID_LOG_VERBOSE = 2, ANDROID_LOG_DEBUG, ANDROID_LOG_INFO, ANDROID_LOG_WARN, ANDROID_LOG_ERROR, ANDROID_LOG_FATAL };

static inline int __android_log_print(int, const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    std::fprintf(stderr, "[%s] ", tag);
    int n = std::vfprintf(stderr, fmt, args);
    std::fputc('\n', stderr);
    va_end(args);
    return n;
}

#endif
