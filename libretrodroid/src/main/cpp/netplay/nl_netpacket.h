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

/*
 * The libretro netpacket interface as the current libretro.h defines it (the copy in NX Redux minarch, and gpSP
 * 2db57b11's own). The libretro.h bundled with this runner predates it (env 76, a send with a broadcast flag, no
 * poll_receive): never use those definitions for it. Android arm64/x86_64 have no RETRO_CALLCONV.
 */

#ifndef LIBRETRODROID_NL_NETPACKET_H
#define LIBRETRODROID_NL_NETPACKET_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define NL_ENVIRONMENT_SET_NETPACKET_INTERFACE 78
#define NL_NETPACKET_BROADCAST 0xFFFF

typedef void (*nl_netpacket_send_t)(int flags, const void* buf, size_t len, uint16_t client_id);
typedef void (*nl_netpacket_poll_receive_t)(void);
typedef void (*nl_netpacket_start_t)(uint16_t client_id, nl_netpacket_send_t send_fn, nl_netpacket_poll_receive_t poll_receive_fn);
typedef void (*nl_netpacket_receive_t)(const void* buf, size_t len, uint16_t client_id);
typedef void (*nl_netpacket_stop_t)(void);
typedef void (*nl_netpacket_poll_t)(void);
typedef bool (*nl_netpacket_connected_t)(uint16_t client_id);
typedef void (*nl_netpacket_disconnected_t)(uint16_t client_id);

struct nl_netpacket_callback {
	nl_netpacket_start_t start;
	nl_netpacket_receive_t receive;
	nl_netpacket_stop_t stop;				  /* optional */
	nl_netpacket_poll_t poll;				  /* optional, unused (as on the handheld) */
	nl_netpacket_connected_t connected;		  /* optional */
	nl_netpacket_disconnected_t disconnected; /* optional */
	const char* protocol_version;			  /* optional, never compared (as on the handheld) */
};

#endif /* LIBRETRODROID_NL_NETPACKET_H */
