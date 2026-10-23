package com.nxredux.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationIdsTest {
    // Two foreground services on one id overwrite (and, stopping, remove) each other's notification.
    @Test fun everyServiceHasItsOwnId() = assertEquals(NotificationIds.all.size, NotificationIds.all.toSet().size)
}
