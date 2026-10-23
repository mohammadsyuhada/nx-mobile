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
 * The TCP plumbing the lockstep engine (np_engine.c) and the GBA link (np_link.c) share: the self-pipe that wakes their
 * network thread, the growable send queue, the host's peer-filtered accept, the client's connect retry, the flush on
 * the way out and the start/shutdown fd handling. Protocol, session state and locking stay with each caller: nothing
 * here takes a lock, and the loops that must look at the caller's session ask through a callback that takes the
 * caller's own mutex.
 *
 * Where the two callers differ, the difference is a parameter (the link caps its queue, the engine lingers for the
 * peer's FIN, the log prefix), never a merged behaviour.
 */

#ifndef NP_TCP_H
#define NP_TCP_H

#include <netinet/in.h>
#include <pthread.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ---- Wake pipe --------------------------------------------------------------------------------------------------
 * The frame path never touches the socket: it queues and pokes the network thread, which polls rd beside its socket.
 * Both ends are non-blocking, so a poke never blocks (a full pipe already means "awake") and a drain stops when empty.
 */
typedef struct {
	int rd;
	int wr;
} np_wake;

/* rd = wr = -1. */
void np_wake_init(np_wake* w);
/* pipe(), both ends non-blocking. 0 ok; -1 with w left at -1. */
int np_wake_open(np_wake* w);
/* One byte down the pipe; nothing when it is not open. */
void np_wake_poke(const np_wake* w);
/* Reads rd until empty. */
void np_wake_drain(int rd);
/* Closes whichever end is open, back to -1. */
void np_wake_close(np_wake* w);

/* ---- Send queue -------------------------------------------------------------------------------------------------
 * Bytes [off, len) are still to go. It grows by doubling from 4 KiB, compacting first when the front has been sent.
 */
typedef struct {
	uint8_t* buf;
	size_t len;
	size_t off;
	size_t cap;
} np_outq;

/* Appends n bytes. max caps the bytes still pending after the append (0: no cap). false when capped or out of memory
 * (nothing appended). */
bool np_outq_push(np_outq* q, const void* data, size_t n, size_t max);

static inline bool np_outq_pending(const np_outq* q) {
	return q->off < q->len;
}

/* Forgets what is queued; the buffer stays. */
static inline void np_outq_clear(np_outq* q) {
	q->off = q->len = 0;
}

/* Frees the buffer, back to empty. */
void np_outq_free(np_outq* q);

typedef enum {
	NP_OUTQ_DRAINED = 0, /* every queued byte is written */
	NP_OUTQ_FULL,		 /* the socket buffer is full; the rest waits for POLLOUT */
	NP_OUTQ_ERROR		 /* the send failed; errno is set, the queue is left as it was */
} np_outq_result;

/* Writes as much of the queue as fd takes now (never blocks). *wrote is set when any byte went out. */
np_outq_result np_outq_flush(np_outq* q, int fd, bool* wrote);

/* ---- Thread loops -----------------------------------------------------------------------------------------------
 * quit() takes the caller's mutex itself and tells whether the network thread should stop (stop flag or session
 * over). The loops call it between waits, never with anything held.
 */
typedef bool (*np_tcp_quit_fn)(void);

/* Host: accepts only a connection from `peer` (the address the control channel agreed on). A connection from any other
 * address is logged (after log_prefix), closed at once, and the host keeps listening. The accepted fd is configured
 * (np_net_configure_tcp) and non-blocking. Returns it, or -1 on quit or a poll error. */
int np_tcp_accept_from(int listen_fd, int wake_fd, struct in_addr peer, np_tcp_quit_fn quit, const char* log_prefix);

#define NP_TCP_CONNECT_QUIT (-1)
#define NP_TCP_CONNECT_GAVE_UP (-2)

/* Client: up to `attempts` connects of timeout_ms each, retry_ms apart; a poke cuts the current wait short and the
 * next attempt starts. Returns a connected, non-blocking fd; NP_TCP_CONNECT_QUIT when quit() said so;
 * NP_TCP_CONNECT_GAVE_UP when every attempt failed (the caller logs and ends its session). */
int np_tcp_connect_retry(const char* ip, uint16_t port, int wake_fd, int attempts, int timeout_ms, int retry_ms,
						 np_tcp_quit_fn quit);

/* flush() takes the caller's mutex, writes what the socket takes and tells whether bytes are still queued. */
typedef bool (*np_tcp_flush_fn)(int fd);

/* On the way out: keep flushing (POLLOUT waits of poll_ms) for up to flush_ms of real time or until the queue is
 * empty, then half-close. With linger_ms > 0, then wait that long for the peer's FIN, reading into scratch and
 * dropping what arrives. The caller closes fd. */
void np_tcp_final_flush(int fd, np_tcp_flush_fn flush, int flush_ms, int poll_ms, int linger_ms, uint8_t* scratch,
						size_t scratch_len);

/* ---- Start / shutdown -------------------------------------------------------------------------------------------- */

/* Dotted IPv4 that fits in cap bytes (with its NUL) into *addr. */
bool np_tcp_parse_peer(const char* ip, size_t cap, struct in_addr* addr);

#define NP_TCP_START_LISTEN (-1) /* the host's listener could not be opened; nothing else was touched */
#define NP_TCP_START_SETUP (-2)	 /* the wake pipe or the thread failed; the listener and pipe are closed again */

/* The host listens on port (*listen_fd); either role opens the wake pipe and starts fn on *thread. The caller holds
 * its mutex, which fn takes first, so fn sees the fields the caller sets after a 0 return. 0 or NP_TCP_START_*. */
int np_tcp_start(bool host, uint16_t port, int* listen_fd, np_wake* wake, pthread_t* thread, void* (*fn)(void*));

/* After the thread is joined: closes the socket, listener and wake pipe that are still open and frees the queue. */
void np_tcp_release(int* sock, int* listen_fd, np_wake* wake, np_outq* q);

#ifdef __cplusplus
}
#endif

#endif /* NP_TCP_H */
