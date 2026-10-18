/*
 *     Copyright (C) 2026  nx-mobile contributors
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

#ifndef LIBRETRODROID_CORERETENTION_H
#define LIBRETRODROID_CORERETENTION_H

// Keeps the next core's library loaded after its session ends (GLRetroViewData.keepCoreLoaded). The app sets it before
// every create(). A core such as gpsp maps memory near its own code once and reuses it, so its .so must not be unloaded
// and reloaded at another address between games.
namespace libretrodroid::coreretention {

void setKeepLoaded(bool keep);

// Extra dlopen flags for the core about to be opened: RTLD_NODELETE when it is to stay loaded, else 0.
int dlopenFlags();

}

#endif //LIBRETRODROID_CORERETENTION_H
