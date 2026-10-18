package com.nxredux.mobile.achievements

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Connected = the active network is validated. When it reaches the internet but validation is not reported (the
 * emulator's network often is not), INTERNET alone counts; a real failure then shows up as an RaHttpException.
 */
class AndroidConnectivity(context: Context) : RaConnectivity {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isConnected(): Boolean {
        val cm = cm ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
