package com.nxredux.mobile.netplay.usb

import java.util.concurrent.atomic.AtomicLong

/**
 * The cable's start count: every start bumps it, and a holder's token is current only while no start came after it was
 * read. A newer sheet start makes an older game screen's stop a no-op ([AndroidUsbCable.stopIf]).
 */
class UsbCableGeneration {
    private val n = AtomicLong()
    val current: Long get() = n.get()
    fun bump(): Long = n.incrementAndGet()
    fun isCurrent(token: Long): Boolean = token == n.get()
}
