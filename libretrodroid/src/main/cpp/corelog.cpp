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

#include "corelog.h"

#include <jni.h>
#include <cstdio>

namespace libretrodroid {

std::mutex CoreLog::lock;
std::deque<std::string> CoreLog::lines;

void CoreLog::clear() {
    std::lock_guard<std::mutex> guard(lock);
    lines.clear();
}

void CoreLog::add(char level, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    append(level, fmt, args);
    va_end(args);
}

void CoreLog::append(char level, const char* fmt, va_list args) {
    if (fmt == nullptr) return;
    char buffer[1024];
    int n = vsnprintf(buffer, sizeof(buffer), fmt, args);
    if (n < 0) return;
    std::string line = std::string(1, level) + " " + buffer;
    while (!line.empty() && (line.back() == '\n' || line.back() == '\r' || line.back() == ' ' || line.back() == '\t')) {
        line.pop_back();
    }
    if (line.size() <= 2) return;
    std::lock_guard<std::mutex> guard(lock);
    if (!lines.empty() && lines.back() == line) return;
    lines.push_back(std::move(line));
    while (lines.size() > CAPACITY) lines.pop_front();
}

std::vector<std::string> CoreLog::recent() {
    std::lock_guard<std::mutex> guard(lock);
    return {lines.begin(), lines.end()};
}

}

namespace {

/** Well-formed UTF-8 of 1 to 3 byte sequences (4-byte ones differ in modified UTF-8, so they count as malformed). */
bool isWellFormedUtf8(const std::string& s) {
    size_t i = 0;
    while (i < s.size()) {
        auto c = (unsigned char) s[i];
        size_t extra;
        if (c < 0x80) extra = 0;
        else if ((c & 0xE0) == 0xC0 && c >= 0xC2) extra = 1;
        else if ((c & 0xF0) == 0xE0) extra = 2;
        else return false;
        if (i + extra >= s.size()) return false;
        for (size_t k = 1; k <= extra; k++) if (((unsigned char) s[i + k] & 0xC0) != 0x80) return false;
        i += extra + 1;
    }
    return true;
}

}

extern "C" {

JNIEXPORT jobjectArray JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_getRecentCoreErrors(
    JNIEnv* env,
    jclass obj
) {
    std::vector<std::string> lines = libretrodroid::CoreLog::recent();
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize) lines.size(), stringClass, nullptr);
    for (size_t i = 0; i < lines.size(); i++) {
        // NewStringUTF wants (modified) UTF-8: a line that isn't well-formed has its non-ASCII bytes turned into '?'.
        std::string safe = lines[i];
        if (!isWellFormedUtf8(safe)) for (char& c : safe) if ((unsigned char) c >= 0x80) c = '?';
        jstring s = env->NewStringUTF(safe.c_str());
        env->SetObjectArrayElement(result, (jsize) i, s);
        env->DeleteLocalRef(s);
    }
    env->DeleteLocalRef(stringClass);
    return result;
}

}
