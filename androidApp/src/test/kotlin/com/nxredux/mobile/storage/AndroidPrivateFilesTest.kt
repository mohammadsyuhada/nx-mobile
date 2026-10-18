package com.nxredux.mobile.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class AndroidPrivateFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun aMissingFileReadsAsNull() = runBlocking {
        assertNull(AndroidPrivateFiles(tmp.root).read("job.json"))
    }

    @Test fun aWrittenFileReadsBack() = runBlocking {
        val files = AndroidPrivateFiles(tmp.root)
        files.write("job.json", "{}")
        assertEquals("{}", files.read("job.json"))
    }

    @Test fun appendCreatesTheFileThenAddsToItsEnd() = runBlocking {
        val files = AndroidPrivateFiles(java.io.File(tmp.root, "nested"))   // the folder is made on the first append
        files.append("art-log.txt", "a\n")
        files.append("art-log.txt", "b\n")
        assertEquals("a\nb\n", files.read("art-log.txt"))
    }

    @Test fun appendKeepsWhatWasWrittenWhole() = runBlocking {
        val files = AndroidPrivateFiles(tmp.root)
        files.write("art-log.txt", "a\n")
        files.append("art-log.txt", "b\n")
        assertEquals("a\nb\n", files.read("art-log.txt"))
        assertEquals(listOf("art-log.txt"), tmp.root.list()!!.toList())   // in place: no temp file left beside it
    }

    @Test fun anAppendThatCannotWriteThrows() = runBlocking {
        tmp.newFolder("art-log.txt")
        try {
            AndroidPrivateFiles(tmp.root).append("art-log.txt", "a\n")
            fail("a failed append must throw")
        } catch (_: IOException) {}
    }

    @Test fun aFileThatIsThereButUnreadableThrows() = runBlocking {
        tmp.newFolder("art-log.txt")   // a directory where the file should be: present, not readable as text
        try {
            AndroidPrivateFiles(tmp.root).read("art-log.txt")
            fail("an unreadable file must not read as missing")
        } catch (_: IOException) {}
    }
}
