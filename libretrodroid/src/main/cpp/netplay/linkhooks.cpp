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
 *
 *     The core-facing half of NX Redux workspace/all/netplay/gbalink.c @ 2f25643b (GBALink_notifyConnected,
 *     GBALink_notifyDisconnected, gbalink_netpacket_send, gbalink_netpacket_poll_receive, GBALink_pollAndDeliverPackets).
 */

#include "linkhooks.h"

#include "../log.h"

extern "C" {
#include "np_link.h"
}

namespace libretrodroid::gbalink {

static nl_netpacket_callback callbacks {};
static bool registered = false;
// GL thread only: the core had start() and connected(), and not yet disconnected() and stop().
static bool coreStarted = false;
static uint16_t remoteId = 0;

bool setNetpacket(const nl_netpacket_callback* cb) {
    callbacks = cb != nullptr ? *cb : nl_netpacket_callback {};
    registered = callbacks.start != nullptr && callbacks.receive != nullptr;
    LOGI("GBA link: netpacket interface %s", registered ? "registered" : "cleared");
    return true;
}

void forgetCore() {
    callbacks = nl_netpacket_callback {};
    registered = false;
    coreStarted = false;
}

static void deliver(const uint8_t* buf, size_t len, void*) {
    if (coreStarted && callbacks.receive != nullptr) callbacks.receive(buf, len, remoteId);
}

// The core's send: one SIO_DATA, flushed at once (TCP_NODELAY; the handheld ignores the flags too).
static void coreSend(int flags, const void* buf, size_t len, uint16_t clientId) {
    (void) flags;
    nl_send(buf, len, clientId);
}

// The core's poll from inside retro_run (gpSP and mGBA while they wait on the real cable): real-cable messages only.
static void corePollReceive() {
    if (coreStarted) nl_take_lockstep(deliver, nullptr);
}

bool active() { return coreStarted || nl_active(); }

void stopCore() {
    if (!coreStarted) return;
    coreStarted = false;   // first: nothing reaches the core from here on
    if (callbacks.disconnected != nullptr) callbacks.disconnected(remoteId);
    if (callbacks.stop != nullptr) callbacks.stop();
}

bool preFrame() {
    nl_state state = nl_get_state(nullptr, nullptr);
    bool linked = state == NL_LINKED || state == NL_PEER_PAUSED;
    if (!coreStarted && linked && registered) {
        int id = nl_client_id();   // 0 host, 1 client, as the handheld numbers them
        if (id >= 0) {
            remoteId = id == 0 ? 1 : 0;
            coreStarted = true;
            callbacks.start((uint16_t) id, coreSend, corePollReceive);
            if (callbacks.connected != nullptr) callbacks.connected(remoteId);
        }
    } else if (coreStarted && !linked) {
        stopCore();   // the session ended (or was shut down): the game plays on alone
    }
    return nl_frame(deliver, nullptr) != 0;
}

void shutdown() { nl_shutdown(); }

}
