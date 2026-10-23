/*
 *     Copyright (C) 2026  Mohammad Syuhada
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "utf8.h"

namespace libretrodroid {

namespace {

/** The continuation bytes after a lead byte: the [extra] bytes from [i + 1] are all 10xxxxxx and inside [n]. */
bool continues(const char* s, size_t n, size_t i, size_t extra) {
    if (i + extra >= n) return false;
    for (size_t k = 1; k <= extra; k++) if (((unsigned char) s[i + k] & 0xC0) != 0x80) return false;
    return true;
}

}

bool Utf8::isJniSafe(const char* s, size_t n) {
    size_t i = 0;
    while (i < n) {
        auto c = (unsigned char) s[i];
        size_t extra;
        if (c < 0x80) extra = 0;
        else if ((c & 0xE0) == 0xC0 && c >= 0xC2) extra = 1;
        else if ((c & 0xF0) == 0xE0) extra = 2;
        else return false;
        if (extra > 0 && !continues(s, n, i, extra)) return false;
        i += extra + 1;
    }
    return true;
}

std::u16string Utf8::toUtf16(const char* s, size_t n) {
    std::u16string out;
    out.reserve(n);   // never more UTF-16 units than bytes: a 4-byte sequence is two
    size_t i = 0;
    while (i < n) {
        auto c = (unsigned char) s[i];
        auto at = [&](size_t k) { return (char32_t) ((unsigned char) s[i + k] & 0x3F); };
        if (c < 0x80) {
            out += (char16_t) c;
            i += 1;
        } else if ((c & 0xE0) == 0xC0 && c >= 0xC2 && continues(s, n, i, 1)) {
            out += (char16_t) (((c & 0x1F) << 6) | at(1));
            i += 2;
        } else if ((c & 0xF0) == 0xE0 && continues(s, n, i, 2)) {
            // Taken as it decodes, as NewStringUTF would (a CESU-8 surrogate half included).
            out += (char16_t) (((c & 0x0F) << 12) | (at(1) << 6) | at(2));
            i += 3;
        } else if ((c & 0xF8) == 0xF0 && continues(s, n, i, 3)) {
            char32_t cp = ((char32_t) (c & 0x07) << 18) | (at(1) << 12) | (at(2) << 6) | at(3);
            if (cp < 0x10000 || cp > 0x10FFFF) {   // overlong or past Unicode: no character to pair
                out += u'?';
            } else {
                cp -= 0x10000;
                out += (char16_t) (0xD800 + (cp >> 10));
                out += (char16_t) (0xDC00 + (cp & 0x3FF));
            }
            i += 4;
        } else {
            out += u'?';
            i += 1;
        }
    }
    return out;
}

}
