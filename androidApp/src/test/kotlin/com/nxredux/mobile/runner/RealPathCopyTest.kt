package com.nxredux.mobile.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealPathCopyTest {
    private val stamp = realPathCopyStamp(1_000L, 42L)

    @Test fun reusesACopyWhoseStampMatches() = assertTrue(reuseRealPathCopy(true, 1_000L, 1_000L, stamp, stamp))

    @Test fun aSameSizeSourceWithANewerLastModifiedIsCopiedAgain() =
        assertFalse(reuseRealPathCopy(true, 1_000L, 1_000L, stamp, realPathCopyStamp(1_000L, 43L)))

    @Test fun aCopyWithoutAStampIsCopiedAgain() = assertFalse(reuseRealPathCopy(true, 1_000L, 1_000L, null, stamp))

    @Test fun aTruncatedCopyIsCopiedAgain() = assertFalse(reuseRealPathCopy(true, 999L, 1_000L, stamp, stamp))

    @Test fun aMissingCopyIsCopied() = assertFalse(reuseRealPathCopy(false, 0L, 1_000L, stamp, stamp))

    @Test fun anUnknownSizeNeverReuses() {
        val unknown = realPathCopyStamp(-1L, null)
        assertFalse(reuseRealPathCopy(true, -1L, -1L, unknown, unknown))
    }

    @Test fun withoutALastModifiedTheStampIsSizeOnly() {
        assertEquals(realPathCopyStamp(5L, null), realPathCopyStamp(5L, null))
        assertTrue(reuseRealPathCopy(true, 5L, 5L, realPathCopyStamp(5L, null) + "\n", realPathCopyStamp(5L, null)))
        assertFalse(reuseRealPathCopy(true, 5L, 5L, realPathCopyStamp(5L, null), realPathCopyStamp(5L, 7L)))
    }

    @Test fun aShortNameKeepsItsSiblingName() = assertEquals("Game (USA).iso.part", cacheSiblingName("Game (USA).iso", ".part"))

    @Test fun aLongNameGetsAShortStableSiblingName() {
        val long = "x".repeat(240) + ".iso"
        val name = cacheSiblingName(long, ".nxstamp")
        assertTrue(name.endsWith(".nxstamp"))
        assertTrue(name.encodeToByteArray().size + 14 <= 255)
        assertEquals(name, cacheSiblingName(long, ".nxstamp"))
    }
}
