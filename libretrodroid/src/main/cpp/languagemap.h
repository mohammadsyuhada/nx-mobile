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

#ifndef LIBRETRODROID_LANGUAGEMAP_H
#define LIBRETRODROID_LANGUAGEMAP_H

#include <optional>
#include <string>

namespace libretrodroid {

// The RETRO_LANGUAGE_* for an Android locale: a BCP 47 tag (Locale.toLanguageTag(), "zh-Hant-TW", "pt-BR") or a bare
// language ("ja"). Chinese is Simplified unless the script is Hant or the region TW, HK or MO; Portuguese is Brazilian
// for BR. Empty for a language libretro doesn't list (the caller keeps English).
std::optional<unsigned> retroLanguageFor(const std::string& androidLocale);

}

#endif //LIBRETRODROID_LANGUAGEMAP_H
