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

#include "languagemap.h"

#include <algorithm>
#include <cctype>
#include <unordered_map>
#include <vector>

#include "libretro.h"

namespace libretrodroid {

namespace {

std::vector<std::string> subtags(const std::string& tag) {
    std::vector<std::string> result;
    std::string current;
    for (char c : tag) {
        if (c == '-' || c == '_') {
            result.push_back(current);
            current.clear();
        } else {
            current.push_back((char) std::tolower((unsigned char) c));
        }
    }
    result.push_back(current);
    return result;
}

}

std::optional<unsigned> retroLanguageFor(const std::string& androidLocale) {
    static const std::unordered_map<std::string, unsigned> languages {
        { "en", RETRO_LANGUAGE_ENGLISH },
        { "ja", RETRO_LANGUAGE_JAPANESE },
        { "fr", RETRO_LANGUAGE_FRENCH },
        { "es", RETRO_LANGUAGE_SPANISH },
        { "de", RETRO_LANGUAGE_GERMAN },
        { "it", RETRO_LANGUAGE_ITALIAN },
        { "nl", RETRO_LANGUAGE_DUTCH },
        { "pt", RETRO_LANGUAGE_PORTUGUESE_PORTUGAL },
        { "ru", RETRO_LANGUAGE_RUSSIAN },
        { "ko", RETRO_LANGUAGE_KOREAN },
        { "zh", RETRO_LANGUAGE_CHINESE_SIMPLIFIED },
        { "eo", RETRO_LANGUAGE_ESPERANTO },
        { "pl", RETRO_LANGUAGE_POLISH },
        { "vi", RETRO_LANGUAGE_VIETNAMESE },
        { "ar", RETRO_LANGUAGE_ARABIC },
        { "el", RETRO_LANGUAGE_GREEK },
        { "tr", RETRO_LANGUAGE_TURKISH },
    };

    auto parts = subtags(androidLocale);
    const std::string& language = parts.front();
    auto found = languages.find(language);
    if (found == languages.end()) return std::nullopt;

    auto has = [&](const char* subtag) { return std::find(parts.begin() + 1, parts.end(), subtag) != parts.end(); };
    if (language == "zh" && (has("hant") || (!has("hans") && (has("tw") || has("hk") || has("mo"))))) {
        return RETRO_LANGUAGE_CHINESE_TRADITIONAL;
    }
    if (language == "pt" && has("br")) return RETRO_LANGUAGE_PORTUGUESE_BRAZIL;
    return found->second;
}

}
