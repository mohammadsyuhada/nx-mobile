// Host check for languagemap.cpp (pure). From the repo root:
//   out=$(mktemp -d)/languagemap_test && clang++ -std=c++17 -Ilibretrodroid/src/main/cpp \
//     -Ilibretrodroid/src/main/cpp/libretro/libretro-common/include \
//     libretrodroid/src/test/cpp/languagemap_test.cpp libretrodroid/src/main/cpp/languagemap.cpp -o "$out" && "$out"
#include "languagemap.h"

#include <cstdio>

#include "libretro.h"

using namespace libretrodroid;

static int failures = 0;

static void expect(const char* locale, unsigned expected) {
    auto actual = retroLanguageFor(locale);
    if (!actual || *actual != expected) {
        std::printf("FAIL %s: expected %u, got %d\n", locale, expected, actual ? (int) *actual : -1);
        failures++;
    }
}

int main() {
    expect("en", RETRO_LANGUAGE_ENGLISH);
    expect("en-US", RETRO_LANGUAGE_ENGLISH);
    expect("ja", RETRO_LANGUAGE_JAPANESE);
    expect("ja-JP", RETRO_LANGUAGE_JAPANESE);
    expect("zh", RETRO_LANGUAGE_CHINESE_SIMPLIFIED);
    expect("zh-CN", RETRO_LANGUAGE_CHINESE_SIMPLIFIED);
    expect("zh-SG", RETRO_LANGUAGE_CHINESE_SIMPLIFIED);
    expect("zh-Hans-CN", RETRO_LANGUAGE_CHINESE_SIMPLIFIED);
    expect("zh-Hans-HK", RETRO_LANGUAGE_CHINESE_SIMPLIFIED);
    expect("zh-TW", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("zh-HK", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("zh-MO", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("zh-Hant", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("zh-Hant-TW", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("zh_TW", RETRO_LANGUAGE_CHINESE_TRADITIONAL);
    expect("pt-PT", RETRO_LANGUAGE_PORTUGUESE_PORTUGAL);
    expect("pt-BR", RETRO_LANGUAGE_PORTUGUESE_BRAZIL);
    expect("ko-KR", RETRO_LANGUAGE_KOREAN);
    if (retroLanguageFor("jp")) { std::printf("FAIL jp is not an Android language\n"); failures++; }
    if (retroLanguageFor("ms-MY")) { std::printf("FAIL ms has no libretro language\n"); failures++; }
    if (retroLanguageFor("")) { std::printf("FAIL empty\n"); failures++; }

    if (failures == 0) std::printf("languagemap_test: all passed\n");
    return failures == 0 ? 0 : 1;
}
