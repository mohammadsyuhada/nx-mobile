package com.nxredux.mobile.netplay.usb

import kotlinx.coroutines.flow.MutableStateFlow

/** A [UsbCable] whose [state] tests set; [onStart] decides what start() shows first (Starting, not a handheld, by default). */
class FakeUsbCable(var availableNow: Boolean = true) : UsbCable {
    override val available: Boolean get() = availableNow
    override val state = MutableStateFlow<UsbLinkState>(UsbLinkState.Off)
    override val drops = MutableStateFlow(0)
    var starts = 0
    var stops = 0
    var onStart: (FakeUsbCable) -> Unit = { it.state.value = UsbLinkState.Starting(handheld = false) }
    override fun start() { starts++; onStart(this) }
    override fun stop() { stops++; state.value = UsbLinkState.Off }
}
