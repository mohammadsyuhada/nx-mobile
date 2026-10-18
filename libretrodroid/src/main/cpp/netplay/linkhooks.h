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

#ifndef LIBRETRODROID_LINKHOOKS_H
#define LIBRETRODROID_LINKHOOKS_H

#include "nl_netpacket.h"

// The run loop's side of the GBA link (np_link.h): the core's netpacket interface (env 78), the core's start/stop around
// a session, and packet delivery. setNetpacket, forgetCore, active, preFrame and stopCore run on the frame (GL) thread
// (active reads the GL-only core state); shutdown from any thread.
namespace libretrodroid::gbalink {
    bool setNetpacket(const nl_netpacket_callback* callbacks);   // SET_NETPACKET_INTERFACE; NULL clears it
    void forgetCore();          // before a new core is created: it registers again (retro_init or retro_load_game)
    bool active();              // a link session runs, or the core has yet to hear that it ended
    bool preFrame();            // true: run this frame; false: hold it (this device or the peer is paused)
    void stopCore();            // the core's disconnected() and stop() if its session is open; before retro_unload_game
    void shutdown();            // ends the session (DISCONNECT) and joins its thread; can block ~0.5 s, never from step()
}

#endif //LIBRETRODROID_LINKHOOKS_H
