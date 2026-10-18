package com.nxredux.mobile.storage

/** [AllFilesAccess] for tests: [isGranted] flips by hand; [opened] counts settings-page opens. */
class FakeAllFilesAccess(override val declared: Boolean = true, var isGranted: Boolean = false) : AllFilesAccess {
    var opened = 0
    override fun granted() = isGranted
    override fun openSettings() { opened++ }
}
