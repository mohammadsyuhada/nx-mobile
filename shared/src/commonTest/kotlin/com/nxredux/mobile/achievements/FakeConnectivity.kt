package com.nxredux.mobile.achievements

class FakeConnectivity(var connected: Boolean = true) : RaConnectivity { override fun isConnected() = connected }
