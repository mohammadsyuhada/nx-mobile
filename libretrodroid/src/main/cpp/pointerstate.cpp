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

#include "pointerstate.h"

#include <algorithm>

namespace libretrodroid {

PointerState carryOver(const PointerState& old) {
    PointerState fresh;
    if (old.sticky) {
        fresh.x = old.x;
        fresh.y = old.y;
        fresh.sticky = true;   // an untouched one (marked sticky before any touch) stays released
    }
    return fresh;
}

void PointerState::setSticky(bool on, bool centreIfUntouched) {
    sticky = on;
    if (on && centreIfUntouched && !pressed()) { x = 0.5f; y = 0.5f; }
}

void PointerState::touch(float nx, float ny) {
    if (nx < 0.f || ny < 0.f) {
        if (!sticky) { x = -1.f; y = -1.f; }
        return;
    }
    x = nx;
    y = ny;
}

void PointerState::advance(float seconds) {
    if (vx == 0.f && vy == 0.f) return;
    if (!pressed()) { x = 0.5f; y = 0.5f; }
    x = std::clamp(x + vx * seconds, 0.f, 1.f);
    y = std::clamp(y + vy * seconds, 0.f, 1.f);
}

}
