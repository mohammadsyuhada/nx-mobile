package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerSlotsTest {
    private val a = DeviceKey("a")
    private val b = DeviceKey("b")
    private val c = DeviceKey("c")

    @Test fun pads_claim_the_lowest_free_slot_in_press_order() {
        val s = PlayerSlots(players = 4)
        assertEquals(PlayerSlots.Claim(0, joined = true), s.claim(b))
        assertEquals(PlayerSlots.Claim(1, joined = true), s.claim(a))
        assertEquals(PlayerSlots.Claim(0, joined = false), s.claim(b))   // already in: same slot, no new join
        assertEquals(listOf(SlotView.Playing(b), SlotView.Playing(a), SlotView.Empty, SlotView.Empty), s.slots())
    }

    @Test fun no_slot_past_the_core_s_players() {
        val s = PlayerSlots(players = 2)
        s.claim(a); s.claim(b)
        assertNull(s.claim(c))
        assertNull(s.portOf(c))
    }

    @Test fun port_of_never_claims() {
        val s = PlayerSlots(players = 2)
        assertNull(s.portOf(a))
        assertEquals(listOf(SlotView.Empty, SlotView.Empty), s.slots())
        s.claim(a)
        assertEquals(0, s.portOf(a))
    }

    @Test fun a_disconnected_pad_keeps_its_slot_and_gets_it_back() {
        val s = PlayerSlots(players = 2)
        s.claim(a); s.claim(b)
        assertEquals(0, s.disconnected(a))
        assertEquals(SlotView.Held(a), s.slots()[0])
        assertNull(s.portOf(a))                                         // held, not live: its input is not routed
        assertEquals(PlayerSlots.Claim(0, joined = true), s.claim(a))   // back: same slot, announced again
        assertNull(s.disconnected(c))                                   // never in a slot
    }

    @Test fun a_held_slot_goes_to_a_new_pad_only_when_no_slot_is_free() {
        val s = PlayerSlots(players = 3)
        s.claim(a); s.claim(b)
        s.disconnected(a)
        assertEquals(2, s.claim(c)!!.port)                    // the free slot first
        val d = DeviceKey("d")
        assertEquals(0, s.claim(d)!!.port)                    // then the held one
        assertNull(s.claim(a))                                // a came back to a full table: no slot
    }

    @Test fun assign_moves_a_pad_and_swaps_with_the_slot_it_held() {
        val s = PlayerSlots(players = 2)
        s.claim(a); s.claim(b)
        assertEquals(setOf(0, 1), s.assign(1, a))
        assertEquals(listOf(SlotView.Playing(b), SlotView.Playing(a)), s.slots())
        assertEquals(setOf(0), s.assign(0, null))
        assertEquals(listOf(SlotView.Empty, SlotView.Playing(a)), s.slots())
        assertEquals(setOf(0), s.assign(0, c))                 // a pad in no slot moves into an empty one
        assertEquals(emptySet(), s.assign(0, c))               // already there
    }

    @Test fun assign_over_a_slot_held_by_another_pad_replaces_it() {
        val s = PlayerSlots(players = 2)
        s.claim(a); s.claim(b)
        s.disconnected(b)
        assertEquals(setOf(0, 1), s.assign(1, a))
        assertEquals(listOf(SlotView.Held(b), SlotView.Playing(a)), s.slots())   // the swap keeps b held in a's old slot
    }

    @Test fun out_of_play_frees_the_slot_and_blocks_claims_until_back_in_play() {
        val s = PlayerSlots(players = 2, outOfPlay = setOf(c))
        assertNull(s.claim(c))
        s.claim(a); s.claim(b)
        assertEquals(0, s.setOutOfPlay(a, true))
        assertEquals(setOf(a, c), s.outOfPlay)
        assertEquals(SlotView.Empty, s.slots()[0])
        assertNull(s.claim(a))
        assertNull(s.setOutOfPlay(a, false))                   // back in play: claims again on its next press
        assertEquals(0, s.claim(a)!!.port)
    }

    @Test fun a_pad_back_in_play_with_every_slot_taken_gets_none() {
        val s = PlayerSlots(players = 2, outOfPlay = setOf(c))
        s.claim(a); s.claim(b)
        s.setOutOfPlay(c, false)
        assertNull(s.claim(c))                                 // never steals a connected player's slot
    }

    @Test fun a_not_playing_pad_does_not_count_as_attached_in_a_multiplayer_game() {
        assertEquals(listOf(b), PadPresence.attached(listOf(a, b), outOfPlay = setOf(a)) { it })
    }

    @Test fun every_pad_not_playing_leaves_none_attached() {
        assertEquals(emptyList(), PadPresence.attached(listOf(a, b), outOfPlay = setOf(a, b)) { it })   // the touch pad shows
    }

    @Test fun every_pad_counts_in_a_multiplayer_game_with_none_benched() {
        val pads = listOf(a, b)
        assertEquals(pads, PadPresence.attached(pads, outOfPlay = emptySet()) { it })
    }

    @Test fun a_one_player_game_counts_every_pad() {
        val pads = listOf(a, b)
        assertEquals(pads, PadPresence.attached(pads, outOfPlay = null) { it })   // no slots: nothing is benched
    }

    @Test fun identical_names_are_numbered_from_the_second() {
        assertEquals(
            listOf("Xbox Wireless Controller", "8BitDo Pro 2", "Xbox Wireless Controller (2)", "Xbox Wireless Controller (3)"),
            PadNames.numbered(listOf("Xbox Wireless Controller", "8BitDo Pro 2", "Xbox Wireless Controller", "Xbox Wireless Controller")),
        )
    }
}
