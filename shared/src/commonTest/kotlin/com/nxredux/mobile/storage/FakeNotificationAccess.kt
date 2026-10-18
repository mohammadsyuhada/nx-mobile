package com.nxredux.mobile.storage

class FakeNotificationAccess(var isGranted: Boolean = false) : NotificationAccess {
    var requests = 0
    override fun granted() = isGranted
    override fun request() { requests++ }
}
