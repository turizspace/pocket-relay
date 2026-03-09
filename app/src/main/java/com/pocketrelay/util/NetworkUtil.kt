package com.pocketrelay.util

import java.net.NetworkInterface

object NetworkUtil {
    fun ip(): String =
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress.indexOf(':') < 0 }
            ?.hostAddress ?: "127.0.0.1"
}