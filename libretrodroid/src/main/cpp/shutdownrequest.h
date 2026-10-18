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

#ifndef LIBRETRODROID_SHUTDOWNREQUEST_H
#define LIBRETRODROID_SHUTDOWNREQUEST_H

#include <atomic>

namespace libretrodroid {

// A core's RETRO_ENVIRONMENT_SHUTDOWN (from any of its threads: Dolphin asks from its CPU thread), taken once by the
// host after a frame so the app can close the game the way its own Quit does.
class ShutdownRequest {
public:
    void request() { requested.store(true); }
    bool take() { return requested.exchange(false); }
    void reset() { requested.store(false); }

private:
    std::atomic<bool> requested { false };
};

}

#endif //LIBRETRODROID_SHUTDOWNREQUEST_H
