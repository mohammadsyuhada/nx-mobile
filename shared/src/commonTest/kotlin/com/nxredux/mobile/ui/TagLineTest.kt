package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals

/** The hidden list's second line: the console tag, an extra copy's folder after it; Unassigned keeps its dir. */
class TagLineTest {
    private fun listed(tag: String, subtitle: String?, unassigned: Boolean = false) = Listed(
        SystemFolder(if (unassigned) "Unassigned" else tag, tag, null, emptyList(), unassigned = unassigned),
        GameEntry(ContentRef("r"), "g.chd", "g", folder = ContentRef("f"), sourceId = "e1", relativeDir = "", subtitle = subtitle),
    )

    @Test fun the_tag_alone_without_a_folder() = assertEquals("DC", tagLine(listed("DC", null)))

    @Test fun the_tag_then_the_folder() = assertEquals("DC · My Dreamcast", tagLine(listed("DC", "My Dreamcast")))

    @Test fun the_handhelds_tag_not_the_internal_one() = assertEquals("GPGX", tagLine(listed("GPGX_SMS", null)))

    @Test fun unassigned_shows_its_dir() = assertEquals("a/b", tagLine(listed("", "a/b", unassigned = true)))
}
