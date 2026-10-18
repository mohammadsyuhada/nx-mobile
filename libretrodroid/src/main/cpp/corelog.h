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

#ifndef LIBRETRODROID_CORELOG_H
#define LIBRETRODROID_CORELOG_H

#include <cstdarg>
#include <deque>
#include <mutex>
#include <string>
#include <vector>

namespace libretrodroid {

/**
 * The core's last few warning and error log lines, so a frontend can show why retro_load_game failed (flycast only
 * logs a missing BIOS file at WARN before its load error). Cleared when a core is created. Thread-safe.
 */
class CoreLog {
public:
    static constexpr size_t CAPACITY = 64;

    static void clear();
    /**
     * Formats [fmt] with [args] (consumed: pass a copy) and keeps it as "<level> <text>", newest last, dropping the
     * oldest past CAPACITY; a line identical to the previous one is dropped. [level] is 'E' for an error, 'W' for a warning.
     */
    static void append(char level, const char* fmt, va_list args);
    /** [append] with the arguments inline: the runner's own lines (core.cpp's "core load: <dlerror>"). */
    static void add(char level, const char* fmt, ...);
    /** The kept lines ("E text" / "W text"), oldest first, each trimmed of trailing whitespace. */
    static std::vector<std::string> recent();

private:
    static std::mutex lock;
    static std::deque<std::string> lines;
};

}

#endif
